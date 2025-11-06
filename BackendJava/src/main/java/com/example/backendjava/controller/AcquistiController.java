package com.example.backendjava.controller;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/acquisti")
public class AcquistiController {
    private final JdbcTemplate jdbc;

    public AcquistiController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private String maskCard(String number) {
        if (number == null) return null;
        String n = number.replaceAll("\\s", "");
        if (n.length() < 4) return "****";
        return "**** **** **** " + n.substring(n.length() - 4);
    }

    @PostMapping("/checkout")
    @Transactional
    public Map<String, Object> checkout(@RequestBody Map<String, Object> body) {
        Long idUtente = ((Number) body.get("id_utente")).longValue();
        String metodoPagamento = (String) body.get("metodo_pagamento");
        String nomeIntestatario = (String) body.get("nome_intestatario");
        String numeroCarta = (String) body.get("numero_carta");
        String indirizzoConsegna = (String) body.get("indirizzo_consegna");

        Number totaleOriginaleN = (Number) body.getOrDefault("totale_originale", null);
        Number scontoApplicatoN = (Number) body.getOrDefault("sconto_applicato", 0);
        Number totaleN = (Number) body.getOrDefault("totale", null);
        @SuppressWarnings("unchecked")
        Map<String, Object> coupon = (Map<String, Object>) body.get("coupon_applicato");

        if (indirizzoConsegna == null || indirizzoConsegna.isBlank()) throw new IllegalArgumentException("Indirizzo di consegna mancante");

        // 1. Carrello prodotti con info prodotto/promo
        String cartSql = "SELECT c.*, p.nome, p.prezzo, p.prezzo_scontato, p.promo, p.quantita_disponibile FROM carrello c JOIN prodotto p ON c.id_prodotto = p.id_prodotto WHERE c.id_utente = ?";
        List<Map<String, Object>> cart = new ArrayList<>();
        var rs = jdbc.queryForRowSet(cartSql, idUtente);
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            row.put("tipo", "prodotto");
            row.put("id_prodotto", rs.getLong("id_prodotto"));
            row.put("quantita", rs.getInt("quantita"));
            row.put("nome", rs.getString("nome"));
            row.put("prezzo", rs.getBigDecimal("prezzo"));
            row.put("prezzo_scontato", rs.getBigDecimal("prezzo_scontato"));
            row.put("promo", rs.getBoolean("promo"));
            row.put("quantita_disponibile", rs.getInt("quantita_disponibile"));
            cart.add(row);
        }

        // 2. Carrello pacchetti
        String packetSql = "SELECT cp.id_pacchetto, cp.quantita, pt.nome, pt.prezzo_totale FROM carrello_pacchetto cp JOIN pacchetto_tematico pt ON cp.id_pacchetto = pt.id_pacchetto WHERE cp.id_utente = ?";
        var rsPkt = jdbc.queryForRowSet(packetSql, idUtente);
        while (rsPkt.next()) {
            Map<String, Object> row = new HashMap<>();
            row.put("tipo", "pacchetto");
            row.put("id_pacchetto", rsPkt.getLong("id_pacchetto"));
            row.put("quantita", rsPkt.getInt("quantita"));
            row.put("nome", rsPkt.getString("nome"));
            row.put("prezzo", rsPkt.getBigDecimal("prezzo_totale"));
            row.put("prezzo_scontato", null);
            row.put("promo", false);
            row.put("quantita_disponibile", 999); // I pacchetti non hanno limite di disponibilità
            cart.add(row);
        }

        if (cart.isEmpty()) throw new IllegalStateException("Carrello vuoto");

        // 2. Verifica disponibilità e calcola totale
        BigDecimal totaleCalcolato = BigDecimal.ZERO;
        for (Map<String, Object> item : cart) {
            int qRichiesta = (int) item.get("quantita");
            int qDisp = (int) item.get("quantita_disponibile");
            if (qRichiesta > qDisp) {
                throw new IllegalStateException("Quantità non disponibile per " + item.get("nome") + ". Disponibili: " + qDisp);
            }
            BigDecimal prezzo = (boolean) item.get("promo") && item.get("prezzo_scontato") != null
                    ? (BigDecimal) item.get("prezzo_scontato")
                    : (BigDecimal) item.get("prezzo");
            item.put("prezzo_eff", prezzo);
            totaleCalcolato = totaleCalcolato.add(prezzo.multiply(BigDecimal.valueOf(qRichiesta)));
        }

        BigDecimal totaleOriginale = totaleOriginaleN != null ? new BigDecimal(totaleOriginaleN.toString()) : totaleCalcolato;
        BigDecimal scontoFinale = scontoApplicatoN != null ? new BigDecimal(scontoApplicatoN.toString()) : BigDecimal.ZERO;
        BigDecimal totaleFinale = totaleN != null ? new BigDecimal(totaleN.toString()) : totaleCalcolato;

        // 3. Inserisci ordine
        String couponCodice = coupon != null ? (String) coupon.get("codice") : null;
        String insertOrd = "INSERT INTO ordini (user_id, indirizzo_consegna, totale_originale, sconto_coupon, totale, stato, data_ordine, metodo_pagamento, nome_intestatario, numero_carta_mascherato, coupon_utilizzato) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";
        Long ordineId = jdbc.queryForObject(insertOrd, Long.class,
                idUtente,
                indirizzoConsegna,
                totaleOriginale,
                scontoFinale,
                totaleFinale,
                "In lavorazione",
                Timestamp.from(Instant.now()),
                metodoPagamento,
                nomeIntestatario,
                maskCard(numeroCarta),
                couponCodice
        );

        // 4. Inserisci righe ordine + aggiorna disponibilità
        String insertRiga = "INSERT INTO ordine_prodotti (ordine_id, prodotto_id, quantita, prezzo) VALUES (?, ?, ?, ?)";
        String updProd = "UPDATE prodotto SET quantita_disponibile = quantita_disponibile - ? WHERE id_prodotto = ?";
        
        for (Map<String, Object> item : cart) {
            String tipo = (String) item.get("tipo");
            int qCarrello = (int) item.get("quantita");
            BigDecimal prezzo = (BigDecimal) item.get("prezzo_eff");
            
            if ("prodotto".equals(tipo)) {
                Long idProd = (Long) item.get("id_prodotto");
                jdbc.update(insertRiga, ordineId, idProd, qCarrello, prezzo);
                jdbc.update(updProd, qCarrello, idProd);
            } else if ("pacchetto".equals(tipo)) {
                // Per i pacchetti, espandi nei prodotti costituenti
                Long idPkt = (Long) item.get("id_pacchetto");
                String prodottiPktSql = "SELECT pp.id_prodotto, pp.quantita, p.prezzo, p.prezzo_scontato, p.promo FROM prodotto_pacchetto pp JOIN prodotto p ON pp.id_prodotto = p.id_prodotto WHERE pp.id_pacchetto = ?";
                var rsProdPkt = jdbc.queryForRowSet(prodottiPktSql, idPkt);
                while (rsProdPkt.next()) {
                    Long idProdPkt = rsProdPkt.getLong("id_prodotto");
                    int qProdInPkt = rsProdPkt.getInt("quantita");
                    BigDecimal prezzoProd = rsProdPkt.getBoolean("promo") && rsProdPkt.getBigDecimal("prezzo_scontato") != null
                            ? rsProdPkt.getBigDecimal("prezzo_scontato")
                            : rsProdPkt.getBigDecimal("prezzo");
                    
                    // Moltiplica quantità: qCarrello pacchetti * qProdInPkt prodotti per pacchetto
                    int qTotale = qCarrello * qProdInPkt;
                    jdbc.update(insertRiga, ordineId, idProdPkt, qTotale, prezzoProd);
                    jdbc.update(updProd, qTotale, idProdPkt);
                }
            }
        }

        // 5. Svuota carrello (prodotti e pacchetti)
        jdbc.update("DELETE FROM carrello WHERE id_utente = ?", idUtente);
        jdbc.update("DELETE FROM carrello_pacchetto WHERE id_utente = ?", idUtente);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", "Acquisto completato con successo");
        resp.put("ordine", Map.of("id", ordineId));
        resp.put("totale", totaleFinale);
        return resp;
    }
}
