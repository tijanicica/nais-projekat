package com.nais.analytics_service.model;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import lombok.Data;
import java.time.Instant;

@Data
@Measurement(name = "streaming_performance")
public class StreamingPerformance {
    @Column(tag = true)
    private String userId;
    @Column(tag = true)
    private String movieId;
    @Column(tag = true)
    private String region; // npr. "EU-WEST", "US-EAST"
    @Column(tag = true)
    private String deviceType; // npr. "WEB", "MOBILE", "TV"

    // Polja su stvarne vrednosti koje merimo
    @Column
    private Long bufferingTimeMs; // Vreme baferovanja u milisekundama
    @Column
    private Integer bitrateKbps; // Kvalitet strima
    @Column
    private String resolution; // npr. "1080p", "4K"

    @Column(timestamp = true)
    private Instant time; }
