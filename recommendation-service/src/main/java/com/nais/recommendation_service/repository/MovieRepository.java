package com.nais.recommendation_service.repository;

import com.nais.recommendation_service.model.Movie;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends Neo4jRepository<Movie, Long> {

    // Kompleksni upit 1: Pronađite N filmova sa najboljim prosečnim ocenama
//    @Query("MATCH (m:Movie)<-[w:WATCHED]-(u:User) " +
//            "WITH m, AVG(w.rating) AS averageRating " +
//            "WHERE averageRating IS NOT NULL " +
//            "RETURN m, averageRating " +
//            "ORDER BY averageRating DESC " +
//            "LIMIT $limit")
//    List<MovieProjection> findTopNMoviesByAverageRating(int limit);
//
//    // Projekcioni interfejs za gornji upit, kako bi se vratili i film i njegova prosečna ocena
//    interface MovieProjection {
//        Movie getMovie();
//        Double getAverageRating();
//    }
//
//    // Kompleksni CRUD upit 1:
//    // Povećajte godinu izdanja filmova koje je režirao određeni režiser
//    @Query("MATCH (d:Director)-[:DIRECTED]->(m:Movie) " +
//            "WHERE d.id = $directorId " +
//            "SET m.releaseYear = m.releaseYear + $increment " +
//            "RETURN m")
//    List<Movie> incrementMovieReleaseYearByDirector(Long directorId, Integer increment);
//
//    // Kompleksni CRUD upit 2:
//    // Dodajte novi žanr svim filmovima objavljenim pre određene godine
//    @Query("MATCH (g:Genre) WHERE g.id = $genreId " +
//            "WITH g " +
//            "MATCH (m:Movie) WHERE m.releaseYear < $year " +
//            "MERGE (m)-[:BELONGS_TO]->(g) " +
//            "RETURN m")
//    List<Movie> addGenreToMoviesReleasedBeforeYear(Long genreId, Integer year);
}