package com.nais.history_service.repository;

import com.nais.history_service.model.ViewingActivityByMovieDate;
import com.nais.history_service.model.key.ViewingActivityByMovieDateKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface ViewingActivityByMovieDateRepository extends CassandraRepository<ViewingActivityByMovieDate, ViewingActivityByMovieDateKey> {


    @Query("SELECT COUNT(*) FROM viewing_activity_by_movie_date WHERE movie_id = ?0 AND view_date = ?1")
    Long countByKeyMovieIdAndKeyViewDate(Long movieId, LocalDate viewDate);
}