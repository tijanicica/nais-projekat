package com.nais.analytics_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopInteractionDTO {
    private String movieId;
    private String interactionType;
    private Long count;
}