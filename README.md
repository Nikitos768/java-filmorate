# java-filmorate
Template repository for Filmorate project.
## Схема базы данных
![Схема БД Filmorate](image.png)
#### 1. Получение всех фильмов с их MPA-рейтингом
```sql
SELECT f.film_id, f.name, f.description, f.release_date, f.duration, m.name AS mpa_name
FROM films AS f
LEFT JOIN mpa AS m ON f.mpa_id = m.mpa_id;
```

#### 2. Топ-10 наиболее популярных фильмов (по количеству лайков)
```sql
SELECT f.film_id, f.name, COUNT(l.user_id) AS total_likes
FROM films AS f
LEFT JOIN likes AS l ON f.film_id = l.film_id
GROUP BY f.film_id
ORDER BY total_likes DESC
LIMIT 10;
```

#### 3. Получение списка друзей конкретного пользователя
```sql
SELECT u.user_id, u.email, u.login, u.name
FROM users AS u
JOIN friendship AS f ON u.user_id = f.friend_id
WHERE f.user_id = 1; -- Вместо 1 подставляется ID пользователя
```

#### 4. Список общих друзей с другим пользователем
```sql
SELECT u.user_id, u.email, u.login, u.name
FROM users AS u
WHERE u.user_id IN (
    SELECT friend_id FROM friendship WHERE user_id = 1
    INTERSECT
    SELECT friend_id FROM friendship WHERE user_id = 2
);
```