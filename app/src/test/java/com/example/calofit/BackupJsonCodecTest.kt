package com.example.calofit

import com.example.calofit.data.model.ActivityRecordEntity
import com.example.calofit.data.model.FoodItemEntity
import com.example.calofit.data.model.FoodLogEntryEntity
import com.example.calofit.data.model.SavedRecipeEntity
import com.example.calofit.data.model.UserProfileEntity
import com.example.calofit.data.model.WaterRecordEntity
import com.example.calofit.data.model.WeightRecordEntity
import com.example.calofit.data.repository.BackupJsonCodec
import com.example.calofit.data.repository.BackupValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupJsonCodecTest {

    @Test
    fun backupJsonCodec_validatesBoundsAndRoundTripsCleanly() {
        val profile = UserProfileEntity(
            id = 1,
            name = "Reza",
            age = 30,
            gender = "MALE",
            heightCm = 178.0,
            currentWeightKg = 76.0,
            targetWeightKg = 72.0,
            activityLevel = "MODERATELY_ACTIVE",
            goal = "LOSE_WEIGHT",
            targetCalories = 2150,
            proteinGrams = 140,
            carbsGrams = 230,
            fatGrams = 65,
            waterMl = 2600,
            isSetupComplete = true
        )
        val customFood = FoodItemEntity(
            nameEn = "Homemade Soup",
            nameFa = "سوپ خانگی",
            category = "IRANIAN_MAINS",
            servingSizeGrams = 250.0,
            calories = 220,
            protein = 12.0,
            carbs = 28.0,
            fat = 6.0,
            fiber = 4.0,
            sugar = 3.0,
            sodiumMg = 320.0,
            isIranian = true,
            isVerified = false
        )
        val json = BackupJsonCodec.encode(
            profile = profile,
            customFoods = listOf(customFood),
            foodLogs = listOf(
                FoodLogEntryEntity(
                    dateIso = "2026-11-10",
                    mealType = "SNACK",
                    foodId = null,
                    foodName = "سوپ خانگی",
                    grams = 250.0,
                    calories = 220,
                    protein = 12.0,
                    carbs = 28.0,
                    fat = 6.0,
                    foodNameEn = "Homemade Soup",
                    foodNameFa = "سوپ خانگی"
                )
            ),
            weightRecords = listOf(WeightRecordEntity(dateIso = "2026-11-10", weightKg = 76.0)),
            waterRecords = listOf(WaterRecordEntity(dateIso = "2026-11-10", amountMl = 500)),
            activities = listOf(
                ActivityRecordEntity(
                    dateIso = "2026-11-10",
                    activityType = "WALKING",
                    durationMinutes = 30,
                    burnedCalories = 120
                )
            ),
            savedRecipes = listOf(
                SavedRecipeEntity(
                    name = "Ash Reshteh",
                    description = "Warm noodle soup",
                    ingredientsText = "Herbs, Noodles",
                    optionalAdditionsText = "",
                    instructionsText = "Simmer herbs and beans",
                    prepTimeMinutes = 45,
                    calories = 390,
                    proteinGrams = 15.0,
                    carbsGrams = 58.0,
                    fatGrams = 9.0,
                    isPersian = false
                )
            ),
            achievements = emptyList()
        )

        assertTrue(json.contains("\"KaloFit\""))

        val result = BackupJsonCodec.parseAndValidate(json)
        assertTrue(result is BackupValidationResult.Valid)
        val payload = (result as BackupValidationResult.Valid).payload
        assertEquals("Reza", payload.profile?.name)
        assertEquals(1, payload.customFoods.size)
        assertEquals(1, payload.foodLogs.size)
        assertEquals(1, payload.savedRecipes.size)

        // Legacy "Calo Fit" backup name remains backward-compatible for prior exports
        assertTrue(
            BackupJsonCodec.parseAndValidate(
                """{"appName":"Calo Fit","weightHistory":[{"date":"2026-11-10","weightKg":70.0}]}"""
            ) is BackupValidationResult.Valid
        )

        // Reject malformed or untrusted or out-of-bounds backups
        assertTrue(BackupJsonCodec.parseAndValidate("{bad json") is BackupValidationResult.Invalid)
        assertTrue(
            BackupJsonCodec.parseAndValidate("""{"appName":"OtherApp"}""") is BackupValidationResult.Invalid
        )
        assertTrue(
            BackupJsonCodec.parseAndValidate(
                """{"appName":"KaloFit","weightHistory":[{"date":"bad-date","weightKg":70.0}]}"""
            ) is BackupValidationResult.Invalid
        )
    }
}
