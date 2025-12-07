package com.denizsea1.badmintontracker.controller;

import com.denizsea1.badmintontracker.model.Spieler;
import com.denizsea1.badmintontracker.repository.SpielerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/spieler")
public class SpielerController {

    private final SpielerRepository spielerRepository;

    // Konstruktor-Injection: Spring "gibt" uns automatisch ein SpielerRepository
    public SpielerController(SpielerRepository spielerRepository) {
        this.spielerRepository = spielerRepository;
    }

    // 1) ALLE Spieler holen: GET /spieler
    @GetMapping
    public List<Spieler> getAlleSpieler() {
        return spielerRepository.findAll();
    }

    // 2) EINEN Spieler nach ID holen: GET /spieler/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Spieler> getSpielerById(@PathVariable Long id) {
        Optional<Spieler> spielerOpt = spielerRepository.findById(id);
        // 200 OK + Spieler im Body
        // 404 Not Found
        return spielerOpt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 3) Spieler anlegen: POST /spieler
    @PostMapping
    public Spieler createSpieler(@RequestBody Spieler spieler) {
        // spieler.id ist null → wird von der DB automatisch vergeben
        return spielerRepository.save(spieler);
    }

    // 4) Spieler löschen: DELETE /spieler/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSpieler(@PathVariable Long id) {
        if (spielerRepository.existsById(id)) {
            spielerRepository.deleteById(id);
            return ResponseEntity.noContent().build();     // 204 No Content
        } else {
            return ResponseEntity.notFound().build();      // 404 Not Found
        }
    }
}
