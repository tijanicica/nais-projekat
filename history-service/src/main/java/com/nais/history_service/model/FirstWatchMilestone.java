// Putanja: history-service/src/main/java/com/nais/history_service/model/FirstWatchMilestone.java

package com.nais.history_service.model;

import com.nais.history_service.model.key.FirstWatchMilestoneKey;
import lombok.Data;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.Instant;

@Table("first_watch_milestones")
@Data
public class FirstWatchMilestone {

    @PrimaryKey
    private FirstWatchMilestoneKey key;

    @Column("first_watched_at")
    private Instant firstWatchedAt;
}