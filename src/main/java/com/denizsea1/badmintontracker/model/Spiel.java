package com.denizsea1.badmintontracker.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Repräsentiert ein Badminton-Doppel-Match (Best-of-Three).
 */
@Entity
public class Spiel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Datum, an dem das Spiel stattfindet.
     */
    private LocalDate datum;

    /**
     * Die Teams dieses Spiels (Team A und Team B).
     *
     * Many-to-Many:
     * - Ein Spiel hat typischerweise 2 Teams.
     * - Ein Team kann in mehreren Spielen vorkommen.
     *
     * Wir nutzen die Reihenfolge:
     *   teams.get(0) = Team A
     *   teams.get(1) = Team B
     */
    @ManyToMany
    @JoinTable(
            name = "spiel_team",
            joinColumns = @JoinColumn(name = "spiel_id"),
            inverseJoinColumns = @JoinColumn(name = "team_id")
    )
    private List<Team> teams = new ArrayList<>();

    /**
     * Die Sätze (max. 3) dieses Spiels.
     */
    @OneToMany(mappedBy = "spiel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Satz> saetze = new ArrayList<>();

    /**
     * Gewinnerteam dieses Spiels (falls vorhanden).
     */
    @ManyToOne
    private Team gewinnerTeam;

    /**
     * Welches Team hat aktuell das Aufschlagsrecht?
     */
    @ManyToOne
    private Team aufschlagTeam;

    /**
     * Von welcher Seite (LINKS/RECHTS) wird aktuell aufgeschlagen?
     */
    @Enumerated(EnumType.STRING)
    private Seite aufschlagSeite;

    /**
     * Status des Spiels (LAUFEND, BEENDET, ABGEBROCHEN).
     */
    @Enumerated(EnumType.STRING)
    private SpielStatus status;

    public Spiel() {
    }

    public Spiel(LocalDate datum) {
        this.datum = datum;
        this.status = SpielStatus.LAUFEND;
    }

    // -------------------------------------------------------------------------
    // Hilfsmethoden, um Teams/Sätze sauber zu verknüpfen
    // -------------------------------------------------------------------------

    /**
     * Fügt dem Spiel ein Team hinzu.
     * Reihenfolge der Aufrufe bestimmt, welches Team "A" bzw. "B" ist.
     */
    public void addTeam(Team team) {
        teams.add(team);
    }

    public void addSatz(Satz satz) {
        saetze.add(satz);
        satz.setSpiel(this);
    }

    // -------------------------------------------------------------------------
    // Getter/Setter
    // -------------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public LocalDate getDatum() {
        return datum;
    }

    public void setDatum(LocalDate datum) {
        this.datum = datum;
    }

    public List<Team> getTeams() {
        return teams;
    }

    public void setTeams(List<Team> teams) {
        this.teams = teams;
    }

    public List<Satz> getSaetze() {
        return saetze;
    }

    public void setSaetze(List<Satz> saetze) {
        this.saetze = saetze;
    }

    public Team getGewinnerTeam() {
        return gewinnerTeam;
    }

    /**
     * Setzt das Gewinnerteam und markiert das Spiel als BEENDET.
     */
    public void setGewinnerTeam(Team gewinnerTeam) {
        this.gewinnerTeam = gewinnerTeam;
        this.status = SpielStatus.BEENDET;
    }

    public Team getAufschlagTeam() {
        return aufschlagTeam;
    }

    public void setAufschlagTeam(Team aufschlagTeam) {
        this.aufschlagTeam = aufschlagTeam;
    }

    public Seite getAufschlagSeite() {
        return aufschlagSeite;
    }

    public void setAufschlagSeite(Seite aufschlagSeite) {
        this.aufschlagSeite = aufschlagSeite;
    }

    public SpielStatus getStatus() {
        return status;
    }

    public void setStatus(SpielStatus status) {
        this.status = status;
    }
}
