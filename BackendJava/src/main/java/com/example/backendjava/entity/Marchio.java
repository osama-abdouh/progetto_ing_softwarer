package com.example.backendjava.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "marchio")
public class Marchio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_marchio")
    private Long idMarchio;

    @Column(name = "nome", nullable = false)
    private String nome;

    // Constructors
    public Marchio() {
    }

    // Getters and Setters
    public Long getIdMarchio() {
        return idMarchio;
    }

    public void setIdMarchio(Long idMarchio) {
        this.idMarchio = idMarchio;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }
}
