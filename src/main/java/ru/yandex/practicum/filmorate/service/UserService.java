package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;


import java.util.*;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserStorage userStorage;

    public User addUser(User user) {
        validate(user);
        return userStorage.addUser(user);
    }

    public User updateUser(User user) {
        validate(user);
        return userStorage.updateUser(user);
    }

    public Collection<User> getAllUsers() {
        return userStorage.getAllUsers();
    }

    public Optional<User> deletedUser(User user) {
        validate(user);
        return userStorage.deletedUser(user);
    }

    public Optional<User> findById(Long id) {
        return userStorage.findById(id);
    }

    public void validate(User user) {
        if (user == null) {
            throw new NotFoundException("Пользователь не может быть null.");
        }

        if (user.getLogin() == null || user.getLogin().isBlank()) {
            throw new ValidationException("Логин не может быть пустым.");
        }

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }

    public void addFriend(Long userId, Long friendId) {
        User user = findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + userId + " не найден"));

        User friend = findById(friendId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + friendId + " не найден"));

        if (user.getFriends().contains(friendId)) {
            throw new ValidationException("Пользователи с ID " + userId + " и " + friendId + " уже являются друзьями.");
        }

        user.getFriends().add(friendId);
        friend.getFriends().add(userId);

        userStorage.updateUser(user);
        userStorage.updateUser(friend);
    }

    public void deletedFriend(Long userId, Long friendId) {
        User user = userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + userId + " не найден"));

        User friend = userStorage.findById(friendId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + friendId + " не найден"));

        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId);

        userStorage.updateUser(user);
        userStorage.updateUser(friend);
    }

    public List<User> returnFriends(Long userId) {
        User user = userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + userId + " не найден"));

        return user.getFriends().stream()
                .map(friendId -> userStorage.findById(friendId)
                        .orElseThrow(() -> new NotFoundException("Друг с ID " + friendId + " не найден")))
                .toList();
    }

    public List<User> returnCommonFriends(Long userId, Long otherId) {
        User user = userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + userId + " не найден"));

        User other = userStorage.findById(otherId)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + otherId + " не найден"));

        Set<Long> otherFriends = other.getFriends();

        return user.getFriends().stream()
                .filter(friendId -> otherFriends.contains(friendId))
                .map(friendId -> userStorage.findById(friendId)
                        .orElseThrow(() -> new NotFoundException("Друг с ID " + friendId + " не найден")))
                .toList();
    }
}
