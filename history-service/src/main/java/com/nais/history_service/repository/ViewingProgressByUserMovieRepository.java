package com.nais.history_service.repository;

import com.nais.history_service.model.ViewingProgressByUserMovie;
import com.nais.history_service.model.key.ViewingProgressByUserMovieKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ViewingProgressByUserMovieRepository extends CassandraRepository<ViewingProgressByUserMovie, ViewingProgressByUserMovieKey> {

    List<ViewingProgressByUserMovie> findByKeyUserId(Long userId);
}