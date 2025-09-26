package com.nais.history_service.repository;

import com.nais.history_service.model.UserActivityByDevice;
import com.nais.history_service.model.key.UserActivityByDeviceKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserActivityByDeviceRepository extends CassandraRepository<UserActivityByDevice, UserActivityByDeviceKey> {

    List<UserActivityByDevice> findByKeyUserId(Long userId);
}