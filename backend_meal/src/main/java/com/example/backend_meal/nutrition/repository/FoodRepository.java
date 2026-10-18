package com.example.backend_meal.nutrition.repository;

import com.example.backend_meal.nutrition.model.Food;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FoodRepository extends JpaRepository<Food, Long> {}
