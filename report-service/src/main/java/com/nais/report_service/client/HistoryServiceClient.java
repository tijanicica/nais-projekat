package com.nais.report_service.client;
import com.nais.report_service.dto.LowBitrateExperienceDTO;
import com.nais.report_service.dto.ViewingHistoryDTO;
import com.nais.report_service.dto.ViewingProgressDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "history-service")
public interface HistoryServiceClient {

    @GetMapping("/history/user/{userId}")
    List<ViewingHistoryDTO> getHistoryForUser(@PathVariable("userId") Long userId);

    @GetMapping("/history/progress/{userId}")
    List<ViewingProgressDTO> getProgressForUser(@PathVariable("userId") Long userId);
}
