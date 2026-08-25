package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;


import java.util.*;

@Service
public class UserService {
    private final UserStorage userStorage;

    public UserService(@Qualifier("userDbStorage") UserStorage userStorage) {
        this.userStorage = userStorage;
    }

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
        userStorage.findById(userId).orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        userStorage.findById(friendId).orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        userStorage.addFriend(userId, friendId);
    }

    public void deletedFriend(Long userId, Long friendId) {
        userStorage.findById(userId).orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        userStorage.findById(friendId).orElseThrow(() -> new NotFoundException("Пользователь не найден"));


        userStorage.deleteFriend(userId, friendId);
    }

    public List<User> returnFriends(Long userId) {
        userStorage.findById(userId).orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        return userStorage.getFriends(userId);
    }

    public List<User> returnCommonFriends(Long userId, Long otherId) {
        userStorage.findById(userId).orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        userStorage.findById(otherId).orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        return userStorage.getCommonFriends(userId, otherId);
    }
}
