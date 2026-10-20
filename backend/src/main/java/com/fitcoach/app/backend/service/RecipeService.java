package com.fitcoach.app.backend.service;

import com.fitcoach.app.backend.dto.RecipeRequest;
import com.fitcoach.app.backend.dto.RecipeResponse;
import com.fitcoach.app.backend.service.RecipeMacroService.MacroComputation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeMacroService recipeMacroService;

    /**
     * Deterministic path: calculate macros from the nutrition table.
     * AI recipe composition will be layered on top of this later.
     */
    public RecipeResponse generate(RecipeRequest request) {
        MacroComputation computation = recipeMacroService.compute(request);
        return recipeMacroService.toDeterministicResponse(request, computation);
    }
}
