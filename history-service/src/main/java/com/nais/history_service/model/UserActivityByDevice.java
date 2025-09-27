package com.nais.history_service.model;
import com.nais.history_service.model.key.UserActivityByDeviceKey;
import lombok.Data;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
@Data
@Table("user_activity_by_device")
public class UserActivityByDevice {
    @PrimaryKey
    private UserActivityByDeviceKey key;

    @Column("last_movie_id_watched")
    private Long lastMovieIdWatched;
}