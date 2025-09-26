package com.nais.history_service.model;

import com.nais.history_service.model.key.ViewingProgressByUserMovieKey;
import lombok.Data;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import java.time.Instant;

@Data
@Table("viewing_progress_by_user_movie")
public class ViewingProgressByUserMovie {

    @PrimaryKey
    private ViewingProgressByUserMovieKey key;

    @Column("progress_seconds")
    private int progressSeconds;

    @Column("last_watched_at")
    private Instant lastWatchedAt;
}
