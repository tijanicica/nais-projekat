package com.nais.history_service.model;
import lombok.Data;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.*;
import java.time.Instant;
@Data @Table("user_activity_by_device")
public class UserActivityByDevice {
    @PrimaryKeyColumn(name = "user_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private Long userId;
    @PrimaryKeyColumn(name = "device_type", ordinal = 1, type = PrimaryKeyType.CLUSTERED) // "web", "mobile", "tv"
    private String deviceType;
    @PrimaryKeyColumn(name = "last_active", ordinal = 2, type = PrimaryKeyType.CLUSTERED)
    private Instant lastActive;
    @Column("last_movie_id_watched") private Long lastMovieIdWatched;
}
