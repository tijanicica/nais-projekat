package com.nais.recommendation_service.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.*;

@RelationshipProperties
@Getter
@Setter
public class WatchedRelationship {
    @Id @GeneratedValue
    private Long id;

    @Property("rating")
    private int rating; // Ocena od 1 do 10

    @TargetNode
    private Movie movie;

    // Getters and Setters
}
