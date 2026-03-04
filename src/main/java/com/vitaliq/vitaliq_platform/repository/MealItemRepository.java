package com.vitaliq.vitaliq_platform.repository;

import com.vitaliq.vitaliq_platform.model.nutrition.MealItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MealItemRepository extends JpaRepository<MealItem, UUID> {
    List<MealItem> findByMealId(UUID mealId);
}