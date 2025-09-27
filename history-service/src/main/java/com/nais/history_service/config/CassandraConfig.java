package com.nais.history_service.config;

import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.core.cql.keyspace.CreateKeyspaceSpecification;
import org.springframework.data.cassandra.core.cql.keyspace.KeyspaceOption;
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Configuration
@EnableCassandraRepositories(basePackages = "com.nais.history_service.repository")
public class CassandraConfig extends AbstractCassandraConfiguration {

    // Vrednosti iz application.properties ostaju iste
    @Value("${spring.cassandra.keyspace-name}")
    private String keyspace;

    @Value("${spring.cassandra.contact-points}")
    private String contactPoints;

    @Value("${spring.cassandra.port}")
    private int port;

    // ISPRAVKA: Uklonjena jedna tačka iz @Value anotacije
    @Value("${spring.cassandra.schema-action}")
    private String schemaAction;

    // getContactPoints i getPort ostaju isti
    @Override
    public String getContactPoints() {
        return contactPoints;
    }

    @Override
    protected int getPort() {
        return port;
    }

    // getSchemaAction i getKeyspaceName ostaju isti
    @Override
    public SchemaAction getSchemaAction() {
        return SchemaAction.valueOf(schemaAction);
    }

    @Override
    protected String getKeyspaceName() {
        return keyspace;
    }

    // getKeyspaceCreations ostaje isti
    @Override
    protected List<CreateKeyspaceSpecification> getKeyspaceCreations() {
        return Collections.singletonList(CreateKeyspaceSpecification.createKeyspace(keyspace)
                .ifNotExists()
                .with(KeyspaceOption.DURABLE_WRITES, true)
                .withSimpleReplication(1L));
    }

    // getEntityBasePackages ostaje isti
    @Override
    public String[] getEntityBasePackages() {
        return new String[] {"com.nais.history_service.model"};
    }

    /**
     * DODATO: Ova metoda kreira Bean koji konfiguriše drajver.
     * Ovde podešavamo timeout na 15 sekundi.
     * Spring će automatski koristiti ovaj Bean prilikom kreiranja konekcije.
     */
    @Bean
    public DriverConfigLoader driverConfigLoader() {
        return DriverConfigLoader.programmaticBuilder()
                .withDuration(DefaultDriverOption.REQUEST_TIMEOUT, Duration.ofSeconds(15))
                .build();
    }
}