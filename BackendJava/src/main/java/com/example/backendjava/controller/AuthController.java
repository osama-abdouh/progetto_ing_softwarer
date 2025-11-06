package com.example.backendjava.controller;

import com.example.backendjava.auth.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller REST per la gestione dell'autenticazione utenti.
 * Fornisce endpoint per registrazione, login e gestione JWT token.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final JdbcTemplate jdbc;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Value("${jwt.secret:dev-secret-please-change}")
    private String jwtSecret;

    @Value("${jwt.expiration.millis:7200000}") // 2h
    private long jwtExpirationMillis;

    public AuthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Registra un nuovo utente nel sistema.
     * 
     * @param body Dati registrazione: nome, cognome, email, password
     * @return ResponseEntity con dati utente creato (id, nome, email)
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> body) {
        String nome = ((String) body.get("nome"));
        String cognome = ((String) body.get("cognome"));
        String email = ((String) body.get("email"));
        String password = ((String) body.get("password"));
        if (nome == null || cognome == null || email == null || password == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Tutti i campi sono obbligatori"));
        }
        email = email.toLowerCase();
        String hashed = encoder.encode(password);
        Long id = jdbc.queryForObject(
                "INSERT INTO utenti (nome, cognome, email, password) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class, nome, cognome, email, hashed
        );
        Map<String, Object> res = new HashMap<>();
        res.put("id", id);
        res.put("nome", nome);
        res.put("email", email);
        return ResponseEntity.ok(res);
    }

    /**
     * Effettua il login di un utente esistente.
     * Verifica le credenziali e genera un JWT token.
     * 
     * @param body Credenziali: email, password
     * @return ResponseEntity con token JWT e dati utente (id, nome, email, ruolo)
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> body) {
        String email = ((String) body.get("email"));
        String password = ((String) body.get("password"));
        if (email == null || password == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Email e password obbligatorie"));
        }
        email = email.toLowerCase();

        SqlRowSet rs = jdbc.queryForRowSet("SELECT * FROM utenti WHERE email = ?", email);
        if (!rs.next()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Credenziali non valide"));
        }

        String hashed = rs.getString("password");
        if (!encoder.matches(password, hashed)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Credenziali non valide"));
        }

        if (rs.getBoolean("is_blocked")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Account bloccato, Contatta l'amministratore."));
        }
        long id = rs.getLong("id");
        String nome = rs.getString("nome");
        String ruolo = rs.getString("ruolo");

        Map<String, Object> claims = new HashMap<>();
        claims.put("id", id);
        claims.put("nome", nome);
        claims.put("email", email);
        claims.put("ruolo", ruolo);
        String token = JwtUtil.generateToken(claims, jwtSecret, jwtExpirationMillis);

        Map<String, Object> user = new HashMap<>();
        user.put("id", id);
        user.put("nome", nome);
        user.put("email", email);
        user.put("ruolo", ruolo);
        Map<String, Object> res = new HashMap<>();
        res.put("token", token);
        res.put("user", user);
        return ResponseEntity.ok(res);
    }
}
