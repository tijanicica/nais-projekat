package com.nais.recommendation_service.controller;

import com.nais.recommendation_service.model.Movie;
import com.nais.recommendation_service.service.MovieService;
import com.nais.recommendation_service.dto.TopRatedMovieDTO; // DODAJTE OVAJ IMPORT
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/movies")
public class MovieController {
    private final MovieService movieService;
    private static final Logger logger = LoggerFactory.getLogger(MovieController.class);

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @PostMapping
    public Movie createMovie(@RequestBody Movie movie) {
        return movieService.createMovie(movie);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Movie> getMovieById(@PathVariable Long id) {
        return movieService.getMovieById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<Movie> getAllMovies() {
        return movieService.getAllMovies();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Movie> updateMovie(@PathVariable Long id, @RequestBody Movie movieDetails) {
        try {
            Movie updatedMovie = movieService.updateMovie(id, movieDetails);
            return ResponseEntity.ok(updatedMovie);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMovie(@PathVariable Long id) {
        try {
            movieService.deleteMovie(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // --- Relationship Endpoints ---

    @PostMapping("/{movieId}/actors/{actorId}")
    public ResponseEntity<Movie> addActorToMovie(@PathVariable Long movieId, @PathVariable Long actorId) {
        try {
            Movie movie = movieService.addActorToMovie(movieId, actorId);
            return ResponseEntity.ok(movie);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @DeleteMapping("/{movieId}/actors/{actorId}")
    public ResponseEntity<Movie> removeActorFromMovie(@PathVariable Long movieId, @PathVariable Long actorId) {
        try {
            Movie movie = movieService.removeActorFromMovie(movieId, actorId);
            return ResponseEntity.ok(movie);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/{movieId}/genres/{genreId}")
    public ResponseEntity<Movie> addGenreToMovie(@PathVariable Long movieId, @PathVariable Long genreId) {
        try {
            Movie movie = movieService.addGenreToMovie(movieId, genreId);
            return ResponseEntity.ok(movie);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @DeleteMapping("/{movieId}/genres/{genreId}")
    public ResponseEntity<Movie> removeGenreFromMovie(@PathVariable Long movieId, @PathVariable Long genreId) {
        try {
            Movie movie = movieService.removeGenreFromMovie(movieId, genreId);
            return ResponseEntity.ok(movie);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/{movieId}/director/{directorId}")
    public ResponseEntity<Movie> setDirectorForMovie(@PathVariable Long movieId, @PathVariable Long directorId) {
        try {
            Movie movie = movieService.setDirectorForMovie(movieId, directorId);
            return ResponseEntity.ok(movie);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @DeleteMapping("/{movieId}/director")
    public ResponseEntity<Movie> removeDirectorFromMovie(@PathVariable Long movieId) {
        try {
            Movie movie = movieService.removeDirectorFromMovie(movieId);
            return ResponseEntity.ok(movie);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // Kompleksni Upit 1 : Pronađi Top N filmova po prosečnoj oceni
    @GetMapping("/top-rated")
    public List<TopRatedMovieDTO> getTopRatedMovies(@RequestParam(defaultValue = "10") int limit) {
        return movieService.getTopNMoviesByAverageRating(limit);
    }
  //  Kompleksni CRUD Upit 1 : Povećaj godinu izdanja filmova režisera
    @PutMapping("/director/{directorId}/increment-release-year")
    public ResponseEntity<List<Movie>> incrementMoviesReleaseYearByDirector(
            @PathVariable Long directorId,
            @RequestParam(defaultValue = "1") Integer increment) {

        logger.info("Director ID primljen: {}", directorId);
        List<Movie> updatedMovies = movieService.incrementMovieReleaseYearByDirector(directorId, increment);
        return ResponseEntity.ok(updatedMovies);
    }
    //Kompleksni CRUD Upit 2 (MovieService): Dodaj žanr filmovima objavljenim pre određene godine
    @PostMapping("/before-year/{year}/add-genre/{genreId}")
    public ResponseEntity<List<Movie>> addGenreToMoviesReleasedBeforeYear(
            @PathVariable Integer year,
            @PathVariable Long genreId) {

        logger.info("Genre ID primljen: {}", genreId);
        List<Movie> updatedMovies = movieService.addGenreToMoviesReleasedBeforeYear(genreId, year);
        return ResponseEntity.ok(updatedMovies);
    }
}