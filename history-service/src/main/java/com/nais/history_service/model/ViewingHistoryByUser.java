package com.nais.history_service.model;

import com.nais.history_service.model.key.ViewingHistoryByUserKey;
import lombok.Data;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Data
@Table("viewing_history_by_user")
public class ViewingHistoryByUser {

    @PrimaryKey
    private ViewingHistoryByUserKey key;

    @Column("movie_id")
    private Long movieId;

    @Column("stopped_at_seconds")
    private int stoppedAtSeconds;
}