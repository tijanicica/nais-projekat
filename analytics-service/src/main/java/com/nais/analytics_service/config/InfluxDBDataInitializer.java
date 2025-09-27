package com.nais.analytics_service.config;

import com.influxdb.client.DeleteApi;
import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Value;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApi;
import com.nais.analytics_service.model.StreamingPerformance;
import com.nais.analytics_service.model.UserInteraction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;

@Component
public class InfluxDBDataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(InfluxDBDataInitializer.class);

    @Autowired
    private InfluxDBClient influxDBClient;
    @Value("${spring.influx.bucket}")
    private String bucket;

    @Value("${spring.influx.org}")
    private String org;

    private record WatchedEvent(String userId, String movieId) {}

    @Override
    public void run(String... args) throws Exception {

        clearPreviousData();

        logger.info("Starting InfluxDB data seeding...");

        List<WatchedEvent> watchedEvents = getWatchedEvents();
        logger.info("Found {} base watched events from Neo4j data.", watchedEvents.size());

        List<UserInteraction> userInteractions = new ArrayList<>();
        List<StreamingPerformance> streamingPerformances = new ArrayList<>();
        Random random = new Random();

        Instant timelineCurrentTime = ZonedDateTime.of(2023, 5, 1, 20, 0, 0, 0, ZoneId.of("UTC")).toInstant();

        for (WatchedEvent event : watchedEvents) {
            int interactionCount = random.nextInt(26) + 25; // 25 to 50
            int performanceCount = random.nextInt(26) + 25; // 25 to 50

            generateUserInteractionsForEvent(userInteractions, event, timelineCurrentTime, interactionCount, random);
            generateStreamingPerformanceForEvent(streamingPerformances, event, timelineCurrentTime, performanceCount, random);


            long daysToAdd = random.nextInt(7) + 1;
            long hoursToAdd = random.nextInt(24);
            timelineCurrentTime = timelineCurrentTime.plus(daysToAdd, ChronoUnit.DAYS).plus(hoursToAdd, ChronoUnit.HOURS);
        }

        logger.info("Generated {} UserInteraction records.", userInteractions.size());
        logger.info("Generated {} StreamingPerformance records.", streamingPerformances.size());

        if (userInteractions.size() >= 1000 && streamingPerformances.size() >= 1000) {
            logger.info("Requirement of at least 1000 records per measurement is met.");
        } else {
            logger.warn("Warning: Generated less than 1000 records for one of the measurements.");
        }

        writeDataToInflux(userInteractions, streamingPerformances);
        logger.info("InfluxDB data seeding finished successfully.");
    }

    private void clearPreviousData() {
        logger.info("Clearing previous data from bucket: {}", bucket);
        DeleteApi deleteApi = influxDBClient.getDeleteApi();

        OffsetDateTime start = OffsetDateTime.parse("2023-01-01T00:00:00Z");
        OffsetDateTime stop = OffsetDateTime.now().plusYears(1); // Sada + 1 godina, za svaki slučaj

        try {
            String predicateUserInteraction = "_measurement=\"user_interaction\"";
            deleteApi.delete(start, stop, predicateUserInteraction, bucket, org);
            logger.info("Cleared all data from 'user_interaction' measurement.");

            String predicateStreamingPerformance = "_measurement=\"streaming_performance\"";
            deleteApi.delete(start, stop, predicateStreamingPerformance, bucket, org);
            logger.info("Cleared all data from 'streaming_performance' measurement.");

        } catch (Exception e) {
            logger.error("Failed to clear previous data from InfluxDB.", e);
        }
    }

    private void generateUserInteractionsForEvent(List<UserInteraction> interactions, WatchedEvent event, Instant startTime, int count, Random random) {
        String[] interactionTypes = {"PLAY", "PAUSE", "SEEK_FORWARD", "PLAY", "PAUSE"};
        String[] deviceTypes = {"WEB", "MOBILE", "TV", "TABLET"};
        Instant currentTime = startTime;
        long videoTimestampSec = 0;

        for (int i = 0; i < count; i++) {
            UserInteraction interaction = new UserInteraction();
            interaction.setUserId(event.userId());
            interaction.setMovieId(event.movieId());
            interaction.setDeviceType(deviceTypes[random.nextInt(deviceTypes.length)]);
            String interactionType = interactionTypes[random.nextInt(interactionTypes.length)];
            interaction.setInteractionType(interactionType);

            if ("PLAY".equals(interactionType)) {
                videoTimestampSec += random.nextInt(300) + 30;
            }
            if ("SEEK_FORWARD".equals(interactionType)) {
                videoTimestampSec += random.nextInt(600) + 60;
            }
            interaction.setVideoTimestampSec(videoTimestampSec);
            // Vreme se pomera unapred u odnosu na početno vreme gledanja
            currentTime = currentTime.plusSeconds(random.nextInt(300) + 10);
            interaction.setTime(currentTime);
            interactions.add(interaction);
        }
    }

    private void generateStreamingPerformanceForEvent(List<StreamingPerformance> performances, WatchedEvent event, Instant startTime, int count, Random random) {
        String[] regions = {"EU-WEST", "EU-CENTRAL", "US-EAST", "US-WEST", "ASIA-SOUTH"};
        String[] deviceTypes = {"WEB", "MOBILE", "TV", "TABLET"};
        String[] resolutions = {"720p", "1080p", "4K", "1080p", "1080p"};
        Instant currentTime = startTime; // Počinjemo od prosleđenog vremena

        for (int i = 0; i < count; i++) {
            StreamingPerformance performance = new StreamingPerformance();
            performance.setUserId(event.userId());
            performance.setMovieId(event.movieId());
            performance.setRegion(regions[random.nextInt(regions.length)]);
            performance.setDeviceType(deviceTypes[random.nextInt(deviceTypes.length)]);
            performance.setBufferingTimeMs((long) (random.nextDouble() < 0.1 ? random.nextInt(2000) + 500 : random.nextInt(100)));
            performance.setBitrateKbps(random.nextInt(15000) + 3000);
            performance.setResolution(resolutions[random.nextInt(resolutions.length)]);
            // Vreme se pomera unapred u odnosu na početno vreme gledanja
            currentTime = currentTime.plusSeconds(random.nextInt(120) + 30);
            performance.setTime(currentTime);
            performances.add(performance);
        }
    }

    private void writeDataToInflux(List<UserInteraction> interactions, List<StreamingPerformance> performances) {
        try (WriteApi writeApi = influxDBClient.getWriteApi()) {

            logger.info("Writing {} UserInteraction records to InfluxDB bucket...", interactions.size());
            for (UserInteraction interaction : interactions) {
                // Ručno kreiramo Point objekat iz UserInteraction POJO
                Point point = Point
                        .measurement("user_interaction")
                        .addTag("userId", interaction.getUserId())
                        .addTag("movieId", interaction.getMovieId())
                        .addTag("deviceType", interaction.getDeviceType())
                        .addTag("interactionType", interaction.getInteractionType())
                        .addField("videoTimestampSec", interaction.getVideoTimestampSec())
                        .time(interaction.getTime(), WritePrecision.NS); // Obavezno specificirati preciznost

                writeApi.writePoint(point); // Koristimo metodu writePoint
            }

            logger.info("Writing {} StreamingPerformance records to InfluxDB bucket...", performances.size());
            for (StreamingPerformance performance : performances) {
                // Ručno kreiramo Point objekat iz StreamingPerformance POJO
                Point point = Point
                        .measurement("streaming_performance")
                        .addTag("userId", performance.getUserId())
                        .addTag("movieId", performance.getMovieId())
                        .addTag("region", performance.getRegion())
                        .addTag("deviceType", performance.getDeviceType())
                        .addField("bufferingTimeMs", performance.getBufferingTimeMs())
                        .addField("bitrateKbps", performance.getBitrateKbps())
                        .addField("resolution", performance.getResolution())
                        .time(performance.getTime(), WritePrecision.NS); // Obavezno specificirati preciznost

                writeApi.writePoint(point); // Koristimo metodu writePoint
            }

            writeApi.flush();
            logger.info("Data successfully flushed to InfluxDB.");

        } catch (Exception e) {
            logger.error("Failed to write data to InfluxDB", e);
        }
    }

    private List<WatchedEvent> getWatchedEvents() {
        return List.of(
                new WatchedEvent("1001", "1"), new WatchedEvent("1001", "2"), new WatchedEvent("1001", "4"),
                new WatchedEvent("1002", "1"), new WatchedEvent("1002", "3"), new WatchedEvent("1002", "8"),
                new WatchedEvent("1003", "2"), new WatchedEvent("1003", "5"), new WatchedEvent("1003", "10"),
                new WatchedEvent("1004", "7"), new WatchedEvent("1004", "11"), new WatchedEvent("1004", "25"),
                new WatchedEvent("1005", "13"), new WatchedEvent("1005", "14"), new WatchedEvent("1005", "30"),
                new WatchedEvent("1006", "42"), new WatchedEvent("1006", "50"), new WatchedEvent("1006", "60"),
                new WatchedEvent("1007", "70"), new WatchedEvent("1007", "71"), new WatchedEvent("1007", "72"),
                new WatchedEvent("1008", "80"), new WatchedEvent("1008", "81"), new WatchedEvent("1008", "82"),
                new WatchedEvent("1009", "91"), new WatchedEvent("1009", "92"), new WatchedEvent("1009", "93"),
                new WatchedEvent("1010", "102"), new WatchedEvent("1010", "105"), new WatchedEvent("1010", "110"),
                new WatchedEvent("1011", "121"), new WatchedEvent("1011", "122"), new WatchedEvent("1011", "125"),
                new WatchedEvent("1012", "133"), new WatchedEvent("1012", "137"),
                new WatchedEvent("1013", "138"),
                new WatchedEvent("1014", "150"),
                new WatchedEvent("1015", "160"),
                new WatchedEvent("1016", "170")
        );
    }
}