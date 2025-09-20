package com.nais.analytics_service.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.query.FluxTable;
import com.nais.analytics_service.model.StreamingPerformance;
import com.nais.analytics_service.model.UserInteraction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class InfluxDBDataInitializer implements CommandLineRunner {

    private final InfluxDBClient influxDBClient;

    @Value("${spring.influx.bucket}")
    private String bucketName;

    public InfluxDBDataInitializer(InfluxDBClient influxDBClient) {
        this.influxDBClient = influxDBClient;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Checking if InfluxDB data initialization is needed...");

        if (isDataAlreadyPresent()) {
            System.out.println("InfluxDB data already present. Skipping initialization.");
            return;
        }        System.out.println("Starting InfluxDB data initialization...");

        // Generisanje i upisivanje podataka za oba merenja
        List<StreamingPerformance> performanceData = generateStreamingPerformance(1005);
        List<UserInteraction> interactionData = generateUserInteractions(1010);

        try (WriteApi writeApi = influxDBClient.getWriteApi()) {
            // Upisivanje podataka za StreamingPerformance
            writeApi.writeMeasurements(WritePrecision.NS, performanceData);
            System.out.println(performanceData.size() + " streaming performance records written to InfluxDB.");

            // Upisivanje podataka za UserInteraction
            writeApi.writeMeasurements(WritePrecision.NS, interactionData);
            System.out.println(interactionData.size() + " user interaction records written to InfluxDB.");
        }

        System.out.println("InfluxDB data initialization finished.");
    }

    private boolean isDataAlreadyPresent() {
        // Flux upit koji traži samo JEDAN zapis iz merenja 'streaming_performance'
        String fluxQuery = String.format(
                "from(bucket: \"%s\") |> range(start: -30d) |> filter(fn: (r) => r._measurement == \"streaming_performance\") |> limit(n: 1)",
                bucketName
        );

        QueryApi queryApi = influxDBClient.getQueryApi();
        List<FluxTable> tables = queryApi.query(fluxQuery);

        // Ako upit vrati bilo koju tabelu koja nije prazna, znači da podaci postoje.
        return !tables.isEmpty() && !tables.get(0).getRecords().isEmpty();
    }

    // --- Metoda za generisanje podataka za StreamingPerformance ---
    private List<StreamingPerformance> generateStreamingPerformance(int count) {
        List<StreamingPerformance> dataPoints = new ArrayList<>();

        // Konzistentni podaci sa Neo4j skriptom
        List<String> userIds = List.of("101", "102", "103", "104", "105", "106", "107");
        List<String> movieIds = List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
        List<String> regions = List.of("EU-WEST", "US-EAST", "ASIA-SOUTH", "EU-CENTRAL", "US-WEST");
        List<String> devices = List.of("WEB", "MOBILE", "TV", "TABLET");
        List<String> resolutions = List.of("720p", "1080p", "4K");
        Random random = new Random();
        Instant timestamp = Instant.now();

        for (int i = 0; i < count; i++) {
            StreamingPerformance point = new StreamingPerformance();
            point.setUserId(userIds.get(random.nextInt(userIds.size())));
            point.setMovieId(movieIds.get(random.nextInt(movieIds.size())));
            point.setRegion(regions.get(random.nextInt(regions.size())));
            point.setDeviceType(devices.get(random.nextInt(devices.size())));
            point.setBufferingTimeMs(random.longs(50, 500).findFirst().getAsLong());
            point.setBitrateKbps(random.ints(1500, 8000).findFirst().getAsInt());
            point.setResolution(resolutions.get(random.nextInt(resolutions.size())));
            point.setTime(timestamp.minus(i, ChronoUnit.MINUTES));
            dataPoints.add(point);
        }
        return dataPoints;
    }

    // --- Metoda za generisanje podataka za UserInteraction ---
    private List<UserInteraction> generateUserInteractions(int count) {
        List<UserInteraction> interactions = new ArrayList<>();

        // Konzistentni podaci sa Neo4j skriptom
        List<String> userIds = List.of("101", "102", "103", "104", "105", "106", "107");
        List<String> movieIds = List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
        List<String> devices = List.of("WEB", "MOBILE", "TV", "TABLET");
        List<String> interactionTypes = List.of("PLAY", "PAUSE", "SEEK_FORWARD", "SEEK_BACKWARD", "ENDED");
        Random random = new Random();
        Instant timestamp = Instant.now();

        for (int i = 0; i < count; i++) {
            UserInteraction interaction = new UserInteraction();
            interaction.setUserId(userIds.get(random.nextInt(userIds.size())));
            interaction.setMovieId(movieIds.get(random.nextInt(movieIds.size())));
            interaction.setDeviceType(devices.get(random.nextInt(devices.size())));
            interaction.setInteractionType(interactionTypes.get(random.nextInt(interactionTypes.size())));

            // Simulacija pozicije u videu (npr. do 3 sata = 10800 sekundi)
            interaction.setVideoTimestampSec(random.longs(0, 10800).findFirst().getAsLong());

            // Vremenska oznaka kada se desio događaj
            // Množimo sa 30 sekundi da bi se interakcije dešavale češće od metrika performansi
            interaction.setTime(timestamp.minus(i * 30L, ChronoUnit.SECONDS));

            interactions.add(interaction);
        }
        return interactions;
    }
}