package com.nais.recommendation_service.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.*;



@Node("Actor")
@Getter
@Setter
public class Actor {
    @Id @GeneratedValue
    private Long id;
    private String name;
    // Getters and Setters
}

