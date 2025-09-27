package com.nais.recommendation_service.service;

import com.nais.recommendation_service.model.Movie;
import com.nais.recommendation_service.model.User;
import com.nais.recommendation_service.model.WatchedRelationship;
import com.nais.recommendation_service.repository.MovieRepository;
import com.nais.recommendation_service.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final MovieRepository movieRepository; // To link watched movies

    public UserService(UserRepository userRepository, MovieRepository movieRepository) {
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
    }

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User updateUser(Long id, User userDetails) {
        return userRepository.findById(id).map(user -> {
            user.setUsername(userDetails.getUsername());
            // Note: Updating watchedMovies directly here might be complex.
            // It's better to have separate methods for managing relationships.
            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("User not found with id " + id));
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    // --- WatchedRelationship CRUD Operations ---

//    /**
//     * Creates or updates a WATCHED relationship between a User and a Movie.
//     * If the relationship already exists, its rating is updated.
//     *
//     * @param userId The ID of the user.
//     * @param movieId The ID of the movie.
//     * @param rating The rating for the movie (1-10).
//     * @return The updated User entity.
//     */
//    @Transactional
//    public User addOrUpdateWatchedMovie(Long userId, Long movieId, int rating) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
//        Movie movie = movieRepository.findById(movieId)
//                .orElseThrow(() -> new RuntimeException("Movie not found with id: " + movieId));
//
//        // Check if the user already watched this movie
//        Optional<WatchedRelationship> existingWatched = user.getWatchedMovies().stream()
//                .filter(w -> w.getMovie().getId().equals(movieId))
//                .findFirst();
//
//        if (existingWatched.isPresent()) {
//            // Update existing relationship
//            existingWatched.get().setRating(rating);
//        } else {
//            // Create new relationship
//            WatchedRelationship newWatched = new WatchedRelationship();
//            newWatched.setMovie(movie);
//            newWatched.setRating(rating);
//            user.getWatchedMovies().add(newWatched);
//        }
//        return userRepository.save(user);
//    }
//
//    /**
//     * Deletes a WATCHED relationship between a User and a Movie.
//     *
//     * @param userId  The ID of the user.
//     * @param movieId The ID of the movie.
//     * @return The updated User entity.
//     */
//    @Transactional
//    public User deleteWatchedMovie(Long userId, Long movieId) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
//
//        boolean removed = user.getWatchedMovies().removeIf(watched ->
//                watched.getMovie().getId().equals(movieId));
//
//        if (!removed) {
//            throw new RuntimeException("User " + userId + " did not watch movie " + movieId);
//        }
//        return userRepository.save(user);
//    }
//
//    /**
//     * Get a specific watched relationship by its ID.
//     * This might require a custom query or iterating through user's watched movies.
//     * For simplicity, this example retrieves all user's watched movies and filters.
//     */
//    public Optional<WatchedRelationship> getWatchedRelationship(Long userId, Long movieId) {
//        User user = userRepository.findById(userId, 1).orElseThrow(() -> new RuntimeException("User not found")); // Fetch watched movies
//        return user.getWatchedMovies().stream()
//                .filter(wr -> wr.getMovie().getId().equals(movieId))
//                .findFirst();
//    }
//
//    // Complex Query 4
//    public List<User> getUsersWhoWatchedDirectorMoviesAndRatedAbove(String directorName, int minRating) {
//        return userRepository.findUsersWhoWatchedDirectorMoviesAndRatedAbove(directorName, minRating);
//    }

    // Complex Query 5
//    public List<ActorGenreCountProjection> getActorsByMostGenres(int limit) {
//        return userRepository.findActorsByMostGenres(limit);
//    }

}