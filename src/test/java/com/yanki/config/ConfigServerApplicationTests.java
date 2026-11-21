package com.yanki.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ConfigServerApplication.
 *
 * <p>Verifies that the Spring Boot application context loads successfully 
 * and tests REST endpoints for expected behavior.</p>
 *
 * <p><b>Author:</b> Sebastian Retamal</p>
 * <p><b>Email:</b> semorale@emeal.nttdata.com</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConfigServerApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * Ensures the Spring Boot application context loads without issues.
     */
    @Test
    void contextLoads() {
    }

    /**
     * Tests the /application/profile endpoint for successful responses.
     */
    @Test
    void configEndpointTest() {
        ResponseEntity<String> response = restTemplate.getForEntity("/application/profile", String.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    /**
     * Tests the /encrypt endpoint for successful encryption responses.
     */
    @Test
    void encryptEndpointTest() {
        ResponseEntity<String> response = restTemplate.postForEntity("/encrypt", "test", String.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    /**
     * Tests the /decrypt endpoint for successful decryption responses.
     */
    @Test
    void decryptEndpointTest() {
        ResponseEntity<String> response = restTemplate.postForEntity("/decrypt", "{cipher}...", String.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }
}
