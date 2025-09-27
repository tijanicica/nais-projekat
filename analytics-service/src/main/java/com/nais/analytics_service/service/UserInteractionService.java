package com.nais.analytics_service.service;

import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.nais.analytics_service.dto.TopInteractionDTO;
import com.nais.analytics_service.model.UserInteraction;
import com.nais.analytics_service.repository.AnalyticsRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserInteractionService {

    private final AnalyticsRepository analyticsRepository;

    public UserInteractionService(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

     public void recordInteraction(UserInteraction data) {
        if (data.getTime() == null) {
            data.setTime(Instant.now());
        }
        analyticsRepository.save(data);
    }
    public void deleteInteractionData(OffsetDateTime start, OffsetDateTime stop, String predicate) {
        analyticsRepository.delete(start, stop, predicate);
    }


    public List<TopInteractionDTO> getTopInteractionCombinations(String timeRange, int limit) {
        List<FluxTable> fluxTables = analyticsRepository.findTopInteractionCombinations(timeRange, limit);

        List<TopInteractionDTO> results = new ArrayList<>();
        for (FluxTable table : fluxTables) {
            for (FluxRecord record : table.getRecords()) {
                String movieId = (String) record.getValueByKey("movieId");
                String interactionType = (String) record.getValueByKey("interactionType");
                Long count = (Long) record.getValueByKey("_value");

                results.add(new TopInteractionDTO(movieId, interactionType, count));
            }
        }
        return results;
    }
}