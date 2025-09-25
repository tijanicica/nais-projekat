package com.nais.analytics_service.service;

import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.nais.analytics_service.dto.AvgBufferingDTO;
import com.nais.analytics_service.dto.LowBitrateUserDTO;
import com.nais.analytics_service.model.StreamingPerformance;
import com.nais.analytics_service.repository.AnalyticsRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class StreamingPerformanceService {

    private final AnalyticsRepository analyticsRepository;

    public StreamingPerformanceService(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    public void recordPerformance(StreamingPerformance data) {
        data.setTime(Instant.now());
        analyticsRepository.save(data);
    }

    public void deletePerformanceData(OffsetDateTime start, OffsetDateTime stop, String predicate) {
        analyticsRepository.delete(start, stop, predicate);
    }

    public List<AvgBufferingDTO> getAvgBufferingByRegionAndDevice(String timeRange) {
        List<FluxTable> fluxTables = analyticsRepository.findAvgBufferingByRegionAndDevice(timeRange);

        return parseToAvgBufferingDTO(fluxTables);
    }

    private List<AvgBufferingDTO> parseToAvgBufferingDTO(List<FluxTable> fluxTables) {
        List<AvgBufferingDTO> results = new ArrayList<>();

        for (FluxTable table : fluxTables) {
            for (FluxRecord record : table.getRecords()) {
                String region = (String) record.getValueByKey("region");
                String deviceType = (String) record.getValueByKey("deviceType");
                Double avgValue = (Double) record.getValueByKey("_value");

                results.add(new AvgBufferingDTO(region, deviceType, avgValue));
            }
        }
        return results;
    }

    public List<LowBitrateUserDTO> getUsersWithLowBitrate(String timeRange, int bitrateThreshold) {
        List<FluxTable> fluxTables = analyticsRepository.findUsersWithLowBitrate(timeRange, bitrateThreshold);

        List<LowBitrateUserDTO> results = new ArrayList<>();
        for (FluxTable table : fluxTables) {
            for (FluxRecord record : table.getRecords()) {
                String userId = (String) record.getValueByKey("userId");
                String movieId = (String) record.getValueByKey("movieId");
                // InfluxDB može vratiti Double ili Long za 'mean', pa radimo proveru
                Number avgValue = (Number) record.getValueByKey("_value");

                if (avgValue != null) {
                    results.add(new LowBitrateUserDTO(userId, movieId, avgValue.doubleValue()));
                }
            }
        }
        return results;
    }
}