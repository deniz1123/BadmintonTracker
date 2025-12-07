package com.denizsea1.badmintontracker.service;

import com.denizsea1.badmintontracker.model.*;
import com.denizsea1.badmintontracker.repository.SatzRepository;
import com.denizsea1.badmintontracker.repository.SpielRepository;
import com.denizsea1.badmintontracker.repository.TeamRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Kapselt die Spiellogik für eine Badminton-Doppel-Partie:
 *
 * - Start eines neuen Spiels mit zwei Teams
 * - Vergabe von Punkten (inkl. Aufschlag- und Positionslogik)
 * - Erkennen von Satzende und Matchende (Best-of-Three)
 * - Pausenempfehlung bei >=11 Punkten
 *
 * Controller sprechen nur diesen Service an – die fachliche Logik liegt hier.
 */
@Service
public class SpielService {

    private final SpielRepository spielRepository;
    private final TeamRepository teamRepository;
    private final SatzRepository satzRepository;

    public SpielService(SpielRepository spielRepository,
                        TeamRepository teamRepository,
                        SatzRepository satzRepository) {
        this.spielRepository = spielRepository;
        this.teamRepository = teamRepository;
        this.satzRepository = satzRepository;
    }

    // -------------------------------------------------------------------------
    // 1) Spielstart
    // -------------------------------------------------------------------------

    /**
     * Startet ein neues Spiel mit zwei bestehenden Teams.
     *
     * @param teamAId           ID von Team A
     * @param teamBId           ID von Team B
     * @param aufschlagTeamIstA true, falls Team A den ersten Aufschlag hat
     * @param startSeite        Start-Seite für den Aufschlag (LINKS/RECHTS),
     *                          wird in der UI vom Punktezähler ausgewählt.
     *
     * @return gespeichertes Spiel mit initialem Satz (Nummer 1, 0:0 Punkte)
     */
    @Transactional
    public Spiel startNeuesSpiel(Long teamAId,
                                 Long teamBId,
                                 boolean aufschlagTeamIstA,
                                 Seite startSeite) {

        // Teams aus der Datenbank laden
        Team teamA = teamRepository.findById(teamAId)
                .orElseThrow(() -> new NoSuchElementException("Team A nicht gefunden: " + teamAId));
        Team teamB = teamRepository.findById(teamBId)
                .orElseThrow(() -> new NoSuchElementException("Team B nicht gefunden: " + teamBId));

        // Neues Spiel mit aktuellem Datum
        Spiel spiel = new Spiel(LocalDate.now());
        spiel.addTeam(teamA);
        spiel.addTeam(teamB);

        // Aufschlagteam und Startseite setzen
        spiel.setAufschlagTeam(aufschlagTeamIstA ? teamA : teamB);
        spiel.setAufschlagSeite(startSeite);

        // Ersten Satz mit 0:0 anlegen
        Satz ersterSatz = new Satz(1);
        ersterSatz.setPunkteTeamA(0);
        ersterSatz.setPunkteTeamB(0);
        spiel.addSatz(ersterSatz);

        return spielRepository.save(spiel);
    }

    // -------------------------------------------------------------------------
    // 2) Punktevergabe nach außen
    // -------------------------------------------------------------------------

    /**
     * Vergibt einen Punkt an Team A (Teams[0]) im aktuellen Satz.
     */
    @Transactional
    public Spiel punktFuerTeamA(Long spielId) {
        return vergebePunkt(spielId, true);
    }

    /**
     * Vergibt einen Punkt an Team B (Teams[1]) im aktuellen Satz.
     */
    @Transactional
    public Spiel punktFuerTeamB(Long spielId) {
        return vergebePunkt(spielId, false);
    }

    /**
     * Gemeinsame interne Methode für Punktevergabe,
     * um Duplikate in punktFuerTeamA/B zu vermeiden.
     *
     * @param spielId    ID des Spiels
     * @param punktFuerA true -> Punkt für Team A, false -> Team B
     */
    private Spiel vergebePunkt(Long spielId, boolean punktFuerA) {
        Spiel spiel = ladeSpiel(spielId);

        // Nur laufende Spiele dürfen Punkte bekommen
        if (spiel.getStatus() != SpielStatus.LAUFEND) {
            throw new IllegalStateException("Spiel ist nicht mehr LAUFEND (Status: " + spiel.getStatus() + ")");
        }

        // Wir gehen davon aus: Teams[0] = A, Teams[1] = B
        Team teamA = spiel.getTeams().get(0);
        Team teamB = spiel.getTeams().get(1);

        Satz aktuellerSatz = ermittleAktuellenSatz(spiel);

        // 1) Punktestand erhöhen
        if (punktFuerA) {
            aktuellerSatz.setPunkteTeamA(aktuellerSatz.getPunkteTeamA() + 1);
        } else {
            aktuellerSatz.setPunkteTeamB(aktuellerSatz.getPunkteTeamB() + 1);
        }

        // 2) Aufschlag- und Positionslogik anwenden
        wendeAufschlagUndPositionsLogikAn(spiel, teamA, teamB, aktuellerSatz, punktFuerA);

        // 3) Prüfen, ob Satz oder Match beendet ist
        pruefeSatzUndSpielEnde(spiel);

        // 4) Alles speichern (Spiel -> Sätze -> Teams -> Spieler)
        return spielRepository.save(spiel);
    }

    // -------------------------------------------------------------------------
    // 3) Hilfsmethoden zum Laden / aktuellen Satz finden
    // -------------------------------------------------------------------------

    /**
     * Lädt ein Spiel oder wirft eine Exception, falls es nicht existiert.
     */
    private Spiel ladeSpiel(Long spielId) {
        return spielRepository.findById(spielId)
                .orElseThrow(() -> new NoSuchElementException("Spiel nicht gefunden: " + spielId));
    }

    /**
     * Ermittelt den aktuellen Satz:
     * nimmt den Satz mit der höchsten Nummer.
     */
    private Satz ermittleAktuellenSatz(Spiel spiel) {
        return spiel.getSaetze().stream()
                .max(Comparator.comparingInt(Satz::getNummer))
                .orElseThrow(() -> new IllegalStateException("Spiel hat noch keine Sätze"));
    }

    // -------------------------------------------------------------------------
    // 4) Aufschlag- und Positionslogik
    // -------------------------------------------------------------------------

    /**
     * Implementiert die Aufschlag- und Spielerpositionslogik:
     *
     * - Es gibt pro Team einen fixen Aufschläger (fachlich),
     *   technisch wird die Spieleranordnung im Team + positionImTeam genutzt.
     *
     * - Wenn das Aufschlagteam den Punkt macht:
     *   -> Aufschlagsrecht bleibt
     *   -> alle Spieler dieses Teams wechseln LINKS/RECHTS (positionImTeam.invert())
     *   -> die Aufschlagseite wird invertiert (RECHTS <-> LINKS)
     *
     * - Wenn das andere Team den Punkt macht:
     *   -> Aufschlagsrecht wechselt auf dieses Team
     *   -> Aufschlagseite-Empfehlung:
     *      gerade Punktzahl -> RECHTS, ungerade -> LINKS
     *      (UI darf diese Empfehlung manuell anpassen)
     */
    private void wendeAufschlagUndPositionsLogikAn(Spiel spiel,
                                                   Team teamA,
                                                   Team teamB,
                                                   Satz aktuellerSatz,
                                                   boolean punktFuerA) {

        Team punktTeam = punktFuerA ? teamA : teamB;
        Team anderesTeam = punktFuerA ? teamB : teamA;

        Team aktuellesAufschlagTeam = spiel.getAufschlagTeam();

        // Falls aus irgendeinem Grund noch kein Aufschlagteam gesetzt wurde:
        if (aktuellesAufschlagTeam == null) {
            spiel.setAufschlagTeam(punktTeam);
            aktuellesAufschlagTeam = punktTeam;
        }

        // Fall 1: Aufschlagteam macht den Punkt
        if (punktTeam.equals(aktuellesAufschlagTeam)) {
            // Aufschlagsrecht bleibt beim gleichen Team

            // Spieler dieses Teams wechseln ihre Position im Team (LINKS <-> RECHTS)
            wechslePositionenImTeam(punktTeam);

            // Aufschlagseite invertieren (RECHTS -> LINKS / LINKS -> RECHTS)
            Seite aktuelleSeite = spiel.getAufschlagSeite();
            if (aktuelleSeite != null) {
                spiel.setAufschlagSeite(aktuelleSeite.invert());
            }
        } else {
            // Fall 2: Nicht-aufschlagendes Team macht den Punkt
            // -> Aufschlagsrecht wechselt
            spiel.setAufschlagTeam(punktTeam);

            // Empfohlene Aufschlagseite anhand der Punktzahl des Punktteams
            int punktePunktTeam = punktFuerA
                    ? aktuellerSatz.getPunkteTeamA()
                    : aktuellerSatz.getPunkteTeamB();

            boolean gerade = (punktePunktTeam % 2 == 0);
            spiel.setAufschlagSeite(gerade ? Seite.RECHTS : Seite.LINKS);

            // UI kann diese Seite bei Bedarf überschreiben (manuelle Anpassung).
        }
    }

    /**
     * Lässt alle Spieler eines Teams ihre Seite tauschen:
     * LINKS -> RECHTS, RECHTS -> LINKS.
     *
     * Entspricht der Beschreibung:
     * "Jedes Mal wenn es einen Punkt gibt, wechseln die zwei Spieler im Team
     *  auf ihrer Seite ihre Position."
     */
    private void wechslePositionenImTeam(Team team) {
        List<Spieler> spielerListe = team.getSpieler();
        for (Spieler s : spielerListe) {
            if (s.getPositionImTeam() != null) {
                s.setPositionImTeam(s.getPositionImTeam().invert());
            }
        }
    }

    // -------------------------------------------------------------------------
    // 5) Satzende & Spielende (Best-of-Three)
    // -------------------------------------------------------------------------

    /**
     * Prüft nach jedem Punkt, ob der aktuelle Satz beendet ist,
     * und ob dadurch das gesamte Match beendet wird.
     *
     * - Satzende:
     *   mind. 21 Punkte UND mind. 2 Punkte Vorsprung
     *   ODER 30 Punkte (maximale Punktzahl)
     *
     * - Matchende:
     *   Ein Team hat 2 Sätze gewonnen (Best-of-Three).
     *   In diesem Fall wird gewinnerTeam gesetzt und status = BEENDET.
     *
     * - Wenn noch kein Matchgewinner feststeht:
     *   und weniger als 3 Sätze existieren, wird ein neuer Satz angelegt.
     */
    private void pruefeSatzUndSpielEnde(Spiel spiel) {
        Satz aktuellerSatz = ermittleAktuellenSatz(spiel);

        if (!istSatzBeendet(aktuellerSatz)) {
            return; // Satz läuft weiter, nichts zu tun
        }

        // Zähle gewonnene Sätze pro Team
        Team teamA = spiel.getTeams().get(0);
        Team teamB = spiel.getTeams().get(1);

        int gewonneneSaetzeA = 0;
        int gewonneneSaetzeB = 0;

        for (Satz s : spiel.getSaetze()) {
            if (!istSatzBeendet(s)) {
                continue; // nur abgeschlossene Sätze berücksichtigen
            }
            if (s.getPunkteTeamA() > s.getPunkteTeamB()) {
                gewonneneSaetzeA++;
            } else if (s.getPunkteTeamB() > s.getPunkteTeamA()) {
                gewonneneSaetzeB++;
            }
        }

        // Matchende prüfen (Best-of-Three: 2 Gewinnsätze)
        if (gewonneneSaetzeA >= 2) {
            spiel.setGewinnerTeam(teamA); // setzt intern auch status = BEENDET
            return;
        }

        if (gewonneneSaetzeB >= 2) {
            spiel.setGewinnerTeam(teamB);
            return;
        }

        // Noch kein Matchgewinner -> ggf. neuen Satz anlegen (max. 3 Sätze)
        int anzahlSaetze = spiel.getSaetze().size();
        if (anzahlSaetze < 3) {
            Satz neuerSatz = new Satz(anzahlSaetze + 1);
            neuerSatz.setPunkteTeamA(0);
            neuerSatz.setPunkteTeamB(0);
            spiel.addSatz(neuerSatz);
        }
    }

    /**
     * Badminton-Satzregeln:
     *
     * Ein Satz ist beendet, wenn:
     * - ein Team mindestens 21 Punkte hat UND
     *   mindestens 2 Punkte Vorsprung
     * ODER
     * - ein Team 30 Punkte erreicht (maximale Punktzahl)
     */
    private boolean istSatzBeendet(Satz satz) {
        int a = satz.getPunkteTeamA();
        int b = satz.getPunkteTeamB();

        if (a >= 21 || b >= 21) {
            int diff = Math.abs(a - b);
            if (diff >= 2) {
                return true;
            }
        }

        if (a >= 30 || b >= 30) {
            return true;
        }

        return false;
    }

    // -------------------------------------------------------------------------
    // 6) Pausenempfehlung
    // -------------------------------------------------------------------------

    /**
     * Liefert true, sobald im aktuellen Satz eines der Teams
     * mindestens 11 Punkte hat.
     *
     * Die UI kann diese Info nutzen, um eine 2-Minuten-Pause
     * als Pop-up anzubieten.
     */
    public boolean istPauseEmpfohlen(Long spielId) {
        Spiel spiel = ladeSpiel(spielId);
        Satz aktuellerSatz = ermittleAktuellenSatz(spiel);
        int maxPunkte = Math.max(aktuellerSatz.getPunkteTeamA(), aktuellerSatz.getPunkteTeamB());
        return maxPunkte >= 11;
    }
}
