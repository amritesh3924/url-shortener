package com.example.shortener.config;

import com.example.shortener.service.Base62Encoder;
import com.example.shortener.service.UrlValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Bean
    Base62Encoder base62Encoder() { return new Base62Encoder(); }

    @Bean
    UrlValidator urlValidator() { return new UrlValidator(); }
}
