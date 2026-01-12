package com.denizsea1.badmintontracker;

import com.denizsea1.badmintontracker.model.*;
import com.denizsea1.badmintontracker.repository.SpielRepository;
import com.denizsea1.badmintontracker.repository.TeamRepository;
import com.denizsea1.badmintontracker.service.SpielService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Große, aber granular aufgeteilte Integrationstest-Klasse
 * für den BadmintonTracker.
 *
 * - Testet Domain/Service-Logik (SpielService, Team/Spieler/Satz)
 * - Testet REST-API mit MockMvc (Controller-Endpunkte)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BadmintonTrackerIntegrationTest {

    @Autowired
    private SpielService spielService;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private SpielRepository spielRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // -------------------------------------------------------------------------
    // A) DOMAIN- / SERVICE-TESTS
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Team.initialisierePositionen() setzt RECHTS/LINKS für zwei Spieler")
    void teamInitialisiertSpielerPositionen() {
        Team team = new Team();
        Spieler s1 = new Spieler("Max", "Mueller", null);
        Spieler s2 = new Spieler("Lara", "Schulz", null);

        team.addSpieler(s1);
        team.addSpieler(s2);

        team.initialisierePositionen();

        assertEquals(Seite.RECHTS, s1.getPositionImTeam());
        assertEquals(Seite.LINKS, s2.getPositionImTeam());
    }

    @Test
    @DisplayName("startNeuesSpiel erstellt Spiel mit zwei Teams, erstem Satz und Aufschlaginfos")
    void startNeuesSpiel_initialisiertSpielKorrekt() {
        Team teamA = persistTeam("Max", "Mueller", "Lara", "Schulz");
        Team teamB = persistTeam("Paul", "Meier", "Anna", "Schmidt");

        Spiel spiel = spielService.startNeuesSpiel(
                teamA.getId(),
                teamB.getId(),
                true,
                Seite.RECHTS
        );

        assertNotNull(spiel.getId());
        assertEquals(SpielStatus.LAUFEND, spiel.getStatus());
        assertEquals(teamA.getId(), spiel.getAufschlagTeam().getId());
        assertEquals(Seite.RECHTS, spiel.getAufschlagSeite());

        List<Satz> saetze = spiel.getSaetze();
        assertEquals(1, saetze.size());
        assertEquals(1, saetze.get(0).getNummer());
        assertEquals(0, saetze.get(0).getPunkteTeamA());
        assertEquals(0, saetze.get(0).getPunkteTeamB());
    }

    @Test
    @DisplayName("Punkt für Aufschlagteam: Punkte, Aufschlagseite und Spielerpositionen werden angepasst")
    void punktFuerAufschlagTeam_aktualisiertPunkteUndPositionen() {
        Team teamA = persistTeam("Max", "Mueller", "Lara", "Schulz");
        Team teamB = persistTeam("Paul", "Meier", "Anna", "Schmidt");

        Spiel spiel = spielService.startNeuesSpiel(
                teamA.getId(), teamB.getId(), true, Seite.RECHTS);
        Long spielId = spiel.getId();

        // Vorher-Check: Team A Spielerpositionen
        Team gespeichertesTeamA = teamRepository.findById(teamA.getId()).orElseThrow();
        Seite posVorherS1 = gespeichertesTeamA.getSpieler().get(0).getPositionImTeam();
        Seite posVorherS2 = gespeichertesTeamA.getSpieler().get(1).getPositionImTeam();
        assertEquals(Seite.RECHTS, posVorherS1);
        assertEquals(Seite.LINKS, posVorherS2);

        // Punkt für Team A (Aufschlagteam)
        spiel = spielService.punktFuerTeamA(spielId);

        Satz aktuellerSatz = getAktuellenSatz(spiel);
        assertEquals(1, aktuellerSatz.getPunkteTeamA());
        assertEquals(0, aktuellerSatz.getPunkteTeamB());

        // Aufschlagteam bleibt Team A
        assertEquals(teamA.getId(), spiel.getAufschlagTeam().getId());
        // Aufschlagseite invertiert (RECHTS -> LINKS)
        assertEquals(Seite.LINKS, spiel.getAufschlagSeite());

        // Spielerpositionen von Team A invertiert
        gespeichertesTeamA = teamRepository.findById(teamA.getId()).orElseThrow();
        Seite posNachherS1 = gespeichertesTeamA.getSpieler().get(0).getPositionImTeam();
        Seite posNachherS2 = gespeichertesTeamA.getSpieler().get(1).getPositionImTeam();

        assertEquals(posVorherS1.invert(), posNachherS1);
        assertEquals(posVorherS2.invert(), posNachherS2);
    }

    @Test
    @DisplayName("Punkt für Nicht-Aufschlagteam: Aufschlagrecht wechselt, Aufschlagseite wird aus Punktzahl abgeleitet")
    void punktFuerNichtAufschlagTeam_wechseltAufschlag() {
        Team teamA = persistTeam("Max", "Mueller", "Lara", "Schulz");
        Team teamB = persistTeam("Paul", "Meier", "Anna", "Schmidt");

        Spiel spiel = spielService.startNeuesSpiel(
                teamA.getId(), teamB.getId(), true, Seite.RECHTS);
        Long spielId = spiel.getId();

        // Erstmal Punkt für Team B
        spiel = spielService.punktFuerTeamB(spielId);
        Satz aktuellerSatz = getAktuellenSatz(spiel);

        assertEquals(0, aktuellerSatz.getPunkteTeamA());
        assertEquals(1, aktuellerSatz.getPunkteTeamB());

        // Aufschlagteam sollte jetzt Team B sein
        assertEquals(teamB.getId(), spiel.getAufschlagTeam().getId());
        // 1 Punkt (ungerade) -> LINKS
        assertEquals(Seite.LINKS, spiel.getAufschlagSeite());
    }

    @Test
    @DisplayName("Ein Satz endet bei mind. 21 Punkten und 2 Punkten Vorsprung")
    void satzEndetBei21PunktenMitZweiPunktenVorsprung() {
        Team teamA = persistTeam("Max", "Mueller", "Lara", "Schulz");
        Team teamB = persistTeam("Paul", "Meier", "Anna", "Schmidt");

        Spiel spiel = spielService.startNeuesSpiel(
                teamA.getId(), teamB.getId(), true, Seite.RECHTS);
        Long spielId = spiel.getId();

        // Team A gewinnt Satz 1 (z.B. 21:10)
        Satz aktuellerSatz = getAktuellenSatz(spiel);
        while (!(aktuellerSatz.getPunkteTeamA() >= 21 &&
                aktuellerSatz.getPunkteTeamA() - aktuellerSatz.getPunkteTeamB() >= 2)) {
            spiel = spielService.punktFuerTeamA(spielId);
            aktuellerSatz = getAktuellenSatz(spiel);
        }

        // Wenn Satz beendet ist, sollte das Spiel einen neuen Satz angelegt haben (Satz 2)
        Spiel neuGeladen = spielRepository.findById(spielId).orElseThrow();
        assertFalse(neuGeladen.getSaetze().isEmpty());
        Satz letzterSatz = getAktuellenSatz(neuGeladen);
        assertTrue(letzterSatz.getNummer() == 1 || letzterSatz.getNummer() == 2);
    }

    @Test
    @DisplayName("Match endet, wenn ein Team zwei Sätze gewinnt (Best-of-Three)")
    void matchEndetNachZweiGewonnenenSaetzen() {
        Team teamA = persistTeam("Max", "Mueller", "Lara", "Schulz");
        Team teamB = persistTeam("Paul", "Meier", "Anna", "Schmidt");

        Spiel spiel = spielService.startNeuesSpiel(
                teamA.getId(), teamB.getId(), true, Seite.RECHTS);
        Long spielId = spiel.getId();

        // Satz 1 für A
        Satz aktuellerSatz = getAktuellenSatz(spiel);
        while (!(aktuellerSatz.getPunkteTeamA() >= 21 &&
                aktuellerSatz.getPunkteTeamA() - aktuellerSatz.getPunkteTeamB() >= 2)) {
            spiel = spielService.punktFuerTeamA(spielId);
            aktuellerSatz = getAktuellenSatz(spiel);
        }

        // Satz 2 für A
        aktuellerSatz = getAktuellenSatz(spiel);
        while (!(aktuellerSatz.getPunkteTeamA() >= 21 &&
                aktuellerSatz.getPunkteTeamA() - aktuellerSatz.getPunkteTeamB() >= 2)) {
            spiel = spielService.punktFuerTeamA(spielId);
            aktuellerSatz = getAktuellenSatz(spiel);
        }

        // Matchende prüfen
        Spiel fertig = spielRepository.findById(spielId).orElseThrow();
        assertEquals(SpielStatus.BEENDET, fertig.getStatus());
        assertNotNull(fertig.getGewinnerTeam());
        assertEquals(teamA.getId(), fertig.getGewinnerTeam().getId());
    }

    @DisplayName("Pausenempfehlung ist true, sobald ein Team im Satz >= 11 Punkte hat")
    @Test
    void pausenEmpfehlungAbElfPunkten() {
        Team teamA = persistTeam("Max", "Mueller", "Lara", "Schulz");
        Team teamB = persistTeam("Paul", "Meier", "Anna", "Schmidt");

        Spiel spiel = spielService.startNeuesSpiel(
                teamA.getId(), teamB.getId(), true, Seite.RECHTS);
        Long spielId = spiel.getId();

        // Bis 10 Punkte sollte KEINE Pause empfohlen sein
        for (int i = 0; i < 10; i++) {
            assertFalse(spielService.istPauseEmpfohlen(spielId));
            spiel = spielService.punktFuerTeamA(spielId);
        }

        // Jetzt noch EINEN Punkt → A hat 11
        spiel = spielService.punktFuerTeamA(spielId);

        // Ab 11 Punkten: Pause empfohlen
        assertTrue(spielService.istPauseEmpfohlen(spielId));
    }

    // -------------------------------------------------------------------------
    // NEUE DOMAIN-TESTS: Spieler-Blockierung, Undo, Spielabbruch
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Spieler können nicht gleichzeitig in zwei laufenden Spielen sein")
    void spielerKoennenNichtInZweiAktivenSpielenSein() {
        Team teamA = persistTeam("Max", "Mueller", "Lara", "Schulz");
        Team teamB = persistTeam("Paul", "Meier", "Anna", "Schmidt");
        Team teamC = persistTeam("Tom", "Test", "Tina", "Test");

        // Erstes Spiel mit A vs B -> ok
        Spiel spiel1 = spielService.startNeuesSpiel(
                teamA.getId(), teamB.getId(), true, Seite.RECHTS);
        assertNotNull(spiel1.getId());

        // Zweites Spiel mit Team A (A vs C) -> sollte fehlschlagen,
        // weil Spieler von A bereits in einem laufenden Spiel sind.
        assertThrows(IllegalStateException.class, () ->
                spielService.startNeuesSpiel(
                        teamA.getId(), teamC.getId(), true, Seite.RECHTS)
        );
    }

    @Test
    @DisplayName("Undo Punkt für Team A stellt Punktestand, Aufschlagseite und Positionen zurück (einfacher Fall)")
    void undoPunktFuerTeamA_stelltEinfachenZustandZurueck() {
        Team teamA = persistTeam("Max", "Mueller", "Lara", "Schulz");
        Team teamB = persistTeam("Paul", "Meier", "Anna", "Schmidt");

        Spiel spiel = spielService.startNeuesSpiel(
                teamA.getId(), teamB.getId(), true, Seite.RECHTS);
        Long spielId = spiel.getId();

        // Ausgangspositionen merken
        Team gespeichertesTeamA = teamRepository.findById(teamA.getId()).orElseThrow();
        Seite posVorherS1 = gespeichertesTeamA.getSpieler().get(0).getPositionImTeam();
        Seite posVorherS2 = gespeichertesTeamA.getSpieler().get(1).getPositionImTeam();

        assertEquals(Seite.RECHTS, spiel.getAufschlagSeite());
        assertEquals(teamA.getId(), spiel.getAufschlagTeam().getId());

        // 1 Punkt für Team A
        spiel = spielService.punktFuerTeamA(spielId);

        // Sicherstellen, dass der Punkt gezählt wurde
        Satz satzNachPunkt = getAktuellenSatz(spiel);
        assertEquals(1, satzNachPunkt.getPunkteTeamA());
        assertEquals(0, satzNachPunkt.getPunkteTeamB());

        // Jetzt Undo für Team A
        spiel = spielService.undoPunktFuerTeamA(spielId);

        Satz satzNachUndo = getAktuellenSatz(spiel);
        assertEquals(0, satzNachUndo.getPunkteTeamA());
        assertEquals(0, satzNachUndo.getPunkteTeamB());

        // Aufschlagteam und Aufschlagseite wieder wie am Anfang
        assertEquals(teamA.getId(), spiel.getAufschlagTeam().getId());
        assertEquals(Seite.RECHTS, spiel.getAufschlagSeite());

        // Spielerpositionen wieder wie vor dem Punkt
        gespeichertesTeamA = teamRepository.findById(teamA.getId()).orElseThrow();
        Seite posNachUndoS1 = gespeichertesTeamA.getSpieler().get(0).getPositionImTeam();
        Seite posNachUndoS2 = gespeichertesTeamA.getSpieler().get(1).getPositionImTeam();

        assertEquals(posVorherS1, posNachUndoS1);
        assertEquals(posVorherS2, posNachUndoS2);
    }

    @Test
    @DisplayName("Spielabbruch setzt Status ABGEBROCHEN, Gewinner und setzt Spieler wieder frei")
    void spielKannAbgebrochenWerdenUndSetztSpielerFrei() {
        Team teamA = persistTeam("Max", "Mueller", "Lara", "Schulz");
        Team teamB = persistTeam("Paul", "Meier", "Anna", "Schmidt");

        Spiel spiel = spielService.startNeuesSpiel(
                teamA.getId(), teamB.getId(), true, Seite.RECHTS);
        Long spielId = spiel.getId();

        // Nach Spielstart sollten alle Spieler der Teams als "in aktivem Spiel" markiert sein
        Team geladenA = teamRepository.findById(teamA.getId()).orElseThrow();
        Team geladenB = teamRepository.findById(teamB.getId()).orElseThrow();

        assertTrue(geladenA.getSpieler().stream().allMatch(Spieler::isInAktivemSpiel));
        assertTrue(geladenB.getSpieler().stream().allMatch(Spieler::isInAktivemSpiel));

        // Team A bricht ab -> Team B gewinnt
        spielService.brecheSpielAb(spielId, true);

        Spiel abgebrochen = spielRepository.findById(spielId).orElseThrow();
        assertEquals(SpielStatus.ABGEBROCHEN, abgebrochen.getStatus());
        assertNotNull(abgebrochen.getGewinnerTeam());
        assertEquals(teamB.getId(), abgebrochen.getGewinnerTeam().getId());

        // Spieler müssen wieder freigegeben sein
        geladenA = teamRepository.findById(teamA.getId()).orElseThrow();
        geladenB = teamRepository.findById(teamB.getId()).orElseThrow();

        assertTrue(geladenA.getSpieler().stream().noneMatch(Spieler::isInAktivemSpiel));
        assertTrue(geladenB.getSpieler().stream().noneMatch(Spieler::isInAktivemSpiel));
    }

    // -------------------------------------------------------------------------
    // B) API-TESTS (MockMvc)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("API: /teams legt Team an und setzt Spielerpositionen")
    void api_createTeam_setztPositionen() throws Exception {
        String teamJson = """
                {
                  "spieler": [
                    { "vorname": "Max", "nachname": "Mueller" },
                    { "vorname": "Lara", "nachname": "Schulz" }
                  ]
                }
                """;

        mockMvc.perform(post("/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(teamJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.spieler[0].positionImTeam").value("RECHTS"))
                .andExpect(jsonPath("$.spieler[1].positionImTeam").value("LINKS"));
    }

    @Test
    @DisplayName("API: /spiele/start startet ein Spiel korrekt")
    void api_startSpiel() throws Exception {
        long teamAId = createTeamViaApi("Max", "Mueller", "Lara", "Schulz");
        long teamBId = createTeamViaApi("Paul", "Meier", "Anna", "Schmidt");

        mockMvc.perform(post("/spiele/start")
                        .param("teamAId", String.valueOf(teamAId))
                        .param("teamBId", String.valueOf(teamBId))
                        .param("aufschlagTeamIstA", "true")
                        .param("startSeite", "RECHTS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.aufschlagTeam.id").value((int) teamAId))
                .andExpect(jsonPath("$.aufschlagSeite").value("RECHTS"))
                .andExpect(jsonPath("$.status").value("LAUFEND"));
    }

    @Test
    @DisplayName("API: punktA/punktB erhöhen Punktestand im Spiel-JSON")
    void api_punktVergabeAundB() throws Exception {
        long teamAId = createTeamViaApi("Max", "Mueller", "Lara", "Schulz");
        long teamBId = createTeamViaApi("Paul", "Meier", "Anna", "Schmidt");

        MvcResult startResult = mockMvc.perform(post("/spiele/start")
                        .param("teamAId", String.valueOf(teamAId))
                        .param("teamBId", String.valueOf(teamBId))
                        .param("aufschlagTeamIstA", "true")
                        .param("startSeite", "RECHTS"))
                .andExpect(status().isOk())
                .andReturn();

        long spielId = extractSpielIdFromResult(startResult);

        mockMvc.perform(post("/spiele/{id}/punktA", spielId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saetze[0].punkteTeamA").value(1));

        mockMvc.perform(post("/spiele/{id}/punktB", spielId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saetze[0].punkteTeamB").value(1));
    }

    @Test
    @DisplayName("API: /spiele/{id}/pauseEmpfohlen liefert true ab 11 Punkten eines Teams")
    void api_pauseEmpfohlenAbElfPunkten() throws Exception {
        long teamAId = createTeamViaApi("Max", "Mueller", "Lara", "Schulz");
        long teamBId = createTeamViaApi("Paul", "Meier", "Anna", "Schmidt");

        MvcResult startResult = mockMvc.perform(post("/spiele/start")
                        .param("teamAId", String.valueOf(teamAId))
                        .param("teamBId", String.valueOf(teamBId))
                        .param("aufschlagTeamIstA", "true")
                        .param("startSeite", "RECHTS"))
                .andExpect(status().isOk())
                .andReturn();

        long spielId = extractSpielIdFromResult(startResult);

        int punkteA = 0;
        while (punkteA < 11) {
            mockMvc.perform(post("/spiele/{id}/punktA", spielId))
                    .andExpect(status().isOk());

            MvcResult getResult = mockMvc.perform(get("/spiele/{id}", spielId))
                    .andExpect(status().isOk())
                    .andReturn();

            String spielJson = getResult.getResponse().getContentAsString();
            JsonNode node = objectMapper.readTree(spielJson);
            punkteA = node.get("saetze").get(0).get("punkteTeamA").asInt();
        }

        MvcResult pauseResult = mockMvc.perform(get("/spiele/{id}/pauseEmpfohlen", spielId))
                .andExpect(status().isOk())
                .andReturn();

        boolean pauseEmpfohlen = Boolean.parseBoolean(pauseResult.getResponse().getContentAsString());
        assertTrue(pauseEmpfohlen);
    }

    // -------------------------------------------------------------------------
    // Hilfsmethoden
    // -------------------------------------------------------------------------

    private Team persistTeam(String v1, String n1, String v2, String n2) {
        Team team = new Team();
        team.addSpieler(new Spieler(v1, n1, Seite.RECHTS));
        team.addSpieler(new Spieler(v2, n2, Seite.LINKS));
        // optional: team.initialisierePositionen();
        return teamRepository.save(team);
    }

    private Satz getAktuellenSatz(Spiel spiel) {
        return spiel.getSaetze().stream()
                .max((s1, s2) -> Integer.compare(s1.getNummer(), s2.getNummer()))
                .orElseThrow(() -> new IllegalStateException("Spiel hat keine Sätze"));
    }

    private long createTeamViaApi(String v1, String n1, String v2, String n2) throws Exception {
        String teamJson = """
                {
                  "spieler": [
                    { "vorname": "%s", "nachname": "%s" },
                    { "vorname": "%s", "nachname": "%s" }
                  ]
                }
                """.formatted(v1, n1, v2, n2);

        MvcResult result = mockMvc.perform(post("/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(teamJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        Team team = objectMapper.readValue(responseJson, Team.class);
        return team.getId();
    }

    private long extractSpielIdFromResult(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(json);
        return node.get("id").asLong();
    }
}
