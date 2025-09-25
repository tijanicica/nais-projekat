package com.nais.history_service.model;

import lombok.Data;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.*;
import java.time.LocalDate;
@Data @Table("viewing_activity_by_movie_date")
public class ViewingActivityByMovieDate {
    @PrimaryKeyColumn(name = "movie_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private Long movieId;
    @PrimaryKeyColumn(name = "view_date", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    private LocalDate viewDate;
    @PrimaryKeyColumn(name = "user_id", ordinal = 2, type = PrimaryKeyType.CLUSTERED)
    private Long userId;
}