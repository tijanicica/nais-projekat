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

        // KOMPLETNA LISTA OD 45 DOGAĐAJA PREPISANA IZ neo4j.txt
        List<WatchedEvent> sourceOfTruthEvents = Arrays.asList(
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
                new WatchedEvent(1006, 42, "V for Vendetta"),
                new WatchedEvent(1006, 50, "The Social Network"),
                new WatchedEvent(1006, 60, "Glass Onion"),
                new WatchedEvent(1007, 70, "Black Panther"),
                new WatchedEvent(1007, 71, "Wonder Woman"),
                new WatchedEvent(1007, 72, "Thor: Ragnarok"),
                new WatchedEvent(1008, 80, "Fantastic Mr. Fox"),
                new WatchedEvent(1008, 81, "Isle of Dogs"),
                new WatchedEvent(1008, 82, "The French Dispatch"),
                new WatchedEvent(1009, 91, "Star Wars: A New Hope"),
                new WatchedEvent(1009, 92, "The Empire Strikes Back"),
                new WatchedEvent(1009, 93, "Return of the Jedi"),
                new WatchedEvent(1010, 102, "Iron Man"),
                new WatchedEvent(1010, 105, "Iron Man 2"),
                new WatchedEvent(1010, 110, "Ant-Man"),
                new WatchedEvent(1011, 121, "The Batman"),
                new WatchedEvent(1011, 122, "Justice League"),
                new WatchedEvent(1011, 125, "Suicide Squad"),
                new WatchedEvent(1012, 133, "John Wick"),
                new WatchedEvent(1012, 137, "Speed"),
                new WatchedEvent(1013, 138, "Point Break"),
                new WatchedEvent(1014, 150, "My Own Private Idaho"),
                new WatchedEvent(1015, 160, "Prince of Pennsylvania"),
                new WatchedEvent(1016, 170, "Johnny Mnemonic"),
                new WatchedEvent(1010, 1, "Inception"),
                new WatchedEvent(1015, 2, "The Matrix"),
                new WatchedEvent(1020, 4, "The Dark Knight"),
                new WatchedEvent(1018, 10, "Interstellar"),
                new WatchedEvent(1017, 133, "John Wick")
        );

        Random random = new Random();
        String[] deviceTypes = {"Smart TV", "Laptop", "Mobile", "Tablet"};
        int totalEventsCreated = 0;

        // Prolazimo kroz svaku od 45 "istinitih" veza
        for (WatchedEvent event : sourceOfTruthEvents) {

            // Za svaku vezu, generišemo nasumičan broj događaja (npr. između 5 i 14)
            int numberOfSessions = 5 + random.nextInt(10); // Generisaće od 5 do 14 sesija

            for (int i = 0; i < numberOfSessions; i++) {
                // Generišemo nasumične podatke za polja koja se menjaju po sesiji
                int stoppedAtSeconds = 100 + random.nextInt(8000); // Gledao između ~2 min i ~2.2h
                String deviceType = deviceTypes[random.nextInt(deviceTypes.length)];

                // Pozivamo servisnu metodu sa KONZISTENTNIM ID-jevima i nasumičnim podacima
                historyService.recordViewingActivity(
                        event.userId,
                        event.movieId,
                        stoppedAtSeconds,
                        deviceType,
                        event.movieTitle
                );
                totalEventsCreated++;
            }
        }

        System.out.println("Data initialization finished. " + totalEventsCreated + " consistent viewing events recorded.");
    }
}



/*package com.nais.history_service;

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
}*/