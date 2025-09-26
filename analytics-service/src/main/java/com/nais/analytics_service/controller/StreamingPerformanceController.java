package com.nais.analytics_service.controller;

import com.influxdb.query.FluxTable;
import com.nais.analytics_service.dto.AvgBufferingDTO;
import com.nais.analytics_service.dto.LowBitrateUserDTO;
import com.nais.analytics_service.model.StreamingPerformance;
import com.nais.analytics_service.service.StreamingPerformanceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/analytics/performance")
public class StreamingPerformanceController {

    private final StreamingPerformanceService performanceService;

    public StreamingPerformanceController(StreamingPerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    @PostMapping
    public ResponseEntity<Void> recordPerformanceEvent(@RequestBody StreamingPerformance event) {
        performanceService.recordPerformance(event);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @DeleteMapping
    public ResponseEntity<Void> deletePerformanceData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime stop,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String movieId) {

        String predicate = buildPredicate("_measurement=\"streaming_performance\"", userId, movieId);
        if (!predicate.contains("AND")) {
            return ResponseEntity.badRequest().body(null); // Ne dozvoljavamo brisanje bez filtera
        }
        performanceService.deletePerformanceData(start, stop, predicate);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/avg-buffering/detailed")
    public List<AvgBufferingDTO> getAvgBufferingDetailed(@RequestParam(defaultValue = "24h") String range) {
        return performanceService.getAvgBufferingByRegionAndDevice(range);
    }

    @GetMapping("/low-bitrate-users/list")
    public List<LowBitrateUserDTO> getLowBitrateUsersCount(
            @RequestParam(defaultValue = "1h") String range,
            @RequestParam(defaultValue = "1000") int threshold) {
        return performanceService.getUsersWithLowBitrate(range, threshold);
    }

    private String buildPredicate(String basePredicate, String userId, String movieId) {
        StringBuilder sb = new StringBuilder(basePredicate);
        if (userId != null && !userId.isEmpty()) sb.append(String.format(" AND userId=\"%s\"", userId));
        if (movieId != null && !movieId.isEmpty()) sb.append(String.format(" AND movieId=\"%s\"", movieId));
        return sb.toString();
    }
}