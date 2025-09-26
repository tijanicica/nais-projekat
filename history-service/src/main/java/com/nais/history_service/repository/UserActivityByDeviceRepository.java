package com.nais.history_service.repository;

import com.nais.history_service.model.UserActivityByDevice;
import com.nais.history_service.model.key.UserActivityByDeviceKey;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserActivityByDeviceRepository extends CassandraRepository<UserActivityByDevice, UserActivityByDeviceKey> {

    /**
     * READ operacija: Dohvata sve zapise (uređaje) za jednog korisnika.
     */
    List<UserActivityByDevice> findByKeyUserId(Long userId);

    /**
     * READ operacija: Dohvata sve zapise za jednog korisnika I specifičan tip uređaja.
     * Potrebno je da bismo mogli da izvršimo ciljanu DELETE operaciju.
     */
    List<UserActivityByDevice> findByKeyUserIdAndKeyDeviceType(Long userId, String deviceType);
}