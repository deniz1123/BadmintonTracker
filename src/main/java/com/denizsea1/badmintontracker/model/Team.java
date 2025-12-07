package com.denizsea1.badmintontracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repräsentiert ein Team (zwei Spieler) in einem Spiel.
 */
@Entity
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JsonIgnore
    private Spiel spiel;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Spieler> spieler = new ArrayList<>();

    // ggf. weitere Felder, z. B. Name des Teams

    public Team() {
    }

    /**
     * Fügt einem Team einen Spieler hinzu und setzt die
     * Beziehung Spieler -> Team.
     */
    public void addSpieler(Spieler s) {
        spieler.add(s);
        s.setTeam(this);
    }

    /**
     * Setzt Startpositionen für die Spieler dieses Teams:
     * - Wenn mindestens ein Spieler vorhanden ist:
     *     erster Spieler  -> RECHTS
     * - Wenn mindestens zwei Spieler vorhanden sind:
     *     zweiter Spieler -> LINKS
     *
     * Damit hat man für den Start eine definierte Zuordnung
     * in der 4-Quadranten-Ansicht (pro Team rechts/links).
     */
    public void initialisierePositionen() {
        if (!spieler.isEmpty()) {
            spieler.getFirst().setPositionImTeam(Seite.RECHTS);
        }
        if (spieler.size() >= 2) {
            spieler.get(1).setPositionImTeam(Seite.LINKS);
        }
    }

    // ---- Getter/Setter ----

    public Long getId() {
        return id;
    }

    public Spiel getSpiel() {
        return spiel;
    }

    public void setSpiel(Spiel spiel) {
        this.spiel = spiel;
    }

    public List<Spieler> getSpieler() {
        return spieler;
    }

    public void setSpieler(List<Spieler> spieler) {
        this.spieler = spieler;
    }
}
