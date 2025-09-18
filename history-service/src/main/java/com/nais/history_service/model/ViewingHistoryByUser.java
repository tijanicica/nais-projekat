package com.nais.history_service.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Table("viewing_history_by_user")
@Getter
@Setter
@Data
public class ViewingHistoryByUser {
    @PrimaryKeyColumn(name = "user_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private Long userId;

    @PrimaryKeyColumn(name = "viewing_timestamp", ordinal = 1, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    private LocalDateTime viewingTimestamp;

    @Column("movie_id")
    private Long movieId;

    @Column("stopped_at_seconds")
    private int stoppedAtSeconds;

    // Getters and Setters
}
