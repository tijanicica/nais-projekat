package com.nais.history_service.repository;
import com.nais.history_service.model.ViewingHistoryByUser;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface ViewingHistoryByUserRepository extends CassandraRepository<ViewingHistoryByUser, Long> {
    List<ViewingHistoryByUser> findByUserId(Long userId);
}
