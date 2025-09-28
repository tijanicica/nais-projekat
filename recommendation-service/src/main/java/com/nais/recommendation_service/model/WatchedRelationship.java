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

    // Napomena: Ako se ne vrši direktno mapiranje watchedAt iz Cypher-a,
    // ova anotacija @Property nije obavezna za ručno dodeljivanje u servisu.
    // Dodajmo je za kompletnost ako se očekuje da se i to snima.
    @Property("watchedAt")
    private LocalDateTime watchedAt = LocalDateTime.now(); // Postavite podrazumevanu vrednost ovde

    @TargetNode
    private Movie movie;
}