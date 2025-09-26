package com.nais.history_service.repository;

import com.nais.history_service.model.ViewingHistoryByUser;
import com.nais.history_service.model.key.ViewingHistoryByUserKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ViewingHistoryByUserRepository extends CassandraRepository<ViewingHistoryByUser, ViewingHistoryByUserKey> {

    /**
     * READ operacija: Pronalazi sve događaje u istoriji za određenog korisnika.
     * Ovo je ključni upit za ovu tabelu.
     */
    List<ViewingHistoryByUser> findByKeyUserId(Long userId);
}