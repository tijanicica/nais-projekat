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

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/history") // Vraćamo na ispravan prefiks sa /api
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

    /**
     * DELETE operacija za 'viewing_history_by_user'.
     * Primer poziva: DELETE /api/history/user/1001/event?viewedAt=2025-09-26T10:15:30.00Z
     */
    @DeleteMapping("/user/{userId}/event")
    public ResponseEntity<Void> deleteHistoryEvent(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant viewedAt) {
        historyService.deleteViewingHistoryEvent(userId, viewedAt);
        return ResponseEntity.noContent().build();
    }

    /**
     * DODATO: DELETE operacija za 'viewing_progress_by_user_movie'.
     * Briše film iz "Continue Watching" liste.
     * Primer poziva: DELETE /api/history/progress/user/1001/movie/2
     */
    @DeleteMapping("/progress/user/{userId}/movie/{movieId}")
    public ResponseEntity<Void> deleteViewingProgress(
            @PathVariable Long userId,
            @PathVariable Long movieId) {
        historyService.deleteViewingProgress(userId, movieId);
        return ResponseEntity.noContent().build();
    }

    /**
     * DODATO: DELETE operacija za 'user_activity_by_device'.
     * Briše sve aktivnosti korisnika na određenom uređaju.
     * Primer poziva: DELETE /api/history/devices/user/1001?deviceType=Laptop
     */
    @DeleteMapping("/devices/user/{userId}")
    public ResponseEntity<Void> deleteDeviceActivity(
            @PathVariable Long userId,
            @RequestParam String deviceType) {
        historyService.deleteDeviceActivity(userId, deviceType);
        return ResponseEntity.noContent().build();
    }

    /**
     * DODATO: DELETE operacija za 'viewing_activity_by_movie_date'.
     * Briše analitički zapis o gledanju.
     * Primer poziva: DELETE /api/history/analytics/event/user/1001/movie/2?date=2025-09-26
     */
    @DeleteMapping("/analytics/event/user/{userId}/movie/{movieId}")
    public ResponseEntity<Void> deleteViewingActivityByDate(
            @PathVariable Long userId,
            @PathVariable Long movieId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        historyService.deleteViewingActivityByDate(movieId, date, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * DODATO: DELETE (reset) operacija za 'top_viewed_movies_by_month'.
     * Resetuje brojač za film u određenom mesecu.
     * Primer poziva: DELETE /api/history/top-movies/counter/movie/1?yearMonth=2025-09
     */
    @DeleteMapping("/top-movies/counter/movie/{movieId}")
    public ResponseEntity<Void> resetCounter(
            @PathVariable Long movieId,
            @RequestParam String yearMonth) {
        historyService.resetMovieCounterForMonth(yearMonth, movieId);
        return ResponseEntity.noContent().build();
    }
}