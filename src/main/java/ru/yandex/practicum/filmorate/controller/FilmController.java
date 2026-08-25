package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/films")
public class FilmController {
    private final FilmService filmService;

    @Autowired
    public FilmController(FilmService filmService) {
        this.filmService = filmService;
    }

    @PostMapping
    public Film addFilm(@Valid @RequestBody Film film) {
        return filmService.addFilm(film);
    }

    @PutMapping
    public Film updateFilm(@Valid @RequestBody Film film) {
        return filmService.updateFilm(film);
    }


    @GetMapping
    public Collection<Film> getAllFilm() {
        return filmService.getAllFilm();
    }

    @GetMapping("/{id}")
    public Film getFilm(@PathVariable Long id) {
        return filmService.getFilm(id);
    }

    @DeleteMapping
    public ResponseEntity<Void> deletedFilm(@Valid @RequestBody Film film) {
        return filmService.deletedFilm(film)
                .map(f -> ResponseEntity.noContent().<Void>build())
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/like/{userId}")
    public void addLikes(@PathVariable Long id, @PathVariable Long userId) {
        filmService.addLikes(id, userId);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public void deletedLikes(@PathVariable Long id, @PathVariable Long userId) {
        filmService.deletedLikes(id, userId);
    }

    @GetMapping("/popular")
    public List<Film> getTopFilms(@RequestParam(defaultValue = "10") int count) {
        return filmService.getTopFilms(count);
    }
}