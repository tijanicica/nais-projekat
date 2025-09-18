package com.nais.recommendation_service.repository;
import com.nais.recommendation_service.model.Actor;
import org.springframework.data.neo4j.repository.Neo4jRepository;
public interface ActorRepository extends Neo4jRepository<Actor, Long> {}