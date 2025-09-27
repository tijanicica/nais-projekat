package com.nais.history_service.dto;
import lombok.Data;

@Data
public class UpdateProgressDTO {
    private Long userId;
    private Long movieId;
    private int newProgressSeconds;
}