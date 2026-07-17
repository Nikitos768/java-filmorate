package ru.yandex.practicum.filmorate;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerTest {

    private UserController userController;
    private Validator validator;

    @BeforeEach
    void setUp() {
        userController = new UserController();
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private boolean hasValidationErrors(User user) {
        return !validator.validate(user).isEmpty();
    }

    @Test
    void shouldDetectInvalidEmail() {
        User user = new User();
        user.setLogin("login");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        user.setEmail("");
        assertTrue(hasValidationErrors(user), "Аннотация @NotBlank должна отсечь пустой email");

        user.setEmail("invalid-email.com");
        assertTrue(hasValidationErrors(user), "Аннотация @Email должна отсечь имейл без знака @");
    }

    @Test
    void shouldDetectInvalidLogin() {
        User user = new User();
        user.setEmail("user@yandex.ru");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        user.setLogin("");
        assertTrue(hasValidationErrors(user), "Аннотация @NotBlank должна отсечь пустой логин");

        user.setLogin("my login");
        assertTrue(hasValidationErrors(user), "Аннотация @Pattern должна запретить пробелы в логине");
    }

    @Test
    void shouldDetectFutureBirthday() {
        User user = new User();
        user.setEmail("user@yandex.ru");
        user.setLogin("login");

        user.setBirthday(LocalDate.now().plusDays(1));
        assertTrue(hasValidationErrors(user), "Аннотация @PastOrPresent должна запретить дату в будущем");
    }

    @Test
    void shouldReplaceEmptyNameWithLogin() {
        User user = new User();
        user.setEmail("user@yandex.ru");
        user.setLogin("nagibator");
        user.setBirthday(LocalDate.of(2000, 1, 1));
        user.setName(null);

        userController.validate(user);

        assertEquals("nagibator", user.getName(), "Метод validate должен подменить имя логином");
        assertFalse(hasValidationErrors(user), "Объект должен быть полностью валидным");
    }
}
