package com.denizsea1.badmintontracker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Liefert die HTML-Seiten (Thymeleaf-Templates):
 * - "/"       -> index.html (Startseite)
 * - "/match"  -> match.html (laufendes Spiel anzeigen)
 * - "/history"-> history.html (übersicht gespeicherter Partien)
 *
 * Die eigentlichen Daten holt sich das Frontend über die REST-Controller
 * (SpielController, TeamController, SpielerController).
 */
@Controller
public class PageController {

    @GetMapping("/")
    public String index() {
        return "index"; // src/main/resources/templates/index.html
    }

    /**
     * Seite zur Anzeige / Steuerung eines einzelnen Spiels.
     * Die Spiel-ID wird in der URL übergeben, z.B. /match/1.
     * Wir geben sie zusätzlich als Model-Attribut mit, falls du sie später
     * in Thymeleaf direkt nutzen willst.
     */
    @GetMapping("/match/{spielId}")
    public String match(@PathVariable Long spielId, Model model) {
        model.addAttribute("spielId", spielId);
        return "match"; // src/main/resources/templates/match.html
    }

    /**
     * Übersicht über gespeicherte Partien.
     * Das Template lädt die Daten über GET /spiele.
     */
    @GetMapping("/history")
    public String history() {
        return "history"; // src/main/resources/templates/history.html
    }
}
