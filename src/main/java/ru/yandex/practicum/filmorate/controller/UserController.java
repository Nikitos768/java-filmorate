package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public User addUser(@Valid @RequestBody User user) {
        return userService.addUser(user);
    }

    @PutMapping
    public User updateUser(@Valid @RequestBody User user) {
        return userService.updateUser(user);
    }

    @PutMapping("/{id}/friends/{friendId}")
    public void addFriend(@PathVariable Long id, @PathVariable Long friendId) {
        userService.addFriend(id, friendId);
    }

    @GetMapping
    public Collection<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + id + " не найден."));
    }

    @GetMapping("/{id}/friends")
    public List<User> returnFriends(@PathVariable long id) {
        return userService.returnFriends(id);
    }

    @GetMapping("/{id}/friends/common/{otherId}")
    public List<User> returnCommonFriends(@PathVariable Long id, @PathVariable Long otherId) {
        return userService.returnCommonFriends(id, otherId);
    }

    @DeleteMapping
    public ResponseEntity<Void> deletedUser(@Valid @RequestBody User user) {
        return userService.deletedUser(user)
                .map(f -> ResponseEntity.noContent().<Void>build())
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/friends/{friendId}")
    public void deletedFriend(@PathVariable Long id, @PathVariable Long friendId) {
        userService.deletedFriend(id, friendId);
    }
}