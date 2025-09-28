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
import com.nais.history_service.model.FirstWatchMilestone;
import com.nais.history_service.model.key.FirstWatchMilestoneKey;
import com.nais.history_service.repository.FirstWatchMilestoneRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;
@Service
@RequiredArgsConstructor
public class HistoryService {

    private final ViewingHistoryByUserRepository viewingHistoryRepository;
    private final ViewingProgressByUserMovieRepository viewingProgressRepository;
    private final TopViewedMoviesByMonthRepository topMoviesRepository;
    private final UserActivityByDeviceRepository userActivityRepository;
    private final ViewingActivityByMovieDateRepository viewingActivityByDateRepository;
    private final CassandraTemplate cassandraTemplate;
    private final FirstWatchMilestoneRepository milestoneRepository;
    private final RestTemplate restTemplate;


    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");


    @Value("${services.analytics.url}")
    private String analyticsServiceUrl;

    // ===================================================================================
    // 1. GLAVNA CREATE OPERACIJA
    // ===================================================================================

    /**
     * Kreira zapise u svih 5 tabela na osnovu jednog događaja gledanja.
     * Ovo pokriva 'CREATE' zahtev za sve entitete.
     */
        public void recordViewingActivity(Long userId, Long movieId, int stoppedAtSeconds, String deviceType, String movieTitle) {
        // --- POSTOJEĆA LOGIKA OSTAJE NETAKNUTA ---
        Instant now = Instant.now();
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
        String currentYearMonth = today.format(YEAR_MONTH_FORMATTER);

        ViewingHistoryByUserKey historyKey = new ViewingHistoryByUserKey(userId, now);
        ViewingHistoryByUser historyEvent = new ViewingHistoryByUser();
        historyEvent.setKey(historyKey);
        historyEvent.setMovieId(movieId);
        historyEvent.setStoppedAtSeconds(stoppedAtSeconds);
        viewingHistoryRepository.save(historyEvent);

        updateViewingProgress(userId, movieId, stoppedAtSeconds);

        UserActivityByDeviceKey deviceKey = new UserActivityByDeviceKey(userId, deviceType, now);
        UserActivityByDevice deviceActivity = new UserActivityByDevice();
        deviceActivity.setKey(deviceKey);
        deviceActivity.setLastMovieIdWatched(movieId);
        userActivityRepository.save(deviceActivity);

        ViewingActivityByMovieDateKey activityKey = new ViewingActivityByMovieDateKey(movieId, today, userId);
        ViewingActivityByMovieDate activityByDate = new ViewingActivityByMovieDate();
        activityByDate.setKey(activityKey);
        viewingActivityByDateRepository.save(activityByDate);

        updateTopMovieCounter(currentYearMonth, movieId);

        FirstWatchMilestoneKey milestoneKey = new FirstWatchMilestoneKey();
        milestoneKey.setUserId(userId);
        milestoneKey.setMovieId(movieId);
        
        // Proveravamo u bazi da li zapis već postoji. Ako ne postoji, ovo je PRVO gledanje.
        boolean isFirstWatch = !milestoneRepository.findById(milestoneKey).isPresent();

        if (isFirstWatch) {
            System.out.println("SAGA: Detektovano prvo gledanje za korisnika " + userId + " i film " + movieId + ". Pokrećem sagu.");
            executeFirstWatchSaga(userId, movieId, deviceType, now);
        }
    }


     private void executeFirstWatchSaga(Long userId, Long movieId, String deviceType, Instant eventTime) {
        FirstWatchMilestone milestone = null;

        // --- Korak 1: UPIS "dostignuća" u lokalnu bazu (Cassandra) ---
        try {
            FirstWatchMilestoneKey key = new FirstWatchMilestoneKey();
            key.setUserId(userId);
            key.setMovieId(movieId);
            
            milestone = new FirstWatchMilestone();
            milestone.setKey(key);
            milestone.setFirstWatchedAt(eventTime);
            
            milestoneRepository.save(milestone);
            System.out.println("SAGA [Korak 1]: Uspešno upisan 'milestone' u Cassandru.");
        } catch (Exception e) {
            System.err.println("SAGA FAILED [Korak 1]: Upis u Cassandru nije uspeo. Prekidam.");
            throw new RuntimeException("Neuspeli upis u Cassandru.", e);
        }

        // --- Korak 2: UPIS analitičkog događaja u Analytics servis (InfluxDB) ---
        try {
            Map<String, String> eventData = new HashMap<>();
            eventData.put("userId", String.valueOf(userId));
            eventData.put("movieId", String.valueOf(movieId));
            eventData.put("deviceType", deviceType);

            restTemplate.postForEntity(analyticsServiceUrl + "/interactions/first-watch", eventData, Void.class);
            System.out.println("SAGA [Korak 2]: Uspešno poslat 'first-watch' događaj u Analytics servis.");
        } catch (Exception e) {
            System.err.println("SAGA FAILED [Korak 2]: Slanje događaja u Analytics servis nije uspelo. Pokrećem kompenzaciju!");
            // --- Kompenzacija za Korak 1 ---
            compensateMilestoneCreation(milestone);
            throw new RuntimeException("Neuspelo slanje događaja u Analytics servis.", e);
        }

        System.out.println("SAGA SUCCESS: Događaj prvog gledanja je uspešno zabeležen u oba sistema.");
    }


    private void compensateMilestoneCreation(FirstWatchMilestone milestoneToRollback) {
        System.err.println("COMPENSATION: Brišem novokreirani 'milestone' iz Cassandre.");
        if (milestoneToRollback != null) {
            milestoneRepository.delete(milestoneToRollback);
        }
    }

    public List<FirstWatchMilestone> getAllMilestones() {
        return milestoneRepository.findAll();
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