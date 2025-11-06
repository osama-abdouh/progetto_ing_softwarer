package com.example.backendjava.controller;

import com.example.backendjava.auth.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.sql.Date;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/coupon")
public class CouponController {
    private final JdbcTemplate jdbc;

    @Value("${jwt.secret:dev-secret-please-change}")
    private String jwtSecret;

    public CouponController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private Optional<String> roleFromAuth(String authorization) {
        return JwtUtil.parseToken(authorization, jwtSecret).map(c -> String.valueOf(c.get("ruolo")));
    }

    private static Date toSqlDate(Object o) {
        if (o == null) return null;
        if (o instanceof Date d) return d;
        if (o instanceof java.util.Date ud) return new Date(ud.getTime());
        String s = String.valueOf(o).trim();
        if (s.isEmpty()) return null;
        // Accept ISO strings like 2025-11-04 or 2025-11-04T00:00:00Z
        if (s.length() >= 10) s = s.substring(0, 10);
        try {
            return Date.valueOf(LocalDate.parse(s));
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isAdmin(String authorization) {
        return roleFromAuth(authorization).map(r -> r.equalsIgnoreCase("admin") || r.equalsIgnoreCase("owner")).orElse(false);
    }

    private Optional<Long> userIdFromAuth(String authorization) {
        return JwtUtil.parseToken(authorization, jwtSecret)
                .map(c -> c.get("id"))
                .filter(Objects::nonNull)
                .map(v -> (v instanceof Number) ? ((Number) v).longValue() : Long.parseLong(String.valueOf(v)));
    }

    // Verify coupon validity and compute discount (used by checkout)
    @PostMapping("/verifica")
    public ResponseEntity<?> verifyCoupon(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body
    ) {
        var userIdOpt = userIdFromAuth(authorization);
        if (userIdOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "valido", false,
                    "messaggio", "Non autenticato"
            ));
        }
        long userId = userIdOpt.get();

        String codice = String.valueOf(body.get("codice")).toUpperCase(Locale.ITALIAN);
        BigDecimal totale = toDecimal(body.get("totale_carrello"));
        if (totale == null) totale = BigDecimal.ZERO;

        SqlRowSet rs = jdbc.queryForRowSet(
                "SELECT * FROM coupon WHERE codice = ? AND attivo = TRUE " +
                        "AND (data_scadenza IS NULL OR data_scadenza >= CURRENT_DATE) " +
                        "AND (usi_massimi IS NULL OR usi_attuali < usi_massimi)",
                codice
        );

        if (!rs.next()) {
            return ResponseEntity.ok(Map.of(
                    "valido", false,
                    "messaggio", "Coupon non valido, scaduto o esaurito"
            ));
        }

        long couponId = rs.getLong("id");
        String tipo = rs.getString("tipo_sconto");
        BigDecimal valoreSconto = rs.getBigDecimal("valore_sconto");
        BigDecimal importoMinimo = rs.getBigDecimal("importo_minimo");
        boolean usoSingolo = rs.getBoolean("uso_singolo");

        if (usoSingolo) {
            SqlRowSet used = jdbc.queryForRowSet(
                    "SELECT 1 FROM coupon_utilizzi WHERE coupon_id = ? AND user_id = ?",
                    couponId, userId
            );
            if (used.next()) {
                return ResponseEntity.ok(Map.of(
                        "valido", false,
                        "messaggio", "Hai già utilizzato questo coupon"
                ));
            }
        }

        if (importoMinimo != null && totale.compareTo(importoMinimo) < 0) {
            return ResponseEntity.ok(Map.of(
                    "valido", false,
                    "messaggio", "Importo minimo richiesto: €" + importoMinimo
            ));
        }

        BigDecimal sconto;
        if ("percentuale".equalsIgnoreCase(tipo)) {
            sconto = totale.multiply(valoreSconto).divide(BigDecimal.valueOf(100));
        } else {
            sconto = valoreSconto.min(totale);
        }
        BigDecimal totaleScontato = totale.subtract(sconto);
        if (totaleScontato.compareTo(BigDecimal.ZERO) < 0) totaleScontato = BigDecimal.ZERO;

        Map<String, Object> payload = new HashMap<>();
        payload.put("valido", true);
        payload.put("coupon", Map.of(
                "id", couponId,
                "codice", rs.getString("codice"),
                "descrizione", rs.getString("descrizione"),
                "tipo_sconto", tipo,
                "valore_sconto", valoreSconto
        ));
        payload.put("sconto", sconto.setScale(2, RoundingMode.HALF_UP));
        payload.put("totale_originale", totale);
        payload.put("totale_scontato", totaleScontato.setScale(2, RoundingMode.HALF_UP));
        payload.put("messaggio", "Coupon applicato: " + rs.getString("descrizione"));
        return ResponseEntity.ok(payload);
    }

    // Mark coupon usage (increment counters and record single-use)
    @PostMapping("/usa")
    public ResponseEntity<?> useCoupon(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body
    ) {
        var userIdOpt = userIdFromAuth(authorization);
        if (userIdOpt.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Non autenticato"));
        long userId = userIdOpt.get();

        Long couponId = null;
        Object idObj = body.get("coupon_id");
        if (idObj instanceof Number n) couponId = n.longValue();
        else if (idObj != null) couponId = Long.parseLong(String.valueOf(idObj));
        if (couponId == null) return ResponseEntity.badRequest().body(Map.of("error", "coupon_id mancante"));

        SqlRowSet rs = jdbc.queryForRowSet("SELECT * FROM coupon WHERE id = ?", couponId);
        if (!rs.next()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Coupon non trovato"));

        boolean usoSingolo = rs.getBoolean("uso_singolo");
        if (usoSingolo) {
            jdbc.update("INSERT INTO coupon_utilizzi (coupon_id, user_id) VALUES (?, ?)", couponId, userId);
        }
        jdbc.update("UPDATE coupon SET usi_attuali = COALESCE(usi_attuali, 0) + 1 WHERE id = ?", couponId);
        return ResponseEntity.ok(Map.of("success", true));
    }

    // Admin: list all coupons
    @GetMapping
    public ResponseEntity<?> listAll(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (!isAdmin(authorization)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Solo admin"));
        SqlRowSet rs = jdbc.queryForRowSet("SELECT * FROM coupon ORDER BY created_at DESC");
        List<Map<String, Object>> list = new ArrayList<>();
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", rs.getLong("id"));
            row.put("codice", rs.getString("codice"));
            row.put("descrizione", rs.getString("descrizione"));
            row.put("tipo_sconto", rs.getString("tipo_sconto"));
            row.put("valore_sconto", rs.getBigDecimal("valore_sconto"));
            row.put("importo_minimo", rs.getBigDecimal("importo_minimo"));
            row.put("data_inizio", rs.getDate("data_inizio"));
            row.put("data_scadenza", rs.getDate("data_scadenza"));
            row.put("usi_massimi", rs.getObject("usi_massimi"));
            row.put("usi_attuali", rs.getObject("usi_attuali"));
            row.put("attivo", rs.getBoolean("attivo"));
            row.put("uso_singolo", rs.getBoolean("uso_singolo"));
            row.put("created_at", rs.getTimestamp("created_at"));
            list.add(row);
        }
        return ResponseEntity.ok(list);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @RequestBody Map<String, Object> body) {
        if (!isAdmin(authorization)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Solo admin"));
        String codice = String.valueOf(body.get("codice")).toUpperCase(Locale.ITALIAN);
        SqlRowSet exists = jdbc.queryForRowSet("SELECT id FROM coupon WHERE codice = ?", codice);
        if (exists.next()) {
            SqlRowSet rs = jdbc.queryForRowSet("SELECT * FROM coupon WHERE codice = ?", codice);
            if (rs.next()) {
                Map<String, Object> coupon = new HashMap<>();
                coupon.put("id", rs.getLong("id"));
                coupon.put("codice", rs.getString("codice"));
                coupon.put("descrizione", rs.getString("descrizione"));
                coupon.put("tipo_sconto", rs.getString("tipo_sconto"));
                coupon.put("valore_sconto", rs.getBigDecimal("valore_sconto"));
                coupon.put("importo_minimo", rs.getBigDecimal("importo_minimo"));
                coupon.put("data_inizio", rs.getDate("data_inizio"));
                coupon.put("data_scadenza", rs.getDate("data_scadenza"));
                coupon.put("usi_massimi", rs.getObject("usi_massimi"));
                coupon.put("attivo", rs.getBoolean("attivo"));
                coupon.put("uso_singolo", rs.getBoolean("uso_singolo"));
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("exists", true, "coupon", coupon));
            }
        }
        String descrizione = (String) body.get("descrizione");
        String tipo = (String) body.get("tipo_sconto");
        BigDecimal valore = toDecimal(body.get("valore_sconto"));
        BigDecimal importoMin = toDecimal(body.get("importo_minimo"));
    Date dataInizio = toSqlDate(body.get("data_inizio"));
    Date dataScadenza = toSqlDate(body.get("data_scadenza"));
        Integer usiMassimi = toInteger(body.get("usi_massimi"));
        Boolean attivo = toBoolean(body.get("attivo"));
        Boolean usoSingolo = toBoolean(body.get("uso_singolo"));
        jdbc.update("INSERT INTO coupon (codice, descrizione, tipo_sconto, valore_sconto, importo_minimo, data_inizio, data_scadenza, usi_massimi, attivo, uso_singolo) VALUES (?,?,?,?,?,?,?,?,?,?)",
                codice, descrizione, tipo, valore, importoMin, dataInizio, dataScadenza, usiMassimi, attivo, usoSingolo);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Coupon creato"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable("id") long id,
                                    @RequestBody Map<String, Object> body) {
        if (!isAdmin(authorization)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Solo admin"));
        String codice = String.valueOf(body.get("codice")).toUpperCase(Locale.ITALIAN);
        // Prevent duplicate codice on different ID
        SqlRowSet dup = jdbc.queryForRowSet("SELECT id FROM coupon WHERE codice = ? AND id <> ?", codice, id);
        if (dup.next()) {
            SqlRowSet rs = jdbc.queryForRowSet("SELECT * FROM coupon WHERE codice = ?", codice);
            if (rs.next()) {
                Map<String, Object> coupon = new HashMap<>();
                coupon.put("id", rs.getLong("id"));
                coupon.put("codice", rs.getString("codice"));
                coupon.put("descrizione", rs.getString("descrizione"));
                coupon.put("tipo_sconto", rs.getString("tipo_sconto"));
                coupon.put("valore_sconto", rs.getBigDecimal("valore_sconto"));
                coupon.put("importo_minimo", rs.getBigDecimal("importo_minimo"));
                coupon.put("data_inizio", rs.getDate("data_inizio"));
                coupon.put("data_scadenza", rs.getDate("data_scadenza"));
                coupon.put("usi_massimi", rs.getObject("usi_massimi"));
                coupon.put("attivo", rs.getBoolean("attivo"));
                coupon.put("uso_singolo", rs.getBoolean("uso_singolo"));
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("exists", true, "coupon", coupon));
            }
        }

        jdbc.update("UPDATE coupon SET codice = ?, descrizione = ?, tipo_sconto = ?, valore_sconto = ?, importo_minimo = ?, data_inizio = ?, data_scadenza = ?, usi_massimi = ?, attivo = ?, uso_singolo = ? WHERE id = ?",
                codice,
                body.get("descrizione"),
                body.get("tipo_sconto"),
                toDecimal(body.get("valore_sconto")),
                toDecimal(body.get("importo_minimo")),
                toSqlDate(body.get("data_inizio")),
                toSqlDate(body.get("data_scadenza")),
                toInteger(body.get("usi_massimi")),
                toBoolean(body.get("attivo")),
                toBoolean(body.get("uso_singolo")),
                id);
        return ResponseEntity.ok(Map.of("message", "Coupon aggiornato"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable("id") long id) {
        if (!isAdmin(authorization)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Solo admin"));
        int affected = jdbc.update("DELETE FROM coupon WHERE id = ?", id);
        if (affected == 0) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Coupon non trovato"));
        return ResponseEntity.ok(Map.of("message", "Coupon rimosso con successo"));
    }

    private static BigDecimal toDecimal(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return new BigDecimal(n.toString());
        return new BigDecimal(String.valueOf(o));
    }

    private static Integer toInteger(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        return Integer.parseInt(String.valueOf(o));
    }

    private static Boolean toBoolean(Object o) {
        if (o == null) return null;
        if (o instanceof Boolean b) return b;
        String s = String.valueOf(o);
        if ("true".equalsIgnoreCase(s)) return true;
        if ("false".equalsIgnoreCase(s)) return false;
        return null;
    }
}
