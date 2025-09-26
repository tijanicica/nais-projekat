package com.nais.analytics_service.repository;

import com.influxdb.client.DeleteApi;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.exceptions.InfluxException;
import com.influxdb.query.FluxTable;
import com.nais.analytics_service.model.StreamingPerformance;
import com.nais.analytics_service.model.UserInteraction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class AnalyticsRepositoryImpl implements AnalyticsRepository {

    private final InfluxDBClient influxDBClient;

    @Value("${spring.influx.bucket}")
    private String bucket;
    @Value("${spring.influx.org}")
    private String org;

    // Injektujemo singleton InfluxDBClient bean
    public AnalyticsRepositoryImpl(InfluxDBClient influxDBClient) {
        this.influxDBClient = influxDBClient;
    }

    @Override
    public void save(StreamingPerformance data) {
        try (WriteApi writeApi = influxDBClient.getWriteApi()) {
            writeApi.writeMeasurement(bucket, org, WritePrecision.NS, data);
        } catch (InfluxException e) {
            System.err.println("Exception while writing performance data: " + e.getMessage());
        }
    }

    @Override
    public void save(UserInteraction data) {
        try (WriteApi writeApi = influxDBClient.getWriteApi()) {
            writeApi.writeMeasurement(bucket, org, WritePrecision.NS, data);
        } catch (InfluxException e) {
            System.err.println("Exception while writing user interaction data: " + e.getMessage());
        }
    }

    @Override
    public void delete(OffsetDateTime start, OffsetDateTime stop, String predicate) {
        DeleteApi deleteApi = influxDBClient.getDeleteApi();
        try {
            deleteApi.delete(start, stop, predicate, bucket, org);
            System.out.println("Successfully deleted data with predicate: " + predicate);
        } catch (InfluxException e) {
            System.err.println("Exception while deleting data: " + e.getMessage());
        }
    }

    @Override
    public List<FluxTable> findAvgBufferingByRegionAndDevice(String timeRange) {
        String fluxQuery = String.format(
                "from(bucket: \"%s\")\n" +
                        "  |> range(start: -%s)\n" +
                        "  |> filter(fn: (r) => r._measurement == \"streaming_performance\")\n" +
                        "  |> filter(fn: (r) => r._field == \"bufferingTimeMs\")\n" +
                        "  |> group(columns: [\"region\", \"deviceType\"])\n" +
                        "  |> mean()\n" +
                        "  |> sort(columns: [\"_value\"], desc: true)",
                bucket, timeRange
        );
        return influxDBClient.getQueryApi().query(fluxQuery, org);
    }

    @Override
    public List<FluxTable> findUsersWithLowBitrate(String timeRange, int bitrateThreshold) {
        String fluxQuery = String.format(
                "from(bucket: \"%s\")\n" +
                        "  |> range(start: -%s)\n" +
                        "  |> filter(fn: (r) => r._measurement == \"streaming_performance\" and r._field == \"bitrateKbps\")\n" +
                        "  |> group(columns: [\"userId\", \"movieId\"])\n" + // GRUPIŠEMO I PO FILMU!
                        "  |> mean()\n" +
                        "  |> filter(fn: (r) => r._value < %d)\n" + // Filtriramo proseke
                        "  |> sort(columns: [\"_value\"])", // Sortiramo da najgori budu prvi
                bucket, timeRange, bitrateThreshold
        );
        return influxDBClient.getQueryApi().query(fluxQuery, org);
    }

    @Override
    public List<FluxTable> findTopInteractionCombinations(String timeRange, int limit) {
        String fluxQuery = String.format(
                "from(bucket: \"%s\")\n" +
                        "  |> range(start: -%s)\n" +
                        "  |> filter(fn: (r) => r._measurement == \"user_interaction\")\n" +
                        "  |> group(columns: [\"movieId\", \"interactionType\"])\n" +
                        "  |> count()\n" +
                        "  |> group(columns: [\"movieId\"])\n" +
                        "  |> sort(columns: [\"_value\"], desc: true)\n" +
                        "  |> limit(n: %d)",
                bucket, timeRange, limit
        );
        return influxDBClient.getQueryApi().query(fluxQuery, org);
    }
}