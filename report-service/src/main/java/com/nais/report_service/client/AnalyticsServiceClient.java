package com.nais.report_service.client;
import com.nais.report_service.dto.LowBitrateExperienceDTO;
import com.nais.report_service.dto.ViewingHistoryDTO;
import com.nais.report_service.dto.ViewingProgressDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "analytics-service")
public interface AnalyticsServiceClient {

    @GetMapping("/analytics/performance/low-bitrate-users/list")
    List<LowBitrateExperienceDTO> getLowBitrateUsers(
            @RequestParam(value = "range", defaultValue = "30d") String range,
            @RequestParam(value = "threshold", defaultValue = "1500") int threshold
    );
}