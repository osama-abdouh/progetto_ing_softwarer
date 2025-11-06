package com.example.backendjava.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${uploads.dir:}")
    private String uploadsDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (StringUtils.hasText(uploadsDir)) {
            Path path = Paths.get(uploadsDir).toAbsolutePath().normalize();
            String location = path.toUri().toString();
            registry.addResourceHandler("/api/images/**")
                    .addResourceLocations(location);
        }
    }
}
