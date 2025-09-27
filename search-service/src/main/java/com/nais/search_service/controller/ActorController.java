package com.nais.search_service.controller;

import com.nais.search_service.dto.ActorDocumentDto;
import com.nais.search_service.service.ActorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/actors")
public class ActorController {

    private final ActorService actorService;

    public ActorController(ActorService actorService) {
        this.actorService = actorService;
    }

    @GetMapping("/weaviate/{weaviateId}")
    public ResponseEntity<Map<String, Object>> getActorByWeaviateId(@PathVariable String weaviateId) {
        Map<String, Object> actor = actorService.getActorByWeaviateId(weaviateId);
        return actor != null ?
                ResponseEntity.ok(actor) :
                ResponseEntity.notFound().build();
    }
    @PostMapping
    public ResponseEntity<String> addActor(@RequestBody ActorDocumentDto actor) {
        String id = actorService.createActor(actor);
        return id != null ?
                new ResponseEntity<>(id, HttpStatus.CREATED) :
                new ResponseEntity<>("Failed to create actor", HttpStatus.INTERNAL_SERVER_ERROR);
    }



    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getActorById(@PathVariable Long id) {
        Map<String, Object> actor = actorService.getActorById(id);
        return actor != null ?
                ResponseEntity.ok(actor) :
                ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateActor(@PathVariable Long id, @RequestBody ActorDocumentDto actor) {
        boolean success = actorService.updateActor(id, actor);
        return success ?
                ResponseEntity.ok().build() :
                ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteActor(@PathVariable Long id) {
        boolean success = actorService.deleteActor(id);
        return success ?
                ResponseEntity.noContent().build() :
                ResponseEntity.notFound().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchSimilar(
            @RequestParam String q,
            @RequestParam(defaultValue = "5") int limit) {
        List<Map<String, Object>> results = actorService.searchActorsByBiography(q, limit);
        return results != null ?
                ResponseEntity.ok(results) :
                ResponseEntity.internalServerError().build();
    }


    @GetMapping("/filter")
    public ResponseEntity<List<Map<String, Object>>> filterActors(
            @RequestParam String nationality,
            @RequestParam int year) {
        List<Map<String, Object>> results = actorService.filterActors(nationality, year);
        // ISPRAVNA PROVERA: Da li lista nije null
        return results != null ? ResponseEntity.ok(results) : ResponseEntity.notFound().build();
    }


    @GetMapping("/count")
    public ResponseEntity<Long> countActorsByNationality(@RequestParam String nationality) {
        long count = actorService.countActorsByNationality(nationality);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/search-filtered")
    public ResponseEntity<List<Map<String, Object>>> searchActorsWithComplexFilter(
            @RequestParam String q,
            @RequestParam String nationality,
            @RequestParam int year, // Dodali smo novi parametar za godinu
            @RequestParam(defaultValue = "5") int limit) {
        List<Map<String, Object>> results = actorService.searchActorsWithComplexFilter(q, nationality, year, limit);
        return results != null ? ResponseEntity.ok(results) : ResponseEntity.internalServerError().build();
    }

}