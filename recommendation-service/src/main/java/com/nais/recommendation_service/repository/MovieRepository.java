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
            "RETURN m, COLLECT(DISTINCT g) AS genres, COLLECT(DISTINCT a) AS actors, head(COLLECT(DISTINCT d)) AS director")
    Optional<Movie> findByIdWithAllRelationships(Long id);

    @Query("MATCH (m:Movie) " +
            "OPTIONAL MATCH (m)-[:BELONGS_TO]->(g:Genre) " +
            "OPTIONAL MATCH (a:Actor)-[:ACTED_IN]->(m) " +
            "OPTIONAL MATCH (m)<-[:DIRECTED]-(d:Director) " +
            "RETURN m, COLLECT(DISTINCT g) AS genres, COLLECT(DISTINCT a) AS actors,  head(COLLECT(DISTINCT d)) AS director")
    List<Movie> findAllWithAllRelationships();


    @Query("MATCH (d:Director)-[:DIRECTED]->(m:Movie) " +
            "WHERE d.id = $directorId " +
            "SET m.releaseYear = m.releaseYear + $increment")
    void updateMovieYearsByDirector(Long directorId, Integer increment);

    @Query("MATCH (d:Director)-[:DIRECTED]->(m:Movie) " +
            "WHERE d.id = $directorId " +
            "OPTIONAL MATCH (m)-[:BELONGS_TO]->(g:Genre) " +
            "OPTIONAL MATCH (a:Actor)-[:ACTED_IN]->(m) " +
            "OPTIONAL MATCH (m)<-[:DIRECTED]-(dir:Director) " +
            "RETURN m, COLLECT(DISTINCT g) AS genres, COLLECT(DISTINCT a) AS actors, dir AS director")
    List<Movie> findMoviesByDirectorId(Long directorId);

    @Query("MATCH (g:Genre) WHERE id(g) = $genreId " +
            "WITH g " +
            "MATCH (m:Movie) WHERE m.releaseYear < $year " +
            "MERGE (m)-[:BELONGS_TO]->(g) " +
            "RETURN m")
    List<Movie> addGenreToMoviesReleasedBeforeYear(Long genreId, Integer year);
}