package com.example.backendjava.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test WebMvc per CatalogoController.
 * Verifica che gli endpoint del catalogo restituiscano i dati corretti.
 * Usa @WebMvcTest per testare solo il layer web senza caricare l'intero contesto.
 */
@SuppressWarnings("null")
@WebMvcTest(CatalogoController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class CatalogoControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    /**
     * Test GET /api/catalogo/prodotti - verifica che restituisca la lista prodotti
     */
    @Test
    @WithMockUser(roles = "USER")
    void testGetProdotti_ReturnsProductList() throws Exception {
        // Arrange: prepara dati mock
        Map<String, Object> prodotto1 = new HashMap<>();
        prodotto1.put("id_prodotto", 1L);
        prodotto1.put("nome", "iPhone 15");
        prodotto1.put("prezzo", 999.99);
        prodotto1.put("marchio", "Apple");
        prodotto1.put("categoria", "Smartphone");

        Map<String, Object> prodotto2 = new HashMap<>();
        prodotto2.put("id_prodotto", 2L);
        prodotto2.put("nome", "Galaxy S24");
        prodotto2.put("prezzo", 899.99);
        prodotto2.put("marchio", "Samsung");
        prodotto2.put("categoria", "Smartphone");

        List<Map<String, Object>> prodotti = Arrays.asList(prodotto1, prodotto2);

        // Mock del JdbcTemplate
        when(jdbcTemplate.queryForList(anyString())).thenReturn(prodotti);

        // Act & Assert
        mockMvc.perform(get("/api/catalogo/prodotti"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nome").value("iPhone 15"))
                .andExpect(jsonPath("$[1].nome").value("Galaxy S24"));
    }

    /**
     * Test GET /api/catalogo/marchi - verifica che restituisca i marchi
     */
    @Test
    @WithMockUser(roles = "USER")
    void testGetMarchi_ReturnsBrandList() throws Exception {
        // Arrange
        Map<String, Object> marchio1 = new HashMap<>();
        marchio1.put("id_marchio", 1L);
        marchio1.put("nome", "Apple");

        Map<String, Object> marchio2 = new HashMap<>();
        marchio2.put("id_marchio", 2L);
        marchio2.put("nome", "Samsung");

        when(jdbcTemplate.queryForList(anyString()))
                .thenReturn(Arrays.asList(marchio1, marchio2));

        // Act & Assert
        mockMvc.perform(get("/api/catalogo/marchi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nome").value("Apple"));
    }

    /**
     * Test GET /api/catalogo/categorie - verifica che restituisca le categorie
     */
    @Test
    @WithMockUser(roles = "USER")
    void testGetCategorie_ReturnsCategoryList() throws Exception {
        // Arrange
        Map<String, Object> categoria1 = new HashMap<>();
        categoria1.put("id_categoria", 1L);
        categoria1.put("nome", "Smartphone");

        Map<String, Object> categoria2 = new HashMap<>();
        categoria2.put("id_categoria", 2L);
        categoria2.put("nome", "Laptop");

        when(jdbcTemplate.queryForList(anyString()))
                .thenReturn(Arrays.asList(categoria1, categoria2));

        // Act & Assert
        mockMvc.perform(get("/api/catalogo/categorie"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nome").value("Smartphone"));
    }
}
