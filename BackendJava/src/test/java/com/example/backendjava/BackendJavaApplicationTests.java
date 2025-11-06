package com.example.backendjava;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test di integrazione base che verifica il caricamento del contesto Spring.
 * Se questo test passa, significa che tutte le configurazioni e i bean sono corretti.
 */
@SpringBootTest
@ActiveProfiles("test")
class BackendJavaApplicationTests {

    @Test
    void contextLoads() {
        // Verifica che il contesto Spring si carichi correttamente
        // Se questo test fallisce, c'è un problema nella configurazione dell'applicazione
    }
}
