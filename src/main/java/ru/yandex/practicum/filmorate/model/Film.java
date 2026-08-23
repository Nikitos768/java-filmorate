package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Film.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Film {
    private long id;

    @NotBlank(message = "Название фильма не может быть пустым.")
    private String name;

    @Size(max = 200, message = "Описание не должно превышать 200 символов.")
    private String description;

    @NotNull(message = "Дата релиза должна быть указана.")
    private LocalDate releaseDate;

    @Positive(message = "Продолжительность фильма должна быть больше нуля.")
    private int duration;

    private Mpa mpa;

    @Builder.Default
    private Set<Genre> genres = new HashSet<>();

    @Builder.Default
    private Set<Long> likes = new HashSet<>();
}