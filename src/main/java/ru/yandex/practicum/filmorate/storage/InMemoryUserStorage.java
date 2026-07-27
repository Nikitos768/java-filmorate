package ru.yandex.practicum.filmorate.storage;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;

@Component
public class InMemoryUserStorage implements UserStorage {
    private final Map<Long, User> users = new HashMap<>();
    private long currentId = 0;
    private static final Logger log = LoggerFactory.getLogger(InMemoryUserStorage.class);
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Override
    public User addUser(User user) {
        log.info("Запрос на добавление нового пользователя: {}", user.getName());
        user.setId(getNextId());
        users.put(user.getId(), user);
        log.info("Успешное добовление нового пользователя: {}", user.getName());
        return user;
    }

    @Override
    public User updateUser(User user) {
        log.info("Запрос на обновление пользователя с ID: {}", user.getId());
        if (user.getId() == null || !users.containsKey(user.getId())) {
            log.warn("Пользователь с таким ID {} не найден.", user.getId());
            throw new NotFoundException("Пользователь с таким ID не найден.");
        }
        users.put(user.getId(), user);
        log.info("Успешное изменение пользователя с ID: {}", user.getId());
        return user;
    }

    public Collection<User> getAllUsers() {
        log.info("Получен запрос на получение всех пользователей. Всего пользователей: {}", users.size());
        return users.values();
    }

    @Override
    public Optional<User> deletedUser(User user) {
        if (user.getId() == null || !users.containsKey(user.getId())) {
            log.warn("Пользователь с таким ID {} не сущесвует.", user.getId());
            throw new NotFoundException("Пользователь с таким ID не найден.");
        }

        User remove = users.remove(user.getId());

        return Optional.ofNullable(remove);
    }

    @Override
    public Optional<User> findById(Long id) {
        log.info("Запрос пользователя из памяти по ID: {}", id);
        return Optional.ofNullable(users.get(id));
    }

    public Long getNextId() {
        return ++currentId;
    }
}
