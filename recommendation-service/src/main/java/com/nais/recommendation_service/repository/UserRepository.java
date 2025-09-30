package com.nais.recommendation_service.repository;

import com.nais.recommendation_service.dto.ActorGenreCountDTO;
import com.nais.recommendation_service.model.Actor;
import com.nais.recommendation_service.model.User;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends Neo4jRepository<User, Long> {



    @Query("MATCH (u:User)-[w:WATCHED]->(m:Movie)<-[:DIRECTED]-(d:Director) " +
            "WHERE d.name = $directorName AND w.rating > $minRating " +
            "RETURN DISTINCT u")
    List<User> findUsersWhoWatchedDirectorMoviesAndRatedAbove(String directorName, int minRating);
}