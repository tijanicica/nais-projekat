package com.nais.recommendation_service.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.*;

import java.util.HashSet;
import java.util.Set;


@Getter
@Setter
@Data
@Node("Actor")
public class Actor {
    @Id
    private Long id;
    private String name;

}

