package com.nais.history_service.dto;

import lombok.Data;

@Data
public class ViewingActivityDTO {
    private Long userId;
    private Long movieId;
    private String movieTitle;
    private int stoppedAtSeconds;
    private String deviceType;
}