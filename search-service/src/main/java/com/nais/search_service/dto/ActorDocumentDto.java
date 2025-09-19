package com.nais.search_service.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActorDocumentDto {
    // Nevektorizovana polja
    private String name;
    private Integer birthYear;
    private String nationality;

    // Vektorizovano polje
    private String biography;
}
