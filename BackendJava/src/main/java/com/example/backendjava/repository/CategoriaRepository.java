package com.example.backendjava.repository;

import com.example.backendjava.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    
    // Trova categoria per nome
    Optional<Categoria> findByNome(String nome);
    
    // Verifica se esiste una categoria con un dato nome
    boolean existsByNome(String nome);
}
