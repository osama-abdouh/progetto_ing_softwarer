package com.example.backendjava.controller;

import com.example.backendjava.auth.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Value;
import java.util.*;

/**
 * Controller REST per la gestione del profilo utente.
 * Permette di visualizzare e modificare i dati personali dell'utente autenticato.
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final JdbcTemplate jdbc;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Value("${jwt.secret:dev-secret-please-change}")
    private String jwtSecret;

    public ProfileController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private Optional<Long> userIdFromAuth(String authorization) {
        return JwtUtil.parseToken(authorization, jwtSecret)
                .map(c -> {
                    Object id = c.get("id");
                    if (id instanceof Number n) return n.longValue();
                    if (id instanceof String s && !s.isBlank()) return Long.parseLong(s);
                    return null;
                });
    }

    @GetMapping("")
    public ResponseEntity<?> getProfile(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Optional<Long> uid = userIdFromAuth(authorization);
        if (uid.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Token mancante o non valido"));
        SqlRowSet rs = jdbc.queryForRowSet("SELECT id, nome, cognome, email, telefono, data_nascita, sesso FROM utenti WHERE id = ?", uid.get());
        if (!rs.next()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Utente non trovato"));
        Map<String, Object> out = new HashMap<>();
        out.put("id", rs.getLong("id"));
        out.put("nome", rs.getString("nome"));
        out.put("cognome", rs.getString("cognome"));
        out.put("email", rs.getString("email"));
        out.put("telefono", rs.getString("telefono"));
        out.put("data_nascita", rs.getDate("data_nascita"));
        out.put("sesso", rs.getString("sesso"));
        return ResponseEntity.ok(out);
    }

    @PutMapping("")
    public ResponseEntity<?> updateProfile(@RequestHeader(value = "Authorization", required = false) String authorization,
                                           @RequestBody Map<String, Object> body) {
        Optional<Long> uid = userIdFromAuth(authorization);
        if (uid.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Token mancante o non valido"));
        String nome = Objects.toString(body.get("nome"), null);
        String cognome = Objects.toString(body.get("cognome"), null);
        String telefono = Objects.toString(body.get("telefono"), null);
        Object dataNascita = body.get("data_nascita");
        String sesso = Objects.toString(body.get("sesso"), null);
        SqlRowSet rs = jdbc.queryForRowSet(
                "UPDATE utenti SET nome = ?, cognome = ?, telefono = ?, data_nascita = ?, sesso = ? WHERE id = ? RETURNING id, nome, cognome, email, telefono, data_nascita, sesso",
                nome, cognome, telefono, dataNascita, sesso, uid.get()
        );
        if (!rs.next()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Utente non trovato"));
        Map<String, Object> out = new HashMap<>();
        @SuppressWarnings("null")
        String[] columns = rs.getMetaData().getColumnNames();
        for (String col : columns) out.put(col, rs.getObject(col));
        return ResponseEntity.ok(out);
    }

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(@RequestHeader(value = "Authorization", required = false) String authorization,
                                            @RequestBody Map<String, Object> body) {
        Optional<Long> uid = userIdFromAuth(authorization);
        if (uid.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Token mancante o non valido"));
        String vecchia = Objects.toString(body.get("vecchia_password"), null);
        String nuova = Objects.toString(body.get("nuova_password"), null);
        SqlRowSet rs = jdbc.queryForRowSet("SELECT password FROM utenti WHERE id = ?", uid.get());
        if (!rs.next()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Utente non trovato"));
        String hashed = rs.getString("password");
        if (!encoder.matches(vecchia, hashed)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Password attuale non corretta"));
        }
        String newHash = encoder.encode(nuova);
        jdbc.update("UPDATE utenti SET password = ? WHERE id = ?", newHash, uid.get());
        return ResponseEntity.ok(Map.of("message", "Password cambiata con successo"));
    }

    @PutMapping("/email")
    public ResponseEntity<?> changeEmail(@RequestHeader(value = "Authorization", required = false) String authorization,
                                         @RequestBody Map<String, Object> body) {
        Optional<Long> uid = userIdFromAuth(authorization);
        if (uid.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Token mancante o non valido"));
        String nuovaEmail = Objects.toString(body.get("nuova_email"), null);
        String password = Objects.toString(body.get("password"), null);
        SqlRowSet rs = jdbc.queryForRowSet("SELECT password FROM utenti WHERE id = ?", uid.get());
        if (!rs.next()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Utente non trovato"));
        if (!encoder.matches(password, rs.getString("password"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Password non corretta"));
        }
        SqlRowSet emailCheck = jdbc.queryForRowSet("SELECT id FROM utenti WHERE email = ? AND id != ?", nuovaEmail, uid.get());
        if (emailCheck.next()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "Email già in uso"));
        }
        jdbc.update("UPDATE utenti SET email = ? WHERE id = ?", nuovaEmail, uid.get());
        return ResponseEntity.ok(Map.of("message", "Email cambiata con successo"));
    }
}
