package com.nais.search_service.dto;

import lombok.*;

// MovieDocument.java
@Data // Lombok
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class MovieDocumentDto {
    private Long movieId;
    private String title;
    //vektorizovano polje
    private String description;
    private Integer releaseYear;
    private String genre;
}
