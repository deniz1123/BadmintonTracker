package com.denizsea1.badmintontracker.controller;

import com.denizsea1.badmintontracker.model.Satz;
import com.denizsea1.badmintontracker.repository.SatzRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/saetze")
public class SatzController {

    private final SatzRepository satzRepository;

    public SatzController(SatzRepository satzRepository) {
        this.satzRepository = satzRepository;
    }

    @GetMapping
    public List<Satz> getAlleSaetze() {
        return satzRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Satz> getSatzById(@PathVariable Long id) {
        Optional<Satz> satzOpt = satzRepository.findById(id);
        return satzOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Satz createSatz(@RequestBody Satz satz) {
        return satzRepository.save(satz);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSatz(@PathVariable Long id) {
        if (satzRepository.existsById(id)) {
            satzRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
