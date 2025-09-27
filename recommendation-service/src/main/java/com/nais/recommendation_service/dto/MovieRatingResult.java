package com.nais.recommendation_service.dto;

import lombok.Data;

@Data
public class MovieRatingResult {
    private Long movieId;
    private String elementId;
    private Double averageRating;

    public MovieRatingResult() {}

    public MovieRatingResult(Long movieId,String elementId, Double averageRating) {
        this.movieId = movieId;
        this.elementId = elementId;
        this.averageRating = averageRating;
    }
}