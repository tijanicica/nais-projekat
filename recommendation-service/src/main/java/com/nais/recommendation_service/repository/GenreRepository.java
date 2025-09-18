package com.nais.recommendation_service.repository;

import com.nais.recommendation_service.model.Genre;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface GenreRepository extends Neo4jRepository<Genre, Long> {}
