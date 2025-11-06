package com.example.backendjava.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backendjava.auth.JwtUtil;

/**
 * Controller REST per le funzionalità amministrative.
 * Gestisce operazioni riservate agli amministratori come blocco utenti e gestione prodotti.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final JdbcTemplate jdbc;

    @Value("${jwt.secret:dev-secret-please-change}")
    private String jwtSecret;

    @Value("${uploads.dir:}")
    private String uploadsDir;

    public AdminController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private boolean isAuthenticated(String authorization) {
        return JwtUtil.parseToken(authorization, jwtSecret).isPresent();
    }

    @PatchMapping("/users/{id}/block")
    public ResponseEntity<?> toggleBlock(@RequestHeader(value = "Authorization", required = false) String authorization,
                                        @PathVariable("id") long userId) {
        if (!isAuthenticated(authorization)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        SqlRowSet rs = jdbc.queryForRowSet("SELECT is_blocked FROM utenti WHERE id = ?", userId);
        if (!rs.next()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Utente non trovato");
        boolean newStatus = !rs.getBoolean("is_blocked");
        jdbc.update("UPDATE utenti SET is_blocked = ? WHERE id = ?", newStatus, userId);
        return ResponseEntity.ok(Map.of("success", true, "is_blocked", newStatus));
    }

    @PatchMapping("/users/{id}/admin")
    public ResponseEntity<?> setRole(@RequestHeader(value = "Authorization", required = false) String authorization,
                            @PathVariable("id") long userId,
                            @RequestBody Map<String, Object> body) {
        if (!isAuthenticated(authorization)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String ruolo = Objects.toString(body.getOrDefault("ruolo", body.getOrDefault("makeAdmin", true).equals(Boolean.TRUE) ? "admin" : "user"));
        jdbc.update("UPDATE utenti SET ruolo = ? WHERE id = ?", ruolo, userId);
        return ResponseEntity.ok(Map.of("success", true, "ruolo", ruolo));
    }

    @GetMapping("/statistiche-utenti")
    public ResponseEntity<?> statistiche(@RequestHeader(value = "Authorization", required = false) String authorization) {
        var claims = JwtUtil.parseToken(authorization, jwtSecret);
        if (claims.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        long currentUserId = ((Number) claims.get().get("id")).longValue();
        String sql = "SELECT u.id, u.nome, u.cognome, u.email, u.is_blocked, u.ruolo, COUNT(o.id) AS numero_ordini, COALESCE(SUM(o.totale), 0) AS totale_speso, MAX(o.data_ordine) AS ultimo_ordine FROM utenti u LEFT JOIN ordini o ON u.id = o.user_id WHERE u.id != ? GROUP BY u.id, u.nome, u.cognome, u.email, u.is_blocked, u.ruolo ORDER BY numero_ordini DESC, totale_speso DESC";
        List<Map<String, Object>> list = new ArrayList<>();
        SqlRowSet rs = jdbc.queryForRowSet(sql, currentUserId);
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", rs.getLong("id"));
            row.put("nome", rs.getString("nome"));
            row.put("cognome", rs.getString("cognome"));
            row.put("email", rs.getString("email"));
            row.put("is_blocked", rs.getBoolean("is_blocked"));
            row.put("ruolo", rs.getString("ruolo"));
            row.put("numero_ordini", rs.getLong("numero_ordini"));
            row.put("totale_speso", rs.getBigDecimal("totale_speso"));
            row.put("ultimo_ordine", rs.getTimestamp("ultimo_ordine"));
            list.add(row);
        }
        return ResponseEntity.ok(list);
    }

    @GetMapping("/users/{userId}/ordini")
    public ResponseEntity<?> ordiniUtente(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable("userId") long userId) {
        if (!isAuthenticated(authorization)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String sql = "SELECT o.id, o.totale, o.stato, o.data_ordine, o.metodo_pagamento, COALESCE(SUM(op.quantita), 0) AS numero_prodotti FROM ordini o LEFT JOIN ordine_prodotti op ON o.id = op.ordine_id WHERE o.user_id = ? GROUP BY o.id, o.totale, o.stato, o.data_ordine, o.metodo_pagamento ORDER BY o.data_ordine DESC";
        List<Map<String, Object>> list = new ArrayList<>();
        SqlRowSet rs = jdbc.queryForRowSet(sql, userId);
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", rs.getLong("id"));
            row.put("totale", rs.getBigDecimal("totale"));
            row.put("stato", rs.getString("stato"));
            row.put("data_ordine", rs.getTimestamp("data_ordine"));
            row.put("metodo_pagamento", rs.getString("metodo_pagamento"));
            row.put("numero_prodotti", rs.getLong("numero_prodotti"));
            list.add(row);
        }
        return ResponseEntity.ok(list);
    }

    @DeleteMapping("/prodotti/{id}")
    public ResponseEntity<?> deleteOrBlockProduct(@RequestHeader(value = "Authorization", required = false) String authorization,
        @PathVariable("id") Long prodottoId) {
        if (!isAuthenticated(authorization)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        SqlRowSet check = jdbc.queryForRowSet("SELECT * FROM prodotto WHERE id_prodotto = ?", prodottoId);
        if (!check.next()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Prodotto non trovato"));
        String immagine = check.getString("immagine");
        SqlRowSet ordersCheck = jdbc.queryForRowSet("SELECT COUNT(*) AS count FROM ordine_prodotti WHERE prodotto_id = ?", prodottoId);
        long count = ordersCheck.next() ? ordersCheck.getLong("count") : 0;
        if (count > 0) {
            jdbc.update("UPDATE prodotto SET bloccato = true WHERE id_prodotto = ?", prodottoId);
            return ResponseEntity.ok(Map.of("message", "Prodotto marcato come bloccato (aveva ordini associati)", "action", "blocked"));
        } else {
            SqlRowSet deleted = jdbc.queryForRowSet("DELETE FROM prodotto WHERE id_prodotto = ? RETURNING *", prodottoId);
            if (!deleted.next()) return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Eliminazione fallita"));
            // delete image file if exists
            if (immagine != null && !immagine.isBlank() && uploadsDir != null && !uploadsDir.isBlank()) {
                try {
                    Path path = Paths.get(uploadsDir, "prodotti", immagine);
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Ignore file deletion errors - not critical
                }
            }
            return ResponseEntity.ok(Map.of("message", "Prodotto eliminato con successo", "action", "deleted"));
        }
    }

    @GetMapping("/ordini/{ordineId}/dettaglio")
    public ResponseEntity<?> dettaglioOrdine(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable("ordineId") long ordineId) {
        if (!isAuthenticated(authorization)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        SqlRowSet ordine = jdbc.queryForRowSet("SELECT o.*, u.nome AS nome_cliente, u.cognome AS cognome_cliente, u.email AS email_cliente FROM ordini o JOIN utenti u ON o.user_id = u.id WHERE o.id = ?", ordineId);
        if (!ordine.next()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Ordine non trovato"));
        Map<String, Object> order = new HashMap<>();
        for (String col : ordine.getMetaData().getColumnNames()) order.put(col, ordine.getObject(col));
        List<Map<String, Object>> prodotti = new ArrayList<>();
        SqlRowSet righe = jdbc.queryForRowSet("SELECT p.nome, op.quantita, op.prezzo AS prezzo_unitario, (op.quantita * op.prezzo) AS subtotale FROM ordine_prodotti op JOIN prodotto p ON op.prodotto_id = p.id_prodotto WHERE op.ordine_id = ?", ordineId);
        while (righe.next()) {
            Map<String, Object> r = new HashMap<>();
            r.put("nome", righe.getString("nome"));
            r.put("quantita", righe.getInt("quantita"));
            r.put("prezzo_unitario", righe.getBigDecimal("prezzo_unitario"));
            r.put("subtotale", righe.getBigDecimal("subtotale"));
            prodotti.add(r);
        }
        return ResponseEntity.ok(Map.of("ordine", order, "prodotti", prodotti));
    }

    @PatchMapping("/ordini/{ordineId}/stato")
    public ResponseEntity<?> aggiornaStato(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable("ordineId") long ordineId,
                                    @RequestBody Map<String, Object> body) {
        if (!isAuthenticated(authorization)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        String stato = Objects.toString(body.get("stato"), "");
        String corriere = Objects.toString(body.get("corriere"), null);
        String codice_spedizione = Objects.toString(body.get("codice_spedizione"), null);
        String dettagli_pacco = Objects.toString(body.get("dettagli_pacco"), null);

        SqlRowSet result = jdbc.queryForRowSet("UPDATE ordini SET stato = ? WHERE id = ? RETURNING *", stato, ordineId);
        if (!result.next()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Ordine non trovato"));

        if (stato.trim().equalsIgnoreCase("spedito")) {
            SqlRowSet ord = jdbc.queryForRowSet("SELECT indirizzo_consegna FROM ordini WHERE id = ?", ordineId);
            String indirizzo = ord.next() ? ord.getString("indirizzo_consegna") : null;
            String dettaglioConData = (dettagli_pacco != null && !dettagli_pacco.isBlank()) ? (java.time.LocalDateTime.now().toString().replace('T', ' ') + " " + dettagli_pacco) : null;
            jdbc.update("INSERT INTO tracking_ordine (id_ordine, stato, corriere, codice_spedizione, dettagli_pacco, indirizzo_spedizione, data_aggiornamento) VALUES (?, ?, ?, ?, ?, ?, NOW()) ON CONFLICT (id_ordine) DO NOTHING",
                    ordineId, stato, corriere, codice_spedizione, dettaglioConData, indirizzo);
        } else {
            String dettaglioConData = (dettagli_pacco != null && !dettagli_pacco.isBlank()) ? (java.time.LocalDateTime.now().toString().replace('T', ' ') + " " + dettagli_pacco) : null;
            jdbc.update("UPDATE tracking_ordine SET stato = ?, dettagli_pacco = COALESCE(dettagli_pacco, '') || E'\n' || ?, data_aggiornamento = NOW() WHERE id_ordine = ?",
                    stato, dettaglioConData, ordineId);
        }

        Map<String, Object> updated = new HashMap<>();
        for (String col : result.getMetaData().getColumnNames()) updated.put(col, result.getObject(col));
        return ResponseEntity.ok(Map.of("message", "Stato aggiornato con successo", "ordine", updated));
    }

    @DeleteMapping("/utenti/{id}")
    public ResponseEntity<?> deleteUser(@RequestHeader(value = "Authorization", required = false) String authorization,
                                        @PathVariable("id") long id) {
        if (!isAuthenticated(authorization)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        jdbc.update("DELETE FROM utenti WHERE id = ?", id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Utente rimosso"));
    }
}
