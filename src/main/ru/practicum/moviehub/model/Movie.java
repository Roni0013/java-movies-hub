package ru.practicum.moviehub.model;

import java.util.Objects;

public class Movie {
    private final int id;
    private final String title;
    private final int year;

    public Movie(int id, String title, int year) {
        this.id = id;
        this.title = title.trim();
        this.year = year;
    }

    public Movie(String title, int year) {
        this(-1, title, year);
    }

    public String getTitle() {
        return title;
    }

    public int getYear() {
        return year;
    }

    public int getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Movie movie = (Movie) o;
        return id == movie.id && year == movie.year && Objects.equals(title, movie.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, year);
    }

    @Override
    public String toString() {
        return "Movie{" +
            "id=" + id +
            ", title='" + title + '\'' +
            ", year=" + year +
            '}';
    }
}