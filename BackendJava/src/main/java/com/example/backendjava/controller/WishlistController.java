package com.example.backendjava.controller;

import com.example.backendjava.auth.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {
    private final JdbcTemplate jdbc;

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${jwt.secret:dev-secret-please-change}")
    private String jwtSecret;

    public WishlistController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @SuppressWarnings("PatternVariableCanBeUsed")
    private Optional<Long> userIdFromAuth(String authorization) {
        return JwtUtil.parseToken(authorization, jwtSecret)
                .map(c -> {
                    Object id = c.get("id");
                    if (id instanceof Number n) return n.longValue();
                    if (id instanceof String s && !s.isBlank()) return Long.parseLong(s);
                    return null;
                });
    }

    private String productImageUrl(String filename) {
        if (filename == null || filename.isBlank()) {
            return "http://localhost:" + serverPort + "/api/immagine/uploads/prodotti/default.jpg";
        }
        return "http://localhost:" + serverPort + "/api/immagine/uploads/prodotti/" + filename;
    }

    @GetMapping("")
    public ResponseEntity<?> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Optional<Long> uid = userIdFromAuth(authorization);
        if (uid.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Token mancante o non valido"));
        String sql = "SELECT p.id_prodotto as id, p.nome, p.prezzo, p.immagine, p.quantita_disponibile, p.descrizione, m.nome as marchio, c.nome as categoria FROM wish_list w JOIN prodotto p ON w.prodotto_id = p.id_prodotto JOIN categoria c on p.id_categoria = c.id_categoria JOIN marchio m on p.id_marchio = m.id_marchio WHERE w.user_id = ?";
        List<Map<String, Object>> list = new ArrayList<>();
        SqlRowSet rs = jdbc.queryForRowSet(sql, uid.get());
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", rs.getLong("id"));
            row.put("nome", rs.getString("nome"));
            row.put("prezzo", rs.getBigDecimal("prezzo"));
            row.put("immagine", rs.getString("immagine"));
            row.put("immagine_url", productImageUrl(rs.getString("immagine")));
            row.put("quantita_disponibile", rs.getInt("quantita_disponibile"));
            row.put("descrizione", rs.getString("descrizione"));
            row.put("marchio", rs.getString("marchio"));
            row.put("categoria", rs.getString("categoria"));
            list.add(row);
        }
        return ResponseEntity.ok(list);
    }

    @PostMapping("")
    public ResponseEntity<?> add(@RequestHeader(value = "Authorization", required = false) String authorization,
                                 @RequestBody Map<String, Object> body) {
        Optional<Long> uid = userIdFromAuth(authorization);
        if (uid.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Token mancante o non valido"));
        Object pid = body.get("prodotto_id");
        if (pid == null) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "prodotto_id obbligatorio"));
        long prodottoId = (pid instanceof Number n) ? n.longValue() : Long.parseLong(String.valueOf(pid));
        jdbc.update("INSERT INTO wish_list (user_id, prodotto_id) VALUES (?, ?) ON CONFLICT DO NOTHING", uid.get(), prodottoId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @DeleteMapping("/{prodotto_id}")
    public ResponseEntity<?> remove(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable("prodotto_id") long prodottoId) {
        Optional<Long> uid = userIdFromAuth(authorization);
        if (uid.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Token mancante o non valido"));
        jdbc.update("DELETE FROM wish_list WHERE user_id = ? AND prodotto_id = ?", uid.get(), prodottoId);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
