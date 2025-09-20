package com.nais.analytics_service.model;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import lombok.Data;

import java.time.Instant;

@Data
@Measurement(name = "user_interaction")
public class UserInteraction {
    @Column(tag = true)
    private String userId;
    @Column(tag = true)
    private String movieId;
    @Column(tag = true)
    private String deviceType;
    @Column(tag = true)
    private String interactionType;  //PLAY, PAUSE, SEEK_FORWARD

    @Column
    private Long videoTimestampSec; //na kojoj sekundi se desila interakcija

    @Column(timestamp = true)
    private Instant time;
}
