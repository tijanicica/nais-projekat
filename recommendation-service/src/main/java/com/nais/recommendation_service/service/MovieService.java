package com.nais.recommendation_service.service;

import com.nais.recommendation_service.dto.TopRatedMovieDTO;
import com.nais.recommendation_service.dto.MovieRatingResult;
import com.nais.recommendation_service.model.Actor;
import com.nais.recommendation_service.model.Director;
import com.nais.recommendation_service.model.Genre;
import com.nais.recommendation_service.model.Movie;
import com.nais.recommendation_service.repository.ActorRepository;
import com.nais.recommendation_service.repository.DirectorRepository;
import com.nais.recommendation_service.repository.GenreRepository;
import com.nais.recommendation_service.repository.MovieRepository;
import org.neo4j.driver.internal.InternalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Dodaj ove import-e na vrh MovieService klase

import org.neo4j.driver.internal.InternalNode;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MovieService {
    private final MovieRepository movieRepository;
    private final ActorRepository actorRepository;
    private final DirectorRepository directorRepository;
    private final GenreRepository genreRepository;
    private final Neo4jClient neo4jClient;
    private static final Logger logger = LoggerFactory.getLogger(MovieService.class);



    public MovieService(MovieRepository movieRepository, ActorRepository actorRepository,
                        DirectorRepository directorRepository, GenreRepository genreRepository,
                        Neo4jClient neo4jClient) {
        this.movieRepository = movieRepository;
        this.actorRepository = actorRepository;
        this.directorRepository = directorRepository;
        this.genreRepository = genreRepository;
        this.neo4jClient = neo4jClient;
    }

    @Transactional
    public Movie createMovie(Movie movie) {
        // Obrada režisera
        if (movie.getDirector() != null) {
            if (movie.getDirector().getId() == null) {
                movie.setDirector(directorRepository.save(movie.getDirector()));
            } else {
                movie.setDirector(directorRepository.findById(movie.getDirector().getId())
                        .orElseThrow(() -> new RuntimeException("Director not found: " + movie.getDirector().getId())));
            }
        }

        // Obrada glumaca
        Set<Actor> managedActors = new HashSet<>();
        if (movie.getActors() != null && !movie.getActors().isEmpty()) {
            for (Actor actor : movie.getActors()) {
                if (actor.getId() == null) {
                    managedActors.add(actorRepository.save(actor));
                } else {
                    managedActors.add(actorRepository.findById(actor.getId())
                            .orElseThrow(() -> new RuntimeException("Actor not found: " + actor.getId())));
                }
            }
        }
        movie.setActors(managedActors);

        // Obrada žanrova
        Set<Genre> managedGenres = new HashSet<>();
        if (movie.getGenres() != null && !movie.getGenres().isEmpty()) {
            for (Genre genre : movie.getGenres()) {
                if (genre.getId() == null) {
                    managedGenres.add(genreRepository.save(genre));
                } else {
                    managedGenres.add(genreRepository.findById(genre.getId())
                            .orElseThrow(() -> new RuntimeException("Genre not found: " + genre.getId())));
                }
            }
        }
        movie.setGenres(managedGenres);

        Movie savedMovie = movieRepository.save(movie);

        return movieRepository.findByIdWithAllRelationships(savedMovie.getId())
                .orElse(savedMovie);
    }

    public Optional<Movie> getMovieById(Long id) {
        return movieRepository.findByIdWithAllRelationships(id); // Koristi metodu koja učitava sve relacije
    }

    public List<Movie> getAllMovies() {
        return movieRepository.findAllWithAllRelationships(); // Koristi metodu koja učitava sve relacije
    }

    @Transactional
    public Movie updateMovie(Long id, Movie movieDetails) {
        return movieRepository.findById(id).map(movie -> {
            movie.setTitle(movieDetails.getTitle());
            movie.setReleaseYear(movieDetails.getReleaseYear());
            movie.setDurationMinutes(movieDetails.getDurationMinutes());

            // Ažuriranje režisera
            if (movieDetails.getDirector() != null) {
                if (movieDetails.getDirector().getId() == null) {
                    movie.setDirector(directorRepository.save(movieDetails.getDirector()));
                } else {
                    movie.setDirector(directorRepository.findById(movieDetails.getDirector().getId())
                            .orElseThrow(() -> new RuntimeException("Director not found for update: " + movieDetails.getDirector().getId())));
                }
            } else {
                movie.setDirector(null);
            }

            // Ažuriranje glumaca
            Set<Actor> updatedActors = new HashSet<>();
            if (movieDetails.getActors() != null && !movieDetails.getActors().isEmpty()) {
                for (Actor actorDetail : movieDetails.getActors()) {
                    if (actorDetail.getId() == null) {
                        updatedActors.add(actorRepository.save(actorDetail));
                    } else {
                        updatedActors.add(actorRepository.findById(actorDetail.getId())
                                .orElseThrow(() -> new RuntimeException("Actor not found for update: " + actorDetail.getId())));
                    }
                }
            }
            movie.setActors(updatedActors);

            // Ažuriranje žanrova
            Set<Genre> updatedGenres = new HashSet<>();
            if (movieDetails.getGenres() != null && !movieDetails.getGenres().isEmpty()) {
                for (Genre genreDetail : movieDetails.getGenres()) {
                    if (genreDetail.getId() == null) {
                        updatedGenres.add(genreRepository.save(genreDetail));
                    } else {
                        updatedGenres.add(genreRepository.findById(genreDetail.getId())
                                .orElseThrow(() -> new RuntimeException("Genre not found for update: " + genreDetail.getId())));
                    }
                }
            }
            movie.setGenres(updatedGenres);

            Movie updatedMovie = movieRepository.save(movie);

            return movieRepository.findByIdWithAllRelationships(updatedMovie.getId())
                    .orElse(updatedMovie);
        }).orElseThrow(() -> new RuntimeException("Movie not found with id " + id));
    }

    public void deleteMovie(Long id) {
        movieRepository.deleteById(id);
    }

    // --- Relationship Management Methods ---

    @Transactional
    public Movie addActorToMovie(Long movieId, Long actorId) {
        Movie movie = movieRepository.findById(movieId).orElseThrow(() -> new RuntimeException("Movie not found"));
        Actor actor = actorRepository.findById(actorId).orElseThrow(() -> new RuntimeException("Actor not found"));
        movie.getActors().add(actor);
        Movie updatedMovie = movieRepository.save(movie);
        return movieRepository.findByIdWithAllRelationships(updatedMovie.getId())
                .orElse(updatedMovie);
    }

    @Transactional
    public Movie removeActorFromMovie(Long movieId, Long actorId) {
        Movie movie = movieRepository.findById(movieId).orElseThrow(() -> new RuntimeException("Movie not found"));
        Actor actorToRemove = actorRepository.findById(actorId).orElseThrow(() -> new RuntimeException("Actor not found"));
        movie.getActors().removeIf(actor -> actor.getId().equals(actorToRemove.getId()));
        Movie updatedMovie = movieRepository.save(movie);
        return movieRepository.findByIdWithAllRelationships(updatedMovie.getId())
                .orElse(updatedMovie);
    }

    @Transactional
    public Movie addGenreToMovie(Long movieId, Long genreId) {
        Movie movie = movieRepository.findById(movieId).orElseThrow(() -> new RuntimeException("Movie not found"));
        Genre genre = genreRepository.findById(genreId).orElseThrow(() -> new RuntimeException("Genre not found"));
        movie.getGenres().add(genre);
        Movie updatedMovie = movieRepository.save(movie);
        return movieRepository.findByIdWithAllRelationships(updatedMovie.getId())
                .orElse(updatedMovie);
    }

    @Transactional
    public Movie removeGenreFromMovie(Long movieId, Long genreId) {
        Movie movie = movieRepository.findById(movieId).orElseThrow(() -> new RuntimeException("Movie not found"));
        Genre genreToRemove = genreRepository.findById(genreId).orElseThrow(() -> new RuntimeException("Genre not found"));
        movie.getGenres().removeIf(genre -> genre.getId().equals(genreToRemove.getId()));
        Movie updatedMovie = movieRepository.save(movie);
        return movieRepository.findByIdWithAllRelationships(updatedMovie.getId())
                .orElse(updatedMovie);
    }

    @Transactional
    public Movie setDirectorForMovie(Long movieId, Long directorId) {
        Movie movie = movieRepository.findById(movieId).orElseThrow(() -> new RuntimeException("Movie not found"));
        Director director = directorRepository.findById(directorId).orElseThrow(() -> new RuntimeException("Director not found"));
        movie.setDirector(director);
        Movie updatedMovie = movieRepository.save(movie);
        return movieRepository.findByIdWithAllRelationships(updatedMovie.getId())
                .orElse(updatedMovie);
    }

    @Transactional
    public Movie removeDirectorFromMovie(Long movieId) {
        Movie movie = movieRepository.findById(movieId).orElseThrow(() -> new RuntimeException("Movie not found"));
        movie.setDirector(null);
        Movie updatedMovie = movieRepository.save(movie);
        return movieRepository.findByIdWithAllRelationships(updatedMovie.getId())
                .orElse(updatedMovie);
    }

//Kompleksni Upit 1 : Pronađi Top N filmova po prosečnoj oceni
    public List<TopRatedMovieDTO> getTopNMoviesByAverageRating(int limit) {
        String cypher = "MATCH (u:User)-[w:WATCHED]->(m:Movie) " +
                "OPTIONAL MATCH (m)-[:BELONGS_TO]->(g:Genre) " +
                "OPTIONAL MATCH (a:Actor)-[:ACTED_IN]->(m) " +
                "OPTIONAL MATCH (m)<-[:DIRECTED]-(d:Director) " +
                "WITH m, AVG(w.rating) AS averageRating, COLLECT(DISTINCT g) AS genres, COLLECT(DISTINCT a) AS actors, head(COLLECT(DISTINCT d)) AS director " +
                "WHERE averageRating IS NOT NULL " +
                "RETURN m.id AS movieId, m.title AS title, m.releaseYear AS releaseYear, m.durationMinutes AS durationMinutes, averageRating, genres, actors, director " +
                "ORDER BY averageRating DESC " +
                "LIMIT $limit";

        logger.debug("Executing Cypher query: {}", cypher);

        List<TopRatedMovieDTO> topRatedResults = neo4jClient.query(cypher)
                .bind(limit).to("limit")
                .fetch().all()
                .stream()
                .map(record -> {
                    TopRatedMovieDTO dto = new TopRatedMovieDTO();
                    try {
                        Object movieIdObj = record.get("movieId");
                        if (movieIdObj != null) {
                            dto.setId(((Number) movieIdObj).longValue());
                        } else {
                            logger.warn("movieId is null for record: {}", record.size());
                        }

                        dto.setTitle((String) record.get("title"));
                        dto.setReleaseYear(((Number) record.get("releaseYear")).intValue());
                        dto.setDurationMinutes(((Number) record.get("durationMinutes")).longValue());
                        dto.setAverageRating(((Number) record.get("averageRating")).doubleValue());

                        // Konverzija za žanrove
                        List<Genre> genres = new ArrayList<>();
                        List<Object> genreObjects = (List<Object>) record.get("genres");
                        if (genreObjects != null) {
                            logger.debug("Genres found: {}", genreObjects);
                            for (Object genreObject : genreObjects) {
                                if (genreObject instanceof InternalNode) {
                                    try {
                                        Long genreId = ((InternalNode) genreObject).id();
                                        Optional<Genre> genre = genreRepository.findById(genreId);
                                        genre.ifPresent(genres::add);
                                    } catch (Exception e) {
                                        logger.warn("Could not map genre object: {}", genreObject, e);
                                    }
                                } else {
                                    logger.warn("Genre object is not an InternalNode: {}", genreObject);
                                }
                            }
                        }
                        dto.setGenres(genres);

                        // Konverzija za glumce
                        List<Actor> actors = new ArrayList<>();
                        List<Object> actorObjects = (List<Object>) record.get("actors");
                        if (actorObjects != null) {
                            logger.debug("Actors found: {}", actorObjects);
                            for (Object actorObject : actorObjects) {
                                if (actorObject instanceof InternalNode) {
                                    try {
                                        Long actorId = ((InternalNode) actorObject).id();
                                        Optional<Actor> actor = actorRepository.findById(actorId);
                                        actor.ifPresent(actors::add);
                                    } catch (Exception e) {
                                        logger.warn("Could not map actor object: {}", actorObject, e);
                                    }
                                } else {
                                    logger.warn("Actor object is not an InternalNode: {}", actorObject);
                                }
                            }
                        }
                        dto.setActors(actors);

                        // Konverzija za režisera
                        Object directorObject = record.get("director");
                        Director director = null;
                        if (directorObject instanceof InternalNode) {
                            try {
                                Long directorId = ((InternalNode) directorObject).id();
                                Optional<Director> directorOptional = directorRepository.findById(directorId);
                                director = directorOptional.orElse(null);
                            } catch (Exception e) {
                                logger.warn("Could not map director object: {}", directorObject, e);
                            }
                        } else {
                            logger.warn("Director object is not an InternalNode: {}", directorObject);
                        }
                        dto.setDirector(director);
                    } catch (Exception e) {
                        logger.error("Error mapping record: {}", record.size(), e);
                    }

                    return dto;
                })
                .collect(Collectors.toList());

        logger.debug("Top rated movies: {}", topRatedResults);
        return topRatedResults;
    }


// Kompleksni CRUD Upit 1 : Povećaj godinu izdanja filmova režisera
@Transactional
public List<Movie> incrementMovieReleaseYearByDirector(Long directorId, Integer increment) {
    System.out.println("Director ID: " + directorId + ", Increment: " + increment);

    String cypher = "MATCH (d:Director)-[:DIRECTED]->(m:Movie) " +
            "WHERE d.id = $directorId " +
            "SET m.releaseYear = m.releaseYear + $increment " +
            "WITH m " +
            "OPTIONAL MATCH (m)-[:BELONGS_TO]->(g:Genre) " +
            "OPTIONAL MATCH (a:Actor)-[:ACTED_IN]->(m) " +
            "OPTIONAL MATCH (m)<-[:DIRECTED]-(director:Director) " +
            "RETURN m.id AS movieId, m.title AS movieTitle, m.releaseYear AS movieYear, m.durationMinutes AS movieDuration, " +
            "COLLECT(DISTINCT CASE WHEN g IS NOT NULL THEN {id: g.id, name: g.name} END) AS genres, " +
            "COLLECT(DISTINCT CASE WHEN a IS NOT NULL THEN {id: a.id, name: a.name} END) AS actors, " +
            "director.id AS directorId, director.name AS directorName";

    List<Movie> results = neo4jClient.query(cypher)
            .bind(directorId).to("directorId")
            .bind(increment).to("increment")
            .fetch().all()
            .stream()
            .map(record -> {
                Movie movie = new Movie();
                try {
                    // Mapiranje osnovnih podataka o filmu
                    Object movieIdObj = record.get("movieId");
                    if (movieIdObj != null) {
                        movie.setId(((Number) movieIdObj).longValue());
                        movie.setTitle((String) record.get("movieTitle"));
                        movie.setReleaseYear(((Number) record.get("movieYear")).intValue());
                        movie.setDurationMinutes(((Number) record.get("movieDuration")).longValue());
                    }

                    // Mapiranje žanrova
                    Set<Genre> genres = new HashSet<>();
                    List<Map<String, Object>> genreList = (List<Map<String, Object>>) record.get("genres");
                    if (genreList != null) {
                        for (Map<String, Object> genreMap : genreList) {
                            if (genreMap != null && genreMap.get("id") != null && genreMap.get("name") != null) {
                                Object genreIdObj = genreMap.get("id");
                                String genreName = (String) genreMap.get("name");
                                if (genreIdObj != null && genreName != null && !genreName.trim().isEmpty()) {
                                    Genre genre = new Genre();
                                    genre.setId(((Number) genreIdObj).longValue());
                                    genre.setName(genreName);
                                    genres.add(genre);
                                }
                            }
                        }
                    }
                    movie.setGenres(genres);

                    // Mapiranje glumaca
                    Set<Actor> actors = new HashSet<>();
                    List<Map<String, Object>> actorList = (List<Map<String, Object>>) record.get("actors");
                    if (actorList != null) {
                        for (Map<String, Object> actorMap : actorList) {
                            if (actorMap != null && actorMap.get("id") != null && actorMap.get("name") != null) {
                                Object actorIdObj = actorMap.get("id");
                                String actorName = (String) actorMap.get("name");
                                if (actorIdObj != null && actorName != null && !actorName.trim().isEmpty()) {
                                    Actor actor = new Actor();
                                    actor.setId(((Number) actorIdObj).longValue());
                                    actor.setName(actorName);
                                    actors.add(actor);
                                }
                            }
                        }
                    }
                    movie.setActors(actors);

                    // Mapiranje režisera
                    Object directorIdObj = record.get("directorId");
                    String directorName = (String) record.get("directorName");
                    if (directorIdObj != null && directorName != null) {
                        Director director = new Director();
                        director.setId(((Number) directorIdObj).longValue());
                        director.setName(directorName);
                        movie.setDirector(director);
                    }

                } catch (Exception e) {
                    System.err.println("Error mapping movie: " + e.getMessage());
                    e.printStackTrace();
                }
                return movie;
            })
            .filter(m -> m.getId() != null)
            .collect(Collectors.toList());

    System.out.println("Results count: " + results.size());
    return results;
}
    // Kompleksni CRUD Upit 2 : Dodaj žanr filmovima objavljenim pre određene godine
    @Transactional
    public List<Movie> addGenreToMoviesReleasedBeforeYear(Long genreId, Integer year) {
        System.out.println("Genre ID: " + genreId + ", Year: " + year);

        String cypher = "MATCH (g:Genre) WHERE g.id = $genreId " +
                "WITH g " +
                "MATCH (m:Movie) WHERE m.releaseYear < $year " +
                "MERGE (m)-[:BELONGS_TO]->(g) " +
                "WITH m " +
                "OPTIONAL MATCH (m)-[:BELONGS_TO]->(genre:Genre) " +
                "OPTIONAL MATCH (a:Actor)-[:ACTED_IN]->(m) " +
                "OPTIONAL MATCH (m)<-[:DIRECTED]-(director:Director) " +
                "RETURN m.id AS movieId, m.title AS movieTitle, m.releaseYear AS movieYear, m.durationMinutes AS movieDuration, " +
                "COLLECT(DISTINCT CASE WHEN genre IS NOT NULL THEN {id: genre.id, name: genre.name} END) AS genres, " +
                "COLLECT(DISTINCT CASE WHEN a IS NOT NULL THEN {id: a.id, name: a.name} END) AS actors, " +
                "director.id AS directorId, director.name AS directorName";

        List<Movie> results = neo4jClient.query(cypher)
                .bind(genreId).to("genreId")
                .bind(year).to("year")
                .fetch().all()
                .stream()
                .map(record -> {
                    Movie movie = new Movie();
                    try {
                        // Mapiranje osnovnih podataka o filmu
                        Object movieIdObj = record.get("movieId");
                        if (movieIdObj != null) {
                            movie.setId(((Number) movieIdObj).longValue());
                            movie.setTitle((String) record.get("movieTitle"));
                            movie.setReleaseYear(((Number) record.get("movieYear")).intValue());
                            movie.setDurationMinutes(((Number) record.get("movieDuration")).longValue());
                        }

                        // Mapiranje žanrova
                        Set<Genre> genres = new HashSet<>();
                        List<Map<String, Object>> genreList = (List<Map<String, Object>>) record.get("genres");
                        if (genreList != null) {
                            for (Map<String, Object> genreMap : genreList) {
                                if (genreMap != null && genreMap.get("id") != null && genreMap.get("name") != null) {
                                    Object genreIdObj = genreMap.get("id");
                                    String genreName = (String) genreMap.get("name");
                                    if (genreIdObj != null && genreName != null && !genreName.trim().isEmpty()) {
                                        Genre genre = new Genre();
                                        genre.setId(((Number) genreIdObj).longValue());
                                        genre.setName(genreName);
                                        genres.add(genre);
                                    }
                                }
                            }
                        }
                        movie.setGenres(genres);

                        // Mapiranje glumaca
                        Set<Actor> actors = new HashSet<>();
                        List<Map<String, Object>> actorList = (List<Map<String, Object>>) record.get("actors");
                        if (actorList != null) {
                            for (Map<String, Object> actorMap : actorList) {
                                if (actorMap != null && actorMap.get("id") != null && actorMap.get("name") != null) {
                                    Object actorIdObj = actorMap.get("id");
                                    String actorName = (String) actorMap.get("name");
                                    if (actorIdObj != null && actorName != null && !actorName.trim().isEmpty()) {
                                        Actor actor = new Actor();
                                        actor.setId(((Number) actorIdObj).longValue());
                                        actor.setName(actorName);
                                        actors.add(actor);
                                    }
                                }
                            }
                        }
                        movie.setActors(actors);

                        // Mapiranje režisera
                        Object directorIdObj = record.get("directorId");
                        String directorName = (String) record.get("directorName");
                        if (directorIdObj != null && directorName != null) {
                            Director director = new Director();
                            director.setId(((Number) directorIdObj).longValue());
                            director.setName(directorName);
                            movie.setDirector(director);
                        }

                    } catch (Exception e) {
                        System.err.println("Error mapping movie: " + e.getMessage());
                        e.printStackTrace();
                    }
                    return movie;
                })
                .filter(m -> m.getId() != null)
                .collect(Collectors.toList());

        System.out.println("Results count: " + results.size());
        return results;
    }
}