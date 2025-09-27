package com.nais.recommendation_service.service;

import com.nais.recommendation_service.model.Actor;
import com.nais.recommendation_service.service.ActorService;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;

@Service
public class SagaOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(SagaOrchestrator.class);

    private final ActorService actorService;
    private final WebClient webClient;

    public SagaOrchestrator(ActorService actorService) {
        this.actorService = actorService;
        this.webClient = WebClient.builder()
                .baseUrl("http://search-service:8095")
                .build();
    }

    public SagaResult createActorSaga(String name, String biography, Integer birthYear, String nationality) {
        logger.info("Starting Saga for creating actor: {}", name);

        Actor neo4jActor = null;
        String weaviateId = null;

        try {
            // Step 1: Create actor in Neo4j (Graph database)
            logger.info("Step 1: Creating actor in Neo4j");
            Actor actorToCreate = new Actor();
            actorToCreate.setName(name);

            neo4jActor = actorService.createActor(actorToCreate);
            logger.info("Actor created in Neo4j with ID: {}", neo4jActor.getId());

            // Step 2: Create actor in Weaviate (Vector database)
            logger.info("Step 2: Creating actor in Weaviate");
            Map<String, Object> weaviateActor = new HashMap<>();
            weaviateActor.put("actorId", neo4jActor.getId());
            weaviateActor.put("name", name);
            weaviateActor.put("biography", biography);
            weaviateActor.put("birthYear", birthYear);
            weaviateActor.put("nationality", nationality);

            weaviateId = webClient.post()
                    .uri("/actors")
                    .bodyValue(weaviateActor)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (weaviateId == null || weaviateId.contains("Failed")) {
                throw new RuntimeException("Failed to create actor in Weaviate");
            }

            logger.info("Actor created in Weaviate with ID: {}", weaviateId);

            return SagaResult.success(neo4jActor.getId(), weaviateId, "Actor successfully created in both databases");

        } catch (Exception e) {
            logger.error("Saga failed, starting compensation", e);

            // Compensation: Rollback operations
            try {
                // If Weaviate creation failed but Neo4j succeeded, delete from Neo4j
                if (neo4jActor != null && neo4jActor.getId() != null) {
                    logger.info("Compensating: Deleting actor from Neo4j with ID: {}", neo4jActor.getId());
                    actorService.deleteActor(neo4jActor.getId());
                }

                // If both succeeded but something else failed, cleanup both
                if (weaviateId != null) {
                    logger.info("Compensating: Deleting actor from Weaviate with ID: {}", weaviateId);
                    webClient.delete()
                            .uri("/actors/{id}", neo4jActor.getId())
                            .retrieve()
                            .bodyToMono(Void.class)
                            .block();
                }

            } catch (Exception compensationError) {
                logger.error("Compensation failed", compensationError);
                return SagaResult.failure("Transaction failed and compensation also failed: " +
                        e.getMessage() + " | Compensation error: " + compensationError.getMessage());
            }

            return SagaResult.failure("Transaction failed but successfully compensated: " + e.getMessage());
        }
    }

    public static class SagaResult {
        private final boolean success;
        private final Long neo4jId;
        private final String weaviateId;
        private final String message;

        private SagaResult(boolean success, Long neo4jId, String weaviateId, String message) {
            this.success = success;
            this.neo4jId = neo4jId;
            this.weaviateId = weaviateId;
            this.message = message;
        }

        public static SagaResult success(Long neo4jId, String weaviateId, String message) {
            return new SagaResult(true, neo4jId, weaviateId, message);
        }

        public static SagaResult failure(String message) {
            return new SagaResult(false, null, null, message);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public Long getNeo4jId() { return neo4jId; }
        public String getWeaviateId() { return weaviateId; }
        public String getMessage() { return message; }
    }
}