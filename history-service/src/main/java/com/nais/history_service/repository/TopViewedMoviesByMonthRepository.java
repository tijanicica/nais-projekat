package com.nais.history_service.repository;

import com.nais.history_service.model.TopViewedMoviesByMonth;
import com.nais.history_service.model.key.TopViewedMoviesByMonthKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TopViewedMoviesByMonthRepository extends CassandraRepository<TopViewedMoviesByMonth, TopViewedMoviesByMonthKey> {

    /**
     * READ operacija: Dohvata sve filmove za dati mesec.
     * Zbog novog modela sa counter-om, sortiranje se mora raditi u aplikaciji.
     * ALLOW FILTERING je neophodan jer ne filtriramo po celom particionom ključu.
     */
    @Query("SELECT * FROM top_viewed_movies_by_month WHERE year_month = ?0 ALLOW FILTERING")
    List<TopViewedMoviesByMonth> findByYearMonth(String yearMonth);
}