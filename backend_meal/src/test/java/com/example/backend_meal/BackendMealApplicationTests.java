package com.example.backend_meal;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BackendMealApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the Spring context starts correctly with H2 + dynamic AI provider
    }
}
