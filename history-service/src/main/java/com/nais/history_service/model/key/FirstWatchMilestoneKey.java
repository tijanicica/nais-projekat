
package com.nais.history_service.model.key;

import lombok.Data;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

@PrimaryKeyClass
@Data
public class FirstWatchMilestoneKey {


    //na koju policu idu podaci- PARTITIONED
    @PrimaryKeyColumn(name = "user_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private Long userId;

    //kako su podaci poredjani na polici- CLUSTERED
    @PrimaryKeyColumn(name = "movie_id", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    private Long movieId;
}