package com.nais.recommendation_service.service;

import com.nais.recommendation_service.model.Actor;
import com.nais.recommendation_service.repository.ActorRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ActorService {
    private final ActorRepository actorRepository;

    public ActorService(ActorRepository actorRepository) {
        this.actorRepository = actorRepository;
    }

    public Actor createActor(Actor actor) {
        return actorRepository.save(actor);
    }

    public Optional<Actor> getActorById(Long id) {
        return actorRepository.findById(id);
    }

    public List<Actor> getAllActors() {
        return actorRepository.findAll();
    }

    public Actor updateActor(Long id, Actor actorDetails) {
        return actorRepository.findById(id).map(actor -> {
            actor.setName(actorDetails.getName());
            return actorRepository.save(actor);
        }).orElseThrow(() -> new RuntimeException("Actor not found with id " + id));
    }

    public void deleteActor(Long id) {
        actorRepository.deleteById(id);
    }
}