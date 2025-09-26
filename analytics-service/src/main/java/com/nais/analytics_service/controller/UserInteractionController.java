package com.nais.analytics_service.controller;

import com.influxdb.query.FluxTable;
import com.nais.analytics_service.dto.TopInteractionDTO;
import com.nais.analytics_service.model.UserInteraction;
import com.nais.analytics_service.service.UserInteractionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/analytics/interactions")
public class UserInteractionController {

    private final UserInteractionService interactionService;

    public UserInteractionController(UserInteractionService interactionService) {
        this.interactionService = interactionService;
    }

    @PostMapping
    public ResponseEntity<Void> recordInteractionEvent(@RequestBody UserInteraction event) {
        interactionService.recordInteraction(event);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteInteractionData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime stop,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String movieId) {

        String predicate = buildPredicate("_measurement=\"user_interaction\"", userId, movieId);
        if (!predicate.contains("AND")) {
            return ResponseEntity.badRequest().body(null);
        }
        interactionService.deleteInteractionData(start, stop, predicate);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/top-combinations")
    public List<TopInteractionDTO> getTopCombinations(
            @RequestParam(defaultValue = "7d") String range,
            @RequestParam(defaultValue = "5") int limit) {
        return interactionService.getTopInteractionCombinations(range, limit);
    }

    private String buildPredicate(String basePredicate, String userId, String movieId) {
        StringBuilder sb = new StringBuilder(basePredicate);
        if (userId != null && !userId.isEmpty()) sb.append(String.format(" AND userId=\"%s\"", userId));
        if (movieId != null && !movieId.isEmpty()) sb.append(String.format(" AND movieId=\"%s\"", movieId));
        return sb.toString();
    }
}