package com.nais.history_service.controller;

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

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/history") // Osnovna putanja za sve endpoint-e u ovom kontroleru
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    /**
     * Endpoint za beleženje aktivnosti gledanja.
     * Klijent šalje podatke u telu POST zahteva.
     * Primer poziva: POST /api/history/record
     */
    @PostMapping("/record")
    public ResponseEntity<Void> recordActivity(@RequestBody ViewingActivityDTO activityDTO) {
        historyService.recordViewingActivity(
                activityDTO.getUserId(),
                activityDTO.getMovieId(),
                activityDTO.getStoppedAtSeconds(),
                activityDTO.getDeviceType(),
                activityDTO.getMovieTitle()
        );
        return ResponseEntity.ok().build(); // Vraća status 200 OK
    }

    /**
     * Endpoint za dobavljanje kompletne istorije gledanja za korisnika.
     * Primer poziva: GET /api/history/user/1001
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ViewingHistoryByUser>> getHistoryForUser(@PathVariable Long userId) {
        List<ViewingHistoryByUser> history = historyService.getUserViewingHistory(userId);
        return ResponseEntity.ok(history);
    }

    /**
     * Endpoint za dobavljanje liste "Nastavi sa gledanjem" za korisnika.
     * Primer poziva: GET /api/history/progress/1001
     */
    @GetMapping("/progress/{userId}")
    public ResponseEntity<List<ViewingProgressByUserMovie>> getProgressForUser(@PathVariable Long userId) {
        List<ViewingProgressByUserMovie> progressList = historyService.getContinueWatchingList(userId);
        return ResponseEntity.ok(progressList);
    }

    /**
     * Endpoint za dobavljanje najgledanijih filmova u tekućem mesecu.
     * Parametar 'limit' je opcioni.
     * Primer poziva: GET /api/history/top-movies?limit=5
     */
    @GetMapping("/top-movies")
    public ResponseEntity<List<TopViewedMoviesByMonth>> getTopMovies(@RequestParam(defaultValue = "10") int limit) {
        List<TopViewedMoviesByMonth> topMovies = historyService.getTopMoviesForCurrentMonth(limit);
        return ResponseEntity.ok(topMovies);
    }

    /**
     * Endpoint za dobavljanje svih uređaja koje je korisnik koristio.
     * Primer poziva: GET /api/history/devices/1001
     */
    @GetMapping("/devices/{userId}")
    public ResponseEntity<List<UserActivityByDevice>> getDevicesForUser(@PathVariable Long userId) {
        List<UserActivityByDevice> devices = historyService.getUserDeviceActivity(userId);
        return ResponseEntity.ok(devices);
    }

    /**
     * Endpoint za dobavljanje broja jedinstvenih gledalaca za film na određeni dan.
     * Datum se šalje kao query parametar u formatu YYYY-MM-DD.
     * Primer poziva: GET /api/history/analytics/viewers/1?date=2025-09-26
     */
    @GetMapping("/analytics/viewers/{movieId}")
    public ResponseEntity<Long> getUniqueViewers(
            @PathVariable Long movieId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Long count = historyService.getUniqueViewersForMovieOnDate(movieId, date);
        return ResponseEntity.ok(count);
    }
}