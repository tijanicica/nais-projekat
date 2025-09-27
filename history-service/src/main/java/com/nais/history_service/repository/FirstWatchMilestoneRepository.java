// Putanja: history-service/src/main/java/com/nais/history_service/repository/FirstWatchMilestoneRepository.java

package com.nais.history_service.repository;

import com.nais.history_service.model.FirstWatchMilestone;
import com.nais.history_service.model.key.FirstWatchMilestoneKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FirstWatchMilestoneRepository extends CassandraRepository<FirstWatchMilestone, FirstWatchMilestoneKey> {
}