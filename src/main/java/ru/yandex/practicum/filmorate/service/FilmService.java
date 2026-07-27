package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;


import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FilmService {
    private  final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private static final LocalDate FILM_RELEASE = LocalDate.of(1895, 12, 28);

    public Film getFilm(Long id) {
        return filmStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с ID " + id + " не найден."));
    }

    public Film addFilm(Film film) {
        validate(film);
        return filmStorage.addFilm(film);
    }

    public Film updateFilm(Film film) {
        validate(film);
        return filmStorage.updateFilm(film);
    }

    public Collection<Film> getAllFilm() {
        return filmStorage.getAllFilm();
    }

    public Optional<Film> deletedFilm(Film film) {
        return filmStorage.deletedFilm(film);
    }

    private void validate(Film film) {
        if (film == null) {
            throw new ValidationException("Фильм не может быть null.");
        }
        if (film.getReleaseDate() == null || film.getReleaseDate().isBefore(FILM_RELEASE)) {
            throw new ValidationException("Дата релиза не должна быть раньше 28 декабря 1895 года.");
        }
    }

    public void addLikes (Long filmId, Long userId) {
        Film film = filmStorage.findById(filmId)
                .orElseThrow(() -> new NotFoundException("Фильм с ID " + filmId + " не найден"));

        userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + userId + " не найден"));

        if (film.getLikes().contains(userId)) {
            throw  new ValidationException("Пользователь с ID: "+ userId +" уже поставил лайк");
        }

        film.getLikes().add(userId);
        filmStorage.updateFilm(film);
    }

    public void deletedLikes (Long filmId, Long userId) {
        Film film = filmStorage.findById(filmId)
                .orElseThrow(() -> new NotFoundException("Фильм с ID " + filmId + " не найден"));

        userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + userId + " не найден"));

        if (film.getLikes().contains(userId)) {
            film.getLikes().remove(userId);
            filmStorage.updateFilm(film);
        }
    }

    public List<Film> getTopFilms(int count) {
        return filmStorage.getAllFilm()
                .stream()
                .sorted((film1, film2) -> Integer.compare(film2.getLikes().size(), film1.getLikes().size()))
                .limit(count)
                .toList();
    }
}

