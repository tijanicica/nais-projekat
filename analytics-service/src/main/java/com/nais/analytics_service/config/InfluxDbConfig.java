package com.nais.analytics_service.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InfluxDbConfig {

    @Value("${spring.influx.url}")
    private String influxUrl;
    @Value("${spring.influx.token}")
    private char[] token;
    @Value("${spring.influx.org}")
    private String org;

    @Bean
    public InfluxDBClient influxDBClient() {
        return InfluxDBClientFactory.create(influxUrl, token, org);
    }
}
