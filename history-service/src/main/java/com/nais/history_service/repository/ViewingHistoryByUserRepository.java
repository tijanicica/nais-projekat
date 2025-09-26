package com.nais.history_service.repository;

import com.nais.history_service.model.ViewingHistoryByUser;
import com.nais.history_service.model.key.ViewingHistoryByUserKey; // Importujemo Key klasu
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository // Dodata anotacija
// ISPRAVKA 1: Umesto <..., Long>, sada je <..., ViewingHistoryByUserKey>
public interface ViewingHistoryByUserRepository extends CassandraRepository<ViewingHistoryByUser, ViewingHistoryByUserKey> {

    List<ViewingHistoryByUser> findByKeyUserId(Long userId);
}