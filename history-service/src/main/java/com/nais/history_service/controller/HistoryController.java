package com.nais.history_service.controller;

import com.nais.history_service.model.ViewingHistoryByUser;
import com.nais.history_service.service.HistoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/history")
public class HistoryController {
    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }
    // ... konstruktor

    @PostMapping
    public ViewingHistoryByUser recordActivity(@RequestBody ViewingHistoryByUser activity) {
        return historyService.recordViewingActivity(activity);
    }

    @GetMapping("/user/{userId}")
    public List<ViewingHistoryByUser> getUserHistory(@PathVariable Long userId) {
        return historyService.getHistoryForUser(userId);
    }
}
