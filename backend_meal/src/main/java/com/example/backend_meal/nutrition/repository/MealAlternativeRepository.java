package com.example.backend_meal.nutrition.repository;

import com.example.backend_meal.nutrition.model.MealAlternative;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MealAlternativeRepository extends JpaRepository<MealAlternative, Long> {}
