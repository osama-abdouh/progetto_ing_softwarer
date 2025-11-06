package com.example.backendjava.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST per la gestione CRUD dei prodotti (area amministratore).
 * Permette di creare, modificare, eliminare e visualizzare prodotti.
 */
@RestController
@RequestMapping("/api/products")
public class ProductsController {

    private final JdbcTemplate jdbc;

    @Value("${server.port:8080}")
    private int serverPort;

    public ProductsController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private String productImageUrl(String filename) {
        if (filename == null || filename.isBlank()) {
            return "http://localhost:" + serverPort + "/api/immagine/uploads/prodotti/default.jpg";
        }
        return "http://localhost:" + serverPort + "/api/immagine/uploads/prodotti/" + filename;
    }

    /**
     * Carica l'elenco completo di tutti i prodotti con i relativi dettagli.
     * 
     * @return Lista di tutti i prodotti con categoria e marchio associati
     */
    @GetMapping("/load")
    public List<Map<String, Object>> loadAll() {
        // LEFT JOIN per includere anche prodotti senza categoria/marchio validi
        String sql = "SELECT p.*, c.nome AS nome_categoria, m.nome AS nome_marchio FROM prodotto p " +
                "LEFT JOIN categoria c ON p.id_categoria = c.id_categoria " +
                "LEFT JOIN marchio m ON p.id_marchio = m.id_marchio";
        List<Map<String, Object>> list = new ArrayList<>();
        SqlRowSet rs = jdbc.queryForRowSet(sql);
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            row.put("id_prodotto", rs.getLong("id_prodotto"));
            row.put("nome", rs.getString("nome"));
            row.put("descrizione", rs.getString("descrizione"));
            row.put("prezzo", rs.getBigDecimal("prezzo"));
            row.put("prezzo_scontato", rs.getBigDecimal("prezzo_scontato"));
            row.put("quantita_disponibile", rs.getInt("quantita_disponibile"));
            row.put("id_categoria", rs.getLong("id_categoria"));
            row.put("id_marchio", rs.getLong("id_marchio"));
            String img = rs.getString("immagine");
            row.put("immagine", img);
            row.put("immagine_url", productImageUrl(img));
            row.put("in_vetrina", rs.getBoolean("in_vetrina"));
            row.put("promo", rs.getBoolean("promo"));
            row.put("bloccato", rs.getBoolean("bloccato"));
            row.put("nome_categoria", rs.getString("nome_categoria")); // Può essere null
            row.put("nome_marchio", rs.getString("nome_marchio")); // Può essere null
            list.add(row);
        }
        return list;
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable("id") long id, @RequestBody Map<String, Object> body) {
        try {
            SqlRowSet existing = jdbc.queryForRowSet("SELECT * FROM prodotto WHERE id_prodotto = ?", id);
            if (!existing.next()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Prodotto non trovato"));

            BigDecimal baseOriginalPrice = optDecimal(body.get("prezzo"), existing.getBigDecimal("prezzo"));
            Boolean promo = optBoolean(body.get("promo"), existing.getBoolean("promo"));

            BigDecimal prezzoScontato = null;
            if (promo != null && promo) {
                // Prefer explicit prezzo_scontato
                prezzoScontato = optDecimal(body.get("prezzo_scontato"), existing.getBigDecimal("prezzo_scontato"));
                if (prezzoScontato == null) {
                    // compute from percentage sconto if provided
                    BigDecimal sconto = optDecimal(body.get("sconto"), null);
                    if (sconto != null && baseOriginalPrice != null) {
                        BigDecimal oneHundred = new BigDecimal("100");
                        BigDecimal factor = BigDecimal.ONE.subtract(sconto.divide(oneHundred));
                        prezzoScontato = baseOriginalPrice.multiply(factor).setScale(2, RoundingMode.HALF_UP);
                    }
                }
            }

            String sql = "UPDATE prodotto SET nome = ?, prezzo = ?, prezzo_scontato = ?, descrizione = ?, immagine = ?, quantita_disponibile = ?, id_categoria = ?, id_marchio = ?, in_vetrina = ?, promo = ?, bloccato = ? WHERE id_prodotto = ? RETURNING *";
            Object[] args = new Object[]{
                    body.get("nome"),
                    baseOriginalPrice,
                    prezzoScontato,
                    body.get("descrizione"),
                    body.get("immagine"),
                    asInteger(body.get("quantita_disponibile")),
                    asLong(body.get("id_categoria")),
                    asLong(body.get("id_marchio")),
                    optBoolean(body.get("in_vetrina"), existing.getBoolean("in_vetrina")),
                    promo,
                    optBoolean(body.get("bloccato"), existing.getBoolean("bloccato")),
                    id
            };
            SqlRowSet rs = jdbc.queryForRowSet(sql, args);
            if (!rs.next()) return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Update fallito"));
            Map<String, Object> out = new HashMap<>();
            out.put("id_prodotto", rs.getLong("id_prodotto"));
            out.put("nome", rs.getString("nome"));
            out.put("prezzo", rs.getBigDecimal("prezzo"));
            out.put("prezzo_scontato", rs.getBigDecimal("prezzo_scontato"));
            out.put("descrizione", rs.getString("descrizione"));
            out.put("immagine", rs.getString("immagine"));
            out.put("quantita_disponibile", rs.getInt("quantita_disponibile"));
            out.put("id_categoria", rs.getLong("id_categoria"));
            out.put("id_marchio", rs.getLong("id_marchio"));
            out.put("in_vetrina", rs.getBoolean("in_vetrina"));
            out.put("promo", rs.getBoolean("promo"));
            out.put("bloccato", rs.getBoolean("bloccato"));
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }

    @SuppressWarnings("CatchMayIgnoreException")
    @PutMapping("/{id}/vetrina")
    public ResponseEntity<?> setVetrinaPromo(@PathVariable("id") long id, @RequestBody Map<String, Object> body) {
        try {
            Boolean inVetrina = optBoolean(body.get("in_vetrina"), null);
            Boolean promo = optBoolean(body.get("promo"), null);
            if (inVetrina == null || promo == null) return ResponseEntity.badRequest().body(Map.of("message", "in_vetrina e promo richiesti"));
            SqlRowSet rs = jdbc.queryForRowSet("UPDATE prodotto SET in_vetrina = ?, promo = ? WHERE id_prodotto = ? RETURNING *", inVetrina, promo, id);
            if (!rs.next()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Prodotto non trovato"));
            Map<String, Object> out = new HashMap<>();
            out.put("id_prodotto", rs.getLong("id_prodotto"));
            out.put("in_vetrina", rs.getBoolean("in_vetrina"));
            out.put("promo", rs.getBoolean("promo"));
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }

    @SuppressWarnings("CatchMayIgnoreException")
    @PatchMapping("/{id}/blocco")
    public ResponseEntity<?> setBlocco(@PathVariable("id") long id, @RequestBody Map<String, Object> body) {
        try {
            Boolean bloccato = optBoolean(body.get("bloccato"), null);
            if (bloccato == null) return ResponseEntity.badRequest().body(Map.of("message", "bloccato richiesto"));
            SqlRowSet rs = jdbc.queryForRowSet("UPDATE prodotto SET bloccato = ? WHERE id_prodotto = ? RETURNING id_prodotto, bloccato", bloccato, id);
            if (!rs.next()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Prodotto non trovato"));
            return ResponseEntity.ok(Map.of("id_prodotto", rs.getLong("id_prodotto"), "bloccato", rs.getBoolean("bloccato")));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }

    @SuppressWarnings("CatchMayIgnoreException")
    @PostMapping("/insert")
    public ResponseEntity<?> insertProduct(@RequestBody Map<String, Object> body) {
        try {
            BigDecimal prezzo = optDecimal(body.get("prezzo"), null);
            if (prezzo == null) return ResponseEntity.badRequest().body(Map.of("message", "prezzo richiesto"));
            Boolean promo = optBoolean(body.get("promo"), false);
            BigDecimal prezzoScontato = null;
            if (promo != null && promo) {
                prezzoScontato = optDecimal(body.get("prezzo_scontato"), null);
                if (prezzoScontato == null) {
                    BigDecimal sconto = optDecimal(body.get("sconto"), null);
                    if (sconto != null) {
                        BigDecimal oneHundred = new BigDecimal("100");
                        BigDecimal factor = BigDecimal.ONE.subtract(sconto.divide(oneHundred));
                        prezzoScontato = prezzo.multiply(factor).setScale(2, RoundingMode.HALF_UP);
                    }
                }
            }
            jdbc.update(
                    "INSERT INTO prodotto (nome, descrizione, prezzo, prezzo_scontato, quantita_disponibile, id_categoria, id_marchio, immagine, in_vetrina, promo, bloccato) VALUES (?,?,?,?,?,?,?,?,?, ?, ?)",
                    body.get("nome"),
                    body.get("descrizione"),
                    prezzo,
                    prezzoScontato,
                    asInteger(body.get("quantita_disponibile")),
                    asLong(body.get("id_categoria")),
                    asLong(body.get("id_marchio")),
                    body.get("immagine"),
                    optBoolean(body.get("in_vetrina"), false),
                    promo,
                    optBoolean(body.get("bloccato"), false)
            );
            return ResponseEntity.ok(Map.of("message", "Prodotto inserito con successo"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }

    @SuppressWarnings("CatchMayIgnoreException")
    @GetMapping
    public ResponseEntity<?> getByName(@RequestParam("nome") String nome) {
        try {
            SqlRowSet rs = jdbc.queryForRowSet("SELECT * FROM prodotto WHERE LOWER(nome) = LOWER(?)", nome);
            List<Map<String, Object>> list = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("id_prodotto", rs.getLong("id_prodotto"));
                row.put("nome", rs.getString("nome"));
                row.put("descrizione", rs.getString("descrizione"));
                row.put("prezzo", rs.getBigDecimal("prezzo"));
                row.put("prezzo_scontato", rs.getBigDecimal("prezzo_scontato"));
                row.put("quantita_disponibile", rs.getInt("quantita_disponibile"));
                row.put("id_categoria", rs.getLong("id_categoria"));
                row.put("id_marchio", rs.getLong("id_marchio"));
                row.put("immagine", rs.getString("immagine"));
                row.put("in_vetrina", rs.getBoolean("in_vetrina"));
                row.put("promo", rs.getBoolean("promo"));
                row.put("bloccato", rs.getBoolean("bloccato"));
                list.add(row);
            }
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }

    private static BigDecimal optDecimal(Object o, BigDecimal fallback) {
        if (o == null) return fallback;
        try {
            if (o instanceof Number n) return new BigDecimal(n.toString());
            return new BigDecimal(String.valueOf(o));
        } catch (Exception e) {
            return fallback;
        }
    }

    private static Boolean optBoolean(Object o, Boolean fallback) {
        if (o == null) return fallback;
        if (o instanceof Boolean b) return b;
        String s = String.valueOf(o);
        if ("true".equalsIgnoreCase(s)) return true;
        if ("false".equalsIgnoreCase(s)) return false;
        return fallback;
    }

    @SuppressWarnings("PatternVariableCanBeUsed")
    private static Integer asInteger(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        return Integer.parseInt(String.valueOf(o));
    }

    private static Long asLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        return Long.parseLong(String.valueOf(o));
    }
}
