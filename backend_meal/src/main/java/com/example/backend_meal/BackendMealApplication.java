package com.example.backend_meal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BackendMealApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendMealApplication.class, args);
    }

}
