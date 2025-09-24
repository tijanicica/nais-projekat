package com.nais.recommendation_service.model;
import lombok.Data;
import org.springframework.data.neo4j.core.schema.*;
@Data @Node("Director")
public class Director {
    @Id private Long id;
    private String name;
}
