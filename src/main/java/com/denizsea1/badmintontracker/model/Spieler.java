package com.denizsea1.badmintontracker.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Repräsentiert eine:n Spieler:in in einem konkreten Team.
 */
@Entity
public class Spieler {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String vorname;
    private String nachname;

    /**
     * Position dieses Spielers innerhalb seines Teams (LINKS/RECHTS).
     * Wird von der Spiellogik verwendet, um bei einem Punkt des
     * Aufschlagteams die Seiten wechseln zu können.
     */
    @Enumerated(EnumType.STRING)
    private Seite positionImTeam;

    @ManyToOne
    @JsonIgnore
    private Team team;

    public Spieler() {
    }

    public Spieler(String vorname, String nachname, Seite positionImTeam) {
        this.vorname = vorname;
        this.nachname = nachname;
        this.positionImTeam = positionImTeam;
    }

    // ---- Getter/Setter ----

    public Long getId() {
        return id;
    }

    public String getVorname() {
        return vorname;
    }

    public void setVorname(String vorname) {
        this.vorname = vorname;
    }

    public String getNachname() {
        return nachname;
    }

    public void setNachname(String nachname) {
        this.nachname = nachname;
    }

    public Seite getPositionImTeam() {
        return positionImTeam;
    }

    public void setPositionImTeam(Seite positionImTeam) {
        this.positionImTeam = positionImTeam;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }
}
