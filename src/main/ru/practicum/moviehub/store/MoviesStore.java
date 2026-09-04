package ru.practicum.moviehub.store;

import ru.practicum.moviehub.exception.MovieNotFoundException;
import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoviesStore {
    private int autoincrement;
    private final HashMap<Integer, Movie> movies;

    public MoviesStore() {
        autoincrement = 1;
        this.movies = new HashMap<>();
    }

    public List<Movie> getMovies() {
        return Map.copyOf(movies).values().stream().toList();
    }

    public void addMovies(Movie movie) {
        movies.put(autoincrement, new Movie(autoincrement, movie.getTitle(), movie.getYear()));
        autoincrement++;
    }

    public Movie getById(int id) throws MovieNotFoundException {
        if (!movies.containsKey(id)) {
            throw new MovieNotFoundException();
        }
        return movies.get(id);
    }

    public List<Movie> getByYear(int year) {
        List<Movie> moviesResult = new ArrayList<>();
        for (Movie movie : movies.values()) {
            if (movie.getYear() == year) {
                moviesResult.add(movie);
            }
        }
        return moviesResult;
    }

    public void deleteById(int id) throws MovieNotFoundException {
        if (!movies.containsKey(id)) {
            throw new MovieNotFoundException();
        }
        movies.remove(id);
    }

    public void clear() {
        movies.clear();
        autoincrement = 1;
    }

    public int getNextId() {
        return autoincrement;
    }
}
