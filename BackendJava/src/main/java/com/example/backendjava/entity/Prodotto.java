package com.example.backendjava.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "prodotto")
public class Prodotto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_prodotto")
    private Long idProdotto;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "descrizione", columnDefinition = "TEXT")
    private String descrizione;

    @Column(name = "prezzo", nullable = false, precision = 10, scale = 2)
    private BigDecimal prezzo;

    @Column(name = "prezzo_scontato", precision = 10, scale = 2)
    private BigDecimal prezzoScontato;

    @Column(name = "quantita_disponibile", nullable = false)
    private Integer quantitaDisponibile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria")
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_marchio")
    private Marchio marchio;

    @Column(name = "immagine")
    private String immagine;

    @Column(name = "in_vetrina", nullable = false)
    private Boolean inVetrina = false;

    @Column(name = "promo", nullable = false)
    private Boolean promo = false;

    @Column(name = "bloccato", nullable = false)
    private Boolean bloccato = false;

    // Constructors
    public Prodotto() {
    }

    // Getters and Setters
    public Long getIdProdotto() {
        return idProdotto;
    }

    public void setIdProdotto(Long idProdotto) {
        this.idProdotto = idProdotto;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public BigDecimal getPrezzo() {
        return prezzo;
    }

    public void setPrezzo(BigDecimal prezzo) {
        this.prezzo = prezzo;
    }

    public BigDecimal getPrezzoScontato() {
        return prezzoScontato;
    }

    public void setPrezzoScontato(BigDecimal prezzoScontato) {
        this.prezzoScontato = prezzoScontato;
    }

    public Integer getQuantitaDisponibile() {
        return quantitaDisponibile;
    }

    public void setQuantitaDisponibile(Integer quantitaDisponibile) {
        this.quantitaDisponibile = quantitaDisponibile;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public Marchio getMarchio() {
        return marchio;
    }

    public void setMarchio(Marchio marchio) {
        this.marchio = marchio;
    }

    public String getImmagine() {
        return immagine;
    }

    public void setImmagine(String immagine) {
        this.immagine = immagine;
    }

    public Boolean getInVetrina() {
        return inVetrina;
    }

    public void setInVetrina(Boolean inVetrina) {
        this.inVetrina = inVetrina;
    }

    public Boolean getPromo() {
        return promo;
    }

    public void setPromo(Boolean promo) {
        this.promo = promo;
    }

    public Boolean getBloccato() {
        return bloccato;
    }

    public void setBloccato(Boolean bloccato) {
        this.bloccato = bloccato;
    }
}
