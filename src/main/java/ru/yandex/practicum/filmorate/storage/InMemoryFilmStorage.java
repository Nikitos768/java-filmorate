package ru.yandex.practicum.filmorate.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.*;

@Component
public class InMemoryFilmStorage implements FilmStorage {
    private final Map<Long, Film> films = new HashMap<>();
    private long currentId = 0;
    private static final Logger log = LoggerFactory.getLogger(InMemoryFilmStorage.class);

    @Override
    public Film addFilm(Film film) {
        log.info("Получен запрос на добавление фильма: {}", film.getName());
        film.setId(getNewId());
        films.put(film.getId(), film);
        log.info("Фильм успешно добавлен с ID: {}", film.getId());
        return film;
    }

    @Override
    public Film updateFilm(Film film) {
        log.info("Получен запрос на обновление фильма с ID: {}", film.getId());

        if (film.getId() == 0 || !films.containsKey(film.getId())) {
            log.warn("Фильм с таким с ID {} не найден.", film.getId());
            throw new NotFoundException("Фильм с таким ID не найден.");
        }

        Film oldFilm = films.get(film.getId());

        if (oldFilm.getLikes() != null) {
            film.setLikes(oldFilm.getLikes());
        }

        films.put(film.getId(), film);
        log.info("Фильм с ID {} успешно обновлен.", film.getId());
        return film;
    }

    @Override
    public Collection<Film> getAllFilm() {
        log.info("Получен запрос на получение всех фильмов. Всего фильмов: {}", films.size());
        return films.values();
    }

    @Override
    public Optional<Film> deletedFilm(Film film) {
        log.info("Удаление фильма из памяти с ID: {}", film.getId());
        if (film.getId() == 0 || !films.containsKey(film.getId())) {
            log.warn("Фильм с таким с ID {} не существует.", film.getId());
            throw new NotFoundException("Фильм с таким ID не найден.");
        }

        Film remove = films.remove(film.getId());
        return Optional.ofNullable(remove);
    }

    @Override
    public Optional<Film> findById(Long id) {
        log.info("Запрос фильма из памяти по ID: {}", id);
        return Optional.ofNullable(films.get(id));
    }

    private Long getNewId() {
        return ++currentId;
    }
}
