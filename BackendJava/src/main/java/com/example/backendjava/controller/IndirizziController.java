package com.example.backendjava.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backendjava.auth.JwtUtil;

@RestController
@RequestMapping("/api/indirizzi")
public class IndirizziController {
    private final JdbcTemplate jdbc;

    @Value("${jwt.secret:dev-secret-please-change}")
    private String jwtSecret;

    public IndirizziController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static String norm(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? null : s;
    }

    private Optional<Long> userIdFromAuth(String authorization) {
        return JwtUtil.parseToken(authorization, jwtSecret)
                .map(claims -> {
                    Object idObj = claims.get("id");
                    if (idObj instanceof Number n) return n.longValue();
                    if (idObj instanceof String s && !s.isBlank()) return Long.parseLong(s);
                    return null;
                });
    }

    @PostMapping("")
    public ResponseEntity<?> create(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @RequestBody Map<String, Object> body) {
        Optional<Long> maybeUser = userIdFromAuth(authorization);
        if (maybeUser.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Token mancante o non valido"));
        long userId = maybeUser.get();

        String via = norm(body.get("indirizzo"));
        String citta = norm(body.get("citta"));
        String cap = norm(body.get("cap"));
        String provincia = norm(body.get("provincia"));
        String nazione = norm(body.get("paese"));
        Boolean isDefault = body.get("predefinito") instanceof Boolean b ? b : false;
        String nomeDestinatario = norm(body.get("destinatario"));
        String telefonoDestinatario = norm(body.get("telefono"));

        if (via == null || citta == null || cap == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Inserisci indirizzo, città e CAP"));
        }

        try {
            // Controlla duplicati
            SqlRowSet checkDuplicate = jdbc.queryForRowSet(
                "SELECT * FROM indirizzi WHERE user_id = ? AND LOWER(indirizzo) = LOWER(?) AND LOWER(citta) = LOWER(?) AND cap = ?",
                userId, via, citta, cap
            );
            if (checkDuplicate.next()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Questo indirizzo è già presente"));
            }

            // Se viene impostato come predefinito, rimuovi il flag dagli altri
            if (Boolean.TRUE.equals(isDefault)) {
                try {
                    jdbc.update("UPDATE indirizzi SET predefinito = ? WHERE user_id = ?", false, userId);
                } catch (Exception e) {
                    System.err.println("Warning: Could not update defaults: " + e.getMessage());
                }
            }
            
            SqlRowSet rs = jdbc.queryForRowSet(
                "INSERT INTO indirizzi (user_id, indirizzo, citta, cap, provincia, paese, predefinito, destinatario, telefono) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING *",
                userId, via, citta, cap, provincia, nazione, (isDefault != null && isDefault), nomeDestinatario, telefonoDestinatario
            );
            if (rs.next()) {
                return ResponseEntity.ok(mapIndirizzoToFrontend(rs));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Inserimento fallito"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }

    @SuppressWarnings("null")
    private Map<String, Object> mapIndirizzoToFrontend(SqlRowSet rs) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", rs.getObject("id"));
        row.put("indirizzo", rs.getObject("indirizzo"));
        row.put("citta", rs.getObject("citta"));
        row.put("cap", rs.getObject("cap"));
        row.put("provincia", rs.getObject("provincia"));
        row.put("paese", rs.getObject("paese"));
        row.put("predefinito", rs.getObject("predefinito"));
        row.put("destinatario", rs.getObject("destinatario"));
        row.put("telefono", rs.getObject("telefono"));
        row.put("user_id", rs.getObject("user_id"));
        return row;
    }

    @GetMapping("")
    public ResponseEntity<?> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Optional<Long> maybeUser = userIdFromAuth(authorization);
        if (maybeUser.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Token mancante o non valido"));
        long userId = maybeUser.get();
        List<Map<String, Object>> out = new ArrayList<>();
        SqlRowSet rs = jdbc.queryForRowSet("SELECT * FROM indirizzi WHERE user_id = ?", userId);
        while (rs.next()) {
            out.add(mapIndirizzoToFrontend(rs));
        }
        return ResponseEntity.ok(out);
    }

    @PutMapping("/{id}/predefinito")
    public ResponseEntity<?> setDefault(@RequestHeader(value = "Authorization", required = false) String authorization,
                                        @PathVariable("id") long indirizzoId) {
        Optional<Long> maybeUser = userIdFromAuth(authorization);
        if (maybeUser.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Token mancante o non valido"));
        long userId = maybeUser.get();
        jdbc.update("UPDATE indirizzi SET predefinito = ? WHERE user_id = ?", false, userId);
        jdbc.update("UPDATE indirizzi SET predefinito = ? WHERE id = ? AND user_id = ?", true, indirizzoId, userId);
        return ResponseEntity.ok(Map.of("message", "Indirizzo impostato come predefinito"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable("id") long indirizzoId,
                                    @RequestBody Map<String, Object> body) {
        Optional<Long> maybeUser = userIdFromAuth(authorization);
        if (maybeUser.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Token mancante o non valido"));
        long userId = maybeUser.get();

        String via = norm(body.get("indirizzo"));
        String citta = norm(body.get("citta"));
        String cap = norm(body.get("cap"));
        String provincia = norm(body.get("provincia"));
        String nazione = norm(body.get("paese"));
        Boolean isDefault = body.get("predefinito") instanceof Boolean b ? b : false;
        String nomeDestinatario = norm(body.get("destinatario"));
        String telefonoDestinatario = norm(body.get("telefono"));

        if (via == null || citta == null || cap == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Inserisci indirizzo, città e CAP"));
        }

        // Verifica che l'indirizzo appartenga all'utente
        SqlRowSet owner = jdbc.queryForRowSet("SELECT 1 FROM indirizzi WHERE id= ? AND user_id = ?", indirizzoId, userId);
        if (!owner.next()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Indirizzo non trovato"));

        // Controlla duplicati (escludi l'indirizzo corrente)
        SqlRowSet checkDuplicate = jdbc.queryForRowSet(
            "SELECT * FROM indirizzi WHERE user_id = ? AND id != ? AND LOWER(indirizzo) = LOWER(?) AND LOWER(citta) = LOWER(?) AND cap = ?",
            userId, indirizzoId, via, citta, cap
        );
        if (checkDuplicate.next()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Questo indirizzo è già presente"));
        }

        // Se viene impostato come predefinito, rimuovi il flag dagli altri
        if (Boolean.TRUE.equals(isDefault)) {
            try {
                jdbc.update("UPDATE indirizzi SET predefinito = ? WHERE user_id = ?", false, userId);
            } catch (Exception e) {
                System.err.println("Warning: Could not update defaults: " + e.getMessage());
            }
        }
        
        SqlRowSet rs = jdbc.queryForRowSet(
                "UPDATE indirizzi SET indirizzo = ?, citta = ?, cap = ?, provincia = ?, paese = ?, predefinito = ?, destinatario = ?, telefono = ? WHERE id = ? AND user_id = ? RETURNING *",
                via, citta, cap, provincia, nazione, isDefault != null && isDefault, nomeDestinatario, telefonoDestinatario, indirizzoId, userId
        );
        if (!rs.next()) return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Aggiornamento fallito"));
        return ResponseEntity.ok(mapIndirizzoToFrontend(rs));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable("id") long indirizzoId) {
        Optional<Long> maybeUser = userIdFromAuth(authorization);
        if (maybeUser.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Token mancante o non valido"));
        long userId = maybeUser.get();
        SqlRowSet rs = jdbc.queryForRowSet("DELETE FROM indirizzi WHERE id = ? AND user_id = ? RETURNING *", indirizzoId, userId);
        if (!rs.next()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Indirizzo non trovato"));
        return ResponseEntity.ok(Map.of("message", "Indirizzo eliminato con successo"));
    }
}
