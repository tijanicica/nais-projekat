package com.nais.search_service.service;

import com.nais.search_service.dto.MovieDocumentDto;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class SearchService {

    private final VectorStore vectorStore;

    @Autowired
    public SearchService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    // CREATE
    public void addMovie(MovieDocumentDto movie) {
        Document doc = new Document(movie.getDescription(), Map.of(
                "title", movie.getTitle(),
                "releaseYear", movie.getReleaseYear(),
                "genre", movie.getGenre()
        ));
        vectorStore.add(List.of(doc));
    }

    // READ - Simple semantic search
    public List<Document> findSimilarMoviesByDescription(String query, int topK) {
        return vectorStore.similaritySearch(SearchRequest.query(query).withTopK(topK));
    }

    // READ - Hybrid search (kombinacija ključnih reči i semantike)
    // Weaviate podržava hibridnu pretragu, ali Spring AI apstrakcija može varirati.
    // Ovo je primer kako bi izgledao složeniji upit sa filtriranjem.
    public List<Document> findSimilarMoviesWithFilter(String query, String genre, int topK) {
        SearchRequest request = SearchRequest.query(query)
                .withTopK(topK)
                .withFilterExpression("genre == '" + genre + "'");
        return vectorStore.similaritySearch(request);
    }

    // DELETE
    public void deleteMovies(List<String> documentIds) {
        vectorStore.delete(documentIds);
    }
}