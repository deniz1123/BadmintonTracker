package com.denizsea1.badmintontracker.model;

/**
 * Repräsentiert eine Seite des Spielfelds aus Sicht eines Teams:
 * LINKS  = linker Quadrant / linkes Aufschlagfeld
 * RECHTS = rechter Quadrant / rechtes Aufschlagfeld.
 *
 * Wird verwendet für:
 * - Position eines Spielers im Team
 * - aktuelle Aufschlagseite im Spiel
 */
public enum Seite {
    LINKS,
    RECHTS;

    /**
     * Liefert die gegenüberliegende Seite:
     * LINKS  -> RECHTS
     * RECHTS -> LINKS
     */
    public Seite invert() {
        return this == LINKS ? RECHTS : LINKS;
    }
}
