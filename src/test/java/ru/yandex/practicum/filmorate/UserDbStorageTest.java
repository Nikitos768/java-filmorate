package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.dao.UserDbStorage;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Sql(scripts = {"/schema.sql", "/data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class UserDbStorageTest {

    private final UserDbStorage userStorage;

    @Test
    void testFindUserById() {
        User newUser = User.builder()
                .email("user@email.ru")
                .login("vanya123")
                .name("Ivan Ivanov")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
        userStorage.addUser(newUser);

        Optional<User> userOptional = userStorage.findById(newUser.getId());

        assertThat(userOptional)
                .isPresent()
                .hasValueSatisfying(user -> {
                    assertThat(user).hasFieldOrPropertyWithValue("id", newUser.getId());
                    assertThat(user).hasFieldOrPropertyWithValue("name", "Ivan Ivanov");
                    assertThat(user).hasFieldOrPropertyWithValue("login", "vanya123");
                });
    }

    @Test
    void testUpdateUser() {
        User user = User.builder()
                .email("old@email.ru")
                .login("oldLogin")
                .name("Old Name")
                .birthday(LocalDate.of(1995, 5, 5))
                .build();
        userStorage.addUser(user);

        user.setName("New Name");
        user.setLogin("newLogin");
        userStorage.updateUser(user);

        Optional<User> updatedUserOpt = userStorage.findById(user.getId());

        assertThat(updatedUserOpt)
                .isPresent()
                .hasValueSatisfying(updatedUser -> {
                    assertThat(updatedUser).hasFieldOrPropertyWithValue("name", "New Name");
                    assertThat(updatedUser).hasFieldOrPropertyWithValue("login", "newLogin");
                });
    }
}