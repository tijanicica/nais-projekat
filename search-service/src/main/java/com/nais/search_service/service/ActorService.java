package com.nais.search_service.service;

import com.nais.search_service.dto.ActorDocumentDto;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ActorService {

    private final WeaviateClient client;

    public ActorService(WeaviateClient client) {
        this.client = client;
    }

    // CREATE
    public String createActor(ActorDocumentDto actorDto) {
        String newId = UUID.randomUUID().toString();
        Map<String, Object> properties = new HashMap<>();
        properties.put("actorId", actorDto.getActorId());
        properties.put("name", actorDto.getName());
        properties.put("birthYear", actorDto.getBirthYear());
        properties.put("nationality", actorDto.getNationality());
        properties.put("biography", actorDto.getBiography());

        Result<WeaviateObject> result = client.data().creator()
                .withClassName("Actor")
                .withID(newId)
                .withProperties(properties)
                .run();

        if (result.hasErrors()) {
            System.err.println("Error creating actor: " + result.getError().getMessages());
            return null;
        }
        return newId;
    }

    // READ (by custom ID)
    public Map<String, Object> getActorById(Long actorId) {
        Field[] fieldsToReturn = {
                Field.builder().name("actorId").build(),
                Field.builder().name("name").build(),
                Field.builder().name("birthYear").build(),
                Field.builder().name("nationality").build(),
                Field.builder().name("biography").build()
        };

        WhereFilter whereFilter = WhereFilter.builder()
                .path(new String[]{"actorId"})
                .operator(Operator.Equal)
                .valueInt(actorId.intValue())
                .build();

        WhereArgument where = WhereArgument.builder().filter(whereFilter).build();

        Result<GraphQLResponse> result = client.graphQL().get()
                .withClassName("Actor")
                .withFields(fieldsToReturn)
                .withWhere(where)
                .withLimit(1)
                .run();

        if (result.hasErrors() || result.getResult() == null) {
            return null;
        }

        Map<String, Object> data = (Map<String, Object>) result.getResult().getData();
        Map<String, Object> get = (Map<String, Object>) data.get("Get");
        List<Map<String, Object>> actors = (List<Map<String, Object>>) get.get("Actor");

        return actors.isEmpty() ? null : actors.get(0);
    }

    // UPDATE - Korišćenje .updater() sa .withMerge() opcijom za bezbedno ažuriranje
    public boolean updateActor(Long actorId, ActorDocumentDto actorDto) {
        String weaviateId = findWeaviateIdByActorId(actorId);

        if (weaviateId == null) {
            return false;
        }

        Map<String, Object> propertiesToUpdate = new HashMap<>();
        // Važno: Ne dodajemo 'actorId' u mapu, samo polja koja se menjaju
        propertiesToUpdate.put("name", actorDto.getName());
        propertiesToUpdate.put("birthYear", actorDto.getBirthYear());
        propertiesToUpdate.put("nationality", actorDto.getNationality());
        propertiesToUpdate.put("biography", actorDto.getBiography());

        // KORISTIMO .updater() ALI MU DODAJEMO .withMerge() DA SE PONAŠA KAO PATCH
        Result<Boolean> updateResult = client.data().updater()
                .withMerge() // <-- KLJUČNA IZMENA
                .withClassName("Actor")
                .withID(weaviateId)
                .withProperties(propertiesToUpdate)
                .run();

        if (updateResult.hasErrors()) {
            System.err.println("Error merging actor with Weaviate ID " + weaviateId + ": " + updateResult.getError().getMessages());
        }

        return !updateResult.hasErrors() && updateResult.getResult();
    }

    // DELETE
    public boolean deleteActor(Long actorId) {
        String weaviateId = findWeaviateIdByActorId(actorId);

        if (weaviateId == null) {
            return false;
        }

        Result<Boolean> result = client.data().deleter()
                .withClassName("Actor")
                .withID(weaviateId)
                .run();

        return !result.hasErrors() && result.getResult();
    }

    // Pomoćna metoda
    private String findWeaviateIdByActorId(Long actorId) {
        Field idField = Field.builder().name("_additional").fields(Field.builder().name("id").build()).build();

        WhereFilter whereFilter = WhereFilter.builder()
                .path(new String[]{"actorId"})
                .operator(Operator.Equal)
                .valueInt(actorId.intValue())
                .build();

        WhereArgument where = WhereArgument.builder().filter(whereFilter).build();

        Result<GraphQLResponse> result = client.graphQL().get()
                .withClassName("Actor")
                .withFields(idField)
                .withWhere(where)
                .withLimit(1)
                .run();

        if (result.hasErrors() || result.getResult() == null) {
            return null;
        }

        Map<String, Object> data = (Map<String, Object>) result.getResult().getData();
        Map<String, Object> get = (Map<String, Object>) data.get("Get");
        List<Map<String, Object>> actors = (List<Map<String, Object>>) get.get("Actor");

        if (actors == null || actors.isEmpty()) {
            return null;
        }

        Map<String, Object> additional = (Map<String, Object>) actors.get(0).get("_additional");
        return additional != null ? (String) additional.get("id") : null;
    }

    // SEARCH - Semantička pretraga po biografiji
    public List<Map<String, Object>> searchActorsByBiography(String query, int limit) {
        Field[] fields = {
                Field.builder().name("actorId").build(),
                Field.builder().name("name").build(),
                Field.builder().name("birthYear").build(),
                Field.builder().name("nationality").build(),
                Field.builder().name("biography").build(),
                Field.builder().name("_additional").fields(
                        Field.builder().name("distance").build()
                ).build()
        };

        NearTextArgument nearText = NearTextArgument.builder().concepts(new String[]{query}).build();

        Result<GraphQLResponse> result = client.graphQL().get()
                .withClassName("Actor")
                .withFields(fields)
                .withNearText(nearText)
                .withLimit(limit)
                .run();

        if (result.hasErrors() || result.getResult() == null) {
            return null;
        }

        Map<String, Object> data = (Map<String, Object>) result.getResult().getData();
        Map<String, Object> get = (Map<String, Object>) data.get("Get");
        return (List<Map<String, Object>>) get.get("Actor");
    }

    public List<Map<String, Object>> filterActors(String nationality, int birthYear) {
        Field[] fieldsToReturn = {
                Field.builder().name("actorId").build(),
                Field.builder().name("name").build(),
                Field.builder().name("birthYear").build(),
                Field.builder().name("nationality").build()
        };

        // Filter za nacionalnost
        WhereFilter nationalityFilter = WhereFilter.builder()
                .path(new String[]{"nationality"})
                .operator(Operator.Equal)
                .valueText(nationality)
                .build();

        // Filter za godinu rođenja (stariji od, tj. rođeni pre)
        WhereFilter yearFilter = WhereFilter.builder()
                .path(new String[]{"birthYear"})
                .operator(Operator.LessThan) // Manje od
                .valueInt(birthYear)
                .build();

        // Spajanje filtera sa AND
        WhereFilter combinedFilter = WhereFilter.builder()
                .operator(Operator.And)
                .operands(new WhereFilter[]{nationalityFilter, yearFilter})
                .build();

        WhereArgument where = WhereArgument.builder().filter(combinedFilter).build();

        Result<GraphQLResponse> result = client.graphQL().get()
                .withClassName("Actor")
                .withFields(fieldsToReturn)
                .withWhere(where)
                .withLimit(3)
                .run();

        if (result.hasErrors() || result.getResult() == null) {
            return null;
        }

        Map<String, Object> data = (Map<String, Object>) result.getResult().getData();
        Map<String, Object> get = (Map<String, Object>) data.get("Get");
        return (List<Map<String, Object>>) get.get("Actor");
    }


    public long countActorsByNationality(String nationality) {
        WhereFilter nationalityFilter = WhereFilter.builder()
                .path(new String[]{"nationality"})
                .operator(Operator.Equal)
                .valueText(nationality)
                .build();

        Result<GraphQLResponse> result = client.graphQL().aggregate()
                .withClassName("Actor")
                .withWhere(nationalityFilter)
                .withFields(Field.builder().name("meta").fields(Field.builder().name("count").build()).build())
                .run();

        if (result.hasErrors() || result.getResult() == null) {
            return 0;
        }

        try {
            Map<String, Object> data = (Map<String, Object>) result.getResult().getData();
            Map<String, Object> aggregate = (Map<String, Object>) data.get("Aggregate");
            List<Map<String, Object>> actors = (List<Map<String, Object>>) aggregate.get("Actor");
            if (actors != null && !actors.isEmpty()) {
                Map<String, Object> meta = (Map<String, Object>) actors.get(0).get("meta");
                return ((Number) meta.get("count")).longValue();
            }
        } catch (Exception e) {
            // Greška pri parsiranju
            return 0;
        }
        return 0;
    }

    public List<Map<String, Object>> searchActorsWithComplexFilter(String query, String nationality, int birthYear, int limit) {

        // 1. Definišemo vektorsku pretragu na osnovu teksta (biografije)
        NearTextArgument nearText = NearTextArgument.builder().concepts(new String[]{query}).build();

        // 2. Definišemo DVA filtera
        WhereFilter nationalityFilter = WhereFilter.builder()
                .path(new String[]{"nationality"})
                .operator(Operator.Equal)
                .valueText(nationality)
                .build();

        WhereFilter yearFilter = WhereFilter.builder()
                .path(new String[]{"birthYear"})
                .operator(Operator.GreaterThanEqual) // Rođeni TE ili POSLE godine
                .valueInt(birthYear)
                .build();

        // 3. Spajamo filtere koristeći AND operator
        WhereFilter combinedFilter = WhereFilter.builder()
                .operator(Operator.And)
                .operands(new WhereFilter[]{nationalityFilter, yearFilter})
                .build();

        WhereArgument where = WhereArgument.builder().filter(combinedFilter).build();

        // 4. Definišemo polja koja želimo da nam se vrate
        Field[] fieldsToReturn = {
                Field.builder().name("name").build(),
                Field.builder().name("nationality").build(),
                Field.builder().name("birthYear").build(),
                Field.builder().name("_additional").fields(Field.builder().name("distance").build()).build()
        };

        // 5. Sastavljamo i izvršavamo GraphQL upit
        Result<GraphQLResponse> result = client.graphQL().get()
                .withClassName("Actor")
                .withFields(fieldsToReturn)
                .withNearText(nearText)
                .withWhere(where)
                .withLimit(limit)
                .run();

        if (result.hasErrors() || result.getResult() == null) {
            System.err.println("Error in searchActorsWithComplexFilter: " + (result.hasErrors() ? result.getError().getMessages() : "Result is null"));
            return null;
        }

        Map<String, Object> data = (Map<String, Object>) result.getResult().getData();
        Map<String, Object> get = (Map<String, Object>) data.get("Get");
        return (List<Map<String, Object>>) get.get("Actor");
    }
    public Map<String, Object> getActorByWeaviateId(String weaviateId) {
        Field[] fieldsToReturn = {
                Field.builder().name("actorId").build(),
                Field.builder().name("name").build(),
                Field.builder().name("birthYear").build(),
                Field.builder().name("nationality").build(),
                Field.builder().name("biography").build(),
                Field.builder().name("_additional").fields(
                        Field.builder().name("id").build()
                ).build()
        };

        WhereFilter idFilter = WhereFilter.builder()
                .path(new String[]{"id"})
                .operator(Operator.Equal)
                .valueText(weaviateId)
                .build();

        WhereArgument where = WhereArgument.builder().filter(idFilter).build();

        Result<GraphQLResponse> result = client.graphQL().get()
                .withClassName("Actor")
                .withFields(fieldsToReturn)
                .withWhere(where)
                .withLimit(1)
                .run();

        if (result.hasErrors() || result.getResult() == null) {
            System.err.println("Error in GraphQL query by Weaviate ID: " +
                    (result.hasErrors() ? result.getError().getMessages() : "Result is null"));
            return null;
        }

        Map<String, Object> data = (Map<String, Object>) result.getResult().getData();
        Map<String, Object> get = (Map<String, Object>) data.get("Get");
        List<Map<String, Object>> actors = (List<Map<String, Object>>) get.get("Actor");

        return actors.isEmpty() ? null : actors.get(0);
    }
}