package com.nais.recommendation_service.repository;
import com.nais.recommendation_service.model.User;
import org.springframework.data.neo4j.repository.Neo4jRepository;
public interface UserRepository extends Neo4jRepository<User, Long> {}
