//package com.nais.search_service.config;
//
//import org.springframework.ai.embedding.EmbeddingModel;
//import org.springframework.ai.openai.OpenAiEmbeddingModel;
//import org.springframework.ai.openai.api.OpenAiApi;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.ai.vectorstore.WeaviateVectorStore;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import io.weaviate.client.Config;
//import io.weaviate.client.WeaviateClient;
//
//
//@Configuration
//public class WeaviateConfig {
//
//    @Value("${spring.ai.vectorstore.weaviate.scheme:http}")
//    private String scheme;
//
//    @Value("${spring.ai.vectorstore.weaviate.host:localhost}")
//    private String host;
//
//    @Value("${spring.ai.vectorstore.weaviate.port:8080}")
//    private int port;
//
//    @Value("${spring.ai.openai.api-key}")
//    private String openaiApiKey;
//
//    @Bean
//    public WeaviateClient weaviateClient() {
//        Config config = new Config(scheme, host + ":" + port);
//        return new WeaviateClient(config);
//    }
//
//    @Bean
//    public EmbeddingModel embeddingModel() {
//        return new OpenAiEmbeddingModel(new OpenAiApi(openaiApiKey));
//    }
//
//    @Bean
//    public VectorStore vectorStore(WeaviateClient weaviateClient, EmbeddingModel embeddingModel) {
//        // WeaviateVectorStore se takođe kreira preko konstruktora sa config objektom
//        Config config1 = new Config(scheme, host + ":" + port);
//        WeaviateVectorStore.WeaviateVectorStoreConfig config = WeaviateVectorStore.WeaviateVectorStoreConfig.builder()
//
//                // Opciono, možete dodati i ime klase/kolekcije ako je potrebno
//                // .withObjectClass("MyDocument")
//                .build();
//
//        return new WeaviateVectorStore(config, embeddingModel, new WeaviateClient(config1));
//    }
//}