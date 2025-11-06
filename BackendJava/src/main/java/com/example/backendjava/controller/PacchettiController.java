package com.example.backendjava.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@RestController
@RequestMapping("/api/pacchetti")
public class PacchettiController {
    private final JdbcTemplate jdbc;

    public PacchettiController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("")
    public List<Map<String, Object>> getPacchetti() {
        String sql = "SELECT id_pacchetto, nome, descrizione, prezzo_totale FROM pacchetto_tematico ORDER BY id_pacchetto";
        List<Map<String, Object>> res = new ArrayList<>();
        SqlRowSet rs = jdbc.queryForRowSet(sql);
        while (rs.next()) {
            long id = rs.getLong("id_pacchetto");
            String nome = rs.getString("nome");
            String descrizione = rs.getString("descrizione");
            BigDecimal prezzoTot = rs.getBigDecimal("prezzo_totale");

            SqlRowSet prodotti = jdbc.queryForRowSet(
                    "SELECT p.prezzo, p.prezzo_scontato, p.promo, pp.quantita FROM prodotto_pacchetto pp JOIN prodotto p ON pp.id_prodotto = p.id_prodotto WHERE pp.id_pacchetto = ?",
                    id
            );
            BigDecimal totaleEff = BigDecimal.ZERO;
            while (prodotti.next()) {
                BigDecimal prezzo = prodotti.getBoolean("promo") && prodotti.getBigDecimal("prezzo_scontato") != null
                        ? prodotti.getBigDecimal("prezzo_scontato")
                        : prodotti.getBigDecimal("prezzo");
                int quantita = Optional.ofNullable((Integer) prodotti.getObject("quantita")).orElse(1);
                if (prezzo != null) {
                    totaleEff = totaleEff.add(prezzo.multiply(BigDecimal.valueOf(quantita)));
                }
            }
            totaleEff = totaleEff.setScale(2, RoundingMode.HALF_UP);
            BigDecimal prezzoScontato = totaleEff.multiply(new BigDecimal("0.85")).setScale(2, RoundingMode.HALF_UP);

            Map<String, Object> pkt = new HashMap<>();
            pkt.put("id_pacchetto", id);
            pkt.put("nome", nome);
            pkt.put("descrizione", descrizione);
            pkt.put("prezzo_totale", prezzoTot);
            pkt.put("prezzo_originale", prezzoTot);
            pkt.put("prezzo_scontato", prezzoScontato);
            res.add(pkt);
        }
        return res;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getPacchettoDettaglio(@PathVariable("id") long id) {
        SqlRowSet head = jdbc.queryForRowSet("SELECT * FROM pacchetto_tematico WHERE id_pacchetto = ?", id);
        if (!head.next()) throw new NoSuchElementException("Pacchetto non trovato");
        Map<String, Object> pacchetto = new HashMap<>();
        pacchetto.put("id_pacchetto", head.getLong("id_pacchetto"));
        pacchetto.put("nome", head.getString("nome"));
        pacchetto.put("descrizione", head.getString("descrizione"));
        BigDecimal prezzoTot = Optional.ofNullable(head.getBigDecimal("prezzo_totale")).orElse(BigDecimal.ZERO);

        String prodottiSql = "SELECT p.id_prodotto, p.nome, p.descrizione, p.prezzo, p.prezzo_scontato, p.promo, pp.quantita, m.nome as marchio, c.nome as categoria FROM prodotto_pacchetto pp JOIN prodotto p ON pp.id_prodotto = p.id_prodotto LEFT JOIN marchio m ON p.id_marchio = m.id_marchio LEFT JOIN categoria c ON p.id_categoria = c.id_categoria WHERE pp.id_pacchetto = ?";
        SqlRowSet prodotti = jdbc.queryForRowSet(prodottiSql, id);
        List<Map<String, Object>> prodottiList = new ArrayList<>();
        BigDecimal totaleEff = BigDecimal.ZERO;
        while (prodotti.next()) {
            Map<String, Object> pr = new HashMap<>();
            pr.put("id_prodotto", prodotti.getLong("id_prodotto"));
            pr.put("nome", prodotti.getString("nome"));
            pr.put("descrizione", prodotti.getString("descrizione"));
            pr.put("prezzo", prodotti.getBigDecimal("prezzo"));
            pr.put("prezzo_scontato", prodotti.getBigDecimal("prezzo_scontato"));
            pr.put("promo", prodotti.getBoolean("promo"));
            pr.put("quantita", prodotti.getInt("quantita"));
            pr.put("marchio", prodotti.getString("marchio"));
            pr.put("categoria", prodotti.getString("categoria"));
            prodottiList.add(pr);

            BigDecimal prezzo = prodotti.getBoolean("promo") && prodotti.getBigDecimal("prezzo_scontato") != null
                    ? prodotti.getBigDecimal("prezzo_scontato")
                    : prodotti.getBigDecimal("prezzo");
            int quantita = Optional.ofNullable((Integer) prodotti.getObject("quantita")).orElse(1);
            if (prezzo != null) {
                totaleEff = totaleEff.add(prezzo.multiply(BigDecimal.valueOf(quantita)));
            }
        }
        totaleEff = totaleEff.setScale(2, RoundingMode.HALF_UP);
        BigDecimal prezzoScontato = totaleEff.multiply(new BigDecimal("0.85")).setScale(2, RoundingMode.HALF_UP);

        Map<String, Object> response = new HashMap<>();
        Map<String, Object> pacchettoOut = new HashMap<>(pacchetto);
        pacchettoOut.put("prezzo_originale", prezzoTot);
        pacchettoOut.put("prezzo_scontato", prezzoScontato);
        response.put("pacchetto", pacchettoOut);
        response.put("prodotti", prodottiList);
        return response;
    }
}
