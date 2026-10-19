package com.fitcoach.app.backend.controller;

import com.fitcoach.app.backend.dto.RecipeRequest;
import com.fitcoach.app.backend.dto.RecipeResponse;
import com.fitcoach.app.backend.service.RecipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/recipes")
@RequiredArgsConstructor
public class RecipeController {
    private final RecipeService recipeService;

    @PostMapping("/generate")
    public ResponseEntity<RecipeResponse> generate(@Valid @RequestBody RecipeRequest request) {
        return ResponseEntity.ok(recipeService.generate(request));
    }
}
