package com.nais.history_service.service;

import com.nais.history_service.model.*;
import com.nais.history_service.model.key.*;
import com.nais.history_service.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor // Automatski kreira konstruktor za final polja (dependency injection)
public class HistoryService {

    // Repozitorijumi za pristup podacima
    private final ViewingHistoryByUserRepository viewingHistoryRepository;
    private final ViewingProgressByUserMovieRepository viewingProgressRepository;
    private final TopViewedMoviesByMonthRepository topMoviesRepository;
    private final UserActivityByDeviceRepository userActivityRepository;
    private final ViewingActivityByMovieDateRepository viewingActivityByDateRepository;

    // CassandraTemplate je potreban za specifične operacije kao što je ažuriranje counter-a
    private final CassandraTemplate cassandraTemplate;

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * Glavna metoda za beleženje aktivnosti gledanja.
     * Poziva se periodično dok korisnik gleda film.
     */
    public void recordViewingActivity(Long userId, Long movieId, int stoppedAtSeconds, String deviceType, String movieTitle) {
        Instant now = Instant.now();
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
        String currentYearMonth = today.format(YEAR_MONTH_FORMATTER);

        // --- ISPRAVKA: Kreiramo Key objekte pre čuvanja ---

        // 1. Sačuvaj događaj u glavnu istoriju
        ViewingHistoryByUserKey historyKey = new ViewingHistoryByUserKey(userId, now);
        ViewingHistoryByUser historyEvent = new ViewingHistoryByUser();
        historyEvent.setKey(historyKey);
        historyEvent.setMovieId(movieId);
        historyEvent.setStoppedAtSeconds(stoppedAtSeconds);
        viewingHistoryRepository.save(historyEvent);

        // 2. Ažuriraj progres gledanja (UPSERT)
        ViewingProgressByUserMovieKey progressKey = new ViewingProgressByUserMovieKey(userId, movieId);
        ViewingProgressByUserMovie progress = new ViewingProgressByUserMovie();
        progress.setKey(progressKey);
        progress.setProgressSeconds(stoppedAtSeconds);
        progress.setLastWatchedAt(now);
        viewingProgressRepository.save(progress);

        // 3. Ažuriraj aktivnost po uređaju
        UserActivityByDeviceKey deviceKey = new UserActivityByDeviceKey(userId, deviceType, now);
        UserActivityByDevice deviceActivity = new UserActivityByDevice();
        deviceActivity.setKey(deviceKey);
        deviceActivity.setLastMovieIdWatched(movieId);
        userActivityRepository.save(deviceActivity);

        // 4. Zabeleži aktivnost za dnevnu analitiku
        ViewingActivityByMovieDateKey activityKey = new ViewingActivityByMovieDateKey(movieId, today, userId);
        ViewingActivityByMovieDate activityByDate = new ViewingActivityByMovieDate();
        activityByDate.setKey(activityKey);
        viewingActivityByDateRepository.save(activityByDate);

        // 5. Ažuriraj brojač za najgledanije filmove
        updateTopMovieCounter(currentYearMonth, movieId, movieTitle);
    }

    /**
     * Dohvata kompletnu istoriju gledanja za korisnika.
     * Primer upita sa uslovom.
     */
    public List<ViewingHistoryByUser> getUserViewingHistory(Long userId) {
        // ISPRAVKA: Pozivamo 'findByKeyUserId' umesto 'findByUserId'
        return viewingHistoryRepository.findByKeyUserId(userId);
    }

    /**
     * Dohvata listu filmova koje korisnik može nastaviti da gleda.
     * Primer upita sa uslovom.
     */
    public List<ViewingProgressByUserMovie> getContinueWatchingList(Long userId) {
        // ISPRAVKA: Pozivamo 'findByKeyUserId' umesto 'findByUserId'
        return viewingProgressRepository.findByKeyUserId(userId);
    }

    /**
     * Dohvata top N najgledanijih filmova za tekući mesec.
     * Primer upita za grupisanje/agregaciju.
     */
    public List<TopViewedMoviesByMonth> getTopMoviesForCurrentMonth(int limit) {
        String currentYearMonth = LocalDate.now(ZoneOffset.UTC).format(YEAR_MONTH_FORMATTER);
        return topMoviesRepository.findTopNByYearMonth(currentYearMonth, limit);
    }

    /**
     * Dohvata listu uređaja koje je korisnik koristio.
     * Primer upita za grupisanje.
     */
    public List<UserActivityByDevice> getUserDeviceActivity(Long userId) {
        return userActivityRepository.findByKeyUserId(userId);
    }

    /**
     * Vraća broj jedinstvenih gledalaca za film na određeni dan.
     * Primer upita za agregaciju.
     */
    public Long getUniqueViewersForMovieOnDate(Long movieId, LocalDate date) {
        return viewingActivityByDateRepository.countByKeyMovieIdAndKeyViewDate(movieId, date);
    }

    /**
     * Ispravan način za ažuriranje counter kolone u Cassandri.
     * Standardni save() ne radi, mora se koristiti direktan UPDATE upit.
     */
    private void updateTopMovieCounter(String yearMonth, Long movieId, String movieTitle) {
        // Prvo, moramo da "ubacimo" red sa početnim vrednostima ako ne postoji,
        // jer counter može samo da se inkrementira na postojećem redu.
        // Ovo je komplikovanije i može se rešiti na više načina.
        // Za potrebe projekta, fokusirajmo se na samo inkrementiranje.

        String cql = String.format(
                "UPDATE top_viewed_movies_by_month SET view_count = view_count + 1, movie_title = '%s' WHERE year_month = '%s' AND movie_id = %d",
                movieTitle, yearMonth, movieId
        );

        // CassandraTemplate omogućava izvršavanje proizvoljnih CQL upita.
        cassandraTemplate.getCqlOperations().execute(cql);
    }

    public void updateViewingProgress(Long userId, Long movieId, int newProgressSeconds) {
        ViewingProgressByUserMovieKey progressKey = new ViewingProgressByUserMovieKey(userId, movieId);
        ViewingProgressByUserMovie progress = new ViewingProgressByUserMovie();
        progress.setKey(progressKey);
        progress.setProgressSeconds(newProgressSeconds);
        progress.setLastWatchedAt(Instant.now());

        viewingProgressRepository.save(progress);
    }

    /**
     * D (Delete): Briše jedan zapis iz glavne tabele istorije gledanja.
     * NAPOMENA: Za potpunu konzistentnost, trebalo bi obrisati zapise i iz ostalih tabela,
     * ali za potrebe demonstracije CRUD operacije, ovo je dovoljno.
     */
    public void deleteViewingHistoryEvent(Long userId, Instant viewedAt) {
        ViewingHistoryByUserKey key = new ViewingHistoryByUserKey(userId, viewedAt);
        viewingHistoryRepository.deleteById(key);
    }
}