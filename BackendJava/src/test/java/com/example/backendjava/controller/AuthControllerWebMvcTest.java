package com.example.backendjava.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test WebMvc per AuthController.
 * Verifica le funzionalità di autenticazione: registrazione, login e gestione errori.
 * Usa mock per JdbcTemplate e BCryptPasswordEncoder.
 */
@SuppressWarnings("null")
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AuthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private SqlRowSet sqlRowSet;

    /**
     * Test POST /api/auth/register - Registrazione utente con successo
     */
    @Test
    void testRegister_WithValidData_ReturnsSuccess() throws Exception {
        // Arrange
        Map<String, Object> registerRequest = new HashMap<>();
        registerRequest.put("nome", "Mario");
        registerRequest.put("cognome", "Rossi");
        registerRequest.put("email", "mario.rossi@email.com");
        registerRequest.put("password", "password123");

        // Mock dell'inserimento nel database (ritorna ID 1)
        when(jdbcTemplate.queryForObject(
                anyString(), 
                eq(Long.class), 
                eq("Mario"), 
                eq("Rossi"), 
                eq("mario.rossi@email.com"), 
                anyString()))
                .thenReturn(1L);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Mario"))
                .andExpect(jsonPath("$.email").value("mario.rossi@email.com"));
    }

    /**
     * Test POST /api/auth/register - Registrazione senza campi obbligatori
     */
    @Test
    void testRegister_WithMissingFields_ReturnsBadRequest() throws Exception {
        // Arrange: richiesta senza campo "email"
        Map<String, Object> registerRequest = new HashMap<>();
        registerRequest.put("nome", "Mario");
        registerRequest.put("cognome", "Rossi");
        registerRequest.put("password", "password123");
        // email mancante

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Tutti i campi sono obbligatori"));
    }

    /**
     * Test POST /api/auth/register - Registrazione con email già esistente
     */
    @Test
    void testRegister_WithDuplicateEmail_ThrowsException() throws Exception {
        // Arrange
        Map<String, Object> registerRequest = new HashMap<>();
        registerRequest.put("nome", "Mario");
        registerRequest.put("cognome", "Rossi");
        registerRequest.put("email", "existing@email.com");
        registerRequest.put("password", "password123");

        // Mock: simuliamo eccezione per email duplicata
        when(jdbcTemplate.queryForObject(
                anyString(), 
                eq(Long.class), 
                anyString(), 
                anyString(), 
                eq("existing@email.com"), 
                anyString()))
                .thenThrow(new RuntimeException("Duplicate key value violates unique constraint"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().is5xxServerError());
    }

    /**
     * Test POST /api/auth/login - Login con credenziali valide
     */
    @Test
    void testLogin_WithValidCredentials_ReturnsTokenAndUser() throws Exception {
        // Arrange
        Map<String, Object> loginRequest = new HashMap<>();
        loginRequest.put("email", "mario.rossi@email.com");
        loginRequest.put("password", "password123");

        // Mock BCryptPasswordEncoder per verificare password
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashedPassword = encoder.encode("password123");

        // Mock SqlRowSet per simulare utente trovato
        when(jdbcTemplate.queryForRowSet(anyString(), eq("mario.rossi@email.com")))
                .thenReturn(sqlRowSet);
        when(sqlRowSet.next()).thenReturn(true);
        when(sqlRowSet.getString("password")).thenReturn(hashedPassword);
        when(sqlRowSet.getBoolean("is_blocked")).thenReturn(false);
        when(sqlRowSet.getLong("id")).thenReturn(1L);
        when(sqlRowSet.getString("nome")).thenReturn("Mario");
        when(sqlRowSet.getString("ruolo")).thenReturn("USER");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.id").value(1))
                .andExpect(jsonPath("$.user.nome").value("Mario"))
                .andExpect(jsonPath("$.user.email").value("mario.rossi@email.com"))
                .andExpect(jsonPath("$.user.ruolo").value("USER"));
    }

    /**
     * Test POST /api/auth/login - Login con email inesistente
     */
    @Test
    void testLogin_WithNonExistentEmail_ReturnsUnauthorized() throws Exception {
        // Arrange
        Map<String, Object> loginRequest = new HashMap<>();
        loginRequest.put("email", "nonexistent@email.com");
        loginRequest.put("password", "password123");

        // Mock: utente non trovato
        when(jdbcTemplate.queryForRowSet(anyString(), eq("nonexistent@email.com")))
                .thenReturn(sqlRowSet);
        when(sqlRowSet.next()).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenziali non valide"));
    }

    /**
     * Test POST /api/auth/login - Login con password errata
     */
    @Test
    void testLogin_WithWrongPassword_ReturnsUnauthorized() throws Exception {
        // Arrange
        Map<String, Object> loginRequest = new HashMap<>();
        loginRequest.put("email", "mario.rossi@email.com");
        loginRequest.put("password", "wrongpassword");

        // Mock: utente trovato ma password errata
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashedPassword = encoder.encode("correctpassword");

        when(jdbcTemplate.queryForRowSet(anyString(), eq("mario.rossi@email.com")))
                .thenReturn(sqlRowSet);
        when(sqlRowSet.next()).thenReturn(true);
        when(sqlRowSet.getString("password")).thenReturn(hashedPassword);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenziali non valide"));
    }

    /**
     * Test POST /api/auth/login - Login con account bloccato
     */
    @Test
    void testLogin_WithBlockedAccount_ReturnsForbidden() throws Exception {
        // Arrange
        Map<String, Object> loginRequest = new HashMap<>();
        loginRequest.put("email", "blocked@email.com");
        loginRequest.put("password", "password123");

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashedPassword = encoder.encode("password123");

        // Mock: utente bloccato
        when(jdbcTemplate.queryForRowSet(anyString(), eq("blocked@email.com")))
                .thenReturn(sqlRowSet);
        when(sqlRowSet.next()).thenReturn(true);
        when(sqlRowSet.getString("password")).thenReturn(hashedPassword);
        when(sqlRowSet.getBoolean("is_blocked")).thenReturn(true); // Account bloccato!

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Account bloccato, Contatta l'amministratore."));
    }

    /**
     * Test POST /api/auth/login - Login senza email
     */
    @Test
    void testLogin_WithMissingEmail_ReturnsBadRequest() throws Exception {
        // Arrange: solo password, email mancante
        Map<String, Object> loginRequest = new HashMap<>();
        loginRequest.put("password", "password123");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email e password obbligatorie"));
    }

    /**
     * Test POST /api/auth/login - Login senza password
     */
    @Test
    void testLogin_WithMissingPassword_ReturnsBadRequest() throws Exception {
        // Arrange: solo email, password mancante
        Map<String, Object> loginRequest = new HashMap<>();
        loginRequest.put("email", "mario.rossi@email.com");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email e password obbligatorie"));
    }

    /**
     * Test POST /api/auth/login - Email viene convertita in lowercase
     */
    @Test
    void testLogin_EmailConvertedToLowercase() throws Exception {
        // Arrange: email con maiuscole
        Map<String, Object> loginRequest = new HashMap<>();
        loginRequest.put("email", "MARIO.ROSSI@EMAIL.COM");
        loginRequest.put("password", "password123");

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashedPassword = encoder.encode("password123");

        // Mock: verifica che l'email sia stata convertita in lowercase
        when(jdbcTemplate.queryForRowSet(anyString(), eq("mario.rossi@email.com")))
                .thenReturn(sqlRowSet);
        when(sqlRowSet.next()).thenReturn(true);
        when(sqlRowSet.getString("password")).thenReturn(hashedPassword);
        when(sqlRowSet.getBoolean("is_blocked")).thenReturn(false);
        when(sqlRowSet.getLong("id")).thenReturn(1L);
        when(sqlRowSet.getString("nome")).thenReturn("Mario");
        when(sqlRowSet.getString("ruolo")).thenReturn("USER");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("mario.rossi@email.com"));
    }
}
