package ru.yandex.practicum.filmorate;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.storage.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.InMemoryUserStorage;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FilmControllerTest {

    private FilmController filmController;
    private FilmService filmService;
    private Validator validator;

    @BeforeEach
    void setUp() {
        InMemoryFilmStorage filmStorage = new InMemoryFilmStorage();
        InMemoryUserStorage userStorage = new InMemoryUserStorage();
        filmService = new FilmService(filmStorage, userStorage);
        filmController = new FilmController(filmService);

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private boolean hasValidationErrors(Film film) {
        return !validator.validate(film).isEmpty();
    }

    @Test
    void shouldDetectEmptyFilmName() {
        Film film = Film.builder()
                .description("Описание")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(90)
                .name("")
                .build();

        assertTrue(hasValidationErrors(film), "Аннотация @NotBlank должна отсечь пустое название");
        film.setName(null);
        assertTrue(hasValidationErrors(film), "Аннотация @NotBlank должна отсечь название равное null");
        film.setName(" ");
        assertTrue(hasValidationErrors(film), "Аннотация @NotBlank должна отсечь название только из пробелов");
    }

    @Test
    void shouldDetectTooLongDescription() {
        Film film = Film.builder()
                .name("Фильм")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(90)
                .description("a".repeat(200))
                .build();

        assertFalse(hasValidationErrors(film), "Описание в 200 символов разрешено");
        film.setDescription("a".repeat(201));
        assertTrue(hasValidationErrors(film), "Аннотация @Size должна отсечь описание > 200 символов");
    }

    @Test
    void shouldDetectInvalidDuration() {
        Film film = Film.builder()
                .name("Фильм")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(0)
                .build();

        assertTrue(hasValidationErrors(film), "Аннотация @Positive должна запретить длительность 0");
        film.setDuration(1);
        assertFalse(hasValidationErrors(film), "Аннотация @Positive должна разрешить длительность 1");
        film.setDuration(-10);
        assertTrue(hasValidationErrors(film), "Аннотация @Positive должна запретить отрицательную длительность");
    }

    @Test
    void shouldValidateReleaseDateBoundaries() {
        Film film = Film.builder()
                .name("Фильм")
                .description("Описание")
                .duration(90)
                .releaseDate(LocalDate.of(1895, 12, 28))
                .build();

        assertDoesNotThrow(() -> filmController.addFilm(film), "Дата 28.12.1895 должна быть разрешена");

        film.setReleaseDate(LocalDate.of(1895, 12, 27));
        assertThrows(ValidationException.class, () -> filmController.addFilm(film),
                "Дата 27.12.1895 должна вызывать исключение");
    }

    @Test
    void shouldThrowExceptionWhenFilmIsNull() {
        assertThrows(ValidationException.class, () -> filmController.addFilm(null),
                "Передача null в контроллер должна вызывать исключение");
    }

    @Test
    void shouldDetectErrorsInEmptyObject() {
        Film film = new Film();
        assertTrue(hasValidationErrors(film), "Пустой объект не должен проходить валидацию");
    }

    @Test
    void shouldSuccessfullyValidateCorrectFilm() {
        Film film = Film.builder()
                .name("Интерстеллар")
                .description("Отличный фильм")
                .releaseDate(LocalDate.of(2014, 11, 6))
                .duration(169)
                .build();

        assertFalse(hasValidationErrors(film), "Корректный фильм не должен иметь ошибок");
        assertDoesNotThrow(() -> filmController.addFilm(film), "Метод addFilm не должен бросать исключений");
    }

    @Test
    void shouldGenerateIdWhenFilmIsAdded() {
        Film film = Film.builder()
                .name("Матрица")
                .description("Фантастика")
                .releaseDate(LocalDate.of(1999, 3, 31))
                .duration(136)
                .build();

        Film savedFilm = filmController.addFilm(film);

        assertNotNull(savedFilm.getId(), "ID фильма должен быть сгенерирован");
        assertTrue(savedFilm.getId() > 0, "ID фильма должен быть положительным числом");
        assertNotNull(savedFilm.getLikes(), "Список лайков не должен быть null");
        assertTrue(savedFilm.getLikes().isEmpty(), "Новый фильм должен иметь 0 лайков по умолчанию");
    }

    @Test
    void shouldReturnAllAddedFilms() {
        Film film1 = Film.builder().name("Фильм 1").releaseDate(LocalDate.of(2000, 1, 1)).duration(100).build();
        Film film2 = Film.builder().name("Фильм 2").releaseDate(LocalDate.of(2001, 1, 1)).duration(120).build();

        filmController.addFilm(film1);
        filmController.addFilm(film2);

        assertEquals(2, filmController.getAllFilm().size(), "Метод getAllFilm должен возвращать все добавленные фильмы");
    }

    @Test
    void shouldCorrectlySortFilmsByLikesInTop() {
        // Создаем три фильма
        Film film1 = filmController.addFilm(Film.builder().name("Фильм без лайков").releaseDate(LocalDate.of(2000, 1, 1)).duration(100).build());
        Film film2 = filmController.addFilm(Film.builder().name("Фильм с 2 лайками").releaseDate(LocalDate.of(2001, 1, 1)).duration(120).build());
        Film film3 = filmController.addFilm(Film.builder().name("Фильм с 1 лайком").releaseDate(LocalDate.of(2002, 1, 1)).duration(110).build());

        film2.getLikes().add(1L);
        film2.getLikes().add(2L);
        film3.getLikes().add(1L);

        List<Film> topFilms = filmController.getTopFilms(3);

        assertEquals(3, topFilms.size(), "Должно вернуться 3 фильма");
        assertEquals(film2.getId(), topFilms.get(0).getId(), "Первым должен идти фильм с максимальным числом лайков (2)");
        assertEquals(film3.getId(), topFilms.get(1).getId(), "Вторым должен идти фильм с 1 лайком");
        assertEquals(film1.getId(), topFilms.get(2).getId(), "Последним должен идти фильм без лайков");
    }

    @Test
    void shouldLimitTopFilmsAccordingToCountParameter() {
        // Добавляем 5 фильмов
        for (int i = 1; i <= 5; i++) {
            filmController.addFilm(Film.builder().name("Фильм " + i).releaseDate(LocalDate.of(2000, 1, 1)).duration(100).build());
        }

        List<Film> topFilmsLimit2 = filmController.getTopFilms(2);
        assertEquals(2, topFilmsLimit2.size(), "Метод должен вернуть ровно 2 фильма, если count = 2");

        List<Film> topFilmsLimit5 = filmController.getTopFilms(5);
        assertEquals(5, topFilmsLimit5.size(), "Метод должен вернуть ровно 5 фильмов, если count = 5");
    }
}