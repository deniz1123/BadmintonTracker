package com.denizsea1.badmintontracker.controller;

import com.denizsea1.badmintontracker.model.Team;
import com.denizsea1.badmintontracker.model.Spieler;
import com.denizsea1.badmintontracker.repository.TeamRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/teams")
public class TeamController {

    private final TeamRepository teamRepository;

    public TeamController(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    // 1) Alle Teams holen: GET /teams
    @GetMapping
    public List<Team> getAlleTeams() {
        return teamRepository.findAll();
    }

    // 2) Ein Team nach ID holen: GET /teams/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Team> getTeamById(@PathVariable Long id) {
        Optional<Team> teamOpt = teamRepository.findById(id);
        return teamOpt.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 3) Team anlegen: POST /teams
    @PostMapping
    public Team createTeam(@RequestBody Team team) {

        // Beziehung Team <-> Spieler setzen + Flag initialisieren
        if (team.getSpieler() != null) {
            for (Spieler s : team.getSpieler()) {
                s.setTeam(team);
                s.setInAktivemSpiel(false); // neues Team, noch in keinem laufenden Spiel
            }
        }

        // Startpositionen im Team vergeben (RECHTS/LINKS)
        team.initialisierePositionen();

        return teamRepository.save(team);
    }

    // 4) Team löschen: DELETE /teams/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeam(@PathVariable Long id) {
        if (teamRepository.existsById(id)) {
            teamRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
