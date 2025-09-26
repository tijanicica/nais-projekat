package com.nais.history_service.model.key;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.io.Serializable;

@PrimaryKeyClass
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopViewedMoviesByMonthKey implements Serializable {

    @PrimaryKeyColumn(name = "year_month", ordinal = 0, type = PrimaryKeyType.PARTITIONED) // Format "YYYY-MM"
    private String yearMonth;

    @PrimaryKeyColumn(name = "view_count", ordinal = 1, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    private Long viewCount;

    @PrimaryKeyColumn(name = "movie_id", ordinal = 2, type = PrimaryKeyType.CLUSTERED)
    private Long movieId;
}