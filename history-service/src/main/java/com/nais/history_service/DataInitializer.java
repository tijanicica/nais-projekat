package com.nais.history_service;

import com.nais.history_service.service.HistoryService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Component
public class DataInitializer implements CommandLineRunner {

    private final HistoryService historyService;

    public DataInitializer(HistoryService historyService) {
        this.historyService = historyService;
    }

    // Pomoćna klasa samo za čuvanje podataka iz neo4j.txt
    private static class WatchedEvent {
        long userId;
        long movieId;
        String movieTitle;

        WatchedEvent(long userId, long movieId, String movieTitle) {
            this.userId = userId;
            this.movieId = movieId;
            this.movieTitle = movieTitle;
        }
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Starting data initialization for History Service...");

        // Lista događaja "prepisana" iz WATCHED veza u neo4j.txt
        // Dodajte još događaja po potrebi da imate dovoljno test podataka
        List<WatchedEvent> events = Arrays.asList(
                new WatchedEvent(1001, 1, "Inception"),
                new WatchedEvent(1001, 2, "The Matrix"),
                new WatchedEvent(1001, 4, "The Dark Knight"),
                new WatchedEvent(1002, 1, "Inception"),
                new WatchedEvent(1002, 3, "Pulp Fiction"),
                new WatchedEvent(1002, 8, "The Lord of the Rings: The Fellowship of the Ring"),
                new WatchedEvent(1003, 2, "The Matrix"),
                new WatchedEvent(1003, 5, "Forrest Gump"),
                new WatchedEvent(1003, 10, "Interstellar"),
                new WatchedEvent(1004, 7, "Raiders of the Lost Ark"),
                new WatchedEvent(1004, 11, "The Shawshank Redemption"),
                new WatchedEvent(1004, 25, "The Avengers"),
                new WatchedEvent(1005, 13, "Titanic"),
                new WatchedEvent(1005, 14, "Avatar"),
                new WatchedEvent(1005, 30, "Inglourious Basterds"),
                new WatchedEvent(1009, 91, "Star Wars: A New Hope"),
                new WatchedEvent(1009, 92, "The Empire Strikes Back"),
                new WatchedEvent(1012, 133, "John Wick")
        );

        Random random = new Random();
        String[] deviceTypes = {"Smart TV", "Laptop", "Mobile", "Tablet"};

        for (WatchedEvent event : events) {
            // Generišemo nasumične podatke za polja koja nemamo u neo4j.txt
            int stoppedAtSeconds = 600 + random.nextInt(3000); // Gledao između 10 i 60 minuta
            String deviceType = deviceTypes[random.nextInt(deviceTypes.length)];

            // Pozivamo našu glavnu servisnu metodu
            // Ona će konzistentno popuniti SVIH 5 tabela za ovaj jedan događaj
            historyService.recordViewingActivity(
                    event.userId,
                    event.movieId,
                    stoppedAtSeconds,
                    deviceType,
                    event.movieTitle
            );
        }

        System.out.println("Data initialization finished. " + events.size() + " viewing events recorded.");
    }
}