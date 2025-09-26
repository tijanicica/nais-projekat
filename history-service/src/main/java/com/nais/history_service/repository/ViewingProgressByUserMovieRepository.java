package com.nais.history_service.repository;

import com.nais.history_service.model.ViewingProgressByUserMovie;
import com.nais.history_service.model.key.ViewingProgressByUserMovieKey; // Importujemo Key klasu
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ViewingProgressByUserMovieRepository extends CassandraRepository<ViewingProgressByUserMovie, ViewingProgressByUserMovieKey> {

    // ISPRAVKA 2: Metoda se sada zove findByKeyUserId umesto findByUserId
    List<ViewingProgressByUserMovie> findByKeyUserId(Long userId);
}