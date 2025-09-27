package com.nais.analytics_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class FirstWatchEventDTO {
    private String userId;
    private String movieId;
    private String deviceType;
     private Instant eventTime; 
}