package ru.yandex.practicum.filmorate;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FilmControllerTest {

    private FilmController filmController;
    private Validator validator;

    @BeforeEach
    void setUp() {
        filmController = new FilmController();
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private boolean hasValidationErrors(Film film) {
        return !validator.validate(film).isEmpty();
    }

    @Test
    void shouldDetectEmptyFilmName() {
        Film film = new Film();
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(2020, 1, 1));
        film.setDuration(90);

        film.setName("");
        assertTrue(hasValidationErrors(film), "Аннотация @NotBlank должна отсечь пустое название");

        film.setName(null);
        assertTrue(hasValidationErrors(film), "Аннотация @NotBlank должна отсечь название равное null");

        film.setName("  ");
        assertTrue(hasValidationErrors(film), "Аннотация @NotBlank должна отсечь название только из пробелов");
    }

    @Test
    void shouldDetectTooLongDescription() {
        Film film = new Film();
        film.setName("Фильм");
        film.setReleaseDate(LocalDate.of(2020, 1, 1));
        film.setDuration(90);

        film.setDescription("a".repeat(200));
        assertFalse(hasValidationErrors(film), "Описание в 200 символов разрешено");

        film.setDescription("a".repeat(201));
        assertTrue(hasValidationErrors(film), "Аннотация @Size должна отсечь описание > 200 символов");
    }

    @Test
    void shouldDetectInvalidDuration() {
        Film film = new Film();
        film.setName("Фильм");
        film.setReleaseDate(LocalDate.of(2020, 1, 1));

        film.setDuration(0);
        assertTrue(hasValidationErrors(film), "Аннотация @Positive должна запретить длительность 0");

        film.setDuration(1);
        assertFalse(hasValidationErrors(film), "Аннотация @Positive должна разрешить длительность 1");

        film.setDuration(-10);
        assertTrue(hasValidationErrors(film), "Аннотация @Positive должна запретить отрицательную длительность");
    }

    @Test
    void shouldValidateReleaseDateBoundaries() {
        Film film = new Film();
        film.setName("Фильм");
        film.setDescription("Описание");
        film.setDuration(90);

        film.setReleaseDate(LocalDate.of(1895, 12, 28));
        assertDoesNotThrow(() -> filmController.validate(film), "Дата 28.12.1895 должна быть разрешена");

        film.setReleaseDate(LocalDate.of(1895, 12, 27));
        assertThrows(ConditionsNotMetException.class, () -> filmController.validate(film), "Дата 27.12.1895 должна вызывать исключение");
    }

    @Test
    void shouldThrowExceptionWhenFilmIsNull() {
        assertThrows(ConditionsNotMetException.class, () -> filmController.validate(null),
                "Передача null в контроллер должна вызывать исключение");
    }

    @Test
    void shouldDetectErrorsInEmptyObject() {
        Film film = new Film();
        assertTrue(hasValidationErrors(film), "Пустой объект не должен проходить валидацию");
    }

    @Test
    void shouldSuccessfullyValidateCorrectFilm() {
        Film film = new Film();
        film.setName("Интерстеллар");
        film.setDescription("Отличный фильм");
        film.setReleaseDate(LocalDate.of(2014, 11, 6));
        film.setDuration(169);

        assertFalse(hasValidationErrors(film), "Корректный фильм не должен иметь ошибок");
        assertDoesNotThrow(() -> filmController.validate(film), "Метод validate не должен бросать исключений");
    }
}
