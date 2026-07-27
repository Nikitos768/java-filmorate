package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.Optional;

public interface UserStorage {
    User addUser(User user);

    User updateUser(User user);

    Optional<User> deletedUser(User user);

    Collection<User> getAllUsers();

    Optional<User> findById(Long id);
}
