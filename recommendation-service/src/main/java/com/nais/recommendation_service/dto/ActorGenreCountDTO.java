package com.nais.recommendation_service.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActorGenreCountDTO {
    private Long id;
    private String actorName;
    private Integer numberOfGenres;
}