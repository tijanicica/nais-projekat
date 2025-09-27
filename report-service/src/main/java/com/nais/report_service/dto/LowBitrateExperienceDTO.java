package com.nais.report_service.dto;

import lombok.Data;

@Data
public class LowBitrateExperienceDTO {
    private String userId;
    private String movieId;
    private Double averageBitrateKbps;
}