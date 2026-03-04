package com.vitaliq.vitaliq_platform.model.nutrition;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "meal_items")
@Data
@NoArgsConstructor
public class MealItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_id", nullable = false)
    private Meal meal;

    @Column(nullable = false)
    private String usdaFoodId;

    @Column(nullable = false)
    private String foodName;

    @Column(nullable = false)
    private Double quantity;

    @Column(nullable = false)
    private String unit;

    private Double calories;
    private Double proteinG;
    private Double carbsG;
    private Double fatsG;
}