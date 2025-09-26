package com.nais.search_service.controller;

import com.nais.search_service.dto.MovieDocumentDto;
import com.nais.search_service.service.MovieService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/movies") // Promenjena putanja
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @PostMapping
    public ResponseEntity<String> addMovie(@RequestBody MovieDocumentDto movie) {
        String id = movieService.createMovie(movie);
        return id != null ?
                new ResponseEntity<>(id, HttpStatus.CREATED) :
                new ResponseEntity<>("Failed to create movie", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getMovieById(@PathVariable Long id) {
        Map<String, Object> movie = movieService.getMovieById(id);
        return movie != null ?
                ResponseEntity.ok(movie) :
                ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateMovie(@PathVariable Long id, @RequestBody MovieDocumentDto movie) {
        boolean success = movieService.updateMovie(id, movie);
        return success ?
                ResponseEntity.ok().build() :
                ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMovie(@PathVariable Long id) {
        boolean success = movieService.deleteMovie(id);
        return success ?
                ResponseEntity.noContent().build() :
                ResponseEntity.notFound().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchSimilar(
            @RequestParam String q,
            @RequestParam(defaultValue = "5") int limit) {
        List<Map<String, Object>> results = movieService.searchMoviesByDescription(q, limit);
        return results != null ?
                ResponseEntity.ok(results) :
                ResponseEntity.internalServerError().build();
    }

    @GetMapping("/filter")
    public ResponseEntity<List<Map<String, Object>>> filterMovies(
            @RequestParam String genre,
            @RequestParam int year) {
        List<Map<String, Object>> results = movieService.filterMovies(genre, year);
        return results != null ? ResponseEntity.ok(results) : ResponseEntity.notFound().build();
    }

    @GetMapping("/count")
    public ResponseEntity<Long> countMoviesByGenre(@RequestParam String genre) {
        long count = movieService.countMoviesByGenre(genre);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/search-filtered")
    public ResponseEntity<List<Map<String, Object>>> searchWithFilters(
            @RequestParam String q,
            @RequestParam String genre,
            @RequestParam int year,
            @RequestParam(defaultValue = "5") int limit) {
        List<Map<String, Object>> results = movieService.searchWithFilters(q, genre, year, limit);
        return results != null ? ResponseEntity.ok(results) : ResponseEntity.internalServerError().build();
    }

    @GetMapping("/search-paginated")
    public ResponseEntity<List<Map<String, Object>>> searchWithPagination(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> results = movieService.searchWithPagination(q, offset, limit);
        return results != null ? ResponseEntity.ok(results) : ResponseEntity.internalServerError().build();
    }

}