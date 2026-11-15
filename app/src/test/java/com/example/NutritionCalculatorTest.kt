package com.example

import com.example.calofit.domain.calculator.NutritionCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionCalculatorTest {

    @Test
    fun testBmiCalculation() {
        // Height 180 cm (1.8m), Weight 81 kg -> BMI = 81 / 3.24 = 25.0
        val bmi = NutritionCalculator.calculateBmi(81.0, 180.0)
        assertEquals(25.0, bmi, 0.1)

        val cat = NutritionCalculator.getBmiCategory(bmi)
        assertEquals(NutritionCalculator.BmiCategory.OVERWEIGHT, cat)

        // Normal BMI: 70kg, 175cm -> 70 / 3.0625 = 22.86
        val normalBmi = NutritionCalculator.calculateBmi(70.0, 175.0)
        assertEquals(22.9, normalBmi, 0.1)
        assertEquals(NutritionCalculator.BmiCategory.NORMAL, NutritionCalculator.getBmiCategory(normalBmi))
    }

    @Test
    fun testBmrMifflinStJeorMale() {
        // Male: 10 * 75kg + 6.25 * 180cm - 5 * 25 + 5 = 750 + 1125 - 125 + 5 = 1755
        val bmr = NutritionCalculator.calculateBmr(
            weightKg = 75.0,
            heightCm = 180.0,
            age = 25,
            gender = NutritionCalculator.Gender.MALE
        )
        assertEquals(1755, bmr)
    }

    @Test
    fun testBmrMifflinStJeorFemale() {
        // Female: 10 * 60kg + 6.25 * 165cm - 5 * 30 - 161 = 600 + 1031.25 - 150 - 161 = 1320.25 -> 1320
        val bmr = NutritionCalculator.calculateBmr(
            weightKg = 60.0,
            heightCm = 165.0,
            age = 30,
            gender = NutritionCalculator.Gender.FEMALE
        )
        assertEquals(1320, bmr)
    }

    @Test
    fun testTdeeCalculation() {
        val bmr = 1500
        val sedentaryTdee = NutritionCalculator.calculateTdee(bmr, NutritionCalculator.ActivityLevel.SEDENTARY)
        assertEquals(1800, sedentaryTdee)

        val moderateTdee = NutritionCalculator.calculateTdee(bmr, NutritionCalculator.ActivityLevel.MODERATELY_ACTIVE)
        assertEquals(2325, moderateTdee)
    }

    @Test
    fun testFullPlanCalculation() {
        val plan = NutritionCalculator.calculateFullPlan(
            weightKg = 75.0,
            heightCm = 178.0,
            age = 28,
            gender = NutritionCalculator.Gender.MALE,
            activityLevel = NutritionCalculator.ActivityLevel.MODERATELY_ACTIVE,
            goal = NutritionCalculator.WeightGoal.LOSE_WEIGHT
        )

        assertTrue("Target calories should be positive and reasonable", plan.targetCalories in 1500..3500)
        assertTrue("Protein should be calculated", plan.proteinGrams > 80)
        assertTrue("Carbs should be calculated", plan.carbsGrams > 100)
        assertTrue("Water requirement should be between 1.5L and 4.5L", plan.waterMl in 1500..4500)
    }

    @Test
    fun testExerciseBurnedCalories() {
        // 70kg person running for 30 minutes (MET 8.0)
        // Cal = 8.0 * 70 * (30/60) = 280 kcal
        val burned = NutritionCalculator.calculateBurnedCalories("running", 30, 70.0)
        assertEquals(280, burned)
    }
}
