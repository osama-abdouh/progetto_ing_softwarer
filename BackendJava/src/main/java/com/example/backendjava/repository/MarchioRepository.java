package com.example.backendjava.repository;

import com.example.backendjava.entity.Marchio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MarchioRepository extends JpaRepository<Marchio, Long> {
    
    // Trova marchio per nome
    Optional<Marchio> findByNome(String nome);
    
    // Verifica se esiste un marchio con un dato nome
    boolean existsByNome(String nome);
}
