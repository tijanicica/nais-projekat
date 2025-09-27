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
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

@Service
public class MovieService {
    private final MovieRepository movieRepository;
    private final ActorRepository actorRepository;
    private final DirectorRepository directorRepository;
    private final GenreRepository genreRepository;
    private final Neo4jClient neo4jClient; // Dodajemo Neo4jClient

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
        // Vratite ga ponovo učitanog sa svim relacijama
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
            // Vratite ga ponovo učitanog sa svim relacijama
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

    // --- Complex Query Methods ---

    public List<TopRatedMovieDTO> getTopNMoviesByAverageRating(int limit) {
        String cypher = "MATCH (u:User)-[w:WATCHED]->(m:Movie) " +
                "WITH m, AVG(w.rating) AS averageRating " +
                "WHERE averageRating IS NOT NULL " +
                "RETURN m.id AS movieId, elementId(m) AS elementId, averageRating " + // Vrati movieId i elementId
                "ORDER BY averageRating DESC " +
                "LIMIT $limit";

        Collection<MovieRatingResult> topRatedResults = neo4jClient
                .query(cypher)
                .bind(limit).to("limit")
                .fetchAs(MovieRatingResult.class)
                .mappedBy((typeSystem, record) -> {
                    return new MovieRatingResult(
                            record.get("movieId").asLong(), // Long id
                            record.get("elementId").asString(), //element ID
                            record.get("averageRating").asDouble()
                    );
                })
                .all();

        System.out.println("topRatedResults: " + topRatedResults);

        // Then, get the full movie details for each
        List<TopRatedMovieDTO> result = new ArrayList<>();
        for (MovieRatingResult ratingResult : topRatedResults) {
            Optional<Movie> movieOpt = movieRepository.findByIdWithAllRelationships(ratingResult.getMovieId());//PROMENI NA GETMOVIEID!!!!
            System.out.println("Obradjujem movieId: " + ratingResult.getElementId());

            if (movieOpt.isPresent()) {
                Movie movie = movieOpt.get();
                System.out.println("Film pronadjen: " + movie);
                TopRatedMovieDTO dto = new TopRatedMovieDTO();
                dto.setId(movie.getId());
                dto.setTitle(movie.getTitle());
                dto.setReleaseYear(movie.getReleaseYear());
                dto.setDurationMinutes(movie.getDurationMinutes());
                dto.setAverageRating(ratingResult.getAverageRating());
                dto.setGenres(movie.getGenres() != null ? new ArrayList<>(movie.getGenres()) : new ArrayList<>());
                dto.setActors(movie.getActors() != null ? new ArrayList<>(movie.getActors()) : new ArrayList<>());
                dto.setDirector(movie.getDirector());
                result.add(dto);
            } else {
                System.out.println("Film NIJE pronadjen sa ID: " + ratingResult.getElementId());
            }
        }
        return result;
    }
    // Complex CRUD 1
    @Transactional
    public List<Movie> incrementMovieReleaseYearByDirector(Long directorId, Integer increment) {
        return movieRepository.incrementMovieReleaseYearByDirector(directorId, increment);
    }

    // Complex CRUD 2
    @Transactional
    public List<Movie> addGenreToMoviesReleasedBeforeYear(Long genreId, Integer year) {
        return movieRepository.addGenreToMoviesReleasedBeforeYear(genreId, year);
    }
}