package com.nais.history_service.model;

import com.nais.history_service.model.key.TopViewedMoviesByMonthKey;
import lombok.Data;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Data
@Table("top_viewed_movies_by_month")
public class TopViewedMoviesByMonth {

    @PrimaryKey
    private TopViewedMoviesByMonthKey key;

    @Column("movie_title")
    private String movieTitle;
}
