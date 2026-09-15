package com.paymentrouter.router.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS for the Payment Router API.
 * <p>
 * The React app normally calls {@code /api} through a proxy (Vite in development, Nginx in Docker),
 * so the browser sees a single origin and CORS is not involved at all. This allow-list only matters
 * when a frontend calls the router directly from another origin, e.g. http://localhost:5173 →
 * http://localhost:8080. It is deliberately narrow: listed origins, API paths, GET/POST, JSON only.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public CorsConfig(@Value("${app.cors.allowed-origins}") String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST")
                .allowedHeaders("Content-Type")
                .maxAge(3600);
    }
}
