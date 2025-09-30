package com.nais.recommendation_service.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.*;

import java.time.LocalDateTime;

@RelationshipProperties
@Getter
@Setter
public class WatchedRelationship {
    @Id
    @GeneratedValue
    private Long id;

    @Property("rating")
    private int rating; // Ocena od 1 do 10


    @Property("watchedAt")
    private LocalDateTime watchedAt = LocalDateTime.now();

    @TargetNode
    private Movie movie;
}