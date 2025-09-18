package com.nais.history_service.service;

import com.nais.history_service.model.ViewingHistoryByUser;
import com.nais.history_service.repository.ViewingHistoryByUserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HistoryService {
    private final ViewingHistoryByUserRepository repository;

    public HistoryService(ViewingHistoryByUserRepository repository) {
        this.repository = repository;
    }
    // ... konstruktor

    public ViewingHistoryByUser recordViewingActivity(ViewingHistoryByUser activity) {
        activity.setViewingTimestamp(LocalDateTime.now()); // Postavi trenutno vreme
        return repository.save(activity);
    }

    public List<ViewingHistoryByUser> getHistoryForUser(Long userId) {
        // Spring Data Cassandra će automatski generisati upit
        return repository.findByUserId(userId);
    }
    // ... ostale metode
}
