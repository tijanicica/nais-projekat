package com.nais.search_service.controller;

import com.nais.search_service.dto.MovieDocumentDto;
import com.nais.search_service.service.SearchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.ai.document.Document;
import java.util.List;


@RestController
@RequestMapping("/search")
public class SearchController {
    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }
    // ... konstruktor

    @PostMapping("/movies")
    public ResponseEntity<Void> addMovie(@RequestBody MovieDocumentDto movie) {
        searchService.addMovie(movie);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/movies/similar")
    public List<Document> searchSimilar(@RequestParam String q, @RequestParam(defaultValue = "5") int k) {
        return searchService.findSimilarMoviesByDescription(q, k);
    }

    @GetMapping("/movies/filtered")
    public List<Document> searchFiltered(
            @RequestParam String q,
            @RequestParam String genre,
            @RequestParam(defaultValue = "5") int k) {
        return searchService.findSimilarMoviesWithFilter(q, genre, k);
    }
}
