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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HistoryService {

    private final ViewingHistoryByUserRepository viewingHistoryRepository;
    private final ViewingProgressByUserMovieRepository viewingProgressRepository;
    private final TopViewedMoviesByMonthRepository topMoviesRepository;
    private final UserActivityByDeviceRepository userActivityRepository;
    private final ViewingActivityByMovieDateRepository viewingActivityByDateRepository;
    private final CassandraTemplate cassandraTemplate;

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    // ===================================================================================
    // 1. GLAVNA CREATE OPERACIJA
    // ===================================================================================

    /**
     * Kreira zapise u svih 5 tabela na osnovu jednog događaja gledanja.
     * Ovo pokriva 'CREATE' zahtev za sve entitete.
     */
    public void recordViewingActivity(Long userId, Long movieId, int stoppedAtSeconds, String deviceType, String movieTitle) {
        Instant now = Instant.now();
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
        String currentYearMonth = today.format(YEAR_MONTH_FORMATTER);

        // Create: viewing_history_by_user
        ViewingHistoryByUserKey historyKey = new ViewingHistoryByUserKey(userId, now);
        ViewingHistoryByUser historyEvent = new ViewingHistoryByUser();
        historyEvent.setKey(historyKey);
        historyEvent.setMovieId(movieId);
        historyEvent.setStoppedAtSeconds(stoppedAtSeconds);
        viewingHistoryRepository.save(historyEvent);

        // Create/Update: viewing_progress_by_user_movie (koristi UPSERT)
        updateViewingProgress(userId, movieId, stoppedAtSeconds);

        // Create/Update: user_activity_by_device (koristi UPSERT)
        UserActivityByDeviceKey deviceKey = new UserActivityByDeviceKey(userId, deviceType, now);
        UserActivityByDevice deviceActivity = new UserActivityByDevice();
        deviceActivity.setKey(deviceKey);
        deviceActivity.setLastMovieIdWatched(movieId);
        userActivityRepository.save(deviceActivity);

        // Create: viewing_activity_by_movie_date
        ViewingActivityByMovieDateKey activityKey = new ViewingActivityByMovieDateKey(movieId, today, userId);
        ViewingActivityByMovieDate activityByDate = new ViewingActivityByMovieDate();
        activityByDate.setKey(activityKey);
        viewingActivityByDateRepository.save(activityByDate);

        // Update: top_viewed_movies_by_month (uvek ažurira counter)
        updateTopMovieCounter(currentYearMonth, movieId);
    }


    // ===================================================================================
    // 2. READ OPERACIJE (Po jedna za svaku tabelu)
    // ===================================================================================

    public List<ViewingHistoryByUser> getUserViewingHistory(Long userId) {
        return viewingHistoryRepository.findByKeyUserId(userId);
    }

    public List<ViewingProgressByUserMovie> getContinueWatchingList(Long userId) {
        return viewingProgressRepository.findByKeyUserId(userId);
    }

    public List<TopViewedMoviesByMonth> getTopMoviesForCurrentMonth(int limit) {
        String currentYearMonth = LocalDate.now(ZoneOffset.UTC).format(YEAR_MONTH_FORMATTER);
        List<TopViewedMoviesByMonth> movies = topMoviesRepository.findByYearMonth(currentYearMonth);
        movies.sort(Comparator.comparing(TopViewedMoviesByMonth::getViewCount).reversed());
        return movies.stream().limit(limit).collect(Collectors.toList());
    }

    public List<UserActivityByDevice> getUserDeviceActivity(Long userId) {
        return userActivityRepository.findByKeyUserId(userId);
    }

    public Long getUniqueViewersForMovieOnDate(Long movieId, LocalDate date) {
        return viewingActivityByDateRepository.countByKeyMovieIdAndKeyViewDate(movieId, date);
    }


    // ===================================================================================
    // 3. UPDATE OPERACIJE (Po jedna za svaku tabelu gde ima smisla)
    // ===================================================================================

    /**
     * UPDATE za 'viewing_progress_by_user_movie'.
     */
    public void updateViewingProgress(Long userId, Long movieId, int newProgressSeconds) {
        ViewingProgressByUserMovieKey progressKey = new ViewingProgressByUserMovieKey(userId, movieId);
        ViewingProgressByUserMovie progress = new ViewingProgressByUserMovie();
        progress.setKey(progressKey);
        progress.setProgressSeconds(newProgressSeconds);
        progress.setLastWatchedAt(Instant.now());
        viewingProgressRepository.save(progress);
    }

    /**
     * UPDATE za 'top_viewed_movies_by_month'.
     */
    private void updateTopMovieCounter(String yearMonth, Long movieId) {
        String cql = String.format(
                "UPDATE top_viewed_movies_by_month SET view_count = view_count + 1 WHERE year_month = '%s' AND movie_id = %d",
                yearMonth, movieId
        );
        cassandraTemplate.getCqlOperations().execute(cql);
    }

    // NAPOMENA: Za tabele 'ViewingHistoryByUser', 'UserActivityByDevice' i 'ViewingActivityByMovieDate'
    // klasično ažuriranje nema poslovnog smisla jer su one logovi događaja.
    // CREATE operacija je dovoljna demonstracija za njih.


    // ===================================================================================
    // 4. DELETE OPERACIJE (Po jedna za svaku tabelu)
    // ===================================================================================

    /**
     * DELETE za 'viewing_history_by_user'.
     */
    public void deleteViewingHistoryEvent(Long userId, Instant viewedAt) {
        ViewingHistoryByUserKey key = new ViewingHistoryByUserKey(userId, viewedAt);
        viewingHistoryRepository.deleteById(key);
    }

    /**
     * DELETE za 'viewing_progress_by_user_movie'.
     */
    public void deleteViewingProgress(Long userId, Long movieId) {
        ViewingProgressByUserMovieKey key = new ViewingProgressByUserMovieKey(userId, movieId);
        viewingProgressRepository.deleteById(key);
    }

    /**
     * DELETE za 'user_activity_by_device'.
     */
    public void deleteDeviceActivity(Long userId, String deviceType) {
        List<UserActivityByDevice> activities = userActivityRepository.findByKeyUserIdAndKeyDeviceType(userId, deviceType);
        if (activities != null && !activities.isEmpty()) {
            userActivityRepository.deleteAll(activities);
        }
    }

    /**
     * DELETE za 'viewing_activity_by_movie_date'.
     */
    public void deleteViewingActivityByDate(Long movieId, LocalDate viewDate, Long userId) {
        ViewingActivityByMovieDateKey key = new ViewingActivityByMovieDateKey(movieId, viewDate, userId);
        viewingActivityByDateRepository.deleteById(key);
    }

    /**
     * DELETE (resetovanje) za 'top_viewed_movies_by_month'.
     */
    public void resetMovieCounterForMonth(String yearMonth, Long movieId) {
        TopViewedMoviesByMonthKey key = new TopViewedMoviesByMonthKey(yearMonth, movieId);
        topMoviesRepository.findById(key).ifPresent(counter -> {
            Long currentCount = counter.getViewCount();
            if (currentCount != null && currentCount > 0) {
                String cqlDecrement = String.format(
                        "UPDATE top_viewed_movies_by_month SET view_count = view_count - %d WHERE year_month = '%s' AND movie_id = %d",
                        currentCount, yearMonth, movieId
                );
                cassandraTemplate.getCqlOperations().execute(cqlDecrement);
            }
        });
    }
}