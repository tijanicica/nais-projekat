package com.nais.recommendation_service.service;

import com.nais.recommendation_service.model.Genre;
import com.nais.recommendation_service.repository.GenreRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GenreService {
    private final GenreRepository genreRepository;

    public GenreService(GenreRepository genreRepository) {
        this.genreRepository = genreRepository;
    }

    public Genre createGenre(Genre genre) {
        return genreRepository.save(genre);
    }

    public Optional<Genre> getGenreById(Long id) {
        return genreRepository.findById(id);
    }

    public List<Genre> getAllGenres() {
        return genreRepository.findAll();
    }

    public Genre updateGenre(Long id, Genre genreDetails) {
        return genreRepository.findById(id).map(genre -> {
            genre.setName(genreDetails.getName());
            return genreRepository.save(genre);
        }).orElseThrow(() -> new RuntimeException("Genre not found with id " + id));
    }

    public void deleteGenre(Long id) {
        genreRepository.deleteById(id);
    }
}