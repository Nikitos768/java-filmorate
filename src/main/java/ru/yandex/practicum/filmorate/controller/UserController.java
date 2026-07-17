package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolation;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.Exception.ConditionsNotMetException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {
    Map<Long, User> users = new HashMap<>();
    private long currentId = 0;
    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @PostMapping
    public User addUser(@Valid @RequestBody User user) {
        log.info("Запрос на добавление нового пользователя: {}", user.getName());
        validate(user);
        user.setId(getNextId());
        users.put(user.getId(), user);
        log.info("Успешное добовление нового пользователя: {}", user.getName());
        return user;
    }

    @PutMapping
    public User updateUser(@Valid @RequestBody User user) {
        log.info("Запрос на обновление пользователя с ID: {}", user.getId());
        if (user.getId() == null || !users.containsKey(user.getId())) {
            log.warn("Пользователь с таким ID {} не найден.", user.getId());
            throw new ConditionsNotMetException("Пользователь с таким ID не найден.");
        }
        validate(user);
        users.put(user.getId(), user);
        log.info("Успешное изменение пользователя с ID: {}", user.getId());
        return user;
    }

    @GetMapping
    public Collection<User> getAllUsers() {
        log.info("Получен запрос на получение всех пользователей. Всего пользователей: {}", users.size());
        return users.values();
    }

    public void validate(User user) {
        if (user == null) {
            throw new ConditionsNotMetException("Пользователь не может быть null.");
        }

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        if (!violations.isEmpty()) {
            throw new ConditionsNotMetException(violations.iterator().next().getMessage());
        }

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }

    public Long getNextId() {
        return ++currentId;
    }
}