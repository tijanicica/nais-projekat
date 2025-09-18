package com.nais.analytics_service.controller;

import com.influxdb.query.FluxTable;
import com.nais.analytics_service.model.StreamingPerformance;
import com.nais.analytics_service.service.AnalyticsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }
    // ... konstruktor

    @PostMapping("/events")
    public void recordEvent(@RequestBody StreamingPerformance event) {
        analyticsService.writePerformanceData(event);
    }

    @GetMapping("/avg-buffering")
    public List<FluxTable> getAvgBuffering(@RequestParam(defaultValue = "1h") String range) {
        return analyticsService.getAverageBufferingTimeByRegion(range);
    }
}
