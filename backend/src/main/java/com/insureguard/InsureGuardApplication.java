package com.insureguard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Entry point for the InsureGuard AI main backend.
 * Spring Boot is the central service: it owns auth, persistence,
 * Redis caching and Kafka events, and delegates NLP/image analysis
 * to the Python analysis service over REST.
 */
@SpringBootApplication
@EnableCaching
public class InsureGuardApplication {

    public static void main(String[] args) {
        SpringApplication.run(InsureGuardApplication.class, args);
    }
}
