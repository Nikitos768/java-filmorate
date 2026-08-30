package ru.yandex.practicum.filmorate.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component("userDbStorage")
@RequiredArgsConstructor
@Slf4j
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public User addUser(User user) {
        String sqlQuery = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(sqlQuery, new String[]{"user_id"});
            stmt.setString(1, user.getEmail());
            stmt.setString(2, user.getLogin());
            stmt.setString(3, user.getName());
            stmt.setDate(4, Date.valueOf(user.getBirthday()));
            return stmt;
        }, keyHolder);

        user.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        log.info("Пользователь успешно добавлен с ID: {}", user.getId());
        return user;
    }

    @Override
    public User updateUser(User user) {
        String sqlQuery = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE user_id = ?";
        int rowsAffected = jdbcTemplate.update(sqlQuery,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                Date.valueOf(user.getBirthday()),
                user.getId());

        if (rowsAffected == 0) {
            log.error("Пользователь с ID {} не найден для обновления", user.getId());
            throw new NotFoundException("Пользователь не найден");
        }
        log.info("Пользователь с ID {} успешно обновлен", user.getId());
        return user;
    }

    @Override
    public Collection<User> getAllUsers() {
        String sqlQuery = "SELECT * FROM users";
        return jdbcTemplate.query(sqlQuery, this::mapRowToUser);
    }

    @Override
    public Optional<User> findById(Long id) {
        String sqlQuery = "SELECT * FROM users WHERE user_id = ?";
        return jdbcTemplate.query(sqlQuery, this::mapRowToUser, id).stream().findFirst();
    }

    @Override
    public Optional<User> deletedUser(User user) {
        Optional<User> userToReturn = findById(user.getId());

        if (userToReturn.isEmpty()) {
            log.error("Пользователь с ID {} не найден для удаления", user.getId());
            return Optional.empty();
        }

        String sqlQuery = "DELETE FROM users WHERE user_id = ?";
        jdbcTemplate.update(sqlQuery, user.getId());
        log.info("Пользователь с ID {} успешно удален", user.getId());

        return userToReturn;
    }

    @Override
    public void addFriend(Long userId, Long friendId) {
        String sqlQuery = "MERGE INTO friendship KEY(user_id, friend_id) VALUES (?, ?, 'unconfirmed')";
        jdbcTemplate.update(sqlQuery, userId, friendId);
        log.info("Пользователь {} отправил запрос в друзья пользователю {}", userId, friendId);
    }

    @Override
    public void deleteFriend(Long userId, Long friendId) {
        String sqlQuery = "DELETE FROM friendship WHERE user_id = ? AND friend_id = ?";
        jdbcTemplate.update(sqlQuery, userId, friendId);
        log.info("Пользователь {} удалил из друзей пользователя {}", userId, friendId);
    }

    @Override
    public List<User> getFriends(Long userId) {
        String sqlQuery = "SELECT u.* FROM users u " +
                "JOIN friendship f ON u.user_id = f.friend_id " +
                "WHERE f.user_id = ?";
        return jdbcTemplate.query(sqlQuery, this::mapRowToUser, userId);
    }

    @Override
    public List<User> getCommonFriends(Long userId, Long otherId) {
        String sqlQuery = "SELECT u.* FROM users u " +
                "JOIN friendship f1 ON u.user_id = f1.friend_id WHERE f1.user_id = ? " +
                "INTERSECT " +
                "SELECT u.* FROM users u " +
                "JOIN friendship f2 ON u.user_id = f2.friend_id WHERE f2.user_id = ?";

        return jdbcTemplate.query(sqlQuery, this::mapRowToUser, userId, otherId);
    }

    private User mapRowToUser(ResultSet rs, int rowNum) throws SQLException {
        return User.builder()
                .id(rs.getLong("user_id"))
                .email(rs.getString("email"))
                .login(rs.getString("login"))
                .name(rs.getString("name"))
                .birthday(rs.getDate("birthday").toLocalDate())
                .build();
    }
}