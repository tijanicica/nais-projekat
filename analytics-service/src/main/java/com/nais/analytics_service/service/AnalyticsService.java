package com.nais.analytics_service.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.query.FluxTable;
import com.nais.analytics_service.model.StreamingPerformance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AnalyticsService {
    private final InfluxDBClient influxDBClient;

    @Value("${spring.influx.bucket}")
    private String bucket;
    @Value("${spring.influx.org}")
    private String org;

    public AnalyticsService(InfluxDBClient influxDBClient) {
        this.influxDBClient = influxDBClient;
    }

    // ... konstruktor

    public void writePerformanceData(StreamingPerformance data) {
        data.setTime(Instant.now());
        try (WriteApi writeApi = influxDBClient.getWriteApi()) {
            writeApi.writeMeasurement(bucket, org, WritePrecision.NS, data);
        }
    }

    public List<FluxTable> getAverageBufferingTimeByRegion(String timeRange) {
        String fluxQuery = String.format(
                "from(bucket: \"%s\")\n" +
                        "  |> range(start: -%s)\n" +
                        "  |> filter(fn: (r) => r._measurement == \"streaming_performance\")\n" +
                        "  |> filter(fn: (r) => r._field == \"bufferingTimeMs\")\n" +
                        "  |> group(columns: [\"region\"])\n" +
                        "  |> mean()\n" +
                        "  |> sort(columns: [\"_value\"], desc: true)", bucket, timeRange);

        QueryApi queryApi = influxDBClient.getQueryApi();
        return queryApi.query(fluxQuery, org);
    }
}
