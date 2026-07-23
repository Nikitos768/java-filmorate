package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/films")
public class FilmController {
    private final Map<Long, Film> films = new HashMap<>();
    private static final LocalDate FILM_RELEASE = LocalDate.of(1895, 12, 28);
    private long currentId = 0;
    private static final Logger log = LoggerFactory.getLogger(FilmController.class);
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @PostMapping
    public Film addFilm(@Valid @RequestBody Film film) {
        log.info("Получен запрос на добавление фильма: {}", film.getName());
        validate(film);
        film.setId(getNewId());
        films.put(film.getId(), film);
        log.info("Фильм успешно добавлен с ID: {}", film.getId());
        return film;
    }

    @PutMapping
    public Film updateFilm(@Valid @RequestBody Film film) {
        log.info("Получен запрос на обновление фильма с ID: {}", film.getId());

        if (film.getId() == 0 || !films.containsKey(film.getId())) {
            log.warn("Фильм с таким с ID {} не найден.", film.getId());
            throw new ConditionsNotMetException("Фильм с таким ID не найден.");
        }

        validate(film);
        films.put(film.getId(), film);
        log.info("Фильм с ID {} успешно обновлен.", film.getId());
        return film;
    }


    @GetMapping
    public Collection<Film> getAllFilm() {
        log.info("Получен запрос на получение всех фильмов. Всего фильмов: {}", films.size());
        return films.values();
    }

    public void validate(Film film) {
        if (film == null) {
            throw new ConditionsNotMetException("Фильм не может быть null.");
        }

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        if (!violations.isEmpty()) {
            log.warn("Валидация фильма завершилась с ошибкой: {}", violations.iterator().next().getMessage());
            throw new ConditionsNotMetException(violations.iterator().next().getMessage());
        }

        if (film.getReleaseDate() == null || film.getReleaseDate().isBefore(FILM_RELEASE)) {
            throw new ConditionsNotMetException("Дата релиза не должна быть раньше 28 декабря 1895 года.");
        }
    }

    private Long getNewId() {
        return ++currentId;
    }
}