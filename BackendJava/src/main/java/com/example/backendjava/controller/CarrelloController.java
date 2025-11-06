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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST per la gestione del carrello acquisti.
 * Gestisce aggiunta, rimozione e visualizzazione dei prodotti nel carrello.
 */
@RestController
@RequestMapping("/api/carrello")
public class CarrelloController {
    private final JdbcTemplate jdbc;

    @Value("${server.port:8080}")
    private int serverPort;

    public CarrelloController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @SuppressWarnings("PatternVariableCanBeUsed")
    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        if (value instanceof String s && !s.isBlank()) return Long.parseLong(s);
        return null;
    }

    @SuppressWarnings("PatternVariableCanBeUsed")
    private Integer toInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.intValue();
        if (value instanceof String s && !s.isBlank()) return Integer.parseInt(s);
        return null;
    }

    private String productImageUrl(String filename) {
        if (filename == null || filename.isBlank()) {
            return "http://localhost:" + serverPort + "/api/immagine/uploads/prodotti/default.jpg";
        }
        return "http://localhost:" + serverPort + "/api/immagine/uploads/prodotti/" + filename;
    }

    /**
     * Aggiunge un prodotto al carrello dell'utente.
     * 
     * @param body Dati richiesta contenenti id_utente, id_prodotto e quantita
     * @return Risposta con conferma dell'aggiunta o messaggio di errore
     */
    @SuppressWarnings("CatchMayIgnoreException")
    @PostMapping("/aggiungi")
    public ResponseEntity<?> aggiungi(@RequestBody Map<String, Object> body) {
        try {
            if (!body.containsKey("id_utente") || !body.containsKey("id_prodotto") || !body.containsKey("quantita")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Campi obbligatori mancanti: id_utente, id_prodotto, quantita"));
            }
            Long idUtente = toLong(body.get("id_utente"));
            Long idProdotto = toLong(body.get("id_prodotto"));
            Integer quantita = toInt(body.get("quantita"));
            if (idUtente == null || idProdotto == null || quantita == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Parametri non validi"));
            }
            if (quantita <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Quantità non valida"));
            }

        // Verifica disponibilità prodotto (uso solo quantita_disponibile per massima compatibilità schema)
        SqlRowSet prod = jdbc.queryForRowSet(
            "SELECT quantita_disponibile FROM prodotto WHERE id_prodotto = ?",
            idProdotto
        );
            if (!prod.next()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Prodotto non trovato"));
            }
            int disp = prod.getInt("quantita_disponibile");
            if (disp <= 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Prodotto non disponibile"));
            }

            SqlRowSet esistente = jdbc.queryForRowSet(
                    "SELECT 1 FROM carrello WHERE id_utente = ? AND id_prodotto = ?",
                    idUtente, idProdotto
            );
            if (esistente.next()) {
                jdbc.update(
                        "UPDATE carrello SET quantita = quantita + ? WHERE id_utente = ? AND id_prodotto = ?",
                        quantita, idUtente, idProdotto
                );
            } else {
                jdbc.update(
                        "INSERT INTO carrello (id_utente, id_prodotto, quantita) VALUES (?, ?, ?)",
                        idUtente, idProdotto, quantita
                );
            }
            return ResponseEntity.ok(Map.of("success", true, "message", "Prodotto aggiunto al carrello"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Errore nel salvataggio del carrello", "detail", e.getMessage()));
        }
    }

    @SuppressWarnings("CatchMayIgnoreException")
    @PostMapping("/aggiungi-pacchetto")
    public ResponseEntity<?> aggiungiPacchetto(@RequestBody Map<String, Object> body) {
        try {
            if (!body.containsKey("id_utente") || !body.containsKey("id_pacchetto") || !body.containsKey("quantita")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Campi obbligatori mancanti: id_utente, id_pacchetto, quantita"));
            }
            Long idUtente = toLong(body.get("id_utente"));
            Long idPacchetto = toLong(body.get("id_pacchetto"));
            Integer quantita = toInt(body.get("quantita"));
            if (idUtente == null || idPacchetto == null || quantita == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Parametri non validi"));
            }
            if (quantita <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Quantità non valida"));
            }

            SqlRowSet esistente = jdbc.queryForRowSet(
                    "SELECT 1 FROM carrello_pacchetto WHERE id_utente = ? AND id_pacchetto = ?",
                    idUtente, idPacchetto
            );
            if (esistente.next()) {
                jdbc.update(
                        "UPDATE carrello_pacchetto SET quantita = quantita + ? WHERE id_utente = ? AND id_pacchetto = ?",
                        quantita, idUtente, idPacchetto
                );
            } else {
                jdbc.update(
                        "INSERT INTO carrello_pacchetto (id_utente, id_pacchetto, quantita) VALUES (?, ?, ?)",
                        idUtente, idPacchetto, quantita
                );
            }
            return ResponseEntity.ok(Map.of("success", true, "message", "Pacchetto aggiunto al carrello"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Errore nel salvataggio del carrello", "detail", e.getMessage()));
        }
    }

    @GetMapping("/{id_utente}")
    public List<Map<String, Object>> getCarrello(@PathVariable("id_utente") long idUtente) {
        List<Map<String, Object>> out = new ArrayList<>();
        // Prodotti
        String sqlProd = "SELECT c.id_prodotto, c.quantita, p.nome, p.prezzo, p.prezzo_scontato, p.promo, p.immagine FROM carrello c JOIN prodotto p ON c.id_prodotto = p.id_prodotto WHERE c.id_utente = ? ORDER BY c.id_prodotto";
        SqlRowSet rsProd = jdbc.queryForRowSet(sqlProd, idUtente);
    while (rsProd.next()) {
        double prezzo = rsProd.getBoolean("promo") && rsProd.getBigDecimal("prezzo_scontato") != null
            ? java.util.Optional.ofNullable(rsProd.getBigDecimal("prezzo_scontato")).orElse(java.math.BigDecimal.ZERO).doubleValue()
            : java.util.Optional.ofNullable(rsProd.getBigDecimal("prezzo")).orElse(java.math.BigDecimal.ZERO).doubleValue();
            Map<String, Object> item = new HashMap<>();
            item.put("tipo", "prodotto");
            item.put("id_prodotto", rsProd.getLong("id_prodotto"));
            item.put("quantita", rsProd.getInt("quantita"));
            item.put("nome", rsProd.getString("nome"));
            item.put("prezzo", Math.round(prezzo * 100.0) / 100.0);
            String img = rsProd.getString("immagine");
            item.put("immagine", img);
            item.put("immagine_url", productImageUrl(img));
            out.add(item);
        }
        // Pacchetti
        String sqlPkt = "SELECT cp.id_pacchetto, cp.quantita, pt.nome, pt.prezzo_totale FROM carrello_pacchetto cp JOIN pacchetto_tematico pt ON cp.id_pacchetto = pt.id_pacchetto WHERE cp.id_utente = ? ORDER BY cp.id_pacchetto";
        SqlRowSet rsPkt = jdbc.queryForRowSet(sqlPkt, idUtente);
        while (rsPkt.next()) {
            long idPacchetto = rsPkt.getLong("id_pacchetto");
            SqlRowSet prodotti = jdbc.queryForRowSet(
                    "SELECT p.prezzo, p.prezzo_scontato, p.promo, pp.quantita FROM prodotto_pacchetto pp JOIN prodotto p ON pp.id_prodotto = p.id_prodotto WHERE pp.id_pacchetto = ?",
                    idPacchetto
            );
            double totaleEff = 0.0;
            while (prodotti.next()) {
                double prezzo = prodotti.getBoolean("promo") && prodotti.getBigDecimal("prezzo_scontato") != null
                        ? java.util.Optional.ofNullable(prodotti.getBigDecimal("prezzo_scontato")).orElse(java.math.BigDecimal.ZERO).doubleValue()
                        : java.util.Optional.ofNullable(prodotti.getBigDecimal("prezzo")).orElse(java.math.BigDecimal.ZERO).doubleValue();
                int q = Optional.ofNullable((Integer) prodotti.getObject("quantita")).orElse(1);
                totaleEff += prezzo * q;
            }
            totaleEff = Math.round(totaleEff * 100.0) / 100.0;
            double prezzoScontatoPkt = Math.round((totaleEff * 0.85) * 100.0) / 100.0;
            Map<String, Object> item = new HashMap<>();
            item.put("tipo", "pacchetto");
            item.put("id_pacchetto", idPacchetto);
            item.put("quantita", rsPkt.getInt("quantita"));
            item.put("nome", rsPkt.getString("nome"));
            item.put("prezzo", prezzoScontatoPkt);
            out.add(item);
        }
        return out;
    }

    @DeleteMapping("/rimuovi/{id_utente}/{id_prodotto}")
    public Map<String, Object> rimuoviProdotto(@PathVariable long id_utente, @PathVariable long id_prodotto) {
        jdbc.update("DELETE FROM carrello WHERE id_utente = ? AND id_prodotto = ?", id_utente, id_prodotto);
        return Map.of("success", true, "message", "Prodotto rimosso dal carrello");
    }

    @DeleteMapping("/rimuoviPacchetto/{id_utente}/{id_pacchetto}")
    public Map<String, Object> rimuoviPacchetto(@PathVariable long id_utente, @PathVariable long id_pacchetto) {
        jdbc.update("DELETE FROM carrello_pacchetto WHERE id_utente = ? AND id_pacchetto = ?", id_utente, id_pacchetto);
        return Map.of("success", true, "message", "Pacchetto rimosso dal carrello");
    }

    @PutMapping("/aggiorna")
    public Map<String, Object> aggiorna(@RequestBody Map<String, Object> body) {
        Long idUtente = ((Number) body.get("id_utente")).longValue();
        Long idProdotto = ((Number) body.get("id_prodotto")).longValue();
        int quantita = ((Number) body.get("quantita")).intValue();
        if (quantita <= 0) {
            jdbc.update("DELETE FROM carrello WHERE id_utente = ? AND id_prodotto = ?", idUtente, idProdotto);
        } else {
            jdbc.update("UPDATE carrello SET quantita = ? WHERE id_utente = ? AND id_prodotto = ?", quantita, idUtente, idProdotto);
        }
        return Map.of("success", true, "message", "Carrello aggiornato");
    }

    @PutMapping("/aggiornaPacchetto")
    public Map<String, Object> aggiornaPacchetto(@RequestBody Map<String, Object> body) {
        Long idUtente = ((Number) body.get("id_utente")).longValue();
        Long idPacchetto = ((Number) body.get("id_pacchetto")).longValue();
        int quantita = ((Number) body.get("quantita")).intValue();
        if (quantita <= 0) {
            jdbc.update("DELETE FROM carrello_pacchetto WHERE id_utente = ? AND id_pacchetto = ?", idUtente, idPacchetto);
        } else {
            jdbc.update("UPDATE carrello_pacchetto SET quantita = ? WHERE id_utente = ? AND id_pacchetto = ?", quantita, idUtente, idPacchetto);
        }
        return Map.of("success", true, "message", "Pacchetto aggiornato");
    }
}
