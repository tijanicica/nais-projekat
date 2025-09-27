package com.nais.recommendation_service.controller;

import com.nais.recommendation_service.model.Director;
import com.nais.recommendation_service.service.DirectorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/directors")
public class DirectorController {
    private final DirectorService directorService;

    public DirectorController(DirectorService directorService) {
        this.directorService = directorService;
    }

    @PostMapping
    public Director createDirector(@RequestBody Director director) {
        return directorService.createDirector(director);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Director> getDirectorById(@PathVariable Long id) {
        return directorService.getDirectorById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<Director> getAllDirectors() {
        return directorService.getAllDirectors();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Director> updateDirector(@PathVariable Long id, @RequestBody Director directorDetails) {
        try {
            Director updatedDirector = directorService.updateDirector(id, directorDetails);
            return ResponseEntity.ok(updatedDirector);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDirector(@PathVariable Long id) {
        try {
            directorService.deleteDirector(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}