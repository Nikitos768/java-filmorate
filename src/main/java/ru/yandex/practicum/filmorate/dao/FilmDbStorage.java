package ru.yandex.practicum.filmorate.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.FilmStorage;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Component("filmDbStorage")
@RequiredArgsConstructor
@Slf4j
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Film addFilm(Film film) {
        String sqlQuery = "INSERT INTO films (name, description, release_date, duration, mpa_id) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(sqlQuery, new String[]{"film_id"});
            stmt.setString(1, film.getName());
            stmt.setString(2, film.getDescription());
            stmt.setDate(3, Date.valueOf(film.getReleaseDate()));
            stmt.setInt(4, film.getDuration());
            // Проверяем на null, чтобы избежать NullPointerException
            stmt.setInt(5, film.getMpa() != null ? film.getMpa().getId() : null);
            return stmt;
        }, keyHolder);

        film.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());

        saveGenres(film);

        log.info("Фильм успешно добавлен с ID: {}", film.getId());
        return film;
    }

    @Override
    public Film updateFilm(Film film) {
        String sqlQuery = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ? WHERE film_id = ?";
        int rowsAffected = jdbcTemplate.update(sqlQuery,
                film.getName(),
                film.getDescription(),
                Date.valueOf(film.getReleaseDate()),
                film.getDuration(),
                film.getMpa() != null ? film.getMpa().getId() : null,
                film.getId());

        if (rowsAffected == 0) {
            log.error("Фильм с ID {} не найден для обновления", film.getId());
            throw new IllegalArgumentException("Фильм не найден"); // Сюда можно подставить ваше кастомное исключение NotFoundException
        }

        saveGenres(film);

        log.info("Фильм с ID {} успешно обновлен", film.getId());
        return findById(film.getId())
                .orElseThrow(() -> new NotFoundException("Ошибка при получении обновленного фильма с ID " + film.getId()));
    }

    @Override
    public Optional<Film> deletedFilm(Film film) {
        Optional<Film> filmToReturn = findById(film.getId());

        if (filmToReturn.isEmpty()) {
            log.error("Фильм с ID {} не найден для удаления", film.getId());
            return Optional.empty();
        }

        String sqlQuery = "DELETE FROM films WHERE film_id = ?";
        jdbcTemplate.update(sqlQuery, film.getId());
        log.info("Фильм с ID {} успешно удален", film.getId());

        return filmToReturn;
    }

    @Override
    public Collection<Film> getAllFilm() {
        String sqlQuery = "SELECT f.*, m.name AS mpa_name FROM films f LEFT JOIN mpa m ON f.mpa_id = m.mpa_id";
        Collection<Film> films = jdbcTemplate.query(sqlQuery, this::mapRowToFilm);

        loadGenresForFilms(films);

        return films;
    }

    @Override
    public Optional<Film> findById(Long id) {
        String sqlQuery = "SELECT f.*, m.name AS mpa_name FROM films f LEFT JOIN mpa m ON f.mpa_id = m.mpa_id WHERE f.film_id = ?";
        Optional<Film> filmOpt = jdbcTemplate.query(sqlQuery, this::mapRowToFilm, id).stream().findFirst();

        filmOpt.ifPresent(this::loadGenresForSingleFilm);

        return filmOpt;
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        String sqlQuery = "MERGE INTO likes KEY(film_id, user_id) VALUES (?, ?)";
        jdbcTemplate.update(sqlQuery, filmId, userId);
        log.info("Пользователь с ID {} поставил лайк фильму с ID {}", userId, filmId);
    }

    @Override
    public void deleteLike(Long filmId, Long userId) {
        String sqlQuery = "DELETE FROM likes WHERE film_id = ? AND user_id = ?";
        int rowsAffected = jdbcTemplate.update(sqlQuery, filmId, userId);

        if (rowsAffected == 0) {
            log.warn("Лайк пользователя с ID {} фильму с ID {} не найден для удаления", userId, filmId);
        } else {
            log.info("Пользователь с ID {} удалил лайк у фильма с ID {}", userId, filmId);
        }
    }

    @Override
    public List<Film> getTopFilms(int count) {
        String sqlQuery = "SELECT f.*, m.name AS mpa_name, COUNT(l.user_id) AS likes_count " +
                "FROM films f " +
                "LEFT JOIN mpa m ON f.mpa_id = m.mpa_id " +
                "LEFT JOIN likes l ON f.film_id = l.film_id " +
                "GROUP BY f.film_id, m.name " +
                "ORDER BY likes_count DESC " +
                "LIMIT ?";

        List<Film> films = jdbcTemplate.query(sqlQuery, this::mapRowToFilm, count);

        loadGenresForFilms(films);

        return films;
    }

    private void saveGenres(Film film) {
        String deleteSql = "DELETE FROM film_genres WHERE film_id = ?";
        jdbcTemplate.update(deleteSql, film.getId());

        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            return;
        }

        List<Genre> genres = film.getGenres().stream()
                .distinct()
                .toList();
        String insertSql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";

        jdbcTemplate.batchUpdate(insertSql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setLong(1, film.getId());
                ps.setInt(2, genres.get(i).getId());
            }

            @Override
            public int getBatchSize() {
                return genres.size();
            }
        });
    }

    private void loadGenresForSingleFilm(Film film) {
        String sql = "SELECT g.* FROM genres g JOIN film_genres fg ON g.genre_id = fg.genre_id WHERE fg.film_id = ?";
        List<Genre> genres = jdbcTemplate.query(sql, (rs, rowNum) ->
                new Genre(rs.getInt("genre_id"), rs.getString("name")), film.getId());
        film.setGenres(new LinkedHashSet<>(genres));
    }

    private void loadGenresForFilms(Collection<Film> films) {
        if (films.isEmpty()) return;

        String ids = films.stream()
                .map(film -> String.valueOf(film.getId()))
                .collect(Collectors.joining(","));

        String sql = "SELECT fg.film_id, g.genre_id, g.name FROM genres g " +
                "JOIN film_genres fg ON g.genre_id = fg.genre_id " +
                "WHERE fg.film_id IN (" + ids + ")";

        // Мапим результаты: ключ — ID фильма, значение — список его жанров
        Map<Long, List<Genre>> genresMap = new HashMap<>();
        jdbcTemplate.query(sql, (rs) -> {
            long filmId = rs.getLong("film_id");
            Genre genre = new Genre(rs.getInt("genre_id"), rs.getString("name"));
            genresMap.computeIfAbsent(filmId, k -> new ArrayList<>()).add(genre);
        });

        // Раскладываем жанры по соответствующим фильмам
        for (Film film : films) {
            List<Genre> genres = genresMap.getOrDefault(film.getId(), Collections.emptyList());
            film.setGenres(new LinkedHashSet<>(genres));
        }
    }

    private Film mapRowToFilm(@org.jetbrains.annotations.NotNull ResultSet rs, int rowNum) throws SQLException {
        Mpa mpa = null;
        int mpaId = rs.getInt("mpa_id");
        if (!rs.wasNull()) {
            mpa = new Mpa(mpaId, rs.getString("mpa_name"));
        }

        return Film.builder()
                .id(rs.getLong("film_id"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .releaseDate(rs.getDate("release_date").toLocalDate())
                .duration(rs.getInt("duration"))
                .mpa(mpa)
                .genres(new LinkedHashSet<>())
                .build();
    }
}