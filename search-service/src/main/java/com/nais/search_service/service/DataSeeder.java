package com.nais.search_service.service;

import com.nais.search_service.dto.ActorDocumentDto;
import com.nais.search_service.dto.MovieDocumentDto;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.base.Result;
import io.weaviate.client.v1.batch.model.ObjectGetResponse;
import io.weaviate.client.v1.data.model.WeaviateObject;
import io.weaviate.client.v1.schema.model.DataType;
import io.weaviate.client.v1.schema.model.Property;
import io.weaviate.client.v1.schema.model.WeaviateClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.boot.CommandLineRunner;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private final WeaviateClient client;

    // Definišemo imena naših kolekcija (klasa)
    public static final String MOVIE_CLASS = "Movie";
    public static final String ACTOR_CLASS = "Actor";

    public DataSeeder(WeaviateClient weaviateClient) {
        this.client = weaviateClient;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting Weaviate data seeding...");

        // 1. Kreiraj ili ponovo kreiraj šeme za obe kolekcije
        createOrRecreateSchemas();

        // 2. Popuni kolekciju filmova
        seedMovies();

        // 3. Popuni kolekciju glumaca
        seedActors();

        log.info("Data seeding completed successfully.");
    }

    private void createOrRecreateSchemas() {
        log.info("Defining Weaviate schemas...");

        // --- Kreiranje šeme za klasu Movie ---
        deleteClassIfExists(MOVIE_CLASS);
        WeaviateClass movieClass = WeaviateClass.builder()
                .className(MOVIE_CLASS)
                .description("A collection of movies")
                .vectorizer("text2vec-contextionary") // ISPRAVKA: Koristi transformer umesto OpenAI
                .properties(Arrays.asList(
                        Property.builder().name("movieId").dataType(Arrays.asList(DataType.INT)).build(),
                        Property.builder().name("title").dataType(Arrays.asList(DataType.TEXT)).build(),
                        Property.builder().name("releaseYear").dataType(Arrays.asList(DataType.INT)).build(),
                        Property.builder().name("genre").dataType(Arrays.asList(DataType.TEXT)).build(),
                        // Vektorizovano polje sa eksplicitnim indexom
                        Property.builder()
                                .name("description")
                                .dataType(Arrays.asList(DataType.TEXT))
                                .indexSearchable(true) // DODATO: Eksplicitni indeks
                                .build()
                ))
                .build();

        Result<Boolean> movieClassResult = client.schema().classCreator().withClass(movieClass).run();
        if (movieClassResult.hasErrors()) {
            log.error("Failed to create Movie class: {}", movieClassResult.getError().getMessages());
        } else {
            log.info("Successfully created Movie class.");
        }

        // --- Kreiranje šeme za klasu Actor ---
        deleteClassIfExists(ACTOR_CLASS);
        WeaviateClass actorClass = WeaviateClass.builder()
                .className(ACTOR_CLASS)
                .description("A collection of actors")
                .vectorizer("text2vec-contextionary") // ISPRAVKA: Koristi transformer
                .properties(Arrays.asList(
                        Property.builder().name("actorId").dataType(Arrays.asList(DataType.INT)).build(),
                        Property.builder()
                                .name("name")
                                .dataType(Arrays.asList(DataType.TEXT))
                                .indexSearchable(true) // DODATO: Indeks za nevektorizovano polje
                                .build(),
                        Property.builder().name("birthYear").dataType(Arrays.asList(DataType.INT)).build(),
                        Property.builder().name("nationality").dataType(Arrays.asList(DataType.TEXT)).build(),
                        // Vektorizovano polje sa eksplicitnim indexom
                        Property.builder()
                                .name("biography")
                                .dataType(Arrays.asList(DataType.TEXT))
                                .indexSearchable(true) // DODATO: Eksplicitni indeks
                                .build()
                ))
                .build();

        Result<Boolean> actorClassResult = client.schema().classCreator().withClass(actorClass).run();
        if (actorClassResult.hasErrors()) {
            log.error("Failed to create Actor class: {}", actorClassResult.getError().getMessages());
        } else {
            log.info("Successfully created Actor class.");
        }
    }

    private void deleteClassIfExists(String className) {
        Result<Boolean> classExists = client.schema().exists().withClassName(className).run();
        if (classExists.getResult() != null && classExists.getResult()) {
            log.warn("Class '{}' already exists. Deleting it.", className);
            Result<Boolean> deleteResult = client.schema().classDeleter().withClassName(className).run();
            if (deleteResult.hasErrors()) {
                log.error("Failed to delete class '{}': {}", className, deleteResult.getError().getMessages());
            } else {
                log.info("Class '{}' deleted successfully.", className);
            }
        }
    }

    private void seedMovies() {
        log.info("Seeding movies...");
        List<MovieDocumentDto> movies = createCompleteMovieList();

        List<WeaviateObject> movieObjects = movies.stream().map(movie -> {
            Map<String, Object> properties = new HashMap<>();
            properties.put("movieId", movie.getMovieId());
            properties.put("title", movie.getTitle());
            properties.put("description", movie.getDescription());
            properties.put("releaseYear", movie.getReleaseYear());
            properties.put("genre", movie.getGenre());

            return WeaviateObject.builder()
                    .className(MOVIE_CLASS)
                    .id(UUID.randomUUID().toString())
                    .properties(properties)
                    .build();
        }).collect(Collectors.toList());

        Result<ObjectGetResponse[]> batchResult = client.batch().objectsBatcher()
                .withObjects(movieObjects.toArray(new WeaviateObject[0]))
                .run();

        if (batchResult.hasErrors()) {
            log.error("Failed to seed movies: {}", batchResult.getError().getMessages());
        } else {
            log.info("Successfully seeded {} movies.", batchResult.getResult().length);
        }
    }

    private void seedActors() {
        log.info("Seeding actors...");
        List<ActorDocumentDto> actors = createCompleteActorList();

        List<WeaviateObject> actorObjects = actors.stream().map(actor -> {
            Map<String, Object> properties = new HashMap<>();
            properties.put("actorId", actor.getActorId());
            properties.put("name", actor.getName());
            properties.put("biography", actor.getBiography());
            properties.put("birthYear", actor.getBirthYear());
            properties.put("nationality", actor.getNationality());

            return WeaviateObject.builder()
                    .className(ACTOR_CLASS)
                    .id(UUID.randomUUID().toString())
                    .properties(properties)
                    .build();
        }).collect(Collectors.toList());

        Result<ObjectGetResponse[]> batchResult = client.batch().objectsBatcher()
                .withObjects(actorObjects.toArray(new WeaviateObject[0]))
                .run();

        if (batchResult.hasErrors()) {
            log.error("Failed to seed actors: {}", batchResult.getError().getMessages());
        } else {
            log.info("Successfully seeded {} actors.", batchResult.getResult().length);
        }
    }

    /**
     * Kreira kompletnu listu od 200 filmova sa relevantnim podacima.
     */
    private List<MovieDocumentDto> createCompleteMovieList() {
        List<MovieDocumentDto> movies = new ArrayList<>();

        movies.add(new MovieDocumentDto(1L, "Inception", "A thief who steals corporate secrets through dream-sharing technology is given the inverse task of planting an idea into the mind of a C.E.O.", 2010, "Sci-Fi/Thriller"));
        movies.add(new MovieDocumentDto(2L, "The Matrix", "A computer hacker learns from mysterious rebels about the true nature of his reality and his role in the war against its controllers.", 1999, "Sci-Fi/Action"));
        movies.add(new MovieDocumentDto(3L, "Pulp Fiction", "The lives of two mob hitmen, a boxer, a gangster and his wife, and a pair of diner bandits intertwine in four tales of violence and redemption.", 1994, "Crime/Drama"));
        movies.add(new MovieDocumentDto(4L, "The Dark Knight", "When the menace known as the Joker wreaks havoc and chaos on the people of Gotham, Batman must accept one of the greatest psychological and physical tests of his ability to fight injustice.", 2008, "Action/Crime"));
        movies.add(new MovieDocumentDto(5L, "Forrest Gump", "The presidencies of Kennedy and Johnson, the Vietnam War, and other historical events unfold from the perspective of an Alabama man with a low IQ, whose only desire is to be reunited with his childhood sweetheart.", 1994, "Drama/Romance"));
        movies.add(new MovieDocumentDto(6L, "The Godfather", "The aging patriarch of an organized crime dynasty transfers control of his clandestine empire to his reluctant son.", 1972, "Crime/Drama"));
        movies.add(new MovieDocumentDto(7L, "Raiders of the Lost Ark", "In 1936, archaeologist and adventurer Indiana Jones is hired by the U.S. government to find the Ark of the Covenant before Adolf Hitler's Nazis can obtain its awesome powers.", 1981, "Action/Adventure"));
        movies.add(new MovieDocumentDto(8L, "The Lord of the Rings: The Fellowship of the Ring", "A meek Hobbit from the Shire and eight companions set out on a journey to destroy the powerful One Ring and save Middle-earth from the Dark Lord Sauron.", 2001, "Fantasy/Adventure"));
        movies.add(new MovieDocumentDto(9L, "Fight Club", "An insomniac office worker looking for a way to change his life crosses paths with a devil-may-care soap maker and they form an underground fight club that evolves into something much, much more.", 1999, "Drama"));
        movies.add(new MovieDocumentDto(10L, "Interstellar", "A team of explorers travel through a wormhole in space in an attempt to ensure humanity's survival.", 2014, "Sci-Fi/Adventure"));
        movies.add(new MovieDocumentDto(11L, "The Shawshank Redemption", "Two imprisoned men bond over a number of years, finding solace and eventual redemption through acts of common decency.", 1994, "Drama"));
        movies.add(new MovieDocumentDto(12L, "Schindler's List", "In German-occupied Poland during World War II, industrialist Oskar Schindler gradually becomes concerned for his Jewish workforce after witnessing their persecution by the Nazis.", 1993, "Biography/Drama/History"));
        movies.add(new MovieDocumentDto(13L, "Titanic", "A seventeen-year-old aristocrat falls in love with a kind but poor artist aboard the luxurious, ill-fated R.M.S. Titanic.", 1997, "Romance/Drama"));
        movies.add(new MovieDocumentDto(14L, "Avatar", "A paraplegic Marine dispatched to the moon Pandora on a unique mission becomes torn between following orders and protecting the world he feels is his home.", 2009, "Sci-Fi/Action"));
        movies.add(new MovieDocumentDto(15L, "Gladiator", "A former Roman General sets out to exact vengeance against the corrupt emperor who murdered his family and sent him into slavery.", 2000, "Action/Drama"));
        movies.add(new MovieDocumentDto(16L, "Alien", "The crew of the commercial spaceship Nostromo is awakened from their cryo-sleep capsules halfway through their journey home to investigate a distress call from an alien vessel.", 1979, "Horror/Sci-Fi"));
        movies.add(new MovieDocumentDto(17L, "Blade Runner", "A blade runner must pursue and terminate four replicants who stole a ship in space, and have returned to Earth to find their creator.", 1982, "Sci-Fi/Thriller"));
        movies.add(new MovieDocumentDto(18L, "The Departed", "An undercover cop and a mole in the police attempt to identify each other while infiltrating an Irish gang in South Boston.", 2006, "Crime/Thriller"));
        movies.add(new MovieDocumentDto(19L, "Shutter Island", "In 1954, a U.S. Marshal investigates the disappearance of a murderer who escaped from a hospital for the criminally insane.", 2010, "Mystery/Thriller"));
        movies.add(new MovieDocumentDto(20L, "Zodiac", "In the late 1960s/early 1970s, a San Francisco cartoonist becomes an amateur detective obsessed with tracking down the Zodiac Killer.", 2007, "Crime/Mystery"));
        movies.add(new MovieDocumentDto(21L, "Se7en", "Two detectives, a rookie and a veteran, hunt a serial killer who uses the seven deadly sins as his motives.", 1995, "Crime/Thriller"));
        movies.add(new MovieDocumentDto(22L, "The Silence of the Lambs", "A young F.B.I. cadet must receive the help of an incarcerated and manipulative cannibal killer to help catch another serial killer.", 1991, "Thriller/Horror"));
        movies.add(new MovieDocumentDto(23L, "Goodfellas", "The story of Henry Hill and his life in the mob, covering his relationship with his wife Karen Hill and his mob partners Jimmy Conway and Tommy DeVito.", 1990, "Crime/Biography"));
        movies.add(new MovieDocumentDto(24L, "Casino", "A tale of greed, deception, money, power, and murder occur between two best friends: a mafia enforcer and a casino executive.", 1995, "Crime/Drama"));
        movies.add(new MovieDocumentDto(25L, "The Avengers", "Earth's mightiest heroes must come together and learn to fight as a team if they are going to stop Loki and his alien army from enslaving humanity.", 2012, "Action/Sci-Fi"));
        movies.add(new MovieDocumentDto(26L, "Jurassic Park", "A pragmatic paleontologist visiting an almost complete theme park is tasked with protecting a couple of kids after a power failure causes the park's cloned dinosaurs to run loose.", 1993, "Sci-Fi/Adventure"));
        movies.add(new MovieDocumentDto(27L, "E.T. the Extra-Terrestrial", "A troubled child summons the courage to help a friendly alien escape Earth and return to his home planet.", 1982, "Family/Sci-Fi"));
        movies.add(new MovieDocumentDto(28L, "Saving Private Ryan", "Following the Normandy Landings, a group of U.S. soldiers go behind enemy lines to retrieve a paratrooper whose brothers have been killed in action.", 1998, "War/Drama"));
        movies.add(new MovieDocumentDto(29L, "The Green Mile", "The lives of guards on Death Row are affected by one of their charges: a black man accused of child murder and rape, yet who has a mysterious gift.", 1999, "Fantasy/Drama"));
        movies.add(new MovieDocumentDto(30L, "Inglourious Basterds", "In Nazi-occupied France during World War II, a plan to assassinate Nazi leaders by a group of Jewish U.S. soldiers coincides with a theatre owner's vengeful plans for the same.", 2009, "War/Adventure"));
        movies.add(new MovieDocumentDto(31L, "Django Unchained", "With the help of a German bounty hunter, a freed slave sets out to rescue his wife from a brutal Mississippi plantation owner.", 2012, "Western/Drama"));
        movies.add(new MovieDocumentDto(32L, "The Hateful Eight", "In the dead of a Wyoming winter, a bounty hunter and his prisoner find shelter in a cabin currently inhabited by a collection of nefarious characters.", 2015, "Western/Mystery"));
        movies.add(new MovieDocumentDto(33L, "Kill Bill: Vol. 1", "After awakening from a four-year coma, a former assassin wreaks vengeance on the team of assassins who betrayed her.", 2003, "Action/Thriller"));
        movies.add(new MovieDocumentDto(34L, "Reservoir Dogs", "When a simple jewelry heist goes horribly wrong, the surviving criminals begin to suspect that one of them is a police informant.", 1992, "Crime/Thriller"));
        movies.add(new MovieDocumentDto(35L, "The Prestige", "After a tragic accident, two stage magicians engage in a battle to create the ultimate illusion while sacrificing everything they have to outwit each other.", 2006, "Mystery/Drama"));
        movies.add(new MovieDocumentDto(36L, "Memento", "A man with short-term memory loss attempts to track down his wife's murderer.", 2000, "Mystery/Thriller"));
        movies.add(new MovieDocumentDto(37L, "Batman Begins", "After training with his mentor, Batman begins his fight to free crime-ridden Gotham City from corruption.", 2005, "Action/Adventure"));
        movies.add(new MovieDocumentDto(38L, "The Lord of the Rings: The Two Towers", "While Frodo and Sam edge closer to Mordor with the help of the shifty Gollum, the divided fellowship makes a stand against Sauron's new ally, Saruman, and his hordes of Isengard.", 2002, "Fantasy/Adventure"));
        movies.add(new MovieDocumentDto(39L, "The Lord of the Rings: The Return of the King", "Gandalf and Aragorn lead the World of Men against Sauron's army to draw his gaze from Frodo and Sam as they approach Mount Doom with the One Ring.", 2003, "Fantasy/Adventure"));
        movies.add(new MovieDocumentDto(40L, "Apocalypse Now", "A U.S. Army officer serving in Vietnam is tasked with assassinating a renegade Special Forces Colonel who sees himself as a god.", 1979, "War/Drama"));
        movies.add(new MovieDocumentDto(41L, "The Godfather: Part II", "The early life and career of Vito Corleone in 1920s New York City is portrayed, while his son, Michael, expands and tightens his grip on the family crime syndicate.", 1974, "Crime/Drama"));
        movies.add(new MovieDocumentDto(42L, "V for Vendetta", "In a future British tyranny, a shadowy freedom fighter, known only by the alias of 'V', plots to overthrow it with the help of a young woman.", 2005, "Action/Thriller"));
        movies.add(new MovieDocumentDto(43L, "Children of Men", "In 2027, in a chaotic world in which women have become infertile, a former activist agrees to help transport a miraculously pregnant woman to a sanctuary at sea.", 2006, "Sci-Fi/Thriller"));
        movies.add(new MovieDocumentDto(44L, "No Country for Old Men", "Violence and mayhem ensue after a hunter stumbles upon a drug deal gone wrong and more than two million dollars in cash near the Rio Grande.", 2007, "Crime/Thriller"));
        movies.add(new MovieDocumentDto(45L, "There Will Be Blood", "A story of family, religion, hatred, oil and madness, focusing on a turn-of-the-century prospector in the early days of the business.", 2007, "Drama"));
        movies.add(new MovieDocumentDto(46L, "Mad Max: Fury Road", "In a post-apocalyptic wasteland, a woman rebels against a tyrannical ruler in search for her homeland with the help of a group of female prisoners, a psychotic worshiper, and a drifter named Max.", 2015, "Action/Sci-Fi"));
        movies.add(new MovieDocumentDto(47L, "Parasite", "Greed and class discrimination threaten the newly formed symbiotic relationship between the wealthy Park family and the destitute Kim clan.", 2019, "Thriller/Comedy/Drama"));
        movies.add(new MovieDocumentDto(48L, "Joker", "A mentally troubled comedian embarks on a downward spiral of revolution and bloody crime in Gotham City, bringing him face-to-face with his alter-ego: the Joker.", 2019, "Crime/Drama/Thriller"));
        movies.add(new MovieDocumentDto(49L, "Once Upon a Time in Hollywood", "A faded television actor and his stunt double strive to achieve fame and success in the final years of Hollywood's Golden Age in 1969 Los Angeles.", 2019, "Comedy/Drama"));
        movies.add(new MovieDocumentDto(50L, "The Social Network", "As Harvard student Mark Zuckerberg creates the social networking site that would become known as Facebook, he is sued by the twins who claimed he stole their idea, and by the co-founder who was later squeezed out of the business.", 2010, "Biography/Drama"));
        movies.add(new MovieDocumentDto(51L, "Dune", "Paul Atreides, a brilliant and gifted young man born into a great destiny beyond his understanding, must travel to the most dangerous planet in the universe to ensure the future of his family and his people.", 2021, "Sci-Fi/Adventure"));
        movies.add(new MovieDocumentDto(52L, "Blade Runner 2049", "Young Blade Runner K's discovery of a long-buried secret leads him to track down former Blade Runner Rick Deckard, who's been missing for thirty years.", 2017, "Sci-Fi/Mystery"));
        movies.add(new MovieDocumentDto(53L, "Arrival", "A linguist works with the military to communicate with alien lifeforms after twelve mysterious spacecraft appear around the world.", 2016, "Sci-Fi/Drama"));
        movies.add(new MovieDocumentDto(54L, "La La Land", "While navigating their careers in Los Angeles, a pianist and an actress fall in love while attempting to reconcile their aspirations for the future.", 2016, "Musical/Romance"));
        movies.add(new MovieDocumentDto(55L, "Whiplash", "A promising young drummer enrolls at a cut-throat music conservatory where his dreams of greatness are mentored by an instructor who will stop at nothing to realize a student's potential.", 2014, "Drama/Music"));
        movies.add(new MovieDocumentDto(56L, "Get Out", "A young African-American visits his white girlfriend's parents for the weekend, where his simmering uneasiness about their reception of him eventually reaches a boiling point.", 2017, "Horror/Mystery"));
        movies.add(new MovieDocumentDto(57L, "Us", "A family's serene beach vacation turns to chaos when their doppelgängers appear and begin to terrorize them.", 2019, "Horror/Thriller"));
        movies.add(new MovieDocumentDto(58L, "Nope", "The residents of a lonely gulch in inland California bear witness to an uncanny and chilling discovery.", 2022, "Horror/Sci-Fi"));
        movies.add(new MovieDocumentDto(59L, "Knives Out", "A detective investigates the death of a patriarch of an eccentric, combative family.", 2019, "Mystery/Comedy"));
        movies.add(new MovieDocumentDto(60L, "Glass Onion", "Famed Southern detective Benoit Blanc travels to Greece for his latest case.", 2022, "Mystery/Comedy"));
        movies.add(new MovieDocumentDto(61L, "Lady Bird", "In 2002, an artistically inclined seventeen-year-old girl comes of age in Sacramento, California.", 2017, "Comedy/Drama"));
        movies.add(new MovieDocumentDto(62L, "Little Women", "Jo March reflects back and forth on her life, telling the beloved story of the March sisters - four young women each determined to live life on her own terms.", 2019, "Drama/Romance"));
        movies.add(new MovieDocumentDto(63L, "Barbie", "Barbie suffers a crisis that leads her to question her world and her existence.", 2023, "Comedy/Fantasy"));
        movies.add(new MovieDocumentDto(64L, "Oppenheimer", "The story of American scientist J. Robert Oppenheimer and his role in the development of the atomic bomb.", 2023, "Biography/Drama/History"));
        movies.add(new MovieDocumentDto(65L, "Tenet", "Armed with only one word, Tenet, and fighting for the survival of the entire world, a Protagonist journeys through a twilight world of international espionage on a mission that will unfold in something beyond real time.", 2020, "Action/Sci-Fi"));
        movies.add(new MovieDocumentDto(66L, "Dunkirk", "Allied soldiers from Belgium, the British Empire, and France are surrounded by the German Army and evacuated during a fierce battle in World War II.", 2017, "War/History"));
        movies.add(new MovieDocumentDto(67L, "The Shape of Water", "At a top secret research facility in the 1960s, a lonely janitor forms a unique relationship with an amphibious creature that is being held in captivity.", 2017, "Fantasy/Romance"));
        movies.add(new MovieDocumentDto(68L, "Pan's Labyrinth", "In the Falangist Spain of 1944, the bookish young stepdaughter of a sadistic army officer escapes into an eerie but captivating fantasy world.", 2006, "Fantasy/Drama"));
        movies.add(new MovieDocumentDto(69L, "Moonlight", "A young African-American man grapples with his identity and sexuality while experiencing the everyday struggles of childhood, adolescence, and burgeoning adulthood.", 2016, "Drama"));
        movies.add(new MovieDocumentDto(70L, "Black Panther", "T'Challa, heir to the hidden but advanced kingdom of Wakanda, must step forward to lead his people into a new future and must confront a challenger from his country's past.", 2018, "Action/Adventure/Sci-Fi"));
        movies.add(new MovieDocumentDto(71L, "Wonder Woman", "When a pilot crashes and tells of conflict in the outside world, Diana, an Amazonian warrior in training, leaves home to fight a war, discovering her full powers and true destiny.", 2017, "Action/Fantasy"));
        movies.add(new MovieDocumentDto(72L, "Thor: Ragnarok", "Imprisoned on the planet Sakaar, Thor must race against time to return to Asgard and stop Ragnarök, the destruction of his world, at the hands of the powerful and ruthless Hela.", 2017, "Action/Comedy/Sci-Fi"));
        movies.add(new MovieDocumentDto(73L, "Jojo Rabbit", "A young boy in Hitler's army finds out his mother is hiding a Jewish girl in their home.", 2019, "Comedy/War"));
        movies.add(new MovieDocumentDto(74L, "The Grand Budapest Hotel", "The adventures of Gustave H, a legendary concierge at a famous hotel from the fictional Republic of Zubrowka between the first and second World Wars, and Zero Moustafa, the lobby boy who becomes his most trusted friend.", 2014, "Comedy/Adventure"));
        movies.add(new MovieDocumentDto(75L, "Moonrise Kingdom", "A pair of young lovers flee their New England town, which causes a local search party to fan out and find them.", 2012, "Comedy/Romance"));
        movies.add(new MovieDocumentDto(76L, "The Royal Tenenbaums", "The eccentric members of a dysfunctional family reluctantly gather under the same roof for various reasons.", 2001, "Comedy/Drama"));
        movies.add(new MovieDocumentDto(77L, "Rushmore", "A precocious and eccentric teenager vies for the affection of a beautiful elementary school teacher.", 1998, "Comedy/Drama"));
        movies.add(new MovieDocumentDto(78L, "The Darjeeling Limited", "A year after their father's funeral, three brothers travel across India by train in an attempt to bond with each other.", 2007, "Comedy/Adventure"));
        movies.add(new MovieDocumentDto(79L, "Bottle Rocket", "Three friends plan to pull off a simple robbery and go on the run.", 1996, "Comedy/Crime"));
        movies.add(new MovieDocumentDto(80L, "Fantastic Mr. Fox", "An urbane fox cannot resist returning to his farm raiding ways and then must help his community survive the farmers' retaliation.", 2009, "Animation/Comedy"));
        movies.add(new MovieDocumentDto(81L, "Isle of Dogs", "Set in Japan, a group of alpha dogs are exiled to a remote island, and a young boy sets off on a journey to find his lost dog.", 2018, "Animation/Comedy"));
        movies.add(new MovieDocumentDto(82L, "The French Dispatch", "A love letter to journalists set in an outpost of an American newspaper in a fictional twentieth-century French city that brings to life a collection of stories.", 2021, "Comedy/Drama"));
        movies.add(new MovieDocumentDto(83L, "Asteroid City", "Following a writer on his world famous fictional play about a grieving father who travels with his tech-obsessed family to small rural Asteroid City to compete in a junior stargazing event.", 2023, "Comedy/Sci-Fi"));
        movies.add(new MovieDocumentDto(84L, "Black Swan", "A committed dancer wins the lead role in a production of Tchaikovsky's 'Swan Lake' only to find herself struggling to maintain her sanity.", 2010, "Drama/Thriller"));
        movies.add(new MovieDocumentDto(85L, "Requiem for a Dream", "The drug-induced utopias of four Coney Island people are shattered when their addictions run deep.", 2000, "Drama"));
        movies.add(new MovieDocumentDto(86L, "Pi", "A paranoid mathematician searches for a key number that will unlock the universal patterns found in nature.", 1998, "Thriller/Sci-Fi"));
        movies.add(new MovieDocumentDto(87L, "The Fountain", "As a modern-day scientist, Tommy is struggling with mortality, desperately searching for the medical breakthrough that will save the life of his cancer-stricken wife, Izzi.", 2006, "Sci-Fi/Drama"));
        movies.add(new MovieDocumentDto(88L, "Mother!", "A couple's relationship is tested when uninvited guests arrive at their home, disrupting their tranquil existence.", 2017, "Horror/Mystery"));
        movies.add(new MovieDocumentDto(89L, "The Wrestler", "A faded professional wrestler must retire, but finds his quest for a new life outside the ring a dispiriting struggle.", 2008, "Drama/Sport"));
        movies.add(new MovieDocumentDto(90L, "Noah", "A man is chosen by his world's creator to undertake a momentous mission before an apocalyptic flood cleanses the world.", 2014, "Action/Adventure/Drama"));
        movies.add(new MovieDocumentDto(91L, "Star Wars: A New Hope", "Luke Skywalker joins forces with a Jedi Knight, a cocky pilot, a Wookiee and two droids to save the galaxy from the Empire's world-destroying battle station.", 1977, "Sci-Fi/Fantasy"));
        movies.add(new MovieDocumentDto(92L, "The Empire Strikes Back", "After the Rebels are brutally overpowered by the Empire on the ice planet Hoth, Luke Skywalker begins Jedi training with Yoda, while his friends are pursued by Darth Vader.", 1980, "Sci-Fi/Fantasy"));
        movies.add(new MovieDocumentDto(93L, "Return of the Jedi", "After a daring mission to rescue Han Solo from Jabba the Hutt, the Rebels dispatch to Endor to destroy the second Death Star.", 1983, "Sci-Fi/Fantasy"));
        movies.add(new MovieDocumentDto(94L, "The Phantom Menace", "Two Jedi escape a hostile blockade to find allies and come across a young boy who may bring balance to the Force, but the long dormant Sith resurface to claim their old glory.", 1999, "Sci-Fi/Fantasy"));
        movies.add(new MovieDocumentDto(95L, "Attack of the Clones", "Ten years after initially meeting, Anakin Skywalker shares a forbidden romance with Padmé Amidala, while Obi-Wan Kenobi investigates an assassination attempt on the senator and discovers a secret clone army.", 2002, "Sci-Fi/Fantasy"));
        movies.add(new MovieDocumentDto(96L, "Revenge of the Sith", "Three years into the Clone Wars, the Jedi rescue Palpatine from Count Dooku. As Obi-Wan pursues a new threat, Anakin acts as a double agent between the Jedi Council and Palpatine and is lured into a sinister plan to rule the galaxy.", 2005, "Sci-Fi/Fantasy"));
        movies.add(new MovieDocumentDto(97L, "The Force Awakens", "As a new threat to the galaxy rises, Rey, a desert scavenger, and Finn, an ex-stormtrooper, must join Han Solo and Chewbacca to search for the one hope of restoring peace.", 2015, "Sci-Fi/Fantasy"));
        movies.add(new MovieDocumentDto(98L, "The Last Jedi", "Rey develops her newly discovered abilities with the guidance of Luke Skywalker, who is unsettled by the strength of her powers. Meanwhile, the Resistance prepares for battle with the First Order.", 2017, "Sci-Fi/Fantasy"));
        movies.add(new MovieDocumentDto(99L, "The Rise of Skywalker", "The surviving members of the resistance face the First Order once again, and the legendary conflict between the Jedi and the Sith reaches its peak bringing the Skywalker saga to a definitive end.", 2019, "Sci-Fi/Fantasy"));
        movies.add(new MovieDocumentDto(100L, "Rogue One", "The daughter of an Imperial scientist joins the Rebel Alliance in a risky move to steal the plans for the Death Star.", 2016, "Sci-Fi/Action"));
        movies.add(new MovieDocumentDto(101L, "Solo", "During an adventure into the criminal underworld, Han Solo meets his future co-pilot Chewbacca and encounters Lando Calrissian years before joining the Rebellion.", 2018, "Sci-Fi/Action"));
        movies.add(new MovieDocumentDto(102L, "Iron Man", "After being held captive in an Afghan cave, billionaire engineer Tony Stark creates a unique weaponized suit of armor to fight evil.", 2008, "Action/Sci-Fi"));
        movies.add(new MovieDocumentDto(103L, "Captain America: The First Avenger", "Steve Rogers, a rejected military soldier, transforms into Captain America after taking a dose of a 'Super-Soldier serum'. But being Captain America comes at a price.", 2011, "Action/Sci-Fi"));
        movies.add(new MovieDocumentDto(104L, "Thor", "The powerful but arrogant god Thor is cast out of Asgard to live amongst humans in Midgard (Earth), where he soon becomes one of their finest defenders.", 2011, "Action/Fantasy"));
        movies.add(new MovieDocumentDto(105L, "Iron Man 2", "With the world now aware of his identity as Iron Man, Tony Stark must contend with both his declining health and a vengeful mad man with ties to his father's legacy.", 2010, "Action/Sci-Fi"));
        movies.add(new MovieDocumentDto(106L, "The Incredible Hulk", "Bruce Banner, a scientist on the run from the U.S. Government, must find a cure for the monster he turns into whenever he loses his temper.", 2008, "Action/Sci-Fi"));
        movies.add(new MovieDocumentDto(107L, "Captain America: The Winter Soldier", "As Steve Rogers struggles to embrace his role in the modern world, he teams up with a fellow Avenger and S.H.I.E.L.D agent, Black Widow, to battle a new threat from history: an assassin known as the Winter Soldier.", 2014, "Action/Sci-Fi"));
        movies.add(new MovieDocumentDto(108L, "Guardians of the Galaxy", "A group of intergalactic criminals must pull together to stop a fanatical warrior with plans to purge the universe.", 2014, "Action/Sci-Fi/Comedy"));
        movies.add(new MovieDocumentDto(109L, "Avengers: Age of Ultron", "When Tony Stark and Bruce Banner try to jump-start a dormant peacekeeping program called Ultron, things go horribly wrong and it's up to Earth's mightiest heroes to stop the villainous Ultron from enacting his terrible plan.", 2015, "Action/Sci-Fi"));
        movies.add(new MovieDocumentDto(110L, "Ant-Man", "Armed with a super-suit with the astonishing ability to shrink in scale but increase in strength, cat burglar Scott Lang must embrace his inner hero and help his mentor, Dr. Hank Pym, plan and pull off a heist that will save the world.", 2015, "Action/Sci-Fi/Comedy"));
        movies.add(new MovieDocumentDto(111L, "Captain America: Civil War", "Political involvement in the Avengers' affairs causes a rift between Captain America and Iron Man.", 2016, "Action/Sci-Fi"));
        movies.add(new MovieDocumentDto(112L, "Doctor Strange", "While on a journey of physical and spiritual healing, a brilliant neurosurgeon is drawn into the world of the mystic arts.", 2016, "Action/Fantasy"));
        movies.add(new MovieDocumentDto(113L, "Guardians of the Galaxy Vol. 2", "The Guardians struggle to keep their newfound family together as they unravel the mysteries of Peter Quill's true parentage.", 2017, "Action/Sci-Fi/Comedy"));
        movies.add(new MovieDocumentDto(114L, "Spider-Man: Homecoming", "Peter Parker balances his life as an ordinary high school student in Queens with his superhero alter-ego Spider-Man, and finds himself on the trail of a new menace prowling the skies of New York City.", 2017, "Action/Adventure"));
        movies.add(new MovieDocumentDto(115L, "Avengers: Infinity War", "The Avengers and their allies must be willing to sacrifice all in an attempt to defeat the powerful Thanos before his blitz of devastation and ruin puts an end to the universe.", 2018, "Action/Sci-Fi"));
        movies.add(new MovieDocumentDto(116L, "Ant-Man and the Wasp", "As Scott Lang balances being both a superhero and a father, Hope van Dyne and Dr. Hank Pym present an urgent new mission that finds the Ant-Man fighting alongside The Wasp to uncover secrets from their past.", 2018, "Action/Sci-Fi/Comedy"));
        movies.add(new MovieDocumentDto(117L, "Captain Marvel", "Carol Danvers becomes one of the universe's most powerful heroes when Earth is caught in the middle of a galactic war between two alien races.", 2019, "Action/Sci-Fi"));
        movies.add(new MovieDocumentDto(118L, "Avengers: Endgame", "The remaining Avengers must figure out a way to bring back their vanquished allies for an epic showdown with Thanos -- the evil demigod who decimated the planet and the universe.", 2019, "Action/Sci-Fi/Adventure"));
        movies.add(new MovieDocumentDto(119L, "Spider-Man: Far From Home", "Following the events of Avengers: Endgame (2019), Spider-Man must step up to take on new threats in a world that has changed forever.", 2019, "Action/Adventure"));
        movies.add(new MovieDocumentDto(120L, "Spider-Man: No Way Home", "With Spider-Man's identity now revealed, Peter asks Doctor Strange for help. When a spell goes wrong, dangerous foes from other worlds start to appear, forcing Peter to discover what it truly means to be Spider-Man.", 2021, "Action/Adventure"));
        movies.add(new MovieDocumentDto(121L, "The Batman", "When a sadistic serial killer begins murdering key political figures in Gotham, Batman is forced to investigate the city's hidden corruption and question his family's involvement.", 2022, "Action/Crime/Drama"));
        movies.add(new MovieDocumentDto(122L, "Justice League", "Fueled by his restored faith in humanity and inspired by Superman's selfless act, Bruce Wayne enlists the help of his new-found ally, Diana Prince, to face an even greater enemy.", 2017, "Action/Fantasy"));
        movies.add(new MovieDocumentDto(123L, "Man of Steel", "An alien child is evacuated from his dying world and sent to Earth to live among humans. His peace is threatened when other survivors of his home planet invade Earth.", 2013, "Action/Sci-Fi"));
        movies.add(new MovieDocumentDto(124L, "Batman v Superman: Dawn of Justice", "Fearing that the actions of Superman are left unchecked, Batman takes on the Man of Steel, while the world wrestles with what kind of a hero it really needs.", 2016, "Action/Sci-Fi"));
        movies.add(new MovieDocumentDto(125L, "Suicide Squad", "A secret government agency recruits some of the most dangerous incarcerated super-villains to form a defensive task force. Their first mission: save the world from the apocalypse.", 2016, "Action/Fantasy"));
        movies.add(new MovieDocumentDto(126L, "Aquaman", "Arthur Curry, the human-born heir to the underwater kingdom of Atlantis, goes on a quest to prevent a war between the worlds of ocean and land.", 2018, "Action/Fantasy"));
        movies.add(new MovieDocumentDto(127L, "Shazam!", "A newly fostered young boy in search of his mother instead finds unexpected superpowers and soon gains a powerful enemy.", 2019, "Action/Comedy/Fantasy"));
        movies.add(new MovieDocumentDto(128L, "Birds of Prey", "After splitting with the Joker, Harley Quinn joins superheroes Black Canary, Huntress and Renee Montoya to save a young girl from an evil crime lord.", 2020, "Action/Comedy"));
        movies.add(new MovieDocumentDto(129L, "The Suicide Squad", "Supervillains Harley Quinn, Bloodsport, Peacemaker and a collection of nutty cons at Belle Reve prison join the super-secret, super-shady Task Force X as they are dropped off at the remote, enemy-infused island of Corto Maltese.", 2021, "Action/Comedy"));
        movies.add(new MovieDocumentDto(130L, "The Matrix Reloaded", "Freedom fighters Neo, Trinity and Morpheus continue to lead the revolt against the Machine Army, unleashing their arsenal of extraordinary skills and weaponry against systematic forces of repression and exploitation.", 2003, "Sci-Fi/Action"));
        movies.add(new MovieDocumentDto(131L, "The Matrix Revolutions", "The human city of Zion defends itself against the massive invasion of the machines as Neo fights to end the war at another front while also opposing the rogue Agent Smith.", 2003, "Sci-Fi/Action"));
        movies.add(new MovieDocumentDto(132L, "The Matrix Resurrections", "Return to a world of two realities: one, everyday life; the other, what lies behind it. To find out if his reality is a construct, to truly know himself, Mr. Anderson will have to choose to follow the white rabbit once more.", 2021, "Sci-Fi/Action"));
        movies.add(new MovieDocumentDto(133L, "John Wick", "An ex-hitman comes out of retirement to track down the gangsters that took everything from him.", 2014, "Action/Thriller"));
        movies.add(new MovieDocumentDto(134L, "John Wick: Chapter 2", "After returning to the criminal underworld to repay a debt, John Wick discovers that a large bounty has been put on his life.", 2017, "Action/Thriller"));
        movies.add(new MovieDocumentDto(135L, "John Wick: Chapter 3 - Parabellum", "John Wick is on the run after killing a member of the international assassins' guild, and with a $14 million price tag on his head, he is the target of hit men and women everywhere.", 2019, "Action/Thriller"));
        movies.add(new MovieDocumentDto(136L, "John Wick: Chapter 4", "John Wick uncovers a path to defeating The High Table. But before he can earn his freedom, Wick must face off against a new enemy with powerful alliances across the globe.", 2023, "Action/Thriller"));
        movies.add(new MovieDocumentDto(137L, "Speed", "A young police officer must prevent a bomb exploding aboard a city bus by keeping its speed above 50 mph.", 1994, "Action/Thriller"));
        movies.add(new MovieDocumentDto(138L, "Point Break", "An F.B.I. Agent goes undercover to catch a gang of surfers who may be bank robbers.", 1991, "Action/Crime"));
        movies.add(new MovieDocumentDto(139L, "Constantine", "Supernatural exorcist and demonologist John Constantine helps a policewoman prove her sister's death was not a suicide, but something more.", 2005, "Action/Fantasy/Horror"));
        movies.add(new MovieDocumentDto(140L, "The Devil's Advocate", "An exceptionally adept Florida lawyer is offered a job at a high-end New York City law firm with a high-end boss - the biggest opportunity of his life to date.", 1997, "Drama/Horror/Mystery"));
        movies.add(new MovieDocumentDto(141L, "A Scanner Darkly", "An undercover cop in a not-too-distant future becomes a drug addict while trying to catch a dealer and ends up losing his own identity as a result.", 2006, "Animation/Sci-Fi/Mystery"));
        movies.add(new MovieDocumentDto(142L, "The Lake House", "A lonely doctor who once occupied an unusual lakeside house begins to exchange love letters with its former resident, a frustrated architect. They must try to unravel the mystery behind their extraordinary romance before it's too late.", 2006, "Romance/Fantasy"));
        movies.add(new MovieDocumentDto(143L, "Something's Gotta Give", "A swinger in his 60s falls for the mother of his young girlfriend.", 2003, "Comedy/Romance"));
        movies.add(new MovieDocumentDto(144L, "The Replacements", "During a pro football strike, the owners hire substitute players.", 2000, "Comedy/Sport"));
        movies.add(new MovieDocumentDto(145L, "Sweet November", "A workaholic executive and an unconventional woman agree to a personal relationship for a short period. In this short time she changes his life.", 2001, "Romance/Drama"));
        movies.add(new MovieDocumentDto(146L, "Hardball", "An aimless young man who is scalping tickets, gambling and drinking, agrees to coach a Little League team from the Cabrini-Green housing project in Chicago as a condition of getting a loan from a friend.", 2001, "Drama/Sport"));
        movies.add(new MovieDocumentDto(147L, "The Gift", "A woman with extrasensory perception is asked to help find a young woman who has disappeared.", 2000, "Horror/Thriller"));
        movies.add(new MovieDocumentDto(148L, "Bram Stoker's Dracula", "The centuries old vampire Count Dracula comes to England to seduce his barrister Jonathan Harker's fiancée Mina Murray and inflict havoc in the foreign land.", 1992, "Horror/Romance"));
        movies.add(new MovieDocumentDto(149L, "Little Buddha", "Lama Norbu comes to Seattle in search of the reincarnation of his dead teacher, Lama Dorje. His search leads him to young Jesse Conrad, Raju, a boy from Kathmandu, and an upper class Indian girl.", 1993, "Drama"));
        movies.add(new MovieDocumentDto(150L, "My Own Private Idaho", "Two best friends living on the streets of Portland as hustlers embark on a journey of self discovery and find their relationship stumbling along the way.", 1991, "Drama"));
        movies.add(new MovieDocumentDto(151L, "Bill & Ted's Excellent Adventure", "Two seemingly dumb teens set off on a quest to prepare the ultimate historical presentation with the help of a time machine.", 1989, "Comedy/Sci-Fi"));
        movies.add(new MovieDocumentDto(152L, "Bill & Ted's Bogus Journey", "A tyrant from the future creates evil android doubles of Bill and Ted and sends them back to eliminate the originals.", 1991, "Comedy/Sci-Fi"));
        movies.add(new MovieDocumentDto(153L, "River's Edge", "A group of high school friends must come to terms with the fact that one of them, Samson, has killed his girlfriend and left her body lying on the bank of a river.", 1986, "Crime/Drama"));
        movies.add(new MovieDocumentDto(154L, "Dangerous Liaisons", "In 18th century France, a Marquise and a Vicomte scheme to corrupt a young woman, but their plans go awry.", 1988, "Drama/Romance"));
        movies.add(new MovieDocumentDto(155L, "Parenthood", "The everyday stresses of family life are examined in this comedy about a clan of siblings and their families.", 1989, "Comedy/Drama"));
        movies.add(new MovieDocumentDto(156L, "I Love You to Death", "The wife of a philandering pizzeria owner tries to kill him with the help of her mother and some incompetent criminals.", 1990, "Comedy/Crime"));
        movies.add(new MovieDocumentDto(157L, "Much Ado About Nothing", "Young lovers Hero and Claudio are to be married in one week. To pass the time, they conspire with Don Pedro to set a lover's trap for Benedick and Beatrice.", 1993, "Comedy/Romance"));
        movies.add(new MovieDocumentDto(158L, "Even Cowgirls Get the Blues", "Sissy Hankshaw is a woman born with a mutation that has given her abnormally large thumbs. She uses this to her advantage as a hitchhiker.", 1993, "Comedy/Drama"));
        movies.add(new MovieDocumentDto(159L, "Tune in Tomorrow", "In 1950s New Orleans, a young man falls in love with his aunt, and a radio soap opera writer uses their story for his show.", 1990, "Comedy/Romance"));
        movies.add(new MovieDocumentDto(160L, "Prince of Pennsylvania", "A rebellious young man kidnaps his own father to get money for his escape from his stifling home town.", 1988, "Comedy/Drama"));
        movies.add(new MovieDocumentDto(161L, "Youngblood", "A young hockey player seeks stardom and the love of the coach's daughter.", 1986, "Drama/Sport"));
        movies.add(new MovieDocumentDto(162L, "Flying", "A determined young gymnast who has suffered a terrible accident works to get back into shape for a major competition.", 1986, "Drama/Sport"));
        movies.add(new MovieDocumentDto(163L, "Under the Cherry Moon", "Two brothers from Miami are in the Mediterranean, enjoying life by scamming money off of rich women. One day, they read about a young woman who is to inherit $50 million from her father.", 1986, "Musical/Comedy"));
        movies.add(new MovieDocumentDto(164L, "The Night Before", "A nerdy high school student who is the butt of a joke by the popular kids, gets a date with the prom queen. But he wakes up in an alley with no memory of the night before.", 1988, "Comedy"));
        movies.add(new MovieDocumentDto(165L, "Permanent Record", "A smart, talented, and popular high school student seems to have everything going for him, but the pressures he faces are too much and he commits suicide.", 1988, "Drama/Music"));
        movies.add(new MovieDocumentDto(166L, "The Big Lebowski", "Jeff 'The Dude' Lebowski, mistaken for a millionaire of the same name, seeks restitution for his ruined rug and enlists his bowling buddies to help.", 1998, "Comedy/Crime"));
        movies.add(new MovieDocumentDto(167L, "Chain Reaction", "Two researchers in a green energy project are forced to go on the run when they are framed for murder and treason.", 1996, "Action/Thriller"));
        movies.add(new MovieDocumentDto(168L, "Feeling Minnesota", "A woman on the run from the mob is coerced into marrying a money launderer, but she falls for his brother.", 1996, "Comedy/Crime"));
        movies.add(new MovieDocumentDto(169L, "A Walk in the Clouds", "A young soldier returning from war agrees to pose as the husband of a beautiful woman to help her face her domineering father.", 1995, "Romance/Drama"));
        movies.add(new MovieDocumentDto(170L, "Johnny Mnemonic", "A data courier, with a secret stash of information implanted in his mind, must deliver it before it kills him.", 1995, "Sci-Fi/Action"));
        movies.add(new MovieDocumentDto(171L, "The Watcher", "A retired FBI agent is stalked by a serial killer he's been hunting for years.", 2000, "Crime/Thriller"));
        movies.add(new MovieDocumentDto(172L, "The Day the Earth Stood Still", "A remake of the 1951 classic sci-fi film about an alien visitor and his giant robot counterpart who visit Earth.", 2008, "Sci-Fi/Thriller"));
        movies.add(new MovieDocumentDto(173L, "47 Ronin", "A band of samurai set out to avenge the death and dishonor of their master at the hands of a ruthless shogun.", 2013, "Action/Fantasy"));
        movies.add(new MovieDocumentDto(174L, "Knock Knock", "A devoted father helps two stranded young women who knock on his door, but his kind gesture results in a dangerous seduction and a deadly game of cat and mouse.", 2015, "Horror/Thriller"));
        movies.add(new MovieDocumentDto(175L, "Exposed", "A police detective investigates the truth behind his partner's death, while a young woman experiences strange things after witnessing a miracle.", 2016, "Crime/Drama"));
        movies.add(new MovieDocumentDto(176L, "The Neon Demon", "An aspiring model, Jesse, is new to Los Angeles. However, her beauty and youth, which generate intense fascination and jealousy within the fashion industry, may prove themselves sinister.", 2016, "Horror/Thriller"));
        movies.add(new MovieDocumentDto(177L, "Siberia", "An American diamond merchant travels to Russia to sell rare blue diamonds of questionable origin.", 2018, "Crime/Thriller"));
        movies.add(new MovieDocumentDto(178L, "Replicas", "A daring synthetic biologist, after a car accident kills his family, will stop at nothing to bring them back, even if it means pitting himself against a government-controlled laboratory.", 2018, "Sci-Fi/Thriller"));
        movies.add(new MovieDocumentDto(179L, "Destination Wedding", "The story of two miserable and unpleasant wedding guests, both of whom are cynical about love, who develop a mutual affection for each other.", 2018, "Comedy/Romance"));
        movies.add(new MovieDocumentDto(180L, "Always Be My Maybe", "Everyone assumed Sasha and Marcus would wind up together except for Sasha and Marcus. Reconnecting after 15 years, the two start to wonder... maybe?", 2019, "Comedy/Romance"));
        movies.add(new MovieDocumentDto(181L, "Toy Story 4", "When a new toy called 'Forky' joins Woody and the gang, a road trip alongside old and new friends reveals how big the world can be for a toy.", 2019, "Animation/Comedy"));
        movies.add(new MovieDocumentDto(182L, "The SpongeBob Movie: Sponge on the Run", "After SpongeBob's beloved pet snail Gary is snail-napped, he and Patrick embark on an epic adventure to The Lost City of Atlantic City to bring Gary home.", 2020, "Animation/Comedy"));
        movies.add(new MovieDocumentDto(183L, "Bill & Ted Face the Music", "Once told they'd save the universe during a time-traveling adventure, two would-be rockers from San Dimas, California find themselves as middle-aged dads still trying to crank out a hit song.", 2020, "Comedy/Sci-Fi"));
        movies.add(new MovieDocumentDto(184L, "The SpongeBob Movie: Sponge Out of Water", "When a diabolical pirate above the sea steals the secret Krabby Patty formula, SpongeBob and his nemesis Plankton must team up in order to get it back.", 2015, "Animation/Comedy"));
        movies.add(new MovieDocumentDto(185L, "BRZRKR", "A comic book series created and written by Keanu Reeves and Matt Kindt. A movie adaptation is in development.", 2021, "Action/Comic"));
        movies.add(new MovieDocumentDto(186L, "Generation Um...", "A group of friends in New York City navigates relationships, sex, and work over the course of a single day.", 2012, "Drama"));
        movies.add(new MovieDocumentDto(187L, "Man of Tai Chi", "A young martial artist's unparalleled Tai Chi skills land him in a highly lucrative underworld fight club.", 2013, "Action/Drama"));
        movies.add(new MovieDocumentDto(188L, "Side by Side", "A documentary that investigates the history, process and workflow of both digital and photochemical film creation.", 2012, "Documentary"));
        movies.add(new MovieDocumentDto(189L, "Henry's Crime", "Released from prison for a crime he didn't commit, an ex-con targets the same bank he was sent away for robbing.", 2010, "Comedy/Crime"));
        movies.add(new MovieDocumentDto(190L, "The Private Lives of Pippa Lee", "After her much older husband forces a move to a suburban retirement community, a woman's idyllic life is thrown into turmoil by a new neighbor who forces her to confront her troubled past.", 2009, "Comedy/Drama"));
        movies.add(new MovieDocumentDto(191L, "Street Kings", "An undercover cop, disillusioned with the justice system, is forced to work with a detective to solve his former partner's murder.", 2008, "Action/Crime"));
        movies.add(new MovieDocumentDto(192L, "The Lake House", "A lonely doctor and a frustrated architect exchange love letters through a magical mailbox at a lakeside house, only to discover they are living two years apart.", 2006, "Romance/Fantasy"));
        movies.add(new MovieDocumentDto(193L, "Thumbsucker", "A teenager continues to suck his thumb, which becomes a major issue for his family and his love life.", 2005, "Comedy/Drama"));
        movies.add(new MovieDocumentDto(194L, "Ellie Parker", "An aspiring actress's chaotic life in Los Angeles is documented as she navigates auditions, relationships, and self-doubt.", 2005, "Comedy/Drama"));
        movies.add(new MovieDocumentDto(195L, "Something New", "A successful African-American woman finds love with a white landscape architect, causing her to re-evaluate her 'ideal' man.", 2006, "Comedy/Romance"));
        movies.add(new MovieDocumentDto(196L, "The Animatrix", "A collection of animated short films detailing the backstory of the 'Matrix' universe.", 2003, "Animation/Sci-Fi"));
        movies.add(new MovieDocumentDto(197L, "Enter the Matrix", "A 2003 video game which is set in the same time frame as 'The Matrix Reloaded', telling a concurrent story.", 2003, "Action/Game"));
        movies.add(new MovieDocumentDto(198L, "Neo Yokio: Pink Christmas", "A Christmas special of the animated series 'Neo Yokio', following the adventures of a melancholy demon-slayer.", 2018, "Animation/Comedy"));
        movies.add(new MovieDocumentDto(199L, "BRZRKR II", "A speculative entry for a sequel to the 'BRZRKR' adaptation, continuing the story of the immortal warrior.", 2022, "Action/Comic"));
        movies.add(new MovieDocumentDto(200L, "The Matrix: Path of Neo", "A video game that allows players to control Neo and relive his most iconic moments from the film trilogy.", 2005, "Action/Game"));

        return movies;
    }

    private List<ActorDocumentDto> createCompleteActorList() {
        List<ActorDocumentDto> actors = new ArrayList<>();

        actors.add(new ActorDocumentDto(201L, "Leonardo DiCaprio", 1974, "American", "An acclaimed American actor and producer, known for his roles in 'Titanic', 'The Wolf of Wall Street', and 'The Revenant', for which he won an Academy Award."));
        actors.add(new ActorDocumentDto(202L, "Keanu Reeves", 1964, "Canadian", "A Canadian actor famous for his roles in blockbuster films such as 'The Matrix' trilogy, the 'John Wick' series, and 'Speed'."));
        actors.add(new ActorDocumentDto(203L, "John Travolta", 1954, "American", "An American actor and singer known for his iconic roles in 'Saturday Night Fever', 'Grease', and Quentin Tarantino's 'Pulp Fiction'."));
        actors.add(new ActorDocumentDto(204L, "Uma Thurman", 1970, "American", "An American actress and model, best known for her collaboration with director Quentin Tarantino, starring in 'Pulp Fiction' and the 'Kill Bill' films."));
        actors.add(new ActorDocumentDto(205L, "Joseph Gordon-Levitt", 1981, "American", "An American actor and filmmaker known for his work in 'Inception', '(500) Days of Summer', and 'The Dark Knight Rises'."));
        actors.add(new ActorDocumentDto(206L, "Elliot Page", 1987, "Canadian", "A Canadian actor and producer, known for his roles in 'Juno', 'Inception', and the Netflix series 'The Umbrella Academy'."));
        actors.add(new ActorDocumentDto(207L, "Christian Bale", 1974, "British", "A Welsh-born British actor renowned for his method acting and physical transformations, famous for playing Batman in 'The Dark Knight' trilogy."));
        actors.add(new ActorDocumentDto(208L, "Marlon Brando", 1924, "American", "A legendary American actor and activist, considered one of the greatest actors of all time. Famous for 'The Godfather' and 'On the Waterfront'."));
        actors.add(new ActorDocumentDto(209L, "Al Pacino", 1940, "American", "An American actor and filmmaker, with a celebrated career spanning over five decades. Known for 'The Godfather', 'Scarface', and 'Scent of a Woman'."));
        actors.add(new ActorDocumentDto(210L, "Harrison Ford", 1942, "American", "An American actor famous for his iconic roles as Han Solo in the 'Star Wars' series and the title character in the 'Indiana Jones' film series."));
        actors.add(new ActorDocumentDto(211L, "Elijah Wood", 1981, "American", "An American actor best known for his high-profile role as Frodo Baggins in Peter Jackson's 'The Lord of the Rings' trilogy."));
        actors.add(new ActorDocumentDto(212L, "Tom Hanks", 1956, "American", "An American actor and filmmaker regarded as an American cultural icon. Known for 'Forrest Gump', 'Saving Private Ryan', and 'Cast Away'."));
        actors.add(new ActorDocumentDto(213L, "Morgan Freeman", 1937, "American", "An American actor, director, and narrator known for his deep, authoritative voice and roles in 'The Shawshank Redemption' and 'Million Dollar Baby'."));
        actors.add(new ActorDocumentDto(214L, "Brad Pitt", 1963, "American", "An American actor and film producer with a prolific career, starring in films like 'Fight Club', 'Seven', and 'Once Upon a Time in Hollywood'."));
        actors.add(new ActorDocumentDto(215L, "Edward Norton", 1969, "American", "An American actor and filmmaker known for his critically acclaimed performances in 'Fight Club', 'American History X', and 'Birdman'."));
        actors.add(new ActorDocumentDto(216L, "Matt Damon", 1970, "American", "An American actor, producer, and screenwriter. Famous for co-writing and starring in 'Good Will Hunting' and his role as Jason Bourne."));
        actors.add(new ActorDocumentDto(217L, "Jessica Chastain", 1977, "American", "An American actress and producer known for her roles in films with feminist themes, including 'Zero Dark Thirty' and 'The Help'."));
        actors.add(new ActorDocumentDto(218L, "Anne Hathaway", 1982, "American", "An American actress who rose to prominence with 'The Princess Diaries' and won an Oscar for her role in 'Les Misérables'. Also known for 'The Devil Wears Prada'."));
        actors.add(new ActorDocumentDto(219L, "Robert De Niro", 1943, "American", "An American actor, producer, and director with two Academy Awards, known for his work with Martin Scorsese in films like 'Taxi Driver' and 'Raging Bull'."));
        actors.add(new ActorDocumentDto(220L, "Jodie Foster", 1962, "American", "An American actress, director, and producer who won Academy Awards for 'The Accused' and 'The Silence of the Lambs'."));
        actors.add(new ActorDocumentDto(221L, "Kate Winslet", 1975, "British", "An English actress known for her work in period dramas and tragedies. Famous for 'Titanic', 'The Reader', and 'Eternal Sunshine of the Spotless Mind'."));
        actors.add(new ActorDocumentDto(222L, "Sigourney Weaver", 1949, "American", "An American actress who gained international fame for her role as Ellen Ripley in the 'Alien' franchise. Also known for 'Ghostbusters' and 'Avatar'."));
        actors.add(new ActorDocumentDto(223L, "Russell Crowe", 1964, "New Zealander", "A New Zealand actor, producer, and musician based in Australia. He won an Academy Award for Best Actor for 'Gladiator'."));
        actors.add(new ActorDocumentDto(224L, "Joaquin Phoenix", 1974, "American", "An American actor and producer known for his intense, unconventional roles. He won an Academy Award for his performance in 'Joker'."));
        actors.add(new ActorDocumentDto(225L, "Scarlett Johansson", 1984, "American", "An American actress, one of the world's highest-paid actresses, known for her role as Black Widow in the Marvel Cinematic Universe and films like 'Lost in Translation'."));
        actors.add(new ActorDocumentDto(226L, "Chris Evans", 1981, "American", "An American actor best known for his portrayal of Captain America in the Marvel Cinematic Universe."));
        actors.add(new ActorDocumentDto(227L, "Robert Downey Jr.", 1965, "American", "An American actor and producer who launched the Marvel Cinematic Universe with his iconic role as Tony Stark / Iron Man."));
        actors.add(new ActorDocumentDto(228L, "Mark Ruffalo", 1967, "American", "An American actor and producer known for playing Bruce Banner / Hulk in the Marvel Cinematic Universe and for his roles in films like 'Spotlight'."));
        actors.add(new ActorDocumentDto(229L, "Samuel L. Jackson", 1948, "American", "An American actor and producer, one of the most widely recognized actors of his generation. Known for 'Pulp Fiction' and as Nick Fury in the MCU."));
        actors.add(new ActorDocumentDto(230L, "Natalie Portman", 1981, "Israeli-American", "An Israeli-American actress who won an Academy Award for 'Black Swan'. Also known for her roles in the 'Star Wars' prequel trilogy and the 'Thor' films."));
        actors.add(new ActorDocumentDto(231L, "Hugo Weaving", 1960, "British-Australian", "A British-Australian actor known for his roles as Agent Smith in 'The Matrix' trilogy, Elrond in 'The Lord of the Rings', and V in 'V for Vendetta'."));
        actors.add(new ActorDocumentDto(232L, "Laurence Fishburne", 1961, "American", "An American actor famous for his role as Morpheus in 'The Matrix' trilogy and his work in films like 'What's Love Got to Do with It'."));
        actors.add(new ActorDocumentDto(233L, "Carrie-Anne Moss", 1967, "Canadian", "A Canadian actress best known for her role as Trinity in 'The Matrix' film series."));
        actors.add(new ActorDocumentDto(234L, "Ian McKellen", 1939, "British", "An English actor acclaimed for his work on stage and screen. He is known globally for his roles as Gandalf in 'The Lord of the Rings' and Magneto in the 'X-Men' films."));
        actors.add(new ActorDocumentDto(235L, "Viggo Mortensen", 1958, "American-Danish", "An American-Danish actor, author, musician, and photographer. Best known for playing Aragorn in 'The Lord of the Rings' trilogy."));
        actors.add(new ActorDocumentDto(236L, "Orlando Bloom", 1977, "British", "An English actor who gained fame for his roles as Legolas in 'The Lord of the Rings' trilogy and Will Turner in the 'Pirates of the Caribbean' series."));
        actors.add(new ActorDocumentDto(237L, "Sean Astin", 1971, "American", "An American actor, director, and producer known for his roles as Samwise Gamgee in 'The Lord of the Rings', Mikey Walsh in 'The Goonies', and the title character of 'Rudy'."));
        actors.add(new ActorDocumentDto(238L, "Gary Oldman", 1958, "British", "An English actor and filmmaker known for his versatility. He won an Academy Award for his portrayal of Winston Churchill in 'Darkest Hour'."));
        actors.add(new ActorDocumentDto(239L, "Heath Ledger", 1979, "Australian", "An Australian actor and director who earned critical acclaim for his roles, including his posthumous Academy Award-winning performance as the Joker in 'The Dark Knight'."));
        actors.add(new ActorDocumentDto(240L, "Aaron Eckhart", 1968, "American", "An American actor known for his roles as Harvey Dent / Two-Face in 'The Dark Knight' and for the film 'Thank You for Smoking'."));
        actors.add(new ActorDocumentDto(241L, "Michael Caine", 1933, "British", "An iconic English actor with a career spanning over 60 years. Known for his distinctive Cockney accent and roles in 'Alfie', 'The Dark Knight' trilogy, and 'Inception'."));
        actors.add(new ActorDocumentDto(242L, "Tim Robbins", 1958, "American", "An American actor, screenwriter, director, and producer. Best known for his role as Andy Dufresne in 'The Shawshank Redemption'."));
        actors.add(new ActorDocumentDto(243L, "Liam Neeson", 1952, "Irish", "An Irish actor known for his role in 'Schindler's List' and for reinventing himself as an action star with the 'Taken' series."));
        actors.add(new ActorDocumentDto(244L, "Ben Kingsley", 1943, "British", "An English actor who won an Academy Award for his portrayal of Mahatma Gandhi in the film 'Gandhi'."));
        actors.add(new ActorDocumentDto(245L, "Matthew McConaughey", 1969, "American", "An American actor known for his roles in 'Dazed and Confused', 'Interstellar', and his Oscar-winning performance in 'Dallas Buyers Club'."));
        actors.add(new ActorDocumentDto(246L, "Helena Bonham Carter", 1966, "British", "An English actress known for her eccentric roles, collaborations with Tim Burton, and her portrayal of Bellatrix Lestrange in the 'Harry Potter' series."));
        actors.add(new ActorDocumentDto(247L, "Jake Gyllenhaal", 1980, "American", "An American actor known for a wide range of roles in films such as 'Donnie Darko', 'Brokeback Mountain', and 'Nightcrawler'."));
        actors.add(new ActorDocumentDto(248L, "Michelle Williams", 1980, "American", "An American actress acclaimed for her work in independent cinema, known for 'Brokeback Mountain' and 'Manchester by the Sea'."));
        actors.add(new ActorDocumentDto(249L, "Mark Wahlberg", 1971, "American", "An American actor, producer, and former rapper. Known for films like 'The Departed', 'The Fighter', and the 'Transformers' series."));
        actors.add(new ActorDocumentDto(250L, "Vera Farmiga", 1973, "American", "An American actress known for her role in 'Up in the Air' and as Lorraine Warren in 'The Conjuring' horror franchise."));
        actors.add(new ActorDocumentDto(251L, "Ryan Gosling", 1980, "Canadian", "A Canadian actor known for his work in both independent films and major blockbusters, including 'La La Land', 'Blade Runner 2049', and 'The Notebook'."));
        actors.add(new ActorDocumentDto(252L, "Emma Stone", 1988, "American", "An American actress who won an Academy Award for 'La La Land'. Also known for her roles in 'The Help', 'Easy A', and 'The Favourite'."));
        actors.add(new ActorDocumentDto(253L, "Jennifer Lawrence", 1990, "American", "An American actress known for playing Katniss Everdeen in 'The Hunger Games' series and for her Oscar-winning role in 'Silver Linings Playbook'."));
        actors.add(new ActorDocumentDto(254L, "Chris Hemsworth", 1983, "Australian", "An Australian actor best known for his role as Thor in the Marvel Cinematic Universe."));
        actors.add(new ActorDocumentDto(255L, "Chris Pratt", 1979, "American", "An American actor who gained fame as Andy Dwyer in 'Parks and Recreation' before starring in 'Guardians of the Galaxy' and 'Jurassic World'."));
        actors.add(new ActorDocumentDto(256L, "Zendaya", 1996, "American", "An American actress and singer known for her roles in the 'Spider-Man' MCU films and the acclaimed HBO series 'Euphoria'."));
        actors.add(new ActorDocumentDto(257L, "Timothée Chalamet", 1995, "American-French", "An American-French actor who received critical acclaim for his roles in 'Call Me by Your Name', 'Dune', and 'Little Women'."));
        actors.add(new ActorDocumentDto(258L, "Saoirse Ronan", 1994, "Irish-American", "An Irish-American actress known for her roles in period dramas like 'Brooklyn', 'Lady Bird', and 'Little Women'."));
        actors.add(new ActorDocumentDto(259L, "Florence Pugh", 1996, "British", "An English actress who gained recognition for her roles in 'Midsommar', 'Little Women', and as Yelena Belova in the MCU."));
        actors.add(new ActorDocumentDto(260L, "Oscar Isaac", 1979, "Guatemalan-American", "A Guatemalan-American actor known for his versatile roles as Poe Dameron in the 'Star Wars' sequel trilogy, in 'Inside Llewyn Davis', and 'Dune'."));
        actors.add(new ActorDocumentDto(261L, "Adam Driver", 1983, "American", "An American actor known for his role as Kylo Ren in the 'Star Wars' sequel trilogy and for his work in films like 'Marriage Story'."));
        actors.add(new ActorDocumentDto(262L, "John Boyega", 1992, "British", "A British-Nigerian actor who gained international fame for playing Finn in the 'Star Wars' sequel trilogy."));
        actors.add(new ActorDocumentDto(263L, "Daisy Ridley", 1992, "British", "An English actress best known for her breakthrough role as Rey in the 'Star Wars' sequel trilogy."));
        actors.add(new ActorDocumentDto(264L, "Anthony Mackie", 1978, "American", "An American actor known for playing Sam Wilson / Falcon / Captain America in the Marvel Cinematic Universe."));
        actors.add(new ActorDocumentDto(265L, "Sebastian Stan", 1982, "Romanian-American", "A Romanian-American actor best known for his role as Bucky Barnes / Winter Soldier in the Marvel Cinematic Universe."));
        actors.add(new ActorDocumentDto(266L, "Tom Holland", 1996, "British", "An English actor who achieved global stardom for portraying Peter Parker / Spider-Man in the Marvel Cinematic Universe."));
        actors.add(new ActorDocumentDto(267L, "Zoe Saldana", 1978, "American", "An American actress known for her roles in science fiction films, including Gamora in 'Guardians of the Galaxy', Neytiri in 'Avatar', and Uhura in 'Star Trek'."));
        actors.add(new ActorDocumentDto(268L, "Gal Gadot", 1985, "Israeli", "An Israeli actress and model who gained international fame for portraying Wonder Woman in the DC Extended Universe."));
        actors.add(new ActorDocumentDto(269L, "Margot Robbie", 1990, "Australian", "An Australian actress and producer known for roles in 'The Wolf of Wall Street', as Harley Quinn in the DCEU, and in 'I, Tonya'."));
        actors.add(new ActorDocumentDto(270L, "Ryan Reynolds", 1976, "Canadian", "A Canadian-American actor, comedian, and producer known for his role as the titular character in the 'Deadpool' films."));
        actors.add(new ActorDocumentDto(271L, "Will Smith", 1968, "American", "An American actor, rapper, and producer. Known for the TV show 'The Fresh Prince of Bel-Air' and films like 'Men in Black' and 'King Richard'."));
        actors.add(new ActorDocumentDto(272L, "Denzel Washington", 1954, "American", "An American actor, director, and producer with two Academy Awards, known for powerful performances in 'Training Day', 'Malcolm X', and 'Fences'."));
        actors.add(new ActorDocumentDto(273L, "Jamie Foxx", 1967, "American", "An American actor, singer, and comedian who won an Academy Award for his portrayal of Ray Charles in the film 'Ray'."));
        actors.add(new ActorDocumentDto(274L, "Idris Elba", 1972, "British", "An English actor, producer, and musician known for his roles as Stringer Bell in 'The Wire', Luther, and Heimdall in the MCU."));
        actors.add(new ActorDocumentDto(275L, "Michael B. Jordan", 1987, "American", "An American actor known for his roles in 'Fruitvale Station', as Erik Killmonger in 'Black Panther', and as Adonis Creed in the 'Creed' films."));
        actors.add(new ActorDocumentDto(276L, "Lupita Nyong'o", 1983, "Kenyan-Mexican", "A Kenyan-Mexican actress who won an Academy Award for her debut film role in '12 Years a Slave'. Also known for 'Us' and 'Black Panther'."));
        actors.add(new ActorDocumentDto(277L, "Chadwick Boseman", 1976, "American", "An American actor who gained international fame for playing Black Panther in the MCU. Also known for portraying historical figures like Jackie Robinson and James Brown."));
        actors.add(new ActorDocumentDto(278L, "Mahershala Ali", 1974, "American", "An American actor and rapper who is a two-time Academy Award winner for his roles in 'Moonlight' and 'Green Book'."));
        actors.add(new ActorDocumentDto(279L, "Regina King", 1971, "American", "An American actress and director. She is an Academy Award winner for 'If Beale Street Could Talk' and is acclaimed for her work in 'Watchmen'."));
        actors.add(new ActorDocumentDto(280L, "Viola Davis", 1965, "American", "An American actress and producer, one of the few performers to have won an Academy Award, an Emmy Award, and a Tony Award (the 'Triple Crown of Acting')."));
        actors.add(new ActorDocumentDto(281L, "Octavia Spencer", 1970, "American", "An American actress and author who won an Academy Award for her role in 'The Help'."));
        actors.add(new ActorDocumentDto(282L, "Taraji P. Henson", 1970, "American", "An American actress known for her role as Cookie Lyon in the TV series 'Empire' and her Oscar-nominated performance in 'The Curious Case of Benjamin Button'."));
        actors.add(new ActorDocumentDto(283L, "Forest Whitaker", 1961, "American", "An American actor, producer, and director who won an Academy Award for his portrayal of Idi Amin in 'The Last King of Scotland'."));
        actors.add(new ActorDocumentDto(284L, "Chiwetel Ejiofor", 1977, "British", "A British actor known for his Oscar-nominated lead role in '12 Years a Slave' and as Karl Mordo in the MCU."));
        actors.add(new ActorDocumentDto(285L, "Daniel Kaluuya", 1989, "British", "A British actor who won an Academy Award for 'Judas and the Black Messiah'. Also known for his lead role in 'Get Out'."));
        actors.add(new ActorDocumentDto(286L, "LaKeith Stanfield", 1991, "American", "An American actor and musician known for his roles in 'Get Out', 'Sorry to Bother You', and the series 'Atlanta'."));
        actors.add(new ActorDocumentDto(287L, "John David Washington", 1984, "American", "An American actor and former football player, known for starring in Spike Lee's 'BlacKkKlansman' and Christopher Nolan's 'Tenet'."));
        actors.add(new ActorDocumentDto(288L, "Anya Taylor-Joy", 1996, "American-British-Argentine", "An actress known for her roles in 'The Witch', 'Split', and the acclaimed Netflix miniseries 'The Queen's Gambit'."));
        actors.add(new ActorDocumentDto(289L, "Elisabeth Moss", 1982, "American", "An American actress known for her roles as Peggy Olson in 'Mad Men' and June Osborne in 'The Handmaid's Tale'."));
        actors.add(new ActorDocumentDto(290L, "Amy Adams", 1974, "American", "An American actress known for her versatile dramatic and comedic roles in films like 'Arrival', 'American Hustle', and 'Enchanted'."));
        actors.add(new ActorDocumentDto(291L, "Tilda Swinton", 1960, "British", "A British actress known for her avant-garde roles and transformative performances in both independent and mainstream films, including 'Michael Clayton' and 'Doctor Strange'."));
        actors.add(new ActorDocumentDto(292L, "Cate Blanchett", 1969, "Australian", "An Australian actress and producer, regarded as one of the greatest actresses of her generation, with two Academy Awards for 'The Aviator' and 'Blue Jasmine'."));
        actors.add(new ActorDocumentDto(293L, "Meryl Streep", 1949, "American", "An American actress often described as the 'best actress of her generation'. She holds the record for the most Academy Award nominations of any actor."));
        actors.add(new ActorDocumentDto(294L, "Frances McDormand", 1957, "American", "An American actress and producer who has won three Academy Awards for Best Actress for her roles in 'Fargo', 'Three Billboards Outside Ebbing, Missouri', and 'Nomadland'."));
        actors.add(new ActorDocumentDto(295L, "Julianne Moore", 1960, "American-British", "An American-British actress known for her portrayals of emotionally troubled women. She won an Oscar for 'Still Alice'."));
        actors.add(new ActorDocumentDto(296L, "Nicole Kidman", 1967, "Australian-American", "An Australian-American actress and producer who won an Academy Award for 'The Hours'. Known for a wide range of film and television roles."));
        actors.add(new ActorDocumentDto(297L, "Charlize Theron", 1975, "South African-American", "A South African-American actress and producer who won an Academy Award for her transformative role in 'Monster'. Also known for 'Mad Max: Fury Road'."));
        actors.add(new ActorDocumentDto(298L, "Reese Witherspoon", 1976, "American", "An American actress, producer, and entrepreneur. She won an Oscar for 'Walk the Line' and is also known for 'Legally Blonde' and 'Big Little Lies'."));
        actors.add(new ActorDocumentDto(299L, "Sandra Bullock", 1964, "American-German", "An American-German actress and producer who won an Academy Award for 'The Blind Side'. Known for 'Speed' and 'Gravity'."));
        actors.add(new ActorDocumentDto(300L, "Julia Roberts", 1967, "American", "An American actress who became a Hollywood star after headlining the romantic comedy 'Pretty Woman'. She won an Oscar for 'Erin Brockovich'."));
        actors.add(new ActorDocumentDto(301L, "George Clooney", 1961, "American", "An American actor, director, producer, and screenwriter. Known for his role in 'ER' and films like 'Ocean's Eleven' and 'Syriana', for which he won an Oscar."));
        actors.add(new ActorDocumentDto(302L, "Hugh Jackman", 1968, "Australian", "An Australian actor, singer, and producer best known for playing Wolverine in the 'X-Men' film series for nearly two decades."));
        actors.add(new ActorDocumentDto(303L, "Christian Slater", 1969, "American", "An American actor who rose to fame in the late 80s. Known for his distinctive voice and roles in films like 'Heathers', 'True Romance', and the acclaimed TV series 'Mr. Robot'."));
        actors.add(new ActorDocumentDto(304L, "Ethan Hawke", 1970, "American", "An American actor, writer, and director. Known for his roles in the 'Before' trilogy, 'Training Day', and 'Boyhood'."));
        actors.add(new ActorDocumentDto(305L, "Philip Seymour Hoffman", 1967, "American", "An American actor, director, and producer widely acclaimed for his versatility. He won an Academy Award for his title role in 'Capote'."));
        actors.add(new ActorDocumentDto(306L, "Steve Carell", 1962, "American", "An American actor and comedian, best known for playing Michael Scott on the NBC sitcom 'The Office' and for his roles in films like 'The 40-Year-Old Virgin'."));
        actors.add(new ActorDocumentDto(307L, "Paul Rudd", 1969, "American", "An American actor, comedian, and screenwriter known for his roles in comedies like 'Anchorman' and for playing Ant-Man in the Marvel Cinematic Universe."));
        actors.add(new ActorDocumentDto(308L, "Adam Sandler", 1966, "American", "An American comedian, actor, and filmmaker. A prominent comedy star since the 1990s, also acclaimed for dramatic roles in 'Uncut Gems' and 'Punch-Drunk Love'."));
        actors.add(new ActorDocumentDto(309L, "Ben Stiller", 1965, "American", "An American actor, comedian, director, and producer. Known for starring in comedies such as 'Zoolander', 'Meet the Parents', and 'Tropic Thunder'."));
        actors.add(new ActorDocumentDto(310L, "Owen Wilson", 1968, "American", "An American actor known for his roles in Wes Anderson films and comedies like 'Wedding Crashers'. He also plays Mobius M. Mobius in the MCU series 'Loki'."));
        actors.add(new ActorDocumentDto(311L, "Vince Vaughn", 1970, "American", "An American actor and producer known for his roles in comedies such as 'Old School', 'DodgeBall: A True Underdog Story', and 'Wedding Crashers'."));
        actors.add(new ActorDocumentDto(312L, "Will Ferrell", 1967, "American", "An American actor, comedian, and producer. A former 'Saturday Night Live' cast member, famous for films like 'Elf', 'Anchorman', and 'Step Brothers'."));
        actors.add(new ActorDocumentDto(313L, "Seth Rogen", 1982, "Canadian", "A Canadian actor, comedian, and filmmaker. Known for his roles in comedies he often co-writes, such as 'Superbad', 'Pineapple Express', and 'This Is the End'."));
        actors.add(new ActorDocumentDto(314L, "James Franco", 1978, "American", "An American actor and filmmaker. Known for his roles in the 'Spider-Man' trilogy, '127 Hours', and 'The Disaster Artist'."));
        actors.add(new ActorDocumentDto(315L, "Jonah Hill", 1983, "American", "An American actor, director, and screenwriter. Known for his comedic roles in 'Superbad' and for his Oscar-nominated dramatic performances in 'Moneyball' and 'The Wolf of Wall Street'."));
        actors.add(new ActorDocumentDto(316L, "Michael Cera", 1988, "Canadian", "A Canadian actor and musician known for his awkward, nerdy persona in 'Superbad', 'Juno', and the sitcom 'Arrested Development'."));
        actors.add(new ActorDocumentDto(317L, "Jesse Eisenberg", 1983, "American", "An American actor known for his fast-talking intellectual characters. He received an Oscar nomination for portraying Mark Zuckerberg in 'The Social Network'."));
        actors.add(new ActorDocumentDto(318L, "Andrew Garfield", 1983, "American-British", "An American-British actor known for playing Spider-Man in 'The Amazing Spider-Man' films and for his acclaimed roles in 'Hacksaw Ridge' and 'Tick, Tick... Boom!'."));
        actors.add(new ActorDocumentDto(319L, "Miles Teller", 1987, "American", "An American actor known for his roles in 'Whiplash', the 'Divergent' series, and 'Top Gun: Maverick'."));
        actors.add(new ActorDocumentDto(320L, "Shia LaBeouf", 1986, "American", "An American actor, performance artist, and filmmaker who gained fame on the Disney Channel series 'Even Stevens' before starring in the 'Transformers' series."));
        actors.add(new ActorDocumentDto(321L, "Zac Efron", 1987, "American", "An American actor who rose to prominence in the late 2000s for his leading role in the 'High School Musical' franchise."));
        actors.add(new ActorDocumentDto(322L, "Channing Tatum", 1980, "American", "An American actor known for his roles in 'Magic Mike', '21 Jump Street', and 'Step Up'."));
        actors.add(new ActorDocumentDto(323L, "Ryan Phillippe", 1974, "American", "An American actor who became known in the late 1990s with starring roles in films including 'I Know What You Did Last Summer' and 'Cruel Intentions'."));
        actors.add(new ActorDocumentDto(324L, "Josh Hartnett", 1978, "American", "An American actor who rose to fame in the late 1990s with roles in 'The Faculty', 'Pearl Harbor', and 'Black Hawk Down'."));
        actors.add(new ActorDocumentDto(325L, "Colin Farrell", 1976, "Irish", "An Irish actor known for a wide range of roles in films like 'In Bruges', 'The Lobster', and 'The Banshees of Inisherin'."));
        actors.add(new ActorDocumentDto(326L, "Javier Bardem", 1969, "Spanish", "A Spanish actor who won an Academy Award for his role as the villain Anton Chigurh in 'No Country for Old Men'."));
        actors.add(new ActorDocumentDto(327L, "Benicio del Toro", 1967, "Puerto Rican", "A Puerto Rican actor and producer who won an Academy Award for his role in 'Traffic'. Also known for 'Sicario' and as The Collector in the MCU."));
        actors.add(new ActorDocumentDto(328L, "Antonio Banderas", 1960, "Spanish", "A Spanish actor, director, and producer. Known for his collaborations with Pedro Almodóvar and for voicing Puss in Boots in the 'Shrek' franchise."));
        actors.add(new ActorDocumentDto(329L, "Penélope Cruz", 1974, "Spanish", "A Spanish actress who won an Academy Award for 'Vicky Cristina Barcelona'. A frequent collaborator with director Pedro Almodóvar."));
        actors.add(new ActorDocumentDto(330L, "Salma Hayek", 1966, "Mexican-American", "A Mexican-American actress and producer who received an Oscar nomination for her role as Frida Kahlo in 'Frida'."));
        actors.add(new ActorDocumentDto(331L, "Eva Mendes", 1974, "American", "An American actress known for her roles in 'Training Day', 'Hitch', and 'The Place Beyond the Pines'."));
        actors.add(new ActorDocumentDto(332L, "Jennifer Lopez", 1969, "American", "An American singer, actress, and dancer. A global entertainer known for her music career and film roles, including 'Selena' and 'Hustlers'."));
        actors.add(new ActorDocumentDto(333L, "Cameron Diaz", 1972, "American", "An American actress who rose to stardom in the 1990s. Known for 'The Mask', 'There's Something About Mary', and voicing Princess Fiona in 'Shrek'."));
        actors.add(new ActorDocumentDto(334L, "Drew Barrymore", 1975, "American", "An American actress, producer, and talk show host who achieved fame as a child actor in 'E.T. the Extra-Terrestrial'."));
        actors.add(new ActorDocumentDto(335L, "Kristen Stewart", 1990, "American", "An American actress who became a global star for her role as Bella Swan in 'The Twilight Saga'. Also acclaimed for her work in independent films and 'Spencer'."));
        actors.add(new ActorDocumentDto(336L, "Anna Kendrick", 1985, "American", "An American actress and singer known for her roles in 'Up in the Air' and the 'Pitch Perfect' film series."));
        actors.add(new ActorDocumentDto(337L, "Emma Watson", 1990, "British", "A British actress and activist who rose to prominence as Hermione Granger in the 'Harry Potter' film series."));
        actors.add(new ActorDocumentDto(338L, "Mila Kunis", 1983, "American", "A Ukrainian-born American actress known for her role on 'That '70s Show', voicing Meg Griffin on 'Family Guy', and films like 'Black Swan'."));
        actors.add(new ActorDocumentDto(339L, "Natalie Dormer", 1982, "British", "An English actress known for her roles as Anne Boleyn in 'The Tudors' and Margaery Tyrell in 'Game of Thrones'."));
        actors.add(new ActorDocumentDto(340L, "Keira Knightley", 1985, "British", "An English actress known for starring in period dramas and for her role as Elizabeth Swann in the 'Pirates of the Caribbean' series."));
        actors.add(new ActorDocumentDto(341L, "Rachel McAdams", 1978, "Canadian", "A Canadian actress known for her roles in 'Mean Girls', 'The Notebook', and her Oscar-nominated performance in 'Spotlight'."));
        actors.add(new ActorDocumentDto(342L, "Amy Poehler", 1971, "American", "An American comedian, actress, and writer. A former 'Saturday Night Live' cast member, best known for playing Leslie Knope in 'Parks and Recreation'."));
        actors.add(new ActorDocumentDto(343L, "Tina Fey", 1970, "American", "An American actress, comedian, writer, and producer. The first female head writer of 'Saturday Night Live' and creator and star of '30 Rock'."));
        actors.add(new ActorDocumentDto(344L, "Kristen Wiig", 1973, "American", "An American actress, comedian, and writer. A former 'Saturday Night Live' cast member, known for co-writing and starring in 'Bridesmaids'."));
        actors.add(new ActorDocumentDto(345L, "Melissa McCarthy", 1970, "American", "An American actress, comedian, and writer. Gained fame for her role in 'Gilmore Girls' and an Oscar-nominated performance in 'Bridesmaids'."));
        actors.add(new ActorDocumentDto(346L, "Rebel Wilson", 1980, "Australian", "An Australian actress, comedian, and writer known for her roles in 'Pitch Perfect' and 'Bridesmaids'."));
        actors.add(new ActorDocumentDto(347L, "Mindy Kaling", 1979, "American", "An American actress, comedian, writer, and producer. Known for her work on 'The Office' and as the creator and star of 'The Mindy Project'."));
        actors.add(new ActorDocumentDto(348L, "Awkwafina", 1988, "American", "An American actress, comedian, and rapper who won a Golden Globe for her leading role in 'The Farewell'."));
        actors.add(new ActorDocumentDto(349L, "Constance Wu", 1982, "American", "An American actress known for starring in the sitcom 'Fresh Off the Boat' and the acclaimed film 'Crazy Rich Asians'."));
        actors.add(new ActorDocumentDto(350L, "Sandra Oh", 1971, "Canadian", "A Canadian-American actress who gained fame as Dr. Cristina Yang on 'Grey's Anatomy' and won a Golden Globe for her lead role in 'Killing Eve'."));
        actors.add(new ActorDocumentDto(351L, "Lucy Liu", 1968, "American", "An American actress known for her roles in 'Charlie's Angels', 'Kill Bill', and the TV series 'Elementary'."));
        actors.add(new ActorDocumentDto(352L, "Jackie Chan", 1954, "Hong Konger", "A Hong Kong actor, director, and martial artist known for his acrobatic fighting style, comic timing, and innovative stunts."));
        actors.add(new ActorDocumentDto(353L, "Jet Li", 1963, "Chinese-Singaporean", "A Chinese-born Singaporean martial artist and actor. A wushu champion who became a star in epic martial arts films."));
        actors.add(new ActorDocumentDto(354L, "Donnie Yen", 1963, "Hong Konger-Chinese", "A Hong Kong actor, martial artist, and action director, widely credited with popularizing the martial art of Wing Chun in the 'Ip Man' film series."));
        actors.add(new ActorDocumentDto(355L, "Tony Leung", 1962, "Hong Konger", "A Hong Kong actor and singer, a major star in Asia, known for his collaborations with director Wong Kar-wai, including 'In the Mood for Love'."));
        actors.add(new ActorDocumentDto(356L, "Chow Yun-fat", 1955, "Hong Konger", "A Hong Kong actor known for his collaborations with director John Woo in heroic bloodshed films like 'A Better Tomorrow' and 'The Killer'."));
        actors.add(new ActorDocumentDto(357L, "Andy Lau", 1961, "Hong Konger", "A Hong Kong actor, singer-songwriter, and film producer. One of the most commercially successful film actors in Hong Kong."));
        actors.add(new ActorDocumentDto(358L, "Stephen Chow", 1962, "Hong Konger", "A Hong Kong filmmaker and actor, known for his slapstick comedic films like 'Shaolin Soccer' and 'Kung Fu Hustle'."));
        actors.add(new ActorDocumentDto(359L, "Michelle Yeoh", 1962, "Malaysian", "A Malaysian actress who gained fame in Hong Kong action films and became the first Asian to win the Academy Award for Best Actress for 'Everything Everywhere All at Once'."));
        actors.add(new ActorDocumentDto(360L, "Zhang Ziyi", 1979, "Chinese", "A Chinese actress and model. She is considered one of the Four Dan Actresses of China and is known internationally for 'Crouching Tiger, Hidden Dragon'."));
        actors.add(new ActorDocumentDto(361L, "Gong Li", 1965, "Chinese-Singaporean", "A Chinese-born Singaporean actress credited with helping to bring Chinese cinema to prominence in Europe and the United States."));
        actors.add(new ActorDocumentDto(362L, "Maggie Cheung", 1964, "Hong Konger-British", "A Hong Kong-British actress who is a major star in Asia. Known for her role in 'In the Mood for Love'."));
        actors.add(new ActorDocumentDto(363L, "Hiroyuki Sanada", 1960, "Japanese", "A Japanese actor known for his roles in 'The Last Samurai', 'Rush Hour 3', and the TV series 'Shōgun'."));
        actors.add(new ActorDocumentDto(364L, "Ken Watanabe", 1959, "Japanese", "A Japanese actor known to Western audiences for roles in 'The Last Samurai', 'Inception', and 'Godzilla'."));
        actors.add(new ActorDocumentDto(365L, "Rinko Kikuchi", 1981, "Japanese", "A Japanese actress who was the first Japanese actress to be nominated for an Academy Award in 50 years for her work in 'Babel'."));
        actors.add(new ActorDocumentDto(366L, "Tadanobu Asano", 1973, "Japanese", "A Japanese actor and musician known for roles in 'Ichi the Killer', 'Zatoichi', and as Hogun in the MCU 'Thor' films."));
        actors.add(new ActorDocumentDto(367L, "Takeshi Kitano", 1947, "Japanese", "A Japanese comedian, television personality, director, actor, and author. Known for his stoic, violent yakuza films."));
        actors.add(new ActorDocumentDto(368L, "Toshiro Mifune", 1920, "Japanese", "A Japanese actor who appeared in over 150 feature films. He is best known for his 16-film collaboration with director Akira Kurosawa."));
        actors.add(new ActorDocumentDto(369L, "Min-sik Choi", 1962, "South Korean", "A South Korean actor best known for his critically acclaimed roles in 'Oldboy' and 'I Saw the Devil'."));
        actors.add(new ActorDocumentDto(370L, "Song Kang-ho", 1967, "South Korean", "A South Korean actor who rose to international stardom for his roles in Bong Joon-ho's films, including 'Snowpiercer' and the Oscar-winning 'Parasite'."));
        actors.add(new ActorDocumentDto(371L, "Lee Byung-hun", 1970, "South Korean", "A South Korean actor, singer, and model. Known for 'A Bittersweet Life', 'The Good, the Bad, the Weird', and Hollywood roles in the 'G.I. Joe' series."));
        actors.add(new ActorDocumentDto(372L, "Park So-dam", 1991, "South Korean", "A South Korean actress best known internationally for her role as 'Jessica' in the 2019 film 'Parasite'."));
        actors.add(new ActorDocumentDto(373L, "Choi Woo-shik", 1990, "South Korean-Canadian", "A South Korean-Canadian actor known for his roles in 'Train to Busan' and as the son, Ki-woo, in 'Parasite'."));
        actors.add(new ActorDocumentDto(374L, "Youn Yuh-jung", 1947, "South Korean", "A South Korean actress who won the Academy Award for Best Supporting Actress for her role in the film 'Minari'."));
        actors.add(new ActorDocumentDto(375L, "Steven Yeun", 1983, "South Korean-American", "A South Korean-American actor known for his role as Glenn Rhee in 'The Walking Dead' and his Oscar-nominated performance in 'Minari'."));
        actors.add(new ActorDocumentDto(376L, "John Cho", 1972, "South Korean-American", "A South Korean-American actor known for the 'Harold & Kumar' films and for playing Hikaru Sulu in the 'Star Trek' reboot series."));
        actors.add(new ActorDocumentDto(377L, "Daniel Dae Kim", 1968, "South Korean-American", "A South Korean-American actor known for his roles as Jin-Soo Kwon in 'Lost' and Chin Ho Kelly in 'Hawaii Five-0'."));
        actors.add(new ActorDocumentDto(378L, "Randall Park", 1974, "American", "An American actor, comedian, and writer known for playing Kim Jong-un in 'The Interview', Louis Huang in 'Fresh Off the Boat', and Jimmy Woo in the MCU."));
        actors.add(new ActorDocumentDto(379L, "Kal Penn", 1977, "American", "An American actor and former White House staff member. Known for playing Kumar Patel in the 'Harold & Kumar' film series."));
        actors.add(new ActorDocumentDto(380L, "Dev Patel", 1990, "British", "A British actor of Indian descent who rose to fame in his debut film 'Slumdog Millionaire'. Also known for 'Lion' and 'The Green Knight'."));
        actors.add(new ActorDocumentDto(381L, "Riz Ahmed", 1982, "British", "A British actor and rapper of Pakistani descent. The first Muslim to be nominated for the Academy Award for Best Actor for 'Sound of Metal'."));
        actors.add(new ActorDocumentDto(382L, "Aziz Ansari", 1983, "American", "An American actor, comedian, and filmmaker. Known for playing Tom Haverford on 'Parks and Recreation' and for creating the Netflix series 'Master of None'."));
        // Note: Mindy Kaling is listed twice in the source data (ID 347 & 383). Creating a second entry for consistency.
        actors.add(new ActorDocumentDto(383L, "Mindy Kaling", 1979, "American", "A versatile American talent known for writing for and acting in 'The Office', as well as creating and starring in 'The Mindy Project' and 'Never Have I Ever'."));
        actors.add(new ActorDocumentDto(384L, "Priyanka Chopra", 1982, "Indian", "An Indian actress, singer, and producer. The winner of the Miss World 2000 pageant, she is one of India's highest-paid and most popular entertainers."));
        actors.add(new ActorDocumentDto(385L, "Freida Pinto", 1984, "Indian", "An Indian actress who appears mainly in American and British films. She rose to prominence with her film debut in 'Slumdog Millionaire'."));
        actors.add(new ActorDocumentDto(386L, "Irrfan Khan", 1967, "Indian", "An acclaimed Indian actor who worked in Hindi cinema as well as British and American films. Known for 'The Namesake', 'Life of Pi', and 'The Lunchbox'."));
        actors.add(new ActorDocumentDto(387L, "Om Puri", 1950, "Indian", "An Indian actor who appeared in mainstream commercial Hindi films, as well as independent and art films. Known for his authoritative voice and versatile roles."));
        actors.add(new ActorDocumentDto(388L, "Amitabh Bachchan", 1942, "Indian", "An Indian actor, film producer, and television host. Regarded as one of the most influential actors in the history of Indian cinema."));
        actors.add(new ActorDocumentDto(389L, "Shah Rukh Khan", 1965, "Indian", "An Indian actor, producer, and television personality. Referred to as the 'King of Bollywood', he has appeared in more than 80 Hindi films."));
        actors.add(new ActorDocumentDto(390L, "Aamir Khan", 1965, "Indian", "An Indian actor, filmmaker, and television talk-show host. Known for his critically and commercially successful films like '3 Idiots' and 'Dangal'."));
        actors.add(new ActorDocumentDto(391L, "Salman Khan", 1965, "Indian", "An Indian actor, producer, and television personality. One of the most commercially successful actors of Hindi cinema."));
        actors.add(new ActorDocumentDto(392L, "Hrithik Roshan", 1974, "Indian", "An Indian actor known for his dancing skills and versatile roles in Bollywood films like 'Kaho Naa... Pyaar Hai' and 'Super 30'."));
        actors.add(new ActorDocumentDto(393L, "Ranbir Kapoor", 1982, "Indian", "An Indian actor and one of the highest-paid celebrities in India, known for films like 'Rockstar', 'Barfi!', and 'Sanju'."));
        actors.add(new ActorDocumentDto(394L, "Ranveer Singh", 1985, "Indian", "An Indian actor known for his flamboyant style and energetic performances in Hindi films like 'Padmaavat' and 'Gully Boy'."));
        actors.add(new ActorDocumentDto(395L, "Shahid Kapoor", 1981, "Indian", "An Indian actor noted for his performances in romantic comedies and thrillers, including 'Jab We Met' and 'Kabir Singh'."));
        actors.add(new ActorDocumentDto(396L, "Deepika Padukone", 1986, "Indian", "An Indian actress and producer, one of India's highest-paid actresses. Known for 'Padmaavat', 'Piku', and Hollywood film 'xXx: Return of Xander Cage'."));
        actors.add(new ActorDocumentDto(397L, "Katrina Kaif", 1983, "British", "A British actress who works in Hindi films. One of India's highest-paid actresses, known for her roles in action films and romantic comedies."));
        actors.add(new ActorDocumentDto(398L, "Aishwarya Rai", 1973, "Indian", "An Indian actress and the winner of the Miss World 1994 pageant. A leading contemporary actress of Indian cinema."));
        actors.add(new ActorDocumentDto(399L, "Kareena Kapoor", 1980, "Indian", "An Indian actress from a prominent acting family. Known for a variety of film genres, from romantic comedies to crime dramas."));
        actors.add(new ActorDocumentDto(400L, "Vidya Balan", 1979, "Indian", "An Indian actress known for pioneering a change in the concept of a Hindi film heroine with her roles in female-led films."));

        return actors;
    }
}