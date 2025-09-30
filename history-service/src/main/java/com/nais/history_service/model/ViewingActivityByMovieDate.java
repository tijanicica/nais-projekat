package com.nais.history_service.model;

import com.nais.history_service.model.key.ViewingActivityByMovieDateKey;
import lombok.Data;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Data
@Table("viewing_activity_by_movie_date")
public class ViewingActivityByMovieDate {

    @PrimaryKey
    private ViewingActivityByMovieDateKey key;

}