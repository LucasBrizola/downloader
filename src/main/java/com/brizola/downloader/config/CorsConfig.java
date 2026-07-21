package com.brizola.downloader.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("https://brizolavideodownloader.netlify.app",
                        "http://localhost:5500",
                        "http://127.0.0.1:5500"
                ) // netlify url + url tests from localhost
                .allowedMethods("GET", "POST")
                .allowedHeaders("*");
    }
}
