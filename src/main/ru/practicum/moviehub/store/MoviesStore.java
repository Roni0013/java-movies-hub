package ru.practicum.moviehub.store;

import ru.practicum.moviehub.exception.MovieNotFoundException;
import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MoviesStore {
    private static int autoincrement;
    private final List<Movie> movies;

    public MoviesStore() {
        autoincrement = 1;
        this.movies = new ArrayList<>();
    }

    public List<Movie> getMovies() {
        return List.copyOf(movies);
    }

    public void addMovies(Movie movie) {
        movies.add(new Movie(autoincrement++, movie.getTitle(), movie.getYear()));
    }

    public Movie getById(int id) throws MovieNotFoundException {
        for (Movie movie : movies) {
            if (movie.getId() == id) {
                return movie;
            }
        }
        throw new MovieNotFoundException();
    }

    public List<Movie> getByYear(int year) {
        return movies.stream().filter(movie -> movie.getYear() == year).collect(Collectors.toList());
    }

    public void deleteById(int id) throws MovieNotFoundException {
        for (int i = 0; i < movies.size(); i++) {
            if (movies.get(i).getId() == id) {
                movies.remove(i);
                return;
            }
        }
        throw new MovieNotFoundException();
    }

    public void clear() {
        movies.clear();
        autoincrement = 1;
    }

    public static int getNextId() {
        return autoincrement;
    }
}
