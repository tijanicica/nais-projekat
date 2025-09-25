package com.nais.analytics_service.repository;

import com.influxdb.query.FluxTable;
import com.nais.analytics_service.model.StreamingPerformance;
import com.nais.analytics_service.model.UserInteraction;
import java.time.OffsetDateTime;
import java.util.List;

public interface AnalyticsRepository {

    // create
    void save(StreamingPerformance data);
    void save(UserInteraction data);

    // delete
    void delete(OffsetDateTime start, OffsetDateTime stop, String predicate);

    // slozeni upiti
    // Upit 1: Prosečno vreme baferovanja po regionu i tipu uređaja
    List<FluxTable> findAvgBufferingByRegionAndDevice(String timeRange);

    // Upit 2: Broj korisnika sa niskim bitrate-om
    List<FluxTable> findUsersWithLowBitrate(String timeRange, int bitrateThreshold);

    // Upit 3: Top N najčešćih interakcija po filmu
    List<FluxTable> findTopInteractionCombinations(String timeRange, int limit);
}