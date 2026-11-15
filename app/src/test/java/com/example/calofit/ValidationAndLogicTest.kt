package com.example.calofit

import com.example.calofit.data.model.FoodItemEntity
import com.example.calofit.domain.calculator.ComparisonGoal
import com.example.calofit.domain.calculator.ComparisonMode
import com.example.calofit.domain.calculator.ComparisonWinner
import com.example.calofit.domain.calculator.FoodComparisonEvaluator
import com.example.calofit.domain.calculator.NutritionCalculator
import com.example.calofit.domain.calendar.JalaliCalendarHelper
import com.example.calofit.localization.StringsProvider
import com.example.calofit.ui.screens.buildDynamicComparisonSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationAndLogicTest {

    @Test
    fun profileBounds_validateCorrectly() {
        assertTrue(NutritionCalculator.isValidAge(25))
        assertFalse(NutritionCalculator.isValidAge(5))
        assertFalse(NutritionCalculator.isValidAge(120))
        assertFalse(NutritionCalculator.isValidAge(null))

        assertTrue(NutritionCalculator.isValidHeightCm(175.0))
        assertFalse(NutritionCalculator.isValidHeightCm(50.0))
        assertFalse(NutritionCalculator.isValidHeightCm(270.0))

        assertTrue(NutritionCalculator.isValidWeightKg(68.5))
        assertFalse(NutritionCalculator.isValidWeightKg(10.0))
        assertFalse(NutritionCalculator.isValidWeightKg(350.0))
    }

    @Test
    fun localization_enforcesCleanCopyAndLanguageBasedDigits() {
        assertEquals("Version 1.0", StringsProvider.get("app_version_value", isPersian = false))
        assertEquals("نسخه ۱.۰", StringsProvider.get("app_version_value", isPersian = true))
        assertEquals("Snack", StringsProvider.get("meal_snack", isPersian = false))
        assertEquals("میان‌وعده", StringsProvider.get("meal_snack", isPersian = true))
        assertEquals("Gender", StringsProvider.get("gender_label", isPersian = false))
        assertEquals("جنسیت", StringsProvider.get("gender_label", isPersian = true))
        assertEquals("Light", StringsProvider.get("theme_light", isPersian = false))
        assertEquals("Dark", StringsProvider.get("theme_dark", isPersian = false))
        assertEquals("System Default", StringsProvider.get("theme_system", isPersian = false))

        assertEquals("English | انگلیسی", StringsProvider.get("lang_english_label", isPersian = false))
        assertEquals("English | انگلیسی", StringsProvider.get("lang_english_label", isPersian = true))
        assertEquals("Persian | فارسی", StringsProvider.get("lang_persian_label", isPersian = true))
        assertEquals("Persian | فارسی", StringsProvider.get("lang_persian_label", isPersian = false))
        assertEquals("وزن فعلی و هدف", StringsProvider.get("weight_tracking", isPersian = true))
        assertEquals("وزن فعلی و هدف", StringsProvider.get("weight_summary", isPersian = true))
        assertEquals("Current and Goal Weight", StringsProvider.get("weight_tracking", isPersian = false))
        assertEquals("Current and Goal Weight", StringsProvider.get("weight_summary", isPersian = false))
        assertEquals("Current and Goal Weight", StringsProvider.get("weight_current_target_title", isPersian = false))
        assertEquals(
            "Please enable your internet connection to use the AI Nutrition Coach.",
            StringsProvider.get("err_coach_no_internet", isPersian = false)
        )
        assertEquals(
            "در حال حاضر امکان اتصال به هیچ‌یک از سرویس‌های هوش مصنوعی وجود ندارد. لطفاً اتصال اینترنت خود را بررسی کنید و بعداً دوباره تلاش کنید.",
            StringsProvider.get("err_ai_unavailable", isPersian = true)
        )
        assertEquals(
            "All available AI services are currently unavailable. Please check your internet connection and try again later.",
            StringsProvider.get("err_ai_unavailable", isPersian = false)
        )

        assertEquals("۱۲۵۰", StringsProvider.formatNumber(1250, isPersian = true))
        assertEquals("1250", StringsProvider.formatNumber(1250, isPersian = false))
        assertEquals("72.5", JalaliCalendarHelper.toAsciiDigits("۷۲٫۵"))
    }

    @Test
    fun dynamicComparisonSummary_computesRealMetricsAndHandlesIdenticalSelection() {
        val chicken = FoodItemEntity(
            id = 1L,
            nameEn = "Grilled Chicken Breast",
            nameFa = "سینه مرغ گریل‌شده",
            category = "MEAT_PROTEIN",
            servingSizeGrams = 100.0,
            calories = 165,
            protein = 31.0,
            carbs = 0.0,
            fat = 3.6,
            fiber = 0.0,
            sugar = 0.0,
            sodiumMg = 74.0
        )
        val kebab = FoodItemEntity(
            id = 2L,
            nameEn = "Koobideh Kebab",
            nameFa = "کباب کوبیده",
            category = "IRANIAN_MAINS",
            servingSizeGrams = 100.0,
            calories = 280,
            protein = 18.0,
            carbs = 2.0,
            fat = 22.0,
            fiber = 0.0,
            sugar = 0.5,
            sodiumMg = 420.0
        )

        val sameMsgEn = buildDynamicComparisonSummary(chicken, chicken, isPersian = false)
        assertEquals(StringsProvider.get("same_food_selected", false), sameMsgEn)

        val summaryEn = buildDynamicComparisonSummary(chicken, kebab, isPersian = false)
        assertTrue(summaryEn.contains("Grilled Chicken Breast"))
        assertTrue(summaryEn.contains("115"))
        assertTrue(summaryEn.contains("13.0"))

        val summaryFa = buildDynamicComparisonSummary(chicken, kebab, isPersian = true)
        assertTrue(summaryFa.contains("سینه مرغ گریل‌شده"))
        assertTrue(summaryFa.contains("۱۱۵"))
    }

    @Test
    fun foodComparisonEvaluator_isPositionIndependent_andHandlesConflictsWithoutUniversalGreen() {
        val grilledChicken = FoodItemEntity(
            id = 10L,
            nameEn = "Chicken Breast",
            nameFa = "سینه مرغ",
            category = "MEAT_PROTEIN",
            servingSizeGrams = 150.0,
            calories = 248,
            protein = 46.5,
            carbs = 0.0,
            fat = 5.4,
            fiber = 0.0,
            sugar = 0.0,
            sodiumMg = 110.0
        )
        val donut = FoodItemEntity(
            id = 11L,
            nameEn = "Glazed Donut",
            nameFa = "دونات",
            category = "SNACKS",
            servingSizeGrams = 70.0,
            calories = 290,
            protein = 3.5,
            carbs = 34.0,
            fat = 15.0,
            fiber = 1.0,
            sugar = 16.0,
            sodiumMg = 250.0
        )

        val lr = com.example.calofit.domain.calculator.FoodComparisonEvaluator.evaluate(
            grilledChicken, donut, com.example.calofit.domain.calculator.ComparisonMode.PER_SERVING
        )
        val rl = com.example.calofit.domain.calculator.FoodComparisonEvaluator.evaluate(
            donut, grilledChicken, com.example.calofit.domain.calculator.ComparisonMode.PER_SERVING
        )
        assertEquals(com.example.calofit.domain.calculator.ComparisonWinner.LEFT, lr.greenHighlightSide)
        assertEquals(com.example.calofit.domain.calculator.ComparisonWinner.RIGHT, rl.greenHighlightSide)
        assertFalse(lr.isCalorieVsNutritionConflict)

        // Conflict case: lower-calorie soda/candy bar vs nutrient-dense salmon bowl
        val lowCalCandy = FoodItemEntity(
            id = 12L,
            nameEn = "Hard Candy",
            nameFa = "آبنبات",
            category = "SNACKS",
            servingSizeGrams = 30.0,
            calories = 115,
            protein = 0.0,
            carbs = 29.0,
            fat = 0.0,
            fiber = 0.0,
            sugar = 24.0,
            sodiumMg = 15.0
        )
        val lentilStew = FoodItemEntity(
            id = 13L,
            nameEn = "Lentil & Herb Bowl",
            nameFa = "خوراک عدس",
            category = "IRANIAN_MAINS",
            servingSizeGrams = 250.0,
            calories = 290,
            protein = 18.0,
            carbs = 42.0,
            fat = 4.5,
            fiber = 11.0,
            sugar = 2.0,
            sodiumMg = 240.0
        )
        val conflictEval = com.example.calofit.domain.calculator.FoodComparisonEvaluator.evaluate(
            lowCalCandy, lentilStew, com.example.calofit.domain.calculator.ComparisonMode.PER_SERVING
        )
        assertTrue(conflictEval.isCalorieVsNutritionConflict)
        assertEquals(com.example.calofit.domain.calculator.ComparisonWinner.LEFT, conflictEval.lowerCaloriesSide)
        assertEquals(com.example.calofit.domain.calculator.ComparisonWinner.RIGHT, conflictEval.healthierProfileSide)
        assertEquals(com.example.calofit.domain.calculator.ComparisonWinner.NONE, conflictEval.greenHighlightSide)

        // Per-100g normalization reverses calorie winner (lentil bowl = 116 kcal/100g vs candy = 383 kcal/100g)
        val per100Eval = FoodComparisonEvaluator.evaluate(
            lowCalCandy, lentilStew, ComparisonMode.PER_100G
        )
        assertFalse(per100Eval.isCalorieVsNutritionConflict)
        assertEquals(ComparisonWinner.RIGHT, per100Eval.greenHighlightSide)

        // Goal-specific highlights override overall conflict cleanly
        val goalLowerCal = FoodComparisonEvaluator.evaluate(
            lowCalCandy, lentilStew, ComparisonMode.PER_SERVING, ComparisonGoal.LOWER_CALORIES
        )
        assertEquals(ComparisonWinner.LEFT, goalLowerCal.greenHighlightSide)

        val goalHigherProt = FoodComparisonEvaluator.evaluate(
            lowCalCandy, lentilStew, ComparisonMode.PER_SERVING, ComparisonGoal.HIGHER_PROTEIN
        )
        assertEquals(ComparisonWinner.RIGHT, goalHigherProt.greenHighlightSide)

        // Insufficient nutrition data prevents false winner badges
        val emptyFood = FoodItemEntity(
            id = 99L,
            nameEn = "Unverified Entry",
            nameFa = "مورد ناقص",
            category = "SNACKS",
            servingSizeGrams = 100.0,
            calories = 0,
            protein = 0.0,
            carbs = 0.0,
            fat = 0.0
        )
        val insufficientEval = FoodComparisonEvaluator.evaluate(
            emptyFood, lentilStew, ComparisonMode.PER_SERVING
        )
        assertTrue(insufficientEval.hasInsufficientData)
        assertEquals(ComparisonWinner.NONE, insufficientEval.greenHighlightSide)
    }
}
