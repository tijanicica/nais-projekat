package com.nais.report_service.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class ViewingHistoryDTO {
    private Key key;
    private Long movieId;
    private int stoppedAtSeconds;

    @Data
    public static class Key {
        private Long userId;
        private Instant viewedAt;
    }
}
