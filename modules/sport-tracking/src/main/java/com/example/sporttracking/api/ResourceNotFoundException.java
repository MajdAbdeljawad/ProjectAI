package com.example.sporttracking.api;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resourceName) {
        super(resourceName + " introuvable");
    }
}
