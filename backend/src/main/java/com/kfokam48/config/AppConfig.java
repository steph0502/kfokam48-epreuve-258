package com.kfokam48.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Horloge injectable : permet aux tests de figer le temps et de vérifier
 * exactement la fenêtre de 15 minutes du code de présence (RG1).
 */
@Configuration
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
