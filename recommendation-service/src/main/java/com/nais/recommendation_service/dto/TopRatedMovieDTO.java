package com.nais.recommendation_service.dto;

import com.nais.recommendation_service.model.Actor;
import com.nais.recommendation_service.model.Director;
import com.nais.recommendation_service.model.Genre;
import lombok.Data;
import java.util.List;
import java.util.Set;

@Data
public class TopRatedMovieDTO {
    private Long id;
    private String title;
    private Integer releaseYear;
    private Long durationMinutes;
    private Double averageRating;
    private List<Genre> genres;
    private List<Actor> actors;
    private Director director;
}