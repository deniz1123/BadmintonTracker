package com.denizsea1.badmintontracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

/**
 * Repräsentiert einen einzelnen Satz innerhalb eines Spiels.
 * Ein Spiel im Doppel besteht aus bis zu drei Sätzen.
 */
@Entity
public class Satz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Satznummer innerhalb des Spiels:
     * 1 = erster Satz, 2 = zweiter, 3 = dritter Entscheidungssatz.
     */
    private int nummer;

    /**
     * Punkte von Team A (Teams[0] im Spiel) in diesem Satz.
     */
    private int punkteTeamA;

    /**
     * Punkte von Team B (Teams[1] im Spiel) in diesem Satz.
     */
    private int punkteTeamB;

    @ManyToOne
    @JsonIgnore
    private Spiel spiel;

    public Satz() {
    }

    public Satz(int nummer) {
        this.nummer = nummer;
    }

    // ---- Getter/Setter ----

    public Long getId() {
        return id;
    }

    public int getNummer() {
        return nummer;
    }

    public void setNummer(int nummer) {
        this.nummer = nummer;
    }

    public int getPunkteTeamA() {
        return punkteTeamA;
    }

    public void setPunkteTeamA(int punkteTeamA) {
        this.punkteTeamA = punkteTeamA;
    }

    public int getPunkteTeamB() {
        return punkteTeamB;
    }

    public void setPunkteTeamB(int punkteTeamB) {
        this.punkteTeamB = punkteTeamB;
    }

    public Spiel getSpiel() {
        return spiel;
    }

    public void setSpiel(Spiel spiel) {
        this.spiel = spiel;
    }
}
