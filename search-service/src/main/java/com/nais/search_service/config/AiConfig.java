package com.nais.search_service.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class AiConfig {

    @Bean
    public EmbeddingModel embeddingModel() {
        return new EmbeddingModel() {
            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                // Ključna izmena: Vraćamo listu koja sadrži jedan lažni embedding
                // za svaki dokument u zahtevu.
                List<Embedding> embeddings = request.getInstructions().stream()
                        .map(text -> new Embedding(new float[dimensions()], 0))
                        .collect(Collectors.toList());
                return new EmbeddingResponse(embeddings);
            }

            @Override
            public float[] embed(Document document) {
                return new float[0];
            }

            @Override
            public int dimensions() {
                // Vraćamo podrazumevanu vrednost za contextionary (384)
                return 384;
            }
        };
    }
}