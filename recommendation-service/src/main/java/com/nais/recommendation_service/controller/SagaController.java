package com.nais.recommendation_service.controller;

import com.nais.recommendation_service.service.SagaOrchestrator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/saga")
public class SagaController {

    private final SagaOrchestrator sagaOrchestrator;

    public SagaController(SagaOrchestrator sagaOrchestrator) {
        this.sagaOrchestrator = sagaOrchestrator;
    }

    @PostMapping("/create-actor")
    public ResponseEntity<Map<String, Object>> createActor(@RequestBody CreateActorRequest request) {

        SagaOrchestrator.SagaResult result = sagaOrchestrator.createActorSaga(
                request.getName(),
                request.getBiography(),
                request.getBirthYear(),
                request.getNationality()
        );

        Map<String, Object> response = Map.of(
                "success", result.isSuccess(),
                "message", result.getMessage(),
                "neo4jId", result.getNeo4jId() != null ? result.getNeo4jId() : "null",
                "weaviateId", result.getWeaviateId() != null ? result.getWeaviateId() : "null"
        );

        return result.isSuccess() ?
                ResponseEntity.ok(response) :
                ResponseEntity.badRequest().body(response);
    }

    public static class CreateActorRequest {
        private String name;
        private String biography;
        private Integer birthYear;
        private String nationality;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getBiography() { return biography; }
        public void setBiography(String biography) { this.biography = biography; }

        public Integer getBirthYear() { return birthYear; }
        public void setBirthYear(Integer birthYear) { this.birthYear = birthYear; }

        public String getNationality() { return nationality; }
        public void setNationality(String nationality) { this.nationality = nationality; }
    }
}