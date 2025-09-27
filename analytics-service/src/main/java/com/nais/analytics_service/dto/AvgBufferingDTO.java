package com.nais.analytics_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvgBufferingDTO {
    private String region;
    private String deviceType;
    private Double averageBufferingMs;
}