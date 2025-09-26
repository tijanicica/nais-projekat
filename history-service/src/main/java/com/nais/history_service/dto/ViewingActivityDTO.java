package com.nais.history_service.dto;

import lombok.Data;

// Ova klasa definiše koje podatke klijent (npr. frontend aplikacija)
// treba da pošalje kada želi da zabeleži aktivnost gledanja.
@Data
public class ViewingActivityDTO {
    private Long userId;
    private Long movieId;
    private String movieTitle;
    private int stoppedAtSeconds;
    private String deviceType;
}