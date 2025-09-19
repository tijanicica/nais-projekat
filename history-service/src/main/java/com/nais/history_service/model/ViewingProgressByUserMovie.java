package com.nais.history_service.model;
import lombok.Data;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.*;
import java.time.Instant;
@Data @Table("viewing_progress_by_user_movie")
public class ViewingProgressByUserMovie {
    @PrimaryKeyColumn(name = "user_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private Long userId;
    @PrimaryKeyColumn(name = "movie_id", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    private Long movieId;
    @Column("progress_seconds") private int progressSeconds;
    @Column("last_watched_at") private Instant lastWatchedAt;
}