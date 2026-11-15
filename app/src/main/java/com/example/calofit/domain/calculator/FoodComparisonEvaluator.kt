package com.example.calofit.domain.calculator

import com.example.calofit.data.model.FoodItemEntity
import kotlin.math.roundToInt

enum class ComparisonMode {
    PER_SERVING,
    PER_100G
}

enum class ComparisonGoal {
    OVERALL,
    LOWER_CALORIES,
    HIGHER_PROTEIN
}

enum class ComparisonWinner {
    LEFT,
    RIGHT,
    NONE
}

data class NormalizedNutritionMetrics(
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double,
    val sugar: Double,
    val sodiumMg: Double,
    val weightGrams: Double,
    val qualityScore: Double
)

data class FoodComparisonEvaluation(
    val leftMetrics: NormalizedNutritionMetrics,
    val rightMetrics: NormalizedNutritionMetrics,
    val lowerCaloriesSide: ComparisonWinner,
    val higherProteinSide: ComparisonWinner,
    val higherFiberSide: ComparisonWinner,
    val lowerSugarSide: ComparisonWinner,
    val lowerSodiumSide: ComparisonWinner,
    val healthierProfileSide: ComparisonWinner,
    val greenHighlightSide: ComparisonWinner,
    val isCalorieVsNutritionConflict: Boolean,
    val isSameFood: Boolean,
    val hasInsufficientData: Boolean = false
)

/**
 * Transparent, position-independent food comparison engine.
 *
 * Rules (Section 11):
 * - Compares calories, protein, carbohydrates, fat, fiber, sugar, and sodium (when available).
 * - Supports both selected serving comparison and per-100g normalization.
 * - Supports goal-specific evaluation (Overall Balance, Lower Calories, Higher Protein).
 * - Prevents false winner badges when nutritional data is insufficient.
 * - In OVERALL mode, highlights a side in green ONLY when that food is BOTH lower in calories
 *   AND nutritionally stronger (or tied on one dimension and better on the other).
 * - When one food is lower in calories while the other has a stronger nutritional profile
 *   (a conflicting trade-off) in OVERALL mode, NEITHER side receives the universal green
 *   highlight so separate advantages can be shown honestly on both cards.
 */
object FoodComparisonEvaluator {

    fun evaluate(
        left: FoodItemEntity,
        right: FoodItemEntity,
        mode: ComparisonMode,
        goal: ComparisonGoal = ComparisonGoal.OVERALL
    ): FoodComparisonEvaluation {
        val leftMetrics = normalize(left, mode)
        val rightMetrics = normalize(right, mode)

        if (left.id == right.id && left.id != 0L) {
            return FoodComparisonEvaluation(
                leftMetrics = leftMetrics,
                rightMetrics = rightMetrics,
                lowerCaloriesSide = ComparisonWinner.NONE,
                higherProteinSide = ComparisonWinner.NONE,
                higherFiberSide = ComparisonWinner.NONE,
                lowerSugarSide = ComparisonWinner.NONE,
                lowerSodiumSide = ComparisonWinner.NONE,
                healthierProfileSide = ComparisonWinner.NONE,
                greenHighlightSide = ComparisonWinner.NONE,
                isCalorieVsNutritionConflict = false,
                isSameFood = true,
                hasInsufficientData = false
            )
        }

        val leftInsufficient = !hasMeaningfulNutritionData(left)
        val rightInsufficient = !hasMeaningfulNutritionData(right)
        if (leftInsufficient || rightInsufficient) {
            return FoodComparisonEvaluation(
                leftMetrics = leftMetrics,
                rightMetrics = rightMetrics,
                lowerCaloriesSide = ComparisonWinner.NONE,
                higherProteinSide = ComparisonWinner.NONE,
                higherFiberSide = ComparisonWinner.NONE,
                lowerSugarSide = ComparisonWinner.NONE,
                lowerSodiumSide = ComparisonWinner.NONE,
                healthierProfileSide = ComparisonWinner.NONE,
                greenHighlightSide = ComparisonWinner.NONE,
                isCalorieVsNutritionConflict = false,
                isSameFood = false,
                hasInsufficientData = true
            )
        }

        val lowerCal = compareSmallerBetter(leftMetrics.calories.toDouble(), rightMetrics.calories.toDouble(), minDiff = 5.0)
        val higherProt = compareLargerBetter(leftMetrics.protein, rightMetrics.protein, minDiff = 0.8)
        val higherFiber = compareLargerBetter(leftMetrics.fiber, rightMetrics.fiber, minDiff = 0.5)
        val lowerSugar = compareSmallerBetter(leftMetrics.sugar, rightMetrics.sugar, minDiff = 0.5)
        val hasSodiumData = leftMetrics.sodiumMg > 0.0 || rightMetrics.sodiumMg > 0.0
        val lowerSodium = if (hasSodiumData) {
            compareSmallerBetter(leftMetrics.sodiumMg, rightMetrics.sodiumMg, minDiff = 15.0)
        } else ComparisonWinner.NONE

        val healthierProfile = compareLargerBetter(
            leftMetrics.qualityScore,
            rightMetrics.qualityScore,
            minDiff = 2.5
        )

        val conflict = (lowerCal == ComparisonWinner.LEFT && healthierProfile == ComparisonWinner.RIGHT) ||
            (lowerCal == ComparisonWinner.RIGHT && healthierProfile == ComparisonWinner.LEFT)

        val overallGreen = when {
            conflict -> ComparisonWinner.NONE
            lowerCal == ComparisonWinner.LEFT && (healthierProfile == ComparisonWinner.LEFT || healthierProfile == ComparisonWinner.NONE) ->
                ComparisonWinner.LEFT
            lowerCal == ComparisonWinner.RIGHT && (healthierProfile == ComparisonWinner.RIGHT || healthierProfile == ComparisonWinner.NONE) ->
                ComparisonWinner.RIGHT
            lowerCal == ComparisonWinner.NONE && healthierProfile == ComparisonWinner.LEFT ->
                ComparisonWinner.LEFT
            lowerCal == ComparisonWinner.NONE && healthierProfile == ComparisonWinner.RIGHT ->
                ComparisonWinner.RIGHT
            else -> ComparisonWinner.NONE
        }

        val greenHighlight = when (goal) {
            ComparisonGoal.OVERALL -> overallGreen
            ComparisonGoal.LOWER_CALORIES -> lowerCal
            ComparisonGoal.HIGHER_PROTEIN -> higherProt
        }

        return FoodComparisonEvaluation(
            leftMetrics = leftMetrics,
            rightMetrics = rightMetrics,
            lowerCaloriesSide = lowerCal,
            higherProteinSide = higherProt,
            higherFiberSide = higherFiber,
            lowerSugarSide = lowerSugar,
            lowerSodiumSide = lowerSodium,
            healthierProfileSide = healthierProfile,
            greenHighlightSide = greenHighlight,
            isCalorieVsNutritionConflict = conflict && goal == ComparisonGoal.OVERALL,
            isSameFood = false,
            hasInsufficientData = false
        )
    }

    private fun hasMeaningfulNutritionData(food: FoodItemEntity): Boolean {
        return food.servingSizeGrams > 0.0 &&
            (food.calories > 0 || food.protein > 0.0 || food.carbs > 0.0 || food.fat > 0.0)
    }

    fun normalize(food: FoodItemEntity, mode: ComparisonMode): NormalizedNutritionMetrics {
        val servingGrams = food.servingSizeGrams.coerceAtLeast(1.0)
        val factor = when (mode) {
            ComparisonMode.PER_SERVING -> 1.0
            ComparisonMode.PER_100G -> 100.0 / servingGrams
        }
        val kcal = (food.calories * factor).roundToInt().coerceAtLeast(0)
        val protein = round1((food.protein * factor).coerceAtLeast(0.0))
        val carbs = round1((food.carbs * factor).coerceAtLeast(0.0))
        val fat = round1((food.fat * factor).coerceAtLeast(0.0))
        val fiber = round1((food.fiber * factor).coerceAtLeast(0.0))
        val sugar = round1((food.sugar * factor).coerceAtLeast(0.0))
        val sodium = round1((food.sodiumMg * factor).coerceAtLeast(0.0))
        val displayWeight = if (mode == ComparisonMode.PER_100G) 100.0 else servingGrams

        val per100Factor = 100.0 / servingGrams
        val proteinPer100 = (food.protein * per100Factor).coerceAtLeast(0.0)
        val fiberPer100 = (food.fiber * per100Factor).coerceAtLeast(0.0)
        val sugarPer100 = (food.sugar * per100Factor).coerceAtLeast(0.0)
        val fatPer100 = (food.fat * per100Factor).coerceAtLeast(0.0)
        val kcalPer100 = (food.calories * per100Factor).coerceAtLeast(0.0)
        val sodiumPer100 = (food.sodiumMg * per100Factor).coerceAtLeast(0.0)

        val positivePoints = (proteinPer100 * 1.4).coerceAtMost(35.0) +
            (fiberPer100 * 3.0).coerceAtMost(20.0)
        val negativePoints = (sugarPer100 * 1.1).coerceAtMost(25.0) +
            ((fatPer100 - 8.0).coerceAtLeast(0.0) * 0.6).coerceAtMost(15.0) +
            ((sodiumPer100 - 150.0).coerceAtLeast(0.0) / 80.0).coerceAtMost(10.0) +
            ((kcalPer100 - 160.0).coerceAtLeast(0.0) / 25.0).coerceAtMost(12.0)

        val qualityScore = (50.0 + positivePoints - negativePoints).coerceIn(0.0, 100.0)

        return NormalizedNutritionMetrics(
            calories = kcal,
            protein = protein,
            carbs = carbs,
            fat = fat,
            fiber = fiber,
            sugar = sugar,
            sodiumMg = sodium,
            weightGrams = round1(displayWeight),
            qualityScore = round1(qualityScore)
        )
    }

    private fun compareLargerBetter(left: Double, right: Double, minDiff: Double): ComparisonWinner {
        val diff = left - right
        return when {
            diff >= minDiff -> ComparisonWinner.LEFT
            -diff >= minDiff -> ComparisonWinner.RIGHT
            else -> ComparisonWinner.NONE
        }
    }

    private fun compareSmallerBetter(left: Double, right: Double, minDiff: Double): ComparisonWinner {
        val diff = right - left
        return when {
            diff >= minDiff -> ComparisonWinner.LEFT
            -diff >= minDiff -> ComparisonWinner.RIGHT
            else -> ComparisonWinner.NONE
        }
    }

    private fun round1(v: Double): Double = (v * 10.0).roundToInt() / 10.0
}
