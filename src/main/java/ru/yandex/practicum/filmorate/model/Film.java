package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

/**
 * Film.
 */
@Data
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
}