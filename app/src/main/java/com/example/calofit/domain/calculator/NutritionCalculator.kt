package com.example.calofit.domain.calculator

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Scientific Nutrition Calculation Engine
 * Implements Mifflin-St Jeor formula for BMR, TDEE, BMI, and macro partitioning.
 */
object NutritionCalculator {

    enum class Gender {
        MALE, FEMALE
    }

    enum class ActivityLevel(val multiplier: Double) {
        SEDENTARY(1.2),          // Little or no exercise, desk job
        LIGHTLY_ACTIVE(1.375),   // Light exercise 1-3 days/week
        MODERATELY_ACTIVE(1.55), // Moderate exercise 3-5 days/week
        VERY_ACTIVE(1.725),      // Hard exercise 6-7 days/week
        EXTRA_ACTIVE(1.9)        // Intense daily exercise / physical job
    }

    enum class WeightGoal {
        LOSE_WEIGHT,
        MAINTAIN_WEIGHT,
        GAIN_WEIGHT
    }

    data class NutritionTarget(
        val bmr: Int,
        val tdee: Int,
        val targetCalories: Int,
        val proteinGrams: Int,
        val carbsGrams: Int,
        val fatGrams: Int,
        val waterMl: Int,
        val bmi: Double,
        val bmiCategory: BmiCategory
    )

    enum class BmiCategory {
        UNDERWEIGHT,
        NORMAL,
        OVERWEIGHT,
        OBESE
    }

    fun calculateBmi(weightKg: Double, heightCm: Double): Double {
        if (heightCm <= 0.0 || weightKg <= 0.0) return 0.0
        val heightM = heightCm / 100.0
        val bmi = weightKg / heightM.pow(2)
        return (bmi * 10.0).roundToInt() / 10.0
    }

    fun getBmiCategory(bmi: Double): BmiCategory {
        return when {
            bmi < 18.5 -> BmiCategory.UNDERWEIGHT
            bmi < 25.0 -> BmiCategory.NORMAL
            bmi < 30.0 -> BmiCategory.OVERWEIGHT
            else -> BmiCategory.OBESE
        }
    }

    /**
     * Mifflin-St Jeor equation.
     */
    fun calculateBmr(
        weightKg: Double,
        heightCm: Double,
        age: Int,
        gender: Gender
    ): Int {
        val safeWeight = weightKg.coerceIn(25.0, 300.0)
        val safeHeight = heightCm.coerceIn(100.0, 250.0)
        val safeAge = age.coerceIn(10, 110)
        val base = (10.0 * safeWeight) + (6.25 * safeHeight) - (5.0 * safeAge)
        val bmr = if (gender == Gender.MALE) base + 5.0 else base - 161.0
        return bmr.coerceAtLeast(800.0).roundToInt()
    }

    fun calculateTdee(bmr: Int, activityLevel: ActivityLevel): Int {
        return (bmr * activityLevel.multiplier).roundToInt()
    }

    fun calculateFullPlan(
        weightKg: Double,
        heightCm: Double,
        age: Int,
        gender: Gender,
        activityLevel: ActivityLevel,
        goal: WeightGoal
    ): NutritionTarget {
        val safeWeight = weightKg.coerceIn(25.0, 300.0)
        val safeHeight = heightCm.coerceIn(100.0, 250.0)
        val safeAge = age.coerceIn(10, 110)

        val bmi = calculateBmi(safeWeight, safeHeight)
        val bmiCategory = getBmiCategory(bmi)
        val bmr = calculateBmr(safeWeight, safeHeight, safeAge, gender)
        val tdee = calculateTdee(bmr, activityLevel)

        val targetCalories = when (goal) {
            WeightGoal.LOSE_WEIGHT -> {
                (tdee - 450).coerceAtLeast(if (gender == Gender.MALE) 1500 else 1200)
            }
            WeightGoal.MAINTAIN_WEIGHT -> tdee
            WeightGoal.GAIN_WEIGHT -> tdee + 400
        }

        // Macro distribution: 25% Protein (4 kcal/g), 50% Carbs (4 kcal/g), 25% Fat (9 kcal/g)
        val proteinGrams = ((targetCalories * 0.25) / 4.0).roundToInt()
        val carbsGrams = ((targetCalories * 0.50) / 4.0).roundToInt()
        val fatGrams = ((targetCalories * 0.25) / 9.0).roundToInt()

        // Hydration: ~35 ml per kg of bodyweight
        val waterMl = (safeWeight * 35.0).coerceIn(1500.0, 4500.0).roundToInt()

        return NutritionTarget(
            bmr = bmr,
            tdee = tdee,
            targetCalories = targetCalories,
            proteinGrams = proteinGrams,
            carbsGrams = carbsGrams,
            fatGrams = fatGrams,
            waterMl = waterMl,
            bmi = bmi,
            bmiCategory = bmiCategory
        )
    }

    fun calculateBurnedCalories(
        activityName: String,
        durationMinutes: Int,
        weightKg: Double
    ): Int {
        val met = when (activityName.lowercase()) {
            "walking", "پیاده‌روی" -> 3.5
            "running", "دویدن" -> 8.0
            "cycling", "دوچرخه‌سواری" -> 6.5
            "gym", "strength training", "بدنسازی", "بدنسازی و وزنه" -> 5.0
            "swimming", "شنا" -> 7.0
            "yoga", "یوگا", "یوگا و کشش" -> 3.0
            "hiit", "تمرین اینتروال" -> 8.5
            "home workout", "ورزش در منزل" -> 4.5
            else -> 4.0
        }
        return (met * weightKg.coerceAtLeast(25.0) * (durationMinutes.coerceAtLeast(1) / 60.0)).roundToInt()
    }

    fun isValidAge(age: Int?): Boolean = age != null && age in 12..100
    fun isValidHeightCm(heightCm: Double?): Boolean = heightCm != null && heightCm in 100.0..240.0
    fun isValidWeightKg(weightKg: Double?): Boolean = weightKg != null && weightKg in 25.0..280.0
}
