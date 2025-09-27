package com.nais.recommendation_service.repository;

import com.nais.recommendation_service.model.Director;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DirectorRepository extends Neo4jRepository<Director, Long> {}