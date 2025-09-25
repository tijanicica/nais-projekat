package com.nais.history_service.model;

import lombok.Data;
import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.*;
@Data @Table("top_viewed_movies_by_month")
public class TopViewedMoviesByMonth {
    @PrimaryKeyColumn(name = "year_month", ordinal = 0, type = PrimaryKeyType.PARTITIONED) // Format "YYYY-MM"
    private String yearMonth;
    @PrimaryKeyColumn(name = "view_count", ordinal = 1, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    private Long viewCount;
    @PrimaryKeyColumn(name = "movie_id", ordinal = 2, type = PrimaryKeyType.CLUSTERED)
    private Long movieId;
    @Column("movie_title") private String movieTitle;
}
