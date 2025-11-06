package com.example.backendjava.controller;

import com.example.backendjava.auth.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/suggested")
public class SuggestedController {
    private final JdbcTemplate jdbc;

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${jwt.secret:dev-secret-please-change}")
    private String jwtSecret;

    public SuggestedController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private String productImageUrl(String filename) {
        if (filename == null || filename.isBlank()) {
            return "http://localhost:" + serverPort + "/api/immagine/uploads/prodotti/default.jpg";
        }
        return "http://localhost:" + serverPort + "/api/immagine/uploads/prodotti/" + filename;
    }

    @SuppressWarnings("PatternVariableCanBeUsed")
    private Optional<Long> userIdFromAuth(String authorization) {
        return JwtUtil.parseToken(authorization, jwtSecret)
                .map(claims -> {
                    Object idObj = claims.get("id");
                    if (idObj instanceof Number n) return n.longValue();
                    if (idObj instanceof String s && !s.isBlank()) return Long.parseLong(s);
                    return null;
                });
    }

    @SuppressWarnings("CatchMayIgnoreException")
    @GetMapping("/suggested")
    public ResponseEntity<?> getSuggested(@RequestHeader(value = "Authorization", required = false) String authorization) {
        try {
            Optional<Long> maybeUserId = userIdFromAuth(authorization);
            if (maybeUserId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Token mancante o non valido"));
            }
            long userId = maybeUserId.get();

            // Ultimi prodotti visualizzati (max 3)
            List<Long> viewedIds = new ArrayList<>();
            SqlRowSet visual = jdbc.queryForRowSet(
                    "SELECT prodotto_id FROM visualizzazioni WHERE user_id = ? ORDER BY visualizzato_at DESC LIMIT 3",
                    userId
            );
            while (visual.next()) {
                viewedIds.add(visual.getLong("prodotto_id"));
            }
            if (viewedIds.isEmpty()) {
                return ResponseEntity.ok(List.of());
            }

            // Prezzi e categorie degli ultimi visualizzati
            String placeholders = viewedIds.stream().map(id -> "?").collect(Collectors.joining(","));
            String sqlLast = "SELECT id_categoria, CASE WHEN promo = TRUE AND prezzo_scontato IS NOT NULL THEN prezzo_scontato ELSE prezzo END AS prezzo FROM prodotto WHERE id_prodotto IN (" + placeholders + ")";
            SqlRowSet lastRs = jdbc.queryForRowSet(sqlLast, viewedIds.toArray());
            Set<Long> categorie = new HashSet<>();
            List<BigDecimal> prezzi = new ArrayList<>();
            while (lastRs.next()) {
                categorie.add(lastRs.getLong("id_categoria"));
                BigDecimal p = lastRs.getBigDecimal("prezzo");
                if (p != null) prezzi.add(p);
            }
            if (prezzi.isEmpty() || categorie.isEmpty()) {
                return ResponseEntity.ok(List.of());
            }
            double minPrezzo = prezzi.stream().mapToDouble(BigDecimal::doubleValue).min().orElse(0.0) - 50.0;
            double maxPrezzo = prezzi.stream().mapToDouble(BigDecimal::doubleValue).max().orElse(0.0) + 50.0;
            minPrezzo = Math.round(minPrezzo * 100.0) / 100.0;
            maxPrezzo = Math.round(maxPrezzo * 100.0) / 100.0;

            // Query prodotti suggeriti
            String placeholdersView = viewedIds.stream().map(id -> "?").collect(Collectors.joining(","));
            String placeholdersCat = categorie.stream().map(id -> "?").collect(Collectors.joining(","));
            List<Object> args = new ArrayList<>();
            args.addAll(viewedIds);
            args.addAll(categorie);
            args.add(minPrezzo);
            args.add(maxPrezzo);
        String sql = "SELECT * FROM prodotto WHERE id_prodotto NOT IN (" + placeholdersView + ") " +
            "AND id_categoria IN (" + placeholdersCat + ") " +
            "AND bloccato = FALSE " +
            "AND quantita_disponibile > 0 " +
            "AND (CASE WHEN promo = TRUE AND prezzo_scontato IS NOT NULL THEN prezzo_scontato ELSE prezzo END) BETWEEN ? AND ? " +
            "LIMIT 6";

            SqlRowSet rs = jdbc.queryForRowSet(sql, args.toArray());
            List<Map<String, Object>> out = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id_prodotto", rs.getLong("id_prodotto"));
                row.put("nome", rs.getString("nome"));
                row.put("descrizione", rs.getString("descrizione"));
                row.put("prezzo", rs.getBigDecimal("prezzo"));
                row.put("prezzo_scontato", rs.getBigDecimal("prezzo_scontato"));
                row.put("promo", rs.getBoolean("promo"));
                String img = rs.getString("immagine");
                row.put("immagine", img);
                row.put("immagine_url", productImageUrl(img));
                out.add(row);
            }
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/visualizza")
    public ResponseEntity<?> salvaVisualizzazione(@RequestHeader(value = "Authorization", required = false) String authorization,
                                                  @RequestBody Map<String, Object> body) {
        try {
            Optional<Long> maybeUserId = userIdFromAuth(authorization);
            if (maybeUserId.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Token mancante o non valido"));
            }
            long userId = maybeUserId.get();
            Object pidObj = body.get("prodotto_id");
            if (pidObj == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "prodotto_id obbligatorio"));
            }
            long prodottoId = (pidObj instanceof Number n) ? n.longValue() : Long.parseLong(String.valueOf(pidObj));
            jdbc.update(
                    "INSERT INTO visualizzazioni (user_id, prodotto_id, visualizzato_at) VALUES (?, ?, NOW()) ON CONFLICT (user_id, prodotto_id) DO UPDATE SET visualizzato_at = NOW()",
                    userId, prodottoId
            );
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}
