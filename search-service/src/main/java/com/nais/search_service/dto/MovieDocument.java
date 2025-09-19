package com.nais.search_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// MovieDocument.java
@Data // Lombok
@AllArgsConstructor
@NoArgsConstructor
public class MovieDocument {
    private String title;
    //vektorizovano polje
    private String description;
    private Integer releaseYear;
    private String genre;
}
