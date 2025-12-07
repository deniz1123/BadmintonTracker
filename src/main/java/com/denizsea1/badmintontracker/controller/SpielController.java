package com.denizsea1.badmintontracker.controller;

import com.denizsea1.badmintontracker.model.Seite;
import com.denizsea1.badmintontracker.model.Spiel;
import com.denizsea1.badmintontracker.repository.SpielRepository;
import com.denizsea1.badmintontracker.service.SpielService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST-Controller für die Verwaltung von Spielen.
 *
 * Bietet Endpunkte für:
 * - Liste aller Spiele / einzelnes Spiel
 * - Start eines neuen Spiels
 * - Punktevergabe für Team A / Team B
 * - Pausenempfehlung
 */
@RestController
@RequestMapping("/spiele")
public class SpielController {

    private final SpielRepository spielRepository;
    private final SpielService spielService;

    public SpielController(SpielRepository spielRepository,
                           SpielService spielService) {
        this.spielRepository = spielRepository;
        this.spielService = spielService;
    }

    // ---------------------------------------------------------------------
    // 1) Basis-CRUD (zum Debuggen / Anzeigen)
    // ---------------------------------------------------------------------

    /**
     * Liefert alle Spiele zurück.
     */
    @GetMapping
    public List<Spiel> getAlleSpiele() {
        return spielRepository.findAll();
    }

    /**
     * Liefert ein Spiel nach ID zurück oder 404, wenn nicht gefunden.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Spiel> getSpielById(@PathVariable Long id) {
        Optional<Spiel> spielOpt = spielRepository.findById(id);
        return spielOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // ---------------------------------------------------------------------
    // 2) Spielstart
    // ---------------------------------------------------------------------

    /**
     * Startet ein neues Spiel mit zwei bestehenden Teams.
     *
     * Beispiel-URL:
     * /spiele/start?teamAId=1&teamBId=2&aufschlagTeamIstA=true&startSeite=RECHTS
     */
    @PostMapping("/start")
    public Spiel startSpiel(@RequestParam Long teamAId,
                            @RequestParam Long teamBId,
                            @RequestParam boolean aufschlagTeamIstA,
                            @RequestParam Seite startSeite) {
        return spielService.startNeuesSpiel(teamAId, teamBId, aufschlagTeamIstA, startSeite);
    }

    // ---------------------------------------------------------------------
    // 3) Punktevergabe
    // ---------------------------------------------------------------------

    /**
     * Vergibt einen Punkt an Team A (Teams[0]).
     */
    @PostMapping("/{spielId}/punktA")
    public Spiel punktFuerTeamA(@PathVariable Long spielId) {
        return spielService.punktFuerTeamA(spielId);
    }

    /**
     * Vergibt einen Punkt an Team B (Teams[1]).
     */
    @PostMapping("/{spielId}/punktB")
    public Spiel punktFuerTeamB(@PathVariable Long spielId) {
        return spielService.punktFuerTeamB(spielId);
    }

    // ---------------------------------------------------------------------
    // 4) Pausenempfehlung
    // ---------------------------------------------------------------------

    /**
     * Liefert true, falls im aktuellen Satz eine Pause empfohlen wird
     * (mindestens 11 Punkte eines Teams).
     */
    @GetMapping("/{spielId}/pauseEmpfohlen")
    public boolean istPauseEmpfohlen(@PathVariable Long spielId) {
        return spielService.istPauseEmpfohlen(spielId);
    }
}
