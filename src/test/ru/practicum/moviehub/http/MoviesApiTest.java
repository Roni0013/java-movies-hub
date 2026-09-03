package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.practicum.moviehub.exception.MovieNotFoundException;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080"; // !!! добавьте базовую часть URL
    private static final String CONTENT_TYPE = "application/json; charset=UTF-8";

    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore moviesStore;
    private static HttpResponse.BodyHandler<String> bodyHandler;


    @BeforeAll
    static void beforeAll() {
        moviesStore = new MoviesStore();
        server = new MoviesServer(moviesStore, 8080);
        server.start();
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        bodyHandler = HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
    }

    @BeforeEach
    void beforeEach() {
        moviesStore.clear();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder().GET().uri(URI.create(BASE + "/movies")).build();
        HttpResponse<String> resp = client.send(req, bodyHandler);

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");
        assertContentType(resp);

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"), "Ожидается JSON-массив");
    }

    @Test
    void getMovies_whenExistsOne() throws Exception {
        Movie movie = new Movie("Служебный роман", 1977);
        moviesStore.addMovies(movie);
        HttpRequest req = HttpRequest.newBuilder().GET().uri(URI.create(BASE + "/movies")).build();
        HttpResponse<String> resp = client.send(req, bodyHandler);

        assertEquals(200, resp.statusCode(), "GET /movies, 200");
        assertContentType(resp);

        String body = resp.body().trim();
        List<Movie> movies = new Gson().fromJson(body, new ListOfMoviesTypeToken().getType());
        assertEquals(1, movies.size(), "GET /movies, один фильм в списке");
    }

    @Test
    void getMovieById() throws Exception {
        Movie movie = new Movie("Служебный роман", 1977);
        moviesStore.addMovies(movie);

        int idRequest = MoviesStore.getNextId() - 1;
        HttpRequest req = HttpRequest.newBuilder().GET().uri(URI.create(BASE + "/movies/" + idRequest)).build();
        HttpResponse<String> resp = client.send(req, bodyHandler);

        assertEquals(200, resp.statusCode(), "GET /movies/{id}, 200");
        assertContentType(resp);
        String body = resp.body().trim();
        Movie responsedMovie = new Gson().fromJson(body, Movie.class);
        assertEquals(movie.getTitle(), responsedMovie.getTitle(), "Соответствует название фильма");
        assertEquals(movie.getYear(), responsedMovie.getYear(), "Соответствует год фильма");
    }

    @ParameterizedTest
    @CsvSource({
        "1977,2",
        "1969,1",
        "2000,0"
    })
    void getMovieByYear(int year, int size) throws Exception {
        Movie movie1 = new Movie("Служебный роман", 1977);
        Movie movie2 = new Movie("Другой роман", 1977);
        Movie movie3 = new Movie("Бриллиантовая рука", 1969);
        moviesStore.addMovies(movie1);
        moviesStore.addMovies(movie2);
        moviesStore.addMovies(movie3);

        HttpRequest req = HttpRequest.newBuilder().GET().uri(URI.create(BASE + "/movies?year=" + year)).build();
        HttpResponse<String> resp = client.send(req, bodyHandler);

        assertEquals(200, resp.statusCode(), "GET /movies/?year={year}, 200");
        assertContentType(resp);
        String body = resp.body().trim();
        List<Movie> responseMovies = new Gson().fromJson(body, new ListOfMoviesTypeToken().getType());
        assertEquals(size, responseMovies.size(), String.format("Вернулось %d фильмов", size));
    }

    @ParameterizedTest
    @ValueSource(strings = {"?year=111", "?year=2000&wrong=1", "?yea=2000", "?year(2000)"})
    void getMovieByYearErrors(String params) throws Exception {
        HttpRequest req = HttpRequest.newBuilder().GET().uri(URI.create(BASE + "/movies" + params)).build();
        HttpResponse<String> resp = client.send(req, bodyHandler);

        assertEquals(400, resp.statusCode(), String.format("Параметры запроса '%s', код 400", params));
    }

    @ParameterizedTest()
    @CsvSource({
        "9999, 404",
        "wrong, 400"
    })
    void getMovieByIdErrors(String id, int code) throws Exception {
        HttpRequest req = HttpRequest.newBuilder().GET().uri(URI.create(BASE + "/movies/" + id)).build();
        HttpResponse<String> resp = client.send(req, bodyHandler);

        assertEquals(code, resp.statusCode(), String.format("GET, id=%s, код ошибки=%d", id, code));
    }

    @Test
    void postMovieSuccess() throws IOException, InterruptedException, MovieNotFoundException {
        JsonObject movieJson = new JsonObject();
        movieJson.addProperty("title", "Кино");
        movieJson.addProperty("year", 2026);

        HttpRequest req = HttpRequest.newBuilder().POST(
            HttpRequest.BodyPublishers.ofString(new Gson().toJson(movieJson))
        ).uri(URI.create(BASE + "/movies")).setHeader("Content-Type", CONTENT_TYPE).build();

        HttpResponse<String> resp = client.send(req, bodyHandler);
        assertEquals(201, resp.statusCode(), "POST /movies, 201");
        assertContentType(resp);

        Movie addedMovie = new Gson().fromJson(resp.body().trim(), Movie.class);
        assertEquals(1, moviesStore.getMovies().size(), "Пустое хранилище стало непустым");
        assertEquals(addedMovie, moviesStore.getById(MoviesStore.getNextId() - 1),
            "Ответ соответствует значению в хранилище");
    }


    @ParameterizedTest
    @CsvSource({
        "Название, 2000, 30, 1",
        "Название, 2000, 0, 1",
        "Название, 1300, 1 ,1",
        "Название, 3000, 1 ,1",
        "Название, 3000, 30 ,2",
    })
    void postMovieValidateErrors(String title, int year, int titleRepeat,
                                 int countErrorDetails) throws IOException, InterruptedException {
        JsonObject movieJson = new JsonObject();
        movieJson.addProperty("title", title.repeat(titleRepeat));
        movieJson.addProperty("year", year);

        HttpRequest req = HttpRequest.newBuilder().setHeader("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(new Gson().toJson(movieJson))).uri(URI.create(BASE + "/movies"))
            .build();
        HttpResponse<String> resp = client.send(req, bodyHandler);

        assertEquals(422, resp.statusCode(), "POST /movies ошибка валидации, 422");
        assertContentType(resp);
        JsonObject jsonResponse = JsonParser.parseString(resp.body()).getAsJsonObject();
        String error = jsonResponse.get("error").getAsString();
        assertEquals("Ошибка валидации", error, "Проверка краткого сообщения ошибки");
        assertEquals(countErrorDetails, jsonResponse.get("details").getAsJsonArray().size(),
            "Количество ошибок валидации соответствует");

    }

    @ParameterizedTest
    @CsvSource({
        "1, 204, 1",
        "100, 404, 2"
    })
    void deleteMovie(int id, int code, int size) throws Exception {
        Movie movie1 = new Movie("Служебный роман", 1977);
        Movie movie2 = new Movie("Бриллиантовая рука", 1969);
        moviesStore.addMovies(movie1);
        moviesStore.addMovies(movie2);

        HttpRequest req = HttpRequest.newBuilder().DELETE().uri(URI.create(BASE + "/movies/" + id)).build();
        HttpResponse<String> resp = client.send(req, bodyHandler);

        assertEquals(code, resp.statusCode(), String.format("Попытка удалить по id, код=%d", code));
        assertEquals(size, moviesStore.getMovies().size(), "Проверка изменения размера хранилища");
    }

    private void assertContentType(HttpResponse<String> response) {
        String contentTypeValue = response.headers().firstValue("Content-Type").orElse("");
        assertEquals(CONTENT_TYPE, contentTypeValue, "Content-Type должен содержать формат данных и кодировку");
    }
}