package com.nais.history_service.repository;

import com.nais.history_service.model.TopViewedMoviesByMonth;
import com.nais.history_service.model.key.TopViewedMoviesByMonthKey; // Importujemo Key klasu
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TopViewedMoviesByMonthRepository extends CassandraRepository<TopViewedMoviesByMonth, TopViewedMoviesByMonthKey> {

    @Query("SELECT * FROM top_viewed_movies_by_month WHERE year_month = ?0 LIMIT ?1")
    List<TopViewedMoviesByMonth> findTopNByYearMonth(String yearMonth, int limit);
}