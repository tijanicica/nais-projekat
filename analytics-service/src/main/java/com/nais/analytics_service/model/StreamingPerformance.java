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
    private String region;

    @Column
    private Long bufferingTimeMs;

    @Column
    private Integer bitrateKbps;

    @Column(timestamp = true)
    private Instant time;
}
