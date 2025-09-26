package com.nais.analytics_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LowBitrateUserDTO {
    private String userId;
    private String movieId; // NOVI PODATAK
    private Double averageBitrateKbps; // PREIMENOVANO radi jasnoće
}