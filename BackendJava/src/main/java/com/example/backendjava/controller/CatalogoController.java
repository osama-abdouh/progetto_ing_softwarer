package com.example.backendjava.controller;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST per la gestione del catalogo prodotti.
 * Fornisce endpoint per visualizzare categorie, prodotti, marchi e ricerca.
 */
@RestController
@RequestMapping("/api/catalogo")
public class CatalogoController {

    private final JdbcTemplate jdbc;

    @Value("${server.port:8080}")
    private int serverPort;

    public CatalogoController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private String productImageUrl(String filename) {
        if (filename == null || filename.isBlank()) {
            return "http://localhost:" + serverPort + "/api/immagine/uploads/prodotti/default.jpg";
        }
        return "http://localhost:" + serverPort + "/api/immagine/uploads/prodotti/" + filename;
    }

    /**
     * Restituisce l'elenco di tutte le categorie disponibili con le relative immagini.
     * 
     * @return Lista di categorie con id, nome, immagine e URL immagine
     */
    @GetMapping("/prodotti")
    public List<Map<String, Object>> getCategorie() {
        String sql = "SELECT nome, id_categoria, immagine FROM categoria";
        List<Map<String, Object>> list = new ArrayList<>();
        SqlRowSet rs = jdbc.queryForRowSet(sql);
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            row.put("nome", rs.getString("nome"));
            row.put("id_categoria", rs.getString("id_categoria"));
            String img = rs.getString("immagine");
            row.put("immagine", img);
            row.put("immagine_url", img != null ? ("http://localhost:" + serverPort + "/api/immagine/uploads/categorie/" + img) : null);
            list.add(row);
        }
        return list;
    }

    /**
     * Recupera tutti i prodotti appartenenti a una specifica categoria.
     * 
     * @param nomeCategoria Nome della categoria da filtrare
     * @return Lista di prodotti con dettagli (prezzo, immagine, disponibilità, marchio)
     */
    @GetMapping("/prodotti/categoria/{nome}")
    public List<Map<String, Object>> getProdottiPerCategoria(@PathVariable("nome") String nomeCategoria) {
        String sql = "SELECT p.id_prodotto, p.nome, CASE WHEN p.promo = TRUE AND p.prezzo_scontato IS NOT NULL THEN p.prezzo_scontato ELSE p.prezzo END AS prezzo, p.prezzo_scontato, p.descrizione, p.immagine, p.quantita_disponibile, m.nome AS marchio, c.nome AS categoria FROM prodotto p LEFT JOIN categoria c ON p.id_categoria = c.id_categoria LEFT JOIN marchio m ON p.id_marchio = m.id_marchio WHERE c.nome = ? AND p.bloccato = false";
        List<Map<String, Object>> list = new ArrayList<>();
        SqlRowSet rs = jdbc.queryForRowSet(sql, nomeCategoria);
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            row.put("id_prodotto", rs.getString("id_prodotto"));
            row.put("nome", rs.getString("nome"));
            row.put("prezzo", rs.getBigDecimal("prezzo"));
            row.put("prezzo_scontato", rs.getBigDecimal("prezzo_scontato"));
            row.put("descrizione", rs.getString("descrizione"));
            String img = rs.getString("immagine");
            row.put("immagine", img);
            row.put("quantita_disponibile", rs.getInt("quantita_disponibile"));
            row.put("marchio", rs.getString("marchio"));
            row.put("categoria", rs.getString("categoria"));
            row.put("immagine_url", productImageUrl(img));
            list.add(row);
        }
        return list;
    }

    /**
     * Fornisce suggerimenti di ricerca basati sulla query dell'utente.
     * Cerca per nome prodotto, marchio o categoria con priorità ai risultati più rilevanti.
     * 
     * @param q Query di ricerca
     * @param limit Numero massimo di risultati (default: 5)
     * @return Lista di prodotti suggeriti
     */
    @GetMapping("/search/suggestions")
    public List<Map<String, Object>> getSearchSuggestions(@RequestParam("q") String q,
        @RequestParam(value = "limit", required = false, defaultValue = "5") int limit) {
        if (q == null || q.trim().length() < 1) return List.of();
        String searchTerm = "%" + q.trim().toLowerCase() + "%";
        String startsWith = q.trim().toLowerCase() + "%";
        String sql = "SELECT p.id_prodotto, p.nome, CASE WHEN p.promo = TRUE AND p.prezzo_scontato IS NOT NULL THEN p.prezzo_scontato ELSE p.prezzo END AS prezzo, p.prezzo_scontato, p.immagine, m.nome AS marchio, c.nome AS categoria FROM prodotto p LEFT JOIN categoria c ON p.id_categoria = c.id_categoria LEFT JOIN marchio m ON p.id_marchio = m.id_marchio WHERE (LOWER(p.nome) LIKE ? OR LOWER(m.nome) LIKE ? OR LOWER(c.nome) LIKE ?) AND p.quantita_disponibile > 0 AND p.bloccato = false ORDER BY CASE WHEN LOWER(p.nome) LIKE ? THEN 1 WHEN LOWER(p.nome) LIKE ? THEN 2 WHEN LOWER(m.nome) LIKE ? THEN 3 ELSE 4 END, p.nome LIMIT ?";
        List<Map<String, Object>> list = new ArrayList<>();
        SqlRowSet rs = jdbc.queryForRowSet(sql, searchTerm, searchTerm, searchTerm, startsWith, searchTerm, searchTerm, limit);
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            row.put("id_prodotto", rs.getString("id_prodotto"));
            row.put("nome", rs.getString("nome"));
            row.put("prezzo", rs.getBigDecimal("prezzo"));
            row.put("prezzo_scontato", rs.getBigDecimal("prezzo_scontato"));
            String img = rs.getString("immagine");
            row.put("immagine", img);
            row.put("marchio", rs.getString("marchio"));
            row.put("categoria", rs.getString("categoria"));
            row.put("immagine_url", productImageUrl(img));
            list.add(row);
        }
        return list;
    }

    // GET /api/catalogo/popular
    @GetMapping("/popular")
    public List<Map<String, Object>> getPopular() {
        String sql = "SELECT p.id_prodotto, p.nome, CASE WHEN p.promo = TRUE AND p.prezzo_scontato IS NOT NULL THEN p.prezzo_scontato ELSE p.prezzo END AS prezzo, p.prezzo_scontato, p.descrizione, p.immagine, p.quantita_disponibile, COALESCE(SUM(op.quantita), 0) AS total_purchased FROM prodotto p LEFT JOIN ordine_prodotti op ON op.prodotto_id = p.id_prodotto WHERE p.quantita_disponibile > 0 AND p.bloccato = false GROUP BY p.id_prodotto, p.nome, p.prezzo, p.prezzo_scontato, p.promo, p.descrizione, p.immagine, p.quantita_disponibile ORDER BY total_purchased DESC, p.nome LIMIT 3";
        List<Map<String, Object>> list = new ArrayList<>();
        SqlRowSet rs = jdbc.queryForRowSet(sql);
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            row.put("id_prodotto", rs.getString("id_prodotto"));
            row.put("nome", rs.getString("nome"));
            row.put("prezzo", rs.getBigDecimal("prezzo"));
            row.put("prezzo_scontato", rs.getBigDecimal("prezzo_scontato"));
            row.put("descrizione", rs.getString("descrizione"));
            String img = rs.getString("immagine");
            row.put("immagine", img);
            row.put("quantita_disponibile", rs.getInt("quantita_disponibile"));
            row.put("total_purchased", rs.getLong("total_purchased"));
            row.put("immagine_url", productImageUrl(img));
            list.add(row);
        }
        return list;
    }

    // GET /api/catalogo/brand
    @GetMapping("/brand")
    public List<Map<String, Object>> getBrand() {
        String sql = "SELECT nome, id_marchio FROM marchio ORDER BY nome";
        List<Map<String, Object>> list = new ArrayList<>();
        SqlRowSet rs = jdbc.queryForRowSet(sql);
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            row.put("nome", rs.getString("nome"));
            row.put("id_marchio", rs.getString("id_marchio"));
            list.add(row);
        }
        return list;
    }

    // GET /api/catalogo/prodotto/{id}
    @GetMapping("/prodotto/{id}")
    public ResponseEntity<?> getProdottoById(@PathVariable("id") String id) {
        // Prova a interpretare l'id come numero per evitare problemi di tipo con il driver
        Long idLong = null;
        try {
            idLong = Long.parseLong(String.valueOf(id));
        } catch (NumberFormatException ignored) {
            // Se non è numerico, lasciamo null e useremo la stringa come fallback
        }
        try {
            String sql = "SELECT p.id_prodotto, p.nome, CASE WHEN p.promo = TRUE AND p.prezzo_scontato IS NOT NULL THEN p.prezzo_scontato ELSE p.prezzo END AS prezzo, p.prezzo_scontato, p.descrizione, p.immagine, p.quantita_disponibile, m.nome AS marchio, c.nome AS categoria FROM prodotto p LEFT JOIN categoria c ON p.id_categoria = c.id_categoria LEFT JOIN marchio m ON p.id_marchio = m.id_marchio WHERE p.id_prodotto = ? AND p.bloccato = false LIMIT 1";
            SqlRowSet rs = (idLong != null) ? jdbc.queryForRowSet(sql, idLong) : jdbc.queryForRowSet(sql, id);
            if (!rs.next()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Prodotto non trovato"));
            }
            Map<String, Object> row = new HashMap<>();
            row.put("id_prodotto", rs.getString("id_prodotto"));
            row.put("nome", rs.getString("nome"));
            row.put("prezzo", rs.getBigDecimal("prezzo"));
            row.put("prezzo_scontato", rs.getBigDecimal("prezzo_scontato"));
            row.put("descrizione", rs.getString("descrizione"));
            String img = rs.getString("immagine");
            row.put("immagine", img);
            row.put("quantita_disponibile", rs.getInt("quantita_disponibile"));
            row.put("marchio", rs.getString("marchio"));
            row.put("categoria", rs.getString("categoria"));
            row.put("immagine_url", productImageUrl(img));
            return ResponseEntity.ok(row);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @SuppressWarnings("CatchMayIgnoreException")
    // GET /api/catalogo/vetrina
    @GetMapping("/vetrina")
    public List<Map<String, Object>> getVetrina() {
        String sql = "SELECT p.id_prodotto, p.nome, CASE WHEN p.promo = TRUE AND p.prezzo_scontato IS NOT NULL THEN p.prezzo_scontato ELSE p.prezzo END AS prezzo, p.prezzo_scontato, p.descrizione, p.immagine, p.quantita_disponibile, m.nome AS marchio, c.nome AS categoria FROM prodotto p LEFT JOIN categoria c ON p.id_categoria = c.id_categoria LEFT JOIN marchio m ON p.id_marchio = m.id_marchio WHERE p.in_vetrina = TRUE AND p.bloccato = false";
        List<Map<String, Object>> list = new ArrayList<>();
        SqlRowSet rs = jdbc.queryForRowSet(sql);
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            row.put("id_prodotto", rs.getString("id_prodotto"));
            row.put("nome", rs.getString("nome"));
            row.put("prezzo", rs.getBigDecimal("prezzo"));
            row.put("prezzo_scontato", rs.getBigDecimal("prezzo_scontato"));
            row.put("descrizione", rs.getString("descrizione"));
            String img = rs.getString("immagine");
            row.put("immagine", img);
            row.put("quantita_disponibile", rs.getInt("quantita_disponibile"));
            row.put("marchio", rs.getString("marchio"));
            row.put("categoria", rs.getString("categoria"));
            row.put("immagine_url", productImageUrl(img));
            list.add(row);
        }
        return list;
    }

    // GET /api/catalogo/prodotti/ricerca?q=...
    @GetMapping("/prodotti/ricerca")
    public List<Map<String, Object>> searchProducts(@RequestParam("q") String q) {
        if (q == null || q.trim().length() < 1) return List.of();
        String searchTerm = "%" + q.trim().toLowerCase() + "%";
        String startsWith = q.trim().toLowerCase() + "%";
        String sql = "SELECT p.id_prodotto, p.nome, p.descrizione, CASE WHEN p.promo = TRUE AND p.prezzo_scontato IS NOT NULL THEN p.prezzo_scontato ELSE p.prezzo END AS prezzo, p.prezzo_scontato, p.promo, p.immagine, p.quantita_disponibile, m.nome AS marchio, c.nome AS categoria FROM prodotto p LEFT JOIN categoria c ON p.id_categoria = c.id_categoria LEFT JOIN marchio m ON p.id_marchio = m.id_marchio WHERE (LOWER(p.nome) LIKE ? OR LOWER(m.nome) LIKE ? OR LOWER(c.nome) LIKE ? OR LOWER(p.descrizione) LIKE ?) AND p.quantita_disponibile > 0 AND p.bloccato = false ORDER BY CASE WHEN LOWER(p.nome) LIKE ? THEN 1 WHEN LOWER(m.nome) LIKE ? THEN 2 WHEN LOWER(p.nome) LIKE ? THEN 3 WHEN LOWER(m.nome) LIKE ? THEN 4 ELSE 5 END, p.nome";
        List<Map<String, Object>> list = new ArrayList<>();
        SqlRowSet rs = jdbc.queryForRowSet(sql, searchTerm, searchTerm, searchTerm, searchTerm, startsWith, startsWith, searchTerm, searchTerm);
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            row.put("id_prodotto", rs.getString("id_prodotto"));
            row.put("nome", rs.getString("nome"));
            row.put("descrizione", rs.getString("descrizione"));
            row.put("prezzo", rs.getBigDecimal("prezzo"));
            row.put("prezzo_scontato", rs.getBigDecimal("prezzo_scontato"));
            row.put("promo", rs.getBoolean("promo"));
            String img = rs.getString("immagine");
            row.put("immagine", img);
            row.put("quantita_disponibile", rs.getInt("quantita_disponibile"));
            row.put("marchio", rs.getString("marchio"));
            row.put("categoria", rs.getString("categoria"));
            row.put("immagine_url", productImageUrl(img));
            list.add(row);
        }
        return list;
    }
}
