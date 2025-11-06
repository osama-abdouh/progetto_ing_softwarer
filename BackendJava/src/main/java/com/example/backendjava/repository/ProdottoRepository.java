package com.example.backendjava.repository;

import com.example.backendjava.entity.Prodotto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProdottoRepository extends JpaRepository<Prodotto, Long> {
    
    // Trova prodotti per nome (case insensitive)
    List<Prodotto> findByNomeContainingIgnoreCase(String nome);
    
    // Trova prodotti disponibili
    @Query("SELECT p FROM Prodotto p WHERE p.quantitaDisponibile > 0 AND p.bloccato = false")
    List<Prodotto> findProdottiDisponibili();
    
    // Trova prodotti per marchio
    @Query("SELECT p FROM Prodotto p WHERE p.marchio.id = :marchioId")
    List<Prodotto> findByMarchioId(@Param("marchioId") Long marchioId);
    
    // Trova prodotti per categoria
    @Query("SELECT p FROM Prodotto p WHERE p.categoria.id = :categoriaId")
    List<Prodotto> findByCategoriaId(@Param("categoriaId") Long categoriaId);
    
    // Trova prodotti in vetrina
    @Query("SELECT p FROM Prodotto p WHERE p.inVetrina = true AND p.quantitaDisponibile > 0 AND p.bloccato = false")
    List<Prodotto> findProdottiInVetrina();
}
