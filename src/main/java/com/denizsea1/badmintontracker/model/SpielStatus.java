package com.denizsea1.badmintontracker.model;

/**
 * Status eines Spiels:
 * - LAUFEND    = Spiel ist aktiv, es können Punkte vergeben werden.
 * - BEENDET    = Spiel ist regulär beendet (ein Team hat 2 Sätze gewonnen).
 * - ABGEBROCHEN = Spiel wurde vorzeitig beendet (Abbrecher verliert).
 */
public enum SpielStatus {
    LAUFEND,
    BEENDET,
    ABGEBROCHEN
}
