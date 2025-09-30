package com.nais.history_service.repository;

import com.nais.history_service.model.ViewingHistoryByUser;
import com.nais.history_service.model.key.ViewingHistoryByUserKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ViewingHistoryByUserRepository extends CassandraRepository<ViewingHistoryByUser, ViewingHistoryByUserKey> {

    List<ViewingHistoryByUser> findByKeyUserId(Long userId);
}