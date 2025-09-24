package com.nais.recommendation_service.model;
import lombok.Data;
import org.springframework.data.neo4j.core.schema.*;

@Data
@Node("Genre")
public class Genre {
    @Id
    private Long id;
    private String name;
    // Getters and Setters
}
