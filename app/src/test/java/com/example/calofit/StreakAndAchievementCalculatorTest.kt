package com.example.calofit

import com.example.calofit.data.database.InitialFoodData
import com.example.calofit.data.model.AchievementEntity
import com.example.calofit.data.model.FoodLogEntryEntity
import com.example.calofit.data.model.UserProfileEntity
import com.example.calofit.data.model.WaterRecordEntity
import com.example.calofit.data.model.WeightRecordEntity
import com.example.calofit.domain.calculator.StreakAndAchievementCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakAndAchievementCalculatorTest {

    private val baseProfile = UserProfileEntity(
        id = 1,
        name = "Test",
        targetCalories = 2000,
        proteinGrams = 120,
        waterMl = 2500
    )

    @Test
    fun consecutiveStreak_emptyOrStale_returnsZero() {
        assertEquals(0, StreakAndAchievementCalculator.calculateConsecutiveStreak(emptyList(), "2026-11-14"))
        assertEquals(0, StreakAndAchievementCalculator.calculateConsecutiveStreak(listOf("2026-11-10", "2026-11-11"), "2026-11-14"))
    }

    @Test
    fun consecutiveStreak_endingTodayOrYesterday_countsAccurately() {
        val endingToday = listOf("2026-11-14", "2026-11-13", "2026-11-12", "2026-11-10")
        assertEquals(3, StreakAndAchievementCalculator.calculateConsecutiveStreak(endingToday, "2026-11-14"))

        val endingYesterday = listOf("2026-11-13", "2026-11-12", "2026-11-11")
        assertEquals(3, StreakAndAchievementCalculator.calculateConsecutiveStreak(endingYesterday, "2026-11-14"))
    }

    @Test
    fun longestStreak_findsBestHistoricalRun() {
        val dates = listOf(
            "2026-11-01", "2026-11-02", "2026-11-03", "2026-11-04",
            "2026-11-10", "2026-11-11"
        )
        assertEquals(4, StreakAndAchievementCalculator.calculateLongestStreak(dates))
    }

    @Test
    fun evaluateAll_unlocksBronzeAndSilverAndPreservesPastUnlocks() {
        val logs = listOf(
            log("2026-11-12", cal = 1950, protein = 115.0),
            log("2026-11-13", cal = 2020, protein = 122.0),
            log("2026-11-14", cal = 1980, protein = 125.0)
        )
        val water = listOf(
            WaterRecordEntity(dateIso = "2026-11-14", amountMl = 2500)
        )
        val weights = listOf(
            WeightRecordEntity(dateIso = "2026-11-14", weightKg = 72.0)
        )

        val previouslyUnlockedSilver = InitialFoodData.getInitialAchievements().map { ach ->
            if (ach.code == "STREAK_7") {
                ach.copy(isUnlocked = true, unlockedDate = "2026-10-01", progressCurrent = 7)
            } else {
                ach
            }
        }

        val evaluations = StreakAndAchievementCalculator.evaluateAll(
            profile = baseProfile,
            allFoodLogs = logs,
            allWaterRecords = water,
            allWeightRecords = weights,
            existingAchievements = previouslyUnlockedSilver,
            todayIso = "2026-11-14"
        )

        assertEquals(9, evaluations.size)
        val byCode = evaluations.associateBy { it.code }

        assertTrue(byCode.getValue("FIRST_LOG").shouldUnlock)
        assertTrue(byCode.getValue("FIRST_WEIGHT").shouldUnlock)
        assertTrue(byCode.getValue("HYDRATION_HERO").shouldUnlock)
        assertTrue(byCode.getValue("STREAK_3").shouldUnlock)
        assertTrue(byCode.getValue("MACRO_BALANCE").shouldUnlock)

        // Previously unlocked STREAK_7 stays unlocked with its historical date even though current streak is 3
        val streak7 = byCode.getValue("STREAK_7")
        assertTrue(streak7.shouldUnlock)
        assertEquals("2026-10-01", streak7.unlockedDate)
        assertEquals(7, streak7.progressCurrent)

        // Gold STREAK_30 remains locked
        val streak30 = byCode.getValue("STREAK_30")
        assertFalse(streak30.shouldUnlock)
        assertEquals(3, streak30.progressCurrent)
        assertEquals(30, streak30.progressTarget)
    }

    private fun log(date: String, cal: Int, protein: Double): FoodLogEntryEntity {
        return FoodLogEntryEntity(
            dateIso = date,
            mealType = "LUNCH",
            foodName = "Test Meal",
            foodNameEn = "Test Meal",
            foodNameFa = "غذای تست",
            grams = 200.0,
            calories = cal,
            protein = protein,
            carbs = 150.0,
            fat = 50.0
        )
    }
}
