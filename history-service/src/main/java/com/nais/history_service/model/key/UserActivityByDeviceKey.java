package com.nais.history_service.model.key;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.io.Serializable;
import java.time.Instant;

@PrimaryKeyClass
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityByDeviceKey implements Serializable {

    @PrimaryKeyColumn(name = "user_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private Long userId;

    @PrimaryKeyColumn(name = "device_type", ordinal = 1, type = PrimaryKeyType.CLUSTERED) // "web", "mobile", "tv"
    private String deviceType;

    @PrimaryKeyColumn(name = "last_active", ordinal = 2, type = PrimaryKeyType.CLUSTERED)
    private Instant lastActive;
}