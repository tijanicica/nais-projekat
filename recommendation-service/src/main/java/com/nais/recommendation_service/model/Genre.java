package com.nais.recommendation_service.model;
import org.springframework.data.neo4j.core.schema.*;

@Node("Genre")
public class Genre {
    @Id @GeneratedValue
    private Long id;
    private String name;
    // Getters and Setters
}
