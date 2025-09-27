package com.nais.recommendation_service.controller;

import com.nais.recommendation_service.model.User;
import com.nais.recommendation_service.model.WatchedRelationship;
import com.nais.recommendation_service.service.UserService;
import com.nais.recommendation_service.dto.ActorGenreCountDTO; // DODAJTE OVAJ IMPORT
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
// import java.util.Map; // Uklonjeno ako se ne koristi


@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User userDetails) {
        try {
            User updatedUser = userService.updateUser(id, userDetails);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // --- WatchedRelationship Endpoints ---

    static class WatchedMovieRequest {
        public Long movieId;
        public int rating;
    }

    @PostMapping("/{userId}/watched")
    public ResponseEntity<User> addOrUpdateWatchedMovie(@PathVariable Long userId, @RequestBody WatchedMovieRequest request) {
        try {
            User user = userService.addOrUpdateWatchedMovie(userId, request.movieId, request.rating);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/{userId}/watched/{movieId}")
    public ResponseEntity<WatchedRelationship> getWatchedRelationship(@PathVariable Long userId, @PathVariable Long movieId) {
        return userService.getWatchedRelationship(userId, movieId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{userId}/watched/{movieId}")
    public ResponseEntity<User> deleteWatchedMovie(@PathVariable Long userId, @PathVariable Long movieId) {
        try {
            User user = userService.deleteWatchedMovie(userId, movieId);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // --- Complex Query Endpoints ---

    // Complex Query 4 Endpoint
    @GetMapping("/watched-director-movies")
    public List<User> getUsersWhoWatchedDirectorMoviesAndRatedAbove(
            @RequestParam String directorName,
            @RequestParam(defaultValue = "7") int minRating) {
        return userService.getUsersWhoWatchedDirectorMoviesAndRatedAbove(directorName, minRating);
    }

    // Complex Query 5 Endpoint
    @GetMapping("/actors-by-genre-count")
    public List<ActorGenreCountDTO> getActorsByMostGenres(@RequestParam(defaultValue = "5") int limit) {
        return userService.getActorsByMostGenres(limit);
    }
}