package com.example.backendjava.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/immagine")
public class ImageUploadController {

    @Value("${uploads.dir:uploads}")
    private String uploadsDir;

    @SuppressWarnings("CatchMayIgnoreException")
    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("immagine") MultipartFile file,
                                    @RequestHeader(value = "x-file-name", required = false) String xFileName) throws IOException {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "File mancante"));
        }
        // Save under prodotti folder
        Path dir = Paths.get(uploadsDir, "prodotti");
        Files.createDirectories(dir);

        String original = (xFileName != null && !xFileName.isBlank()) ? xFileName : (file.getOriginalFilename() != null ? file.getOriginalFilename() : "");
        String cleaned = (original != null && !original.isBlank()) ? original.replaceAll("[^a-zA-Z0-9._-]", "_") : ("img_" + Instant.now().toEpochMilli());
        String base = cleaned;
        String ext = "";
        int dot = cleaned.lastIndexOf('.');
        if (dot > 0) { base = cleaned.substring(0, dot); ext = cleaned.substring(dot); }

        // ensure unique
        String filename = base + ext;
        int counter = 1;
        while (Files.exists(dir.resolve(filename))) {
            filename = base + "_" + (counter++) + ext;
        }
        Path dest = dir.resolve(filename);
    file.transferTo(dest.toFile());

        Map<String, Object> out = new HashMap<>();
        out.put("filename", filename);
        return ResponseEntity.ok(out);
    }

    // Serve le immagini statiche
    @SuppressWarnings({"CatchMayIgnoreException", "null"})
    @GetMapping("/uploads/{categoria}/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String categoria, @PathVariable String filename) {
        try {
            Path file = Paths.get(uploadsDir).resolve(categoria).resolve(filename);
            Resource resource = new UrlResource(file.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                String contentType = Files.probeContentType(file);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
                
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
