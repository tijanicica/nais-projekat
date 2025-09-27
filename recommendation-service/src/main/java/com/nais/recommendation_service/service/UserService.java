package com.nais.recommendation_service.service;

import com.nais.recommendation_service.model.Movie;
import com.nais.recommendation_service.model.User;
import com.nais.recommendation_service.model.WatchedRelationship;
import com.nais.recommendation_service.repository.MovieRepository;
import com.nais.recommendation_service.repository.UserRepository;
import com.nais.recommendation_service.dto.ActorGenreCountDTO;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Collection;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final Neo4jClient neo4jClient; // Dodajemo Neo4jClient

    public UserService(UserRepository userRepository, MovieRepository movieRepository, Neo4jClient neo4jClient) {
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
        this.neo4jClient = neo4jClient;
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
            return userRepository.save(user);
        }).orElseThrow(() -> new RuntimeException("User not found with id " + id));
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    // --- WatchedRelationship CRUD Operations ---

    @Transactional
    public User addOrUpdateWatchedMovie(Long userId, Long movieId, int rating) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found with id: " + movieId));

        Optional<WatchedRelationship> existingWatched = user.getWatchedMovies().stream()
                .filter(w -> w.getMovie().getId().equals(movieId))
                .findFirst();

        if (existingWatched.isPresent()) {
            existingWatched.get().setRating(rating);
        } else {
            WatchedRelationship newWatched = new WatchedRelationship();
            newWatched.setMovie(movie);
            newWatched.setRating(rating);
            user.getWatchedMovies().add(newWatched);
        }
        return userRepository.save(user);
    }

    @Transactional
    public User deleteWatchedMovie(Long userId, Long movieId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        boolean removed = user.getWatchedMovies().removeIf(watched ->
                watched.getMovie().getId().equals(movieId));

        if (!removed) {
            throw new RuntimeException("User " + userId + " did not watch movie " + movieId);
        }
        return userRepository.save(user);
    }

    public Optional<WatchedRelationship> getWatchedRelationship(Long userId, Long movieId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        return user.getWatchedMovies().stream()
                .filter(wr -> wr.getMovie().getId().equals(movieId))
                .findFirst();
    }

    // --- Complex Query Methods ---

    // Complex Query 4
    public List<User> getUsersWhoWatchedDirectorMoviesAndRatedAbove(String directorName, int minRating) {
        return userRepository.findUsersWhoWatchedDirectorMoviesAndRatedAbove(directorName, minRating);
    }

    // Complex Query 5 (Actors by Most Genres) - KORISTIMO Neo4jClient
    public List<ActorGenreCountDTO> getActorsByMostGenres(int limit) {
        String cypher = "MATCH (a:Actor)-[:ACTED_IN]->(m:Movie)-[:BELONGS_TO]->(g:Genre) " +
                "WITH a, COLLECT(DISTINCT g.name) AS distinctGenres " +
                "RETURN a.id AS id, a.name AS actorName, SIZE(distinctGenres) AS numberOfGenres " +
                "ORDER BY numberOfGenres DESC " +
                "LIMIT $limit";

        Collection<ActorGenreCountDTO> results = neo4jClient
                .query(cypher)
                .bind(limit).to("limit")
                .fetchAs(ActorGenreCountDTO.class)
                .mappedBy((typeSystem, record) -> {
                    return new ActorGenreCountDTO(
                            record.get("id").asLong(),
                            record.get("actorName").asString(),
                            record.get("numberOfGenres").asInt()
                    );
                })
                .all();

        return results.stream().toList();
    }
}