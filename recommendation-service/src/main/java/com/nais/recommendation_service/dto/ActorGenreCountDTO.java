package com.nais.recommendation_service.dto;

import lombok.Data;

@Data
public class ActorGenreCountDTO {
    private Long id;
    private String actorName;
    private Integer numberOfGenres;

    public ActorGenreCountDTO() {}

    public ActorGenreCountDTO(Long id, String actorName, Integer numberOfGenres) {
        this.id = id;
        this.actorName = actorName;
        this.numberOfGenres = numberOfGenres;
    }
}