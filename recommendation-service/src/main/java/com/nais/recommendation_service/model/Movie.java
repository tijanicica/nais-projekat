package com.nais.recommendation_service.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.*;
import java.util.HashSet;
import java.util.Set;

@Node("Movie")
@Getter
@Setter
public class Movie {
    @Id
    @GeneratedValue
    private Long id;
    private String title;
    private Integer releaseYear;
    private Long durationMinutes;


    @Relationship(type = "ACTED_IN", direction = Relationship.Direction.INCOMING)
    private Set<Actor> actors = new HashSet<>();

    @Relationship(type = "BELONGS_TO", direction = Relationship.Direction.OUTGOING)
    private Set<Genre> genres = new HashSet<>();

    @Relationship(type = "DIRECTED", direction = Relationship.Direction.INCOMING)
    private Director director;
    // Getters and Setters
}
