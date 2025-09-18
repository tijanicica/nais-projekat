package com.nais.recommendation_service.repository;

import com.nais.recommendation_service.model.Movie;
import org.springframework.data.neo4j.repository.Neo4jRepository;
public interface MovieRepository extends Neo4jRepository<Movie, Long> {}