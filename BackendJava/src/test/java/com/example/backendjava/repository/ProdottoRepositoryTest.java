package com.example.backendjava.repository;

import com.example.backendjava.entity.Categoria;
import com.example.backendjava.entity.Marchio;
import com.example.backendjava.entity.Prodotto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test JPA per ProdottoRepository.
 * Usa H2 in-memory database per i test isolati.
 * Verifica le query custom e i finder methods.
 */
@DataJpaTest
@ActiveProfiles("test")
class ProdottoRepositoryTest {

    @Autowired
    private ProdottoRepository prodottoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private MarchioRepository marchioRepository;

    private Categoria smartphone;
    private Marchio apple;
    private Prodotto iPhone15;

    @SuppressWarnings("null")
    @BeforeEach
    void setUp() {
        // Crea categoria
        smartphone = new Categoria();
        smartphone.setNome("Smartphone");
        smartphone = categoriaRepository.save(smartphone);

        // Crea marchio
        apple = new Marchio();
        apple.setNome("Apple");
        apple = marchioRepository.save(apple);

        // Crea prodotto
        iPhone15 = new Prodotto();
        iPhone15.setNome("iPhone 15");
        iPhone15.setDescrizione("Ultimo iPhone di Apple");
        iPhone15.setPrezzo(new BigDecimal("999.99"));
        iPhone15.setQuantitaDisponibile(50);
        iPhone15.setBloccato(false);
        iPhone15.setInVetrina(true);
        iPhone15.setCategoria(smartphone);
        iPhone15.setMarchio(apple);
        iPhone15 = prodottoRepository.save(iPhone15);
    }

    /**
     * Test: verifica che findById restituisca il prodotto corretto
     */
    @Test
    void testFindById_ReturnsProduct() {
        // Act
        Optional<Prodotto> found = prodottoRepository.findById(iPhone15.getIdProdotto());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getNome()).isEqualTo("iPhone 15");
        assertThat(found.get().getPrezzo()).isEqualByComparingTo(new BigDecimal("999.99"));
    }

    /**
     * Test: verifica ricerca per nome (case insensitive)
     */
    @Test
    void testFindByNomeContaining_ReturnsMatchingProducts() {
        // Act
        List<Prodotto> results = prodottoRepository.findByNomeContainingIgnoreCase("iphone");

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getNome()).isEqualTo("iPhone 15");
    }

    /**
     * Test: verifica query per prodotti disponibili
     */
    @Test
    void testFindProdottiDisponibili_ReturnsOnlyAvailableProducts() {
        // Arrange: crea un prodotto non disponibile
        Prodotto outOfStock = new Prodotto();
        outOfStock.setNome("Galaxy S24");
        outOfStock.setPrezzo(new BigDecimal("899.99"));
        outOfStock.setQuantitaDisponibile(0); // Non disponibile
        outOfStock.setBloccato(false);
        outOfStock.setCategoria(smartphone);
        outOfStock.setMarchio(apple);
        prodottoRepository.save(outOfStock);

        // Act
        List<Prodotto> disponibili = prodottoRepository.findProdottiDisponibili();

        // Assert
        assertThat(disponibili).hasSize(1);
        assertThat(disponibili.get(0).getNome()).isEqualTo("iPhone 15");
    }

    /**
     * Test: verifica query per prodotti in vetrina
     */
    @Test
    void testFindProdottiInVetrina_ReturnsOnlyFeaturedProducts() {
        // Act
        List<Prodotto> inVetrina = prodottoRepository.findProdottiInVetrina();

        // Assert
        assertThat(inVetrina).hasSize(1);
        assertThat(inVetrina.get(0).getInVetrina()).isTrue();
    }

    /**
     * Test: verifica ricerca per categoria
     */
    @Test
    void testFindByCategoriaId_ReturnsProductsInCategory() {
        // Act
        List<Prodotto> prodottiSmartphone = prodottoRepository.findByCategoriaId(smartphone.getIdCategoria());

        // Assert
        assertThat(prodottiSmartphone).hasSize(1);
        assertThat(prodottiSmartphone.get(0).getCategoria().getNome()).isEqualTo("Smartphone");
    }

    /**
     * Test: verifica che venga salvato correttamente un nuovo prodotto
     */
    @Test
    void testSave_PersistsNewProduct() {
        // Arrange
        Prodotto newProduct = new Prodotto();
        newProduct.setNome("MacBook Pro");
        newProduct.setDescrizione("Laptop Apple");
        newProduct.setPrezzo(new BigDecimal("2499.99"));
        newProduct.setQuantitaDisponibile(10);
        newProduct.setBloccato(false);
        newProduct.setInVetrina(false);
        newProduct.setCategoria(smartphone);
        newProduct.setMarchio(apple);

        // Act
        Prodotto saved = prodottoRepository.save(newProduct);

        // Assert
        assertThat(saved.getIdProdotto()).isNotNull();
        assertThat(prodottoRepository.findById(saved.getIdProdotto())).isPresent();
    }
}
