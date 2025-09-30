package com.nais.recommendation_service.service;

import com.nais.recommendation_service.model.*;
import com.nais.recommendation_service.repository.MovieRepository;
import com.nais.recommendation_service.repository.UserRepository;
import com.nais.recommendation_service.dto.ActorGenreCountDTO;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
// Dodaj ove import-e na vrh UserService klase

import org.neo4j.driver.internal.InternalNode;
import org.neo4j.driver.internal.InternalRelationship;
import java.util.stream.Collectors;
import java.util.ArrayList;

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

    // Kompleksni Upit 2 : Korisnici koji su gledali filmove režisera i ocenili ih iznad X
    public List<User> getUsersWhoWatchedDirectorMoviesAndRatedAbove(String directorName, int minRating) {
        String cypher = "MATCH (u:User)-[w:WATCHED]->(m:Movie)<-[:DIRECTED]-(d:Director) " +
                "WHERE d.name = $directorName AND w.rating > $minRating " +
                "RETURN DISTINCT u.id AS userId, u.username AS username";

        Collection<Map<String, Object>> userResults = neo4jClient
                .query(cypher)
                .bind(directorName).to("directorName")
                .bind(minRating).to("minRating")
                .fetch().all();

        List<User> users = new ArrayList<>();
        for (Map<String, Object> result : userResults) {
            Long userId = ((Number) result.get("userId")).longValue();
            String username = (String) result.get("username");

            // Kreiraj User objekat
            User user = new User();
            user.setId(userId);
            user.setUsername(username);

            // Učitaj samo watched movies sa ocenom višom od minRating za određenog režisera
            Set<WatchedRelationship> watchedMovies = loadWatchedMoviesForUserWithRatingFilter(userId, directorName, minRating);
            user.setWatchedMovies(watchedMovies);

            users.add(user);
        }

        return users;
    }

    // Nova metoda koja filtrira po oceni i režiseru
    private Set<WatchedRelationship> loadWatchedMoviesForUserWithRatingFilter(Long userId, String directorName, int minRating) {
        String cypher = "MATCH (u:User)-[w:WATCHED]->(m:Movie)<-[:DIRECTED]-(d:Director) " +
                "WHERE u.id = $userId AND d.name = $directorName AND w.rating > $minRating " +
                "OPTIONAL MATCH (m)-[:BELONGS_TO]->(g:Genre) " +
                "OPTIONAL MATCH (a:Actor)-[:ACTED_IN]->(m) " +
                "RETURN w.rating AS rating, " +
                "m.id AS movieId, m.title AS movieTitle, m.releaseYear AS movieYear, m.durationMinutes AS movieDuration, " +
                "COLLECT(DISTINCT {id: g.id, name: g.name}) AS genres, " +
                "COLLECT(DISTINCT {id: a.id, name: a.name}) AS actors, " +
                "d.id AS directorId, d.name AS directorName";

        return neo4jClient.query(cypher)
                .bind(userId).to("userId")
                .bind(directorName).to("directorName")
                .bind(minRating).to("minRating")
                .fetch().all()
                .stream()
                .map(record -> {
                    WatchedRelationship watchedRel = new WatchedRelationship();

                    try {
                        // Mapiranje rating-a
                        Object ratingObj = record.get("rating");
                        if (ratingObj != null) {
                            watchedRel.setRating(((Number) ratingObj).intValue());
                        }

                        // Kreiraj Movie objekat
                        Movie movie = new Movie();
                        Object movieIdObj = record.get("movieId");
                        if (movieIdObj != null) {
                            movie.setId(((Number) movieIdObj).longValue());
                            movie.setTitle((String) record.get("movieTitle"));
                            movie.setReleaseYear(((Number) record.get("movieYear")).intValue());
                            movie.setDurationMinutes(((Number) record.get("movieDuration")).longValue());
                        }

                        // Mapiranje genres
                        Set<Genre> genres = new HashSet<>();
                        List<Map<String, Object>> genreList = (List<Map<String, Object>>) record.get("genres");
                        if (genreList != null) {
                            for (Map<String, Object> genreMap : genreList) {
                                Object genreIdObj = genreMap.get("id");
                                String genreName = (String) genreMap.get("name");
                                if (genreIdObj != null && genreName != null) {
                                    Genre genre = new Genre();
                                    genre.setId(((Number) genreIdObj).longValue());
                                    genre.setName(genreName);
                                    genres.add(genre);
                                }
                            }
                        }
                        movie.setGenres(genres);

                        // Mapiranje actors
                        Set<Actor> actors = new HashSet<>();
                        List<Map<String, Object>> actorList = (List<Map<String, Object>>) record.get("actors");
                        if (actorList != null) {
                            for (Map<String, Object> actorMap : actorList) {
                                Object actorIdObj = actorMap.get("id");
                                String actorName = (String) actorMap.get("name");
                                if (actorIdObj != null && actorName != null) {
                                    Actor actor = new Actor();
                                    actor.setId(((Number) actorIdObj).longValue());
                                    actor.setName(actorName);
                                    actors.add(actor);
                                }
                            }
                        }
                        movie.setActors(actors);

                        // Mapiranje director
                        Object directorIdObj = record.get("directorId");
                        String directorNameFromDb = (String) record.get("directorName");
                        if (directorIdObj != null && directorNameFromDb != null) {
                            Director director = new Director();
                            director.setId(((Number) directorIdObj).longValue());
                            director.setName(directorNameFromDb);
                            movie.setDirector(director);
                        }

                        watchedRel.setMovie(movie);

                    } catch (Exception e) {
                        System.err.println("Error mapping watched relationship: " + e.getMessage());
                        e.printStackTrace();
                    }

                    return watchedRel;
                })
                .filter(w -> w.getMovie() != null) // Filtriraj null movie objekte
                .collect(Collectors.toSet());
    }


    // Kompleksni Upit 3 : Glumci sa najviše žanrova filmova, vraca punu listu za filmove
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

        return new ArrayList<>(results);
    }
}