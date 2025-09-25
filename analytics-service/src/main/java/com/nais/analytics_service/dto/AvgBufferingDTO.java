package com.nais.analytics_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Lombok anotacije za manje pisanja koda
@Data // Generiše gettere, settere, toString, equals, hashCode
@NoArgsConstructor // Generiše prazan konstruktor
@AllArgsConstructor // Generiše konstruktor sa svim argumentima
public class AvgBufferingDTO {
    private String region;
    private String deviceType;
    private Double averageBufferingMs; // Dajemo mu lepo, čitljivo ime
}