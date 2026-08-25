package ru.yandex.practicum.filmorate;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.InMemoryUserStorage;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerTest {

    private UserController userController;
    private Validator validator;

    @BeforeEach
    void setUp() {
        InMemoryUserStorage userStorage = new InMemoryUserStorage();
        UserService userService = new UserService(userStorage);
        userController = new UserController(userService);

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private boolean hasValidationErrors(User user) {
        return !validator.validate(user).isEmpty();
    }

    @Test
    void shouldDetectInvalidEmail() {
        User user = User.builder().login("login").birthday(LocalDate.of(2000, 1, 1)).email("").build();
        assertTrue(hasValidationErrors(user), "Аннотация @NotBlank должна отсечь пустой email");

        user.setEmail("invalid-email.com");
        assertTrue(hasValidationErrors(user), "Аннотация @Email должна отсечь имейл без знака @");

        user.setEmail(null);
        assertTrue(hasValidationErrors(user), "Email со значением null должен быть запрещен");
    }

    @Test
    void shouldDetectInvalidLogin() {
        User user = User.builder().email("user@yandex.ru").birthday(LocalDate.of(2000, 1, 1)).login("").build();
        assertTrue(hasValidationErrors(user), "Аннотация @NotBlank должна отсечь пустой логин");

        user.setLogin("my login");
        assertTrue(hasValidationErrors(user), "Аннотация @Pattern должна запретить пробелы в логине");

        user.setLogin(" ");
        assertTrue(hasValidationErrors(user), "Логин только из пробелов должен быть запрещен");

        user.setLogin(" login");
        assertTrue(hasValidationErrors(user), "Пробел в начале логина должен быть запрещен");

        user.setLogin("login ");
        assertTrue(hasValidationErrors(user), "Пробел в конце логина должен быть запрещен");
    }

    @Test
    void shouldDetectFutureBirthday() {
        User user = User.builder().email("user@yandex.ru").login("login").birthday(LocalDate.now().plusDays(1)).build();
        assertTrue(hasValidationErrors(user), "Аннотация @PastOrPresent должна запретить дату в будущем (сегодня + 1 день)");

        user.setBirthday(LocalDate.now());
        assertFalse(hasValidationErrors(user), "Граница: дата рождения, равная сегодняшнему дню, разрешена");
    }

    @Test
    void shouldReplaceEmptyNameWithLogin() {
        User user = User.builder().email("user@yandex.ru").login("nagibator").birthday(LocalDate.of(2000, 1, 1)).name(null).build();

        User savedUserNull = userController.addUser(user);
        assertEquals("nagibator", savedUserNull.getName(), "Бизнес-логика: Метод должен подменить null-имя логином");

        User userEmpty = User.builder().email("user@yandex.ru").login("nagibator2").birthday(LocalDate.of(2000, 1, 1)).name("").build();
        User savedUserEmpty = userController.addUser(userEmpty);
        assertEquals("nagibator2", savedUserEmpty.getName(), "Бизнес-логика: Метод должен подменить пустое имя логином");

        User userBlank = User.builder().email("user@yandex.ru").login("nagibator3").birthday(LocalDate.of(2000, 1, 1)).name("   ").build();
        User savedUserBlank = userController.addUser(userBlank);
        assertEquals("nagibator3", savedUserBlank.getName(), "Бизнес-логика: Метод должен подменить строку из пробелов логином");
    }

    @Test
    void shouldThrowExceptionWhenUserIsNull() {
        assertThrows(NotFoundException.class, () -> userController.addUser(null),
                "Передача null в контроллер должна вызывать исключение");
    }

    @Test
    void shouldDetectErrorsInEmptyObject() {
        User user = new User();
        assertTrue(hasValidationErrors(user), "Пустой объект не должен проходить валидацию");
    }

    @Test
    void shouldSuccessfullyValidateCorrectUser() {
        User user = User.builder()
                .email("valid.user@mail.com")
                .login("valid_login")
                .name("Иван")
                .birthday(LocalDate.of(1995, 5, 15))
                .build();

        assertFalse(hasValidationErrors(user), "Корректный пользователь не должен иметь ошибок JSR-303");
        assertDoesNotThrow(() -> userController.addUser(user), "Метод добавления не должен бросать исключений");
    }

    @Test
    void shouldInitializeFriendsSetAsEmptyByDefault() {
        User user = User.builder().email("u@mail.ru").login("usr").birthday(LocalDate.of(2000, 1, 1)).build();
        User savedUser = userController.addUser(user);

        assertNotNull(savedUser.getFriends(), "Список друзей не должен быть null");
        assertTrue(savedUser.getFriends().isEmpty(), "Новый пользователь должен иметь 0 друзей по умолчанию");
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoFriends() {
        User user = userController.addUser(User.builder().email("u@mail.ru").login("usr").birthday(LocalDate.of(2000, 1, 1)).build());

        List<User> friends = userController.getAllUsers().stream()
                .filter(u -> u.getId().equals(user.getId()))
                .findFirst()
                .get()
                .getFriends()
                .stream()
                .map(id -> userController.getAllUsers().stream().filter(u -> u.getId().equals(id)).findFirst().get())
                .toList();

        assertTrue(friends.isEmpty(), "Граница: список друзей должен быть пустым, если друзей нет");
    }

    @Test
    void shouldHandleGeneralFriendsWhenThereAreNone() {
        User user1 = userController.addUser(User.builder().email("u1@mail.ru").login("usr1").birthday(LocalDate.of(2000, 1, 1)).build());
        User user2 = userController.addUser(User.builder().email("u2@mail.ru").login("usr2").birthday(LocalDate.of(2000, 1, 1)).build());

        List<User> commonFriends = userController.returnCommonFriends(user1.getId(), user2.getId());

        assertNotNull(commonFriends, "Список общих друзей не должен быть null");
        assertTrue(commonFriends.isEmpty(), "Список общих друзей должен быть пустым, если пересечений нет");
    }
}