package com.nais.history_service.controller;

import com.nais.history_service.dto.UpdateProgressDTO;
import com.nais.history_service.dto.ViewingActivityDTO;
import com.nais.history_service.model.TopViewedMoviesByMonth;
import com.nais.history_service.model.UserActivityByDevice;
import com.nais.history_service.model.ViewingHistoryByUser;
import com.nais.history_service.model.ViewingProgressByUserMovie;
import com.nais.history_service.service.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.nais.history_service.model.FirstWatchMilestone;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/history") 
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    // --- CREATE ---
    @PostMapping("/record")
    public ResponseEntity<Void> recordActivity(@RequestBody ViewingActivityDTO activityDTO) {
        historyService.recordViewingActivity(
                activityDTO.getUserId(),
                activityDTO.getMovieId(),
                activityDTO.getStoppedAtSeconds(),
                activityDTO.getDeviceType(),
                activityDTO.getMovieTitle()
        );
        return ResponseEntity.ok().build();
    }

    // --- READ ---
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ViewingHistoryByUser>> getHistoryForUser(@PathVariable Long userId) {
        List<ViewingHistoryByUser> history = historyService.getUserViewingHistory(userId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/progress/{userId}")
    public ResponseEntity<List<ViewingProgressByUserMovie>> getProgressForUser(@PathVariable Long userId) {
        List<ViewingProgressByUserMovie> progressList = historyService.getContinueWatchingList(userId);
        return ResponseEntity.ok(progressList);
    }

    @GetMapping("/top-movies")
    public ResponseEntity<List<TopViewedMoviesByMonth>> getTopMovies(@RequestParam(defaultValue = "10") int limit) {
        List<TopViewedMoviesByMonth> topMovies = historyService.getTopMoviesForCurrentMonth(limit);
        return ResponseEntity.ok(topMovies);
    }

    @GetMapping("/devices/{userId}")
    public ResponseEntity<List<UserActivityByDevice>> getDevicesForUser(@PathVariable Long userId) {
        List<UserActivityByDevice> devices = historyService.getUserDeviceActivity(userId);
        return ResponseEntity.ok(devices);
    }

    @GetMapping("/analytics/viewers/{movieId}")
    public ResponseEntity<Long> getUniqueViewers(
            @PathVariable Long movieId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Long count = historyService.getUniqueViewersForMovieOnDate(movieId, date);
        return ResponseEntity.ok(count);
    }

    // --- UPDATE ---
    @PutMapping("/progress")
    public ResponseEntity<Void> updateProgress(@RequestBody UpdateProgressDTO progressDTO) {
        historyService.updateViewingProgress(
                progressDTO.getUserId(),
                progressDTO.getMovieId(),
                progressDTO.getNewProgressSeconds()
        );
        return ResponseEntity.ok().build();
    }

    // --- DELETE ---

    @DeleteMapping("/user/{userId}/event")
    public ResponseEntity<Void> deleteHistoryEvent(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant viewedAt) {
        historyService.deleteViewingHistoryEvent(userId, viewedAt);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/progress/user/{userId}/movie/{movieId}")
    public ResponseEntity<Void> deleteViewingProgress(
            @PathVariable Long userId,
            @PathVariable Long movieId) {
        historyService.deleteViewingProgress(userId, movieId);
        return ResponseEntity.noContent().build();
    }


    @DeleteMapping("/devices/user/{userId}")
    public ResponseEntity<Void> deleteDeviceActivity(
            @PathVariable Long userId,
            @RequestParam String deviceType) {
        historyService.deleteDeviceActivity(userId, deviceType);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/analytics/event/user/{userId}/movie/{movieId}")
    public ResponseEntity<Void> deleteViewingActivityByDate(
            @PathVariable Long userId,
            @PathVariable Long movieId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        historyService.deleteViewingActivityByDate(movieId, date, userId);
        return ResponseEntity.noContent().build();
    }


    @DeleteMapping("/top-movies/counter/movie/{movieId}")
    public ResponseEntity<Void> resetCounter(
            @PathVariable Long movieId,
            @RequestParam String yearMonth) {
        historyService.resetMovieCounterForMonth(yearMonth, movieId);
        return ResponseEntity.noContent().build();
    }


     @GetMapping("/milestones")
    public ResponseEntity<List<FirstWatchMilestone>> getAllMilestones() {
        List<FirstWatchMilestone> milestones = historyService.getAllMilestones();
        return ResponseEntity.ok(milestones);
    }
}