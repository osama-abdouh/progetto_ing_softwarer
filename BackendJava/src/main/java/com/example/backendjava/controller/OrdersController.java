package com.example.backendjava.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST per la gestione degli ordini.
 * Fornisce endpoint per visualizzare lo storico ordini e i dettagli.
 */
@SuppressWarnings("null")
@RestController
@RequestMapping("/api/orders")
public class OrdersController {
    private final JdbcTemplate jdbc;

    @Value("${server.port:8080}")
    private int serverPort;

    public OrdersController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private String productImageUrl(String filename) {
        if (filename == null || filename.isBlank()) {
            return "http://localhost:" + serverPort + "/api/immagine/uploads/prodotti/default.jpg";
        }
        return "http://localhost:" + serverPort + "/api/immagine/uploads/prodotti/" + filename;
    }

    @GetMapping("/user/{userId}")
    public List<Map<String, Object>> getOrdersByUser(@PathVariable("userId") long userId) {
        String sql = "SELECT * FROM ordini WHERE user_id = ? ORDER BY data_ordine DESC";
        List<Map<String, Object>> list = new ArrayList<>();
        SqlRowSet rs = jdbc.queryForRowSet(sql, userId);
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            for (String col : rs.getMetaData().getColumnNames()) row.put(col, rs.getObject(col));
            list.add(row);
        }
        return list;
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderDetail(@PathVariable("orderId") long orderId) {
        SqlRowSet ordineResult = jdbc.queryForRowSet("SELECT id, user_id, indirizzo_consegna, totale_originale, sconto_coupon, totale, stato, data_ordine, metodo_pagamento, nome_intestatario, numero_carta_mascherato, coupon_utilizzato FROM ordini WHERE id = ?", orderId);
        if (!ordineResult.next()) return ResponseEntity.status(404).body(Map.of("error", "Ordine non trovato"));
        Map<String, Object> ordine = new HashMap<>();
        for (String col : ordineResult.getMetaData().getColumnNames()) ordine.put(col, ordineResult.getObject(col));

        List<Map<String, Object>> prodotti = new ArrayList<>();
        SqlRowSet prodottiResult = jdbc.queryForRowSet(
                "SELECT p.nome, p.immagine, op.quantita, op.prezzo AS prezzo_unitario, (op.quantita * op.prezzo) AS subtotale FROM ordine_prodotti op JOIN prodotto p ON op.prodotto_id = p.id_prodotto WHERE op.ordine_id = ?",
                orderId
        );
        while (prodottiResult.next()) {
            Map<String, Object> r = new HashMap<>();
            r.put("nome", prodottiResult.getString("nome"));
            String img = prodottiResult.getString("immagine");
            r.put("immagine", img);
            r.put("quantita", prodottiResult.getInt("quantita"));
            r.put("prezzo_unitario", prodottiResult.getBigDecimal("prezzo_unitario"));
            r.put("subtotale", prodottiResult.getBigDecimal("subtotale"));
            r.put("immagine_url", productImageUrl(img));
            prodotti.add(r);
        }
        return ResponseEntity.ok(Map.of("ordine", ordine, "prodotti", prodotti));
    }

    @GetMapping("/tracking/{id}")
    public ResponseEntity<?> getTracking(@PathVariable("id") long ordineId) {
        SqlRowSet rs = jdbc.queryForRowSet("SELECT * FROM tracking_ordine WHERE id_ordine = ? ORDER BY data_aggiornamento DESC LIMIT 1", ordineId);
        if (!rs.next()) return ResponseEntity.status(404).body(Map.of("error", "Tracking non trovato"));
        Map<String, Object> row = new HashMap<>();
        for (String col : rs.getMetaData().getColumnNames()) row.put(col, rs.getObject(col));
        return ResponseEntity.ok(row);
    }
}
