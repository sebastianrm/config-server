package com.yanki.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Entry point of the config-server microservice.
 *
 * <p>Part of the Yanki banking platform. The service follows the
 * database-per-service pattern: it owns and accesses only its own MongoDB
 * database and never reads nor writes the data owned by another
 * microservice. Cross-service collaboration is performed through Kafka
 * events and never through direct REST calls.</p>
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    /**
     * Starts the Spring Boot application.
     *
     * @param args Array of command line arguments passed to the application.
     *             Not needed for usual deployment.
>>>>>>>>

     */
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
