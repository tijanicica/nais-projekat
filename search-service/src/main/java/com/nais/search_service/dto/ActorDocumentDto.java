package com.nais.search_service.dto;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ActorDocumentDto {
    private Long actorId;
    // Nevektorizovana polja
    private String name;
    private Integer birthYear;
    private String nationality;

    // Vektorizovano polje
    private String biography;
}
