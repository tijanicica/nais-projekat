package com.nais.recommendation_service.service;

import com.nais.recommendation_service.model.Director;
import com.nais.recommendation_service.repository.DirectorRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DirectorService {
    private final DirectorRepository directorRepository;

    public DirectorService(DirectorRepository directorRepository) {
        this.directorRepository = directorRepository;
    }

    public Director createDirector(Director director) {
        return directorRepository.save(director);
    }

    public Optional<Director> getDirectorById(Long id) {
        return directorRepository.findById(id);
    }

    public List<Director> getAllDirectors() {
        return directorRepository.findAll();
    }

    public Director updateDirector(Long id, Director directorDetails) {
        return directorRepository.findById(id).map(director -> {
            director.setName(directorDetails.getName());
            return directorRepository.save(director);
        }).orElseThrow(() -> new RuntimeException("Director not found with id " + id));
    }

    public void deleteDirector(Long id) {
        directorRepository.deleteById(id);
    }
}