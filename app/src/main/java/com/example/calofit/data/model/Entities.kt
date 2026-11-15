package com.example.calofit.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val age: Int = 25,
    val gender: String = "MALE", // MALE, FEMALE
    val heightCm: Double = 175.0,
    val currentWeightKg: Double = 70.0,
    val targetWeightKg: Double = 68.0,
    val activityLevel: String = "MODERATELY_ACTIVE",
    val goal: String = "LOSE_WEIGHT", // LOSE_WEIGHT, MAINTAIN_WEIGHT, GAIN_WEIGHT
    val targetCalories: Int = 2000,
    val proteinGrams: Int = 125,
    val carbsGrams: Int = 250,
    val fatGrams: Int = 55,
    val waterMl: Int = 2500,
    val dietaryPreference: String = "STANDARD",
    val allergies: String = "",
    val isSetupComplete: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "food_items",
    indices = [
        Index(value = ["nameFa"]),
        Index(value = ["nameEn"]),
        Index(value = ["category"])
    ]
)
data class FoodItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nameEn: String,
    val nameFa: String,
    val category: String, // IRANIAN_MAINS, BREAD_GRAINS, MEAT_PROTEIN, DAIRY, FRUITS, VEGETABLES, SNACKS, BEVERAGES
    val servingSizeGrams: Double = 100.0,
    val servingUnitEn: String = "100g",
    val servingUnitFa: String = "۱۰۰ گرم",
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double = 0.0,
    val sugar: Double = 0.0,
    val sodiumMg: Double = 0.0,
    val isIranian: Boolean = false,
    val isVerified: Boolean = true
)

@Entity(
    tableName = "food_logs",
    indices = [
        Index(value = ["dateIso"]),
        Index(value = ["mealType"])
    ]
)
data class FoodLogEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateIso: String, // YYYY-MM-DD
    val mealType: String, // BREAKFAST, MORNING_SNACK, LUNCH, SNACK, DINNER, EVENING_SNACK
    val foodId: Long? = null,
    val foodName: String,
    val grams: Double,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val foodNameEn: String = "",
    val foodNameFa: String = ""
) {
    fun localizedName(isPersian: Boolean): String {
        return if (isPersian) {
            foodNameFa.ifBlank { foodName.ifBlank { foodNameEn } }
        } else {
            foodNameEn.ifBlank { foodName.ifBlank { foodNameFa } }
        }
    }
}

@Entity(
    tableName = "weight_records",
    indices = [Index(value = ["dateIso"], unique = true)]
)
data class WeightRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateIso: String,
    val weightKg: Double,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "water_records",
    indices = [Index(value = ["dateIso"])]
)
data class WaterRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateIso: String,
    val amountMl: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "activity_records",
    indices = [Index(value = ["dateIso"])]
)
data class ActivityRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateIso: String,
    val activityType: String, // WALKING, RUNNING, CYCLING, GYM, SWIMMING, YOGA, HOME_WORKOUT
    val durationMinutes: Int,
    val burnedCalories: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "meal_plans")
data class MealPlanItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayOfWeek: Int, // 1 = Saturday/Mon to 7
    val mealType: String, // BREAKFAST, LUNCH, DINNER, SNACK
    val titleEn: String,
    val titleFa: String,
    val recipeSummaryEn: String,
    val recipeSummaryFa: String,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

@Entity(
    tableName = "achievements",
    indices = [Index(value = ["code"], unique = true)]
)
data class AchievementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val titleEn: String,
    val titleFa: String,
    val descriptionEn: String,
    val descriptionFa: String,
    val iconName: String,
    val isUnlocked: Boolean = false,
    val unlockedDate: String? = null,
    val tier: String = "BRONZE", // BRONZE, SILVER, GOLD
    val progressCurrent: Int = 0,
    val progressTarget: Int = 1
)

@Entity(tableName = "saved_recipes")
data class SavedRecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val ingredientsText: String,
    val optionalAdditionsText: String = "",
    val instructionsText: String,
    val prepTimeMinutes: Int = 20,
    val calories: Int = 0,
    val proteinGrams: Double = 0.0,
    val carbsGrams: Double = 0.0,
    val fatGrams: Double = 0.0,
    val isPersian: Boolean = true,
    val savedAt: Long = System.currentTimeMillis()
)
