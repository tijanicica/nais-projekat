package com.nais.recommendation_service.repository;

import com.nais.recommendation_service.model.User;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends Neo4jRepository<User, Long> {

    // Kompleksni upit 4: Korisnici koji su gledali filmove režisera i ocenili ih iznad X
//    @Query("MATCH (u:User)-[w:WATCHED]->(m:Movie)<-[:DIRECTED]-(d:Director) " +
//            "WHERE d.name = $directorName AND w.rating > $minRating " +
//            "RETURN DISTINCT u")
//    List<User> findUsersWhoWatchedDirectorMoviesAndRatedAbove(String directorName, int minRating);
//
//    // Kompleksni upit 5: Glumci koji su glumili u filmovima najviše žanrova
//    @Query("MATCH (a:Actor)-[:ACTED_IN]->(m:Movie)-[:BELONGS_TO]->(g:Genre) " +
//            "WITH a, COLLECT(DISTINCT g.name) AS distinctGenres " +
//            "RETURN a.name AS actorName, SIZE(distinctGenres) AS numberOfGenres " +
//            "ORDER BY numberOfGenres DESC " +
//            "LIMIT $limit")
//    List<ActorGenreCountProjection> findActorsByMostGenres(int limit);
//
//    // Projekcioni interfejs za gornji upit, za vraćanje imena glumca i broja žanrova
//    interface ActorGenreCountProjection {
//        String getActorName();
//        Integer getNumberOfGenres();
//    }
}