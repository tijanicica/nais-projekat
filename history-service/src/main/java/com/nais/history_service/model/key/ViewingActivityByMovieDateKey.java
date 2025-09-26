package com.nais.history_service.model.key;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.io.Serializable;
import java.time.LocalDate;

@PrimaryKeyClass
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ViewingActivityByMovieDateKey implements Serializable {

    @PrimaryKeyColumn(name = "movie_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private Long movieId;

    @PrimaryKeyColumn(name = "view_date", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    private LocalDate viewDate;

    @PrimaryKeyColumn(name = "user_id", ordinal = 2, type = PrimaryKeyType.CLUSTERED)
    private Long userId;
}