package com.nais.search_service.service;

import com.nais.search_service.dto.MovieDocumentDto;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.base.Result;
import io.weaviate.client.v1.data.model.WeaviateObject;
import io.weaviate.client.v1.filters.Operator;
import io.weaviate.client.v1.filters.WhereFilter;
import io.weaviate.client.v1.graphql.model.GraphQLResponse;
import io.weaviate.client.v1.graphql.query.argument.NearTextArgument;
import io.weaviate.client.v1.graphql.query.argument.WhereArgument;
import io.weaviate.client.v1.graphql.query.fields.Field;
import org.springframework.stereotype.Service;
import io.weaviate.client.v1.graphql.query.argument.HybridArgument;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MovieService {

    private final WeaviateClient client;

    public MovieService(WeaviateClient client) {
        this.client = client;
    }

    // CREATE
    public String createMovie(MovieDocumentDto movieDto) {
        String newId = UUID.randomUUID().toString();
        Map<String, Object> properties = new HashMap<>();
        properties.put("movieId", movieDto.getMovieId());
        properties.put("title", movieDto.getTitle());
        properties.put("description", movieDto.getDescription());
        properties.put("releaseYear", movieDto.getReleaseYear());
        properties.put("genre", movieDto.getGenre());

        Result<WeaviateObject> result = client.data().creator()
                .withClassName("Movie") // Zamenio DataSeeder.MOVIE_CLASS sa "Movie"
                .withID(newId)
                .withProperties(properties)
                .run();

        if (result.hasErrors()) {
            System.err.println("Error creating movie: " + result.getError().getMessages());
            return null;
        }
        return newId;
    }

    // READ (by custom ID) - ISPRAVLJENO
    public Map<String, Object> getMovieById(Long movieId) {
        Field[] fieldsToReturn = {
                Field.builder().name("movieId").build(),
                Field.builder().name("title").build(),
                Field.builder().name("description").build(),
                Field.builder().name("releaseYear").build(),
                Field.builder().name("genre").build()
        };

        // Kreiranje WhereFilter objekta umesto Map objekta
        WhereFilter whereFilter = WhereFilter.builder()
                .path(new String[]{"movieId"})
                .operator(Operator.Equal)
                .valueInt(movieId.intValue())
                .build();

        WhereArgument where = WhereArgument.builder().filter(whereFilter).build();

        Result<GraphQLResponse> result = client.graphQL().get()
                .withClassName("Movie")
                .withFields(fieldsToReturn)
                .withWhere(where)
                .withLimit(1)
                .run();

        if (result.hasErrors() || result.getResult() == null) {
            return null;
        }

        Map<String, Object> data = (Map<String, Object>) result.getResult().getData();
        Map<String, Object> get = (Map<String, Object>) data.get("Get");
        List<Map<String, Object>> movies = (List<Map<String, Object>>) get.get("Movie");

        return movies.isEmpty() ? null : movies.get(0);
    }



    /*
    // UPDATE - ISPRAVLJENO
    public boolean updateMovie(Long movieId, MovieDocumentDto movieDto) {
        String weaviateId = findWeaviateIdByMovieId(movieId);

        if (weaviateId == null) {
            return false;
        }

        Map<String, Object> propertiesToUpdate = new HashMap<>();
        propertiesToUpdate.put("title", movieDto.getTitle());
        propertiesToUpdate.put("description", movieDto.getDescription());
        propertiesToUpdate.put("releaseYear", movieDto.getReleaseYear());
        propertiesToUpdate.put("genre", movieDto.getGenre());

        // Merger umesto updater - vraća Result<Boolean>
        Result<Boolean> updateResult = client.data().updater()
                .withClassName("Movie")
                .withID(weaviateId)
                .withProperties(propertiesToUpdate)
                .run();

        return !updateResult.hasErrors() && updateResult.getResult();
    }
*/

    // UPDATE - Korišćenje .updater() sa .withMerge() opcijom
    public boolean updateMovie(Long movieId, MovieDocumentDto movieDto) {
        String weaviateId = findWeaviateIdByMovieId(movieId);

        if (weaviateId == null) {
            return false;
        }

        Map<String, Object> propertiesToUpdate = new HashMap<>();
        propertiesToUpdate.put("title", movieDto.getTitle());
        propertiesToUpdate.put("description", movieDto.getDescription());
        propertiesToUpdate.put("releaseYear", movieDto.getReleaseYear());
        propertiesToUpdate.put("genre", movieDto.getGenre());

        // KORISTIMO .updater() ALI MU DODAJEMO .withMerge() DA SE PONAŠA KAO PATCH
        Result<Boolean> updateResult = client.data().updater()
                .withMerge() // <-- DODAJ OVU LINIJU!
                .withClassName("Movie")
                .withID(weaviateId)
                .withProperties(propertiesToUpdate)
                .run();

        if (updateResult.hasErrors()) {
            System.err.println("Error merging movie with Weaviate ID " + weaviateId + ": " + updateResult.getError().getMessages());
        }

        // Vraćamo se na originalnu proveru, jer sa .withMerge() opet vraća Boolean
        return !updateResult.hasErrors() && updateResult.getResult();
    }


    // DELETE - ISPRAVLJENO
    public boolean deleteMovie(Long movieId) {
        String weaviateId = findWeaviateIdByMovieId(movieId);

        if (weaviateId == null) {
            return false;
        }

        Result<Boolean> result = client.data().deleter()
                .withClassName("Movie")
                .withID(weaviateId)
                .run();

        return !result.hasErrors() && result.getResult();
    }

    // Pomoćna metoda - ISPRAVLJENO
    private String findWeaviateIdByMovieId(Long movieId) {
        Field idField = Field.builder().name("_additional").fields(Field.builder().name("id").build()).build();

        // Kreiranje WhereFilter objekta umesto Map objekta
        WhereFilter whereFilter = WhereFilter.builder()
                .path(new String[]{"movieId"})
                .operator(Operator.Equal)
                .valueInt(movieId.intValue())
                .build();

        WhereArgument where = WhereArgument.builder().filter(whereFilter).build();

        Result<GraphQLResponse> result = client.graphQL().get()
                .withClassName("Movie")
                .withFields(idField)
                .withWhere(where)
                .withLimit(1)
                .run();

        if (result.hasErrors() || result.getResult() == null) {
            return null;
        }

        Map<String, Object> data = (Map<String, Object>) result.getResult().getData();
        Map<String, Object> get = (Map<String, Object>) data.get("Get");
        List<Map<String, Object>> movies = (List<Map<String, Object>>) get.get("Movie");

        if (movies == null || movies.isEmpty()) {
            return null;
        }

        Map<String, Object> additional = (Map<String, Object>) movies.get(0).get("_additional");
        return additional != null ? (String) additional.get("id") : null;
    }

    // SEARCH (Read)
    public List<Map<String, Object>> searchMoviesByDescription(String query, int limit) {
        Field[] fields = {
                Field.builder().name("movieId").build(),
                Field.builder().name("title").build(),
                Field.builder().name("description").build(),
                Field.builder().name("releaseYear").build(),
                Field.builder().name("genre").build(),
                Field.builder().name("_additional").fields(
                        Field.builder().name("distance").build()
                ).build()
        };

        NearTextArgument nearText = NearTextArgument.builder().concepts(new String[]{query}).build();

        Result<GraphQLResponse> result = client.graphQL().get()
                .withClassName("Movie")
                .withFields(fields)
                .withNearText(nearText)
                .withLimit(limit)
                .run();

        if (result.hasErrors() || result.getResult() == null) {
            return null;
        }

        Map<String, Object> data = (Map<String, Object>) result.getResult().getData();
        Map<String, Object> get = (Map<String, Object>) data.get("Get");
        return (List<Map<String, Object>>) get.get("Movie");
    }

    // U MovieService.java

    public List<Map<String, Object>> filterMovies(String genre, int year) {
        Field[] fieldsToReturn = {
                Field.builder().name("movieId").build(),
                Field.builder().name("title").build(),
                Field.builder().name("releaseYear").build(),
                Field.builder().name("genre").build()
        };

        // Kreiramo dva odvojena filtera
        WhereFilter genreFilter = WhereFilter.builder()
                .path(new String[]{"genre"})
                .operator(Operator.Equal)
                .valueText(genre)
                .build();

        WhereFilter yearFilter = WhereFilter.builder()
                .path(new String[]{"releaseYear"})
                .operator(Operator.GreaterThanEqual) // Veće ili jednako
                .valueInt(year)
                .build();

        // Spajamo ih koristeći AND operator
        WhereFilter combinedFilter = WhereFilter.builder()
                .operator(Operator.And)
                .operands(new WhereFilter[]{genreFilter, yearFilter})
                .build();

        WhereArgument where = WhereArgument.builder().filter(combinedFilter).build();

        Result<GraphQLResponse> result = client.graphQL().get()
                .withClassName("Movie")
                .withFields(fieldsToReturn)
                .withWhere(where)
                .run();

        if (result.hasErrors() || result.getResult() == null) {
            return null;
        }

        Map<String, Object> data = (Map<String, Object>) result.getResult().getData();
        Map<String, Object> get = (Map<String, Object>) data.get("Get");
        return (List<Map<String, Object>>) get.get("Movie");
    }

    public long countMoviesByGenre(String genre) {
        WhereFilter genreFilter = WhereFilter.builder()
                .path(new String[]{"genre"})
                .operator(Operator.Equal)
                .valueText(genre)
                .build();

        Result<GraphQLResponse> result = client.graphQL().aggregate()
                .withClassName("Movie")
                .withWhere(genreFilter)
                .withFields(Field.builder().name("meta").fields(Field.builder().name("count").build()).build())
                .run();

        if (result.hasErrors() || result.getResult() == null) {
            return 0;
        }

        try {
            Map<String, Object> data = (Map<String, Object>) result.getResult().getData();
            Map<String, Object> aggregate = (Map<String, Object>) data.get("Aggregate");
            List<Map<String, Object>> movies = (List<Map<String, Object>>) aggregate.get("Movie");
            if (movies != null && !movies.isEmpty()) {
                Map<String, Object> meta = (Map<String, Object>) movies.get(0).get("meta");
                return ((Number) meta.get("count")).longValue();
            }
        } catch (Exception e) {
            // Greška pri parsiranju
            return 0;
        }
        return 0;
    }

    public List<Map<String, Object>> searchWithFilters(String query, String genre, int year, int limit) {
        NearTextArgument nearText = NearTextArgument.builder().concepts(new String[]{query}).build();

        // Isti kombinovani filter kao u prostom upitu
        WhereFilter genreFilter = WhereFilter.builder().path(new String[]{"genre"}).operator(Operator.Equal).valueText(genre).build();
        WhereFilter yearFilter = WhereFilter.builder().path(new String[]{"releaseYear"}).operator(Operator.GreaterThanEqual).valueInt(year).build();
        WhereFilter combinedFilter = WhereFilter.builder().operator(Operator.And).operands(new WhereFilter[]{genreFilter, yearFilter}).build();
        WhereArgument where = WhereArgument.builder().filter(combinedFilter).build();

        Result<GraphQLResponse> result = client.graphQL().get()
                .withClassName("Movie")
                .withFields(Field.builder().name("title").build(), Field.builder().name("genre").build(), Field.builder().name("releaseYear").build())
                .withNearText(nearText)
                .withWhere(where)
                .withLimit(limit)
                .run();

        if (result.hasErrors() || result.getResult() == null) {
            return null;
        }

        Map<String, Object> data = (Map<String, Object>) result.getResult().getData();
        Map<String, Object> get = (Map<String, Object>) data.get("Get");
        return (List<Map<String, Object>>) get.get("Movie");
    }

    public List<Map<String, Object>> searchWithPagination(String query, int offset, int limit) {
        NearTextArgument nearText = NearTextArgument.builder().concepts(new String[]{query}).build();

        Result<GraphQLResponse> result = client.graphQL().get()
                .withClassName("Movie")
                .withFields(Field.builder().name("title").build(), Field.builder().name("movieId").build())
                .withNearText(nearText)
                .withOffset(offset) // Preskoči 'offset' broj rezultata
                .withLimit(limit)   // Vrati 'limit' broj rezultata
                .run();

        if (result.hasErrors() || result.getResult() == null) {
            return null;
        }

        Map<String, Object> data = (Map<String, Object>) result.getResult().getData();
        Map<String, Object> get = (Map<String, Object>) data.get("Get");
        return (List<Map<String, Object>>) get.get("Movie");
    }


}