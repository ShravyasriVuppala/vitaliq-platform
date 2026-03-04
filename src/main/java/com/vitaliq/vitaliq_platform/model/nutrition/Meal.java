package com.vitaliq.vitaliq_platform.model.nutrition;

import com.vitaliq.vitaliq_platform.enums.MealType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meals")
@Data
@NoArgsConstructor
public class Meal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nutrition_plan_id", nullable = false)
    private NutritionPlan nutritionPlan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MealType mealType;

    @OneToMany(mappedBy = "meal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MealItem> mealItems = new ArrayList<>();

    private Double totalCalories;
    private Double totalProteinG;
    private Double totalCarbsG;
    private Double totalFatsG;
}