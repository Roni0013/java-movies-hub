package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.exception.MovieNotFoundException;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore moviesStore;

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String[] pathTokens = ex.getRequestURI().getPath().split("/");

        switch (method.toUpperCase()) {
            case "GET":
                if (pathTokens.length == 2) {
                    sendAllByFilter(ex);
                    break;
                }
                if (pathTokens.length == 3) {
                    sendOne(ex, pathTokens[2]);
                    break;
                }
            case "POST":
                addMovie(ex);
                break;
            case "DELETE":
                if (pathTokens.length == 3) {
                    deleteMovie(ex, pathTokens[2]);
                    break;
                }
            default:
                sendJson(ex, 404, "Путь не найден");
        }
    }

    private void sendAllByFilter(HttpExchange exchange) throws IOException {

        int year = parseYearParam(exchange);

        if (year == -1) {
            sendJson(exchange, 200, new Gson().toJson(moviesStore.getMovies()));
        } else {
            if (!isValidYear(year)) {
                sendJson(exchange, 400, "Некорректный параметр запроса — 'year'");
            }
            sendJson(exchange, 200, new Gson().toJson(moviesStore.getByYear(year)));
        }
    }

    private void sendOne(HttpExchange exchange, String idStr) throws IOException {
        try {
            int id = Integer.parseInt(idStr);
            sendJson(exchange, 200, new Gson().toJson(moviesStore.getById(id)));
        } catch (NumberFormatException e) {
            sendJson(exchange, 400, "Некорректный ID");
        } catch (MovieNotFoundException e) {
            sendJson(exchange, 404, "Фильм не найден");
        }
    }

    private void addMovie(HttpExchange exchange) throws IOException {

        List<String> headers = exchange.getRequestHeaders().get("Content-Type");
        if (headers == null || headers.isEmpty() || !headers.getFirst().contains("application/json")) {
            sendJson(exchange, 415, "Неподдерживаемый тип");
            return;
        }
        Gson gson = new Gson();
        try (InputStream inputStream = exchange.getRequestBody()) {
            String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            List<String> errors = validateMovieFields(body);
            if (!errors.isEmpty()) {
                ErrorResponse errorResponse = new ErrorResponse("Ошибка валидации", errors);
                sendJson(exchange, 422, gson.toJson(errorResponse));
                return;
            }
            Movie newMovie = new Gson().fromJson(body, Movie.class);
            moviesStore.addMovies(newMovie);
        }

        Movie createdMovie;
        try {
            createdMovie = moviesStore.getById(MoviesStore.getNextId() - 1);
        } catch (MovieNotFoundException e) {
            sendJson(exchange, 500, "Ошибка сохранения. Обратитесь в поддержку.");
            return;
        }

        sendJson(exchange, 201, gson.toJson(createdMovie));
    }

    private void deleteMovie(HttpExchange exchange, String idStr) throws IOException {
        try {
            int id = Integer.parseInt(idStr);
            moviesStore.deleteById(id);
            sendNoContent(exchange);
        } catch (NumberFormatException e) {
            sendJson(exchange, 400, "Некорректный ID");
        } catch (MovieNotFoundException e) {
            sendJson(exchange, 404, "Фильм не найден");
        }
    }

    private List<String> validateMovieFields(String bodyJson) {
        List<String> errors = new ArrayList<>();
        JsonElement jsonElement = JsonParser.parseString(bodyJson);
        if (!jsonElement.isJsonObject()) {
            errors.add("Неверный формат.");
            return errors;
        }

        JsonElement titleElement = jsonElement.getAsJsonObject().get("title");
        if (titleElement != null) {
            String title = titleElement.getAsString();
            if (title.isEmpty()) {
                errors.add("Название не может быть пустым");
            }
            if (title.length() > 100) {
                errors.add("Название слишком длинное");
            }
        } else {
            errors.add("Отсутствует поле title");
        }

        JsonElement yearElement = jsonElement.getAsJsonObject().get("year");
        if (yearElement != null) {
            int year = yearElement.getAsInt();
            if (!isValidYear(year)) {
                errors.add("Год фильма указан неверно");
                System.out.println(year);
            }
        } else {
            errors.add("Отсутствует поле year");
        }
        return errors;
    }

    private int parseYearParam(HttpExchange exchange) throws IOException {
        int year = -1;
        String query = exchange.getRequestURI().getQuery();
        if (query == null) {
            return year;
        }

        String[] filterTokens = exchange.getRequestURI().getQuery().split("&");
        if (filterTokens.length > 1) {
            sendJson(exchange, 400, "Неподдерживаемое количество параметров");
        }

        if (filterTokens.length == 1) {
            String[] params = filterTokens[0].split("=");
            if (params.length != 2 || !params[0].equals("year")) {
                sendJson(exchange, 400, "Некорректный параметр запроса — 'year'");
            }
            try {
                year = Integer.parseInt(params[1]);
            } catch (NumberFormatException e) {
                sendJson(exchange, 400, "Некорректный параметр запроса — 'year'");
            }
        }
        return year;
    }

    private boolean isValidYear(int year) {
        int currentYear = LocalDate.now().getYear();
        return year >= 1888 && year <= currentYear + 1;
    }
}
