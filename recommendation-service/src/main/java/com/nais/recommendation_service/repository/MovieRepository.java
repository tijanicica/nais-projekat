package com.nais.recommendation_service.repository;

import com.nais.recommendation_service.model.Movie;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MovieRepository extends Neo4jRepository<Movie, Long> {

    // --- Methods for loading movie with all relationships (for CRUD operations) ---
    @Query("MATCH (m:Movie) WHERE m.id = $id " +
            "OPTIONAL MATCH (m)-[:BELONGS_TO]->(g:Genre) " +
            "OPTIONAL MATCH (a:Actor)-[:ACTED_IN]->(m) " +
            "OPTIONAL MATCH (m)<-[:DIRECTED]-(d:Director) " +
            "RETURN m, COLLECT(DISTINCT g) AS genres, COLLECT(DISTINCT a) AS actors, d AS director")
    Optional<Movie> findByIdWithAllRelationships(Long id); // VRATI NA LONG

    @Query("MATCH (m:Movie) " +
            "OPTIONAL MATCH (m)-[:BELONGS_TO]->(g:Genre) " +
            "OPTIONAL MATCH (a:Actor)-[:ACTED_IN]->(m) " +
            "OPTIONAL MATCH (m)<-[:DIRECTED]-(d:Director) " +
            "RETURN m, COLLECT(DISTINCT g) AS genres, COLLECT(DISTINCT a) AS actors, d AS director")
    List<Movie> findAllWithAllRelationships();

    // Complex CRUD Query 1: Increment movie release year by director
    @Query("MATCH (d:Director)-[:DIRECTED]->(m:Movie) " +
            "WHERE d.id = $directorId " +
            "SET m.releaseYear = m.releaseYear + $increment " +
            "RETURN m")
    List<Movie> incrementMovieReleaseYearByDirector(Long directorId, Integer increment);

    // Complex CRUD Query 2: Add new genre to all movies released before certain year
    @Query("MATCH (g:Genre) WHERE g.id = $genreId " +
            "WITH g " +
            "MATCH (m:Movie) WHERE m.releaseYear < $year " +
            "MERGE (m)-[:BELONGS_TO]->(g) " +
            "RETURN m")
    List<Movie> addGenreToMoviesReleasedBeforeYear(Long genreId, Integer year);

    // UKLONILI SMO findTopNMovieIdsByAverageRating - prebacićemo je u service
}