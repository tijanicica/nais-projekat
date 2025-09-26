package com.nais.history_service.repository;

import com.nais.history_service.model.ViewingProgressByUserMovie;
import com.nais.history_service.model.key.ViewingProgressByUserMovieKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ViewingProgressByUserMovieRepository extends CassandraRepository<ViewingProgressByUserMovie, ViewingProgressByUserMovieKey> {

    /**
     * READ operacija: Pronalazi sve zapise o progresu za jednog korisnika.
     * Korisno za "Continue Watching" listu.
     */
    List<ViewingProgressByUserMovie> findByKeyUserId(Long userId);
}