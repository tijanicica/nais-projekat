package com.nais.report_service.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class ViewingProgressDTO {
    private Key key;
    private int progressSeconds;
    private Instant lastWatchedAt;

    @Data
    public static class Key {
        private Long userId;
        private Long movieId;
    }
}
