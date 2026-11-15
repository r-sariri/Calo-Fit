package com.example.calofit.data.repository

import androidx.room.withTransaction
import com.example.calofit.data.ai.GeneratedRecipe
import com.example.calofit.data.dao.AchievementDao
import com.example.calofit.data.dao.ActivityDao
import com.example.calofit.data.dao.FoodDao
import com.example.calofit.data.dao.FoodLogDao
import com.example.calofit.data.dao.MealPlanDao
import com.example.calofit.data.dao.SavedRecipeDao
import com.example.calofit.data.dao.UserProfileDao
import com.example.calofit.data.dao.WaterDao
import com.example.calofit.data.dao.WeightDao
import com.example.calofit.data.database.CaloFitDatabase
import com.example.calofit.data.database.InitialFoodData
import com.example.calofit.data.model.AchievementEntity
import com.example.calofit.data.model.ActivityRecordEntity
import com.example.calofit.data.model.FoodItemEntity
import com.example.calofit.data.model.FoodLogEntryEntity
import com.example.calofit.data.model.MealPlanItemEntity
import com.example.calofit.data.model.SavedRecipeEntity
import com.example.calofit.data.model.UserProfileEntity
import com.example.calofit.data.model.WaterRecordEntity
import com.example.calofit.data.model.WeightRecordEntity
import com.example.calofit.data.preferences.UserPreferencesManager
import com.example.calofit.domain.calculator.StreakAndAchievementCalculator
import com.example.calofit.domain.calendar.JalaliCalendarHelper
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToInt

class CaloFitRepository(
    private val userProfileDao: UserProfileDao,
    private val foodDao: FoodDao,
    private val foodLogDao: FoodLogDao,
    private val weightDao: WeightDao,
    private val waterDao: WaterDao,
    private val activityDao: ActivityDao,
    private val mealPlanDao: MealPlanDao,
    private val achievementDao: AchievementDao,
    private val savedRecipeDao: SavedRecipeDao,
    val preferencesManager: UserPreferencesManager,
    private val database: CaloFitDatabase? = null
) {
    val userProfile: Flow<UserProfileEntity?> = userProfileDao.getUserProfile()

    suspend fun saveUserProfile(profile: UserProfileEntity) {
        userProfileDao.saveUserProfile(profile)
        refreshAchievements()
    }

    suspend fun updateProfilePreservingWeightHistory(profile: UserProfileEntity) {
        userProfileDao.saveUserProfile(profile)
        refreshAchievements()
    }

    suspend fun getUserProfileOnce(): UserProfileEntity? {
        return userProfileDao.getUserProfileOnce()
    }

    fun getAllFoods(): Flow<List<FoodItemEntity>> = foodDao.getAllFoods()

    fun searchFoods(query: String): Flow<List<FoodItemEntity>> {
        return if (query.isBlank()) foodDao.getAllFoods() else foodDao.searchFoods(query.trim())
    }

    suspend fun insertCustomFood(food: FoodItemEntity): Long {
        return foodDao.insertFood(food)
    }

    suspend fun updateCustomFood(food: FoodItemEntity) {
        if (!food.isVerified && food.id > 0L) {
            foodDao.updateFood(food)
        }
    }

    suspend fun deleteCustomFood(id: Long) {
        foodDao.deleteCustomFoodById(id)
    }

    val allFoodLogs: Flow<List<FoodLogEntryEntity>> = foodLogDao.getAllLogs()

    fun getLogsForDate(dateIso: String): Flow<List<FoodLogEntryEntity>> {
        return foodLogDao.getLogsForDate(dateIso)
    }

    suspend fun addFoodLogEntry(
        dateIso: String,
        mealType: String,
        foodItem: FoodItemEntity,
        grams: Double
    ) {
        val safeServing = foodItem.servingSizeGrams.coerceAtLeast(1.0)
        val multiplier = grams / safeServing
        val calories = (foodItem.calories * multiplier).roundToInt()
        val protein = ((foodItem.protein * multiplier) * 10).roundToInt() / 10.0
        val carbs = ((foodItem.carbs * multiplier) * 10).roundToInt() / 10.0
        val fat = ((foodItem.fat * multiplier) * 10).roundToInt() / 10.0

        val normalizedMeal = if (mealType == "AFTERNOON_SNACK") "SNACK" else mealType

        val entry = FoodLogEntryEntity(
            dateIso = dateIso,
            mealType = normalizedMeal,
            foodId = if (foodItem.id > 0L) foodItem.id else null,
            foodName = foodItem.nameFa.ifBlank { foodItem.nameEn },
            grams = grams,
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat,
            foodNameEn = foodItem.nameEn,
            foodNameFa = foodItem.nameFa
        )
        foodLogDao.insertLog(entry)
        refreshAchievements(dateIso)
    }

    suspend fun updateFoodLogQuantity(entry: FoodLogEntryEntity, newGrams: Double) {
        val safeNewGrams = newGrams.coerceIn(1.0, 10000.0)
        val linkedFood = entry.foodId?.let { foodDao.getFoodById(it) }
        val updated = if (linkedFood != null) {
            val safeServing = linkedFood.servingSizeGrams.coerceAtLeast(1.0)
            val ratio = safeNewGrams / safeServing
            entry.copy(
                grams = safeNewGrams,
                calories = (linkedFood.calories * ratio).roundToInt().coerceAtLeast(0),
                protein = ((linkedFood.protein * ratio) * 10).roundToInt() / 10.0,
                carbs = ((linkedFood.carbs * ratio) * 10).roundToInt() / 10.0,
                fat = ((linkedFood.fat * ratio) * 10).roundToInt() / 10.0
            )
        } else {
            val oldGrams = entry.grams.coerceAtLeast(1.0)
            val ratio = safeNewGrams / oldGrams
            entry.copy(
                grams = safeNewGrams,
                calories = (entry.calories * ratio).roundToInt().coerceAtLeast(0),
                protein = ((entry.protein * ratio) * 10).roundToInt() / 10.0,
                carbs = ((entry.carbs * ratio) * 10).roundToInt() / 10.0,
                fat = ((entry.fat * ratio) * 10).roundToInt() / 10.0
            )
        }
        foodLogDao.updateLog(updated)
        refreshAchievements(entry.dateIso)
    }

    suspend fun deleteFoodLog(id: Long) {
        foodLogDao.deleteLogById(id)
        refreshAchievements()
    }

    val weightRecords: Flow<List<WeightRecordEntity>> = weightDao.getAllWeightRecords()
    val latestWeight: Flow<WeightRecordEntity?> = weightDao.getLatestWeight()

    suspend fun logWeight(dateIso: String, weightKg: Double, note: String = "") {
        weightDao.insertWeight(
            WeightRecordEntity(
                dateIso = dateIso,
                weightKg = weightKg,
                note = note
            )
        )
        val profile = userProfileDao.getUserProfileOnce()
        if (profile != null) {
            userProfileDao.saveUserProfile(
                profile.copy(
                    currentWeightKg = weightKg,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        refreshAchievements(dateIso)
    }

    val allWaterRecords: Flow<List<WaterRecordEntity>> = waterDao.getAllWaterRecords()

    fun getWaterTotalForDate(dateIso: String): Flow<Int> = waterDao.getTotalWaterForDate(dateIso)

    suspend fun logWater(dateIso: String, amountMl: Int) {
        waterDao.insertWater(
            WaterRecordEntity(
                dateIso = dateIso,
                amountMl = amountMl
            )
        )
        refreshAchievements(dateIso)
    }

    val allActivities: Flow<List<ActivityRecordEntity>> = activityDao.getAllActivities()

    fun getActivitiesForDate(dateIso: String): Flow<List<ActivityRecordEntity>> =
        activityDao.getActivitiesForDate(dateIso)

    fun getTotalBurnedCaloriesForDate(dateIso: String): Flow<Int> =
        activityDao.getTotalBurnedCaloriesForDate(dateIso)

    suspend fun logActivity(
        dateIso: String,
        activityType: String,
        durationMinutes: Int,
        burnedCalories: Int
    ) {
        activityDao.insertActivity(
            ActivityRecordEntity(
                dateIso = dateIso,
                activityType = activityType,
                durationMinutes = durationMinutes,
                burnedCalories = burnedCalories
            )
        )
    }

    val allMealPlans: Flow<List<MealPlanItemEntity>> = mealPlanDao.getAllPlans()

    fun getMealPlansForDay(dayOfWeek: Int): Flow<List<MealPlanItemEntity>> =
        mealPlanDao.getPlansForDay(dayOfWeek)

    val savedRecipes: Flow<List<SavedRecipeEntity>> = savedRecipeDao.getAllSavedRecipes()

    suspend fun saveRecipe(recipe: GeneratedRecipe, isPersian: Boolean): Long {
        return savedRecipeDao.insertSavedRecipe(
            SavedRecipeEntity(
                name = recipe.name,
                description = recipe.description,
                ingredientsText = recipe.ingredients.joinToString("\n"),
                optionalAdditionsText = recipe.optionalAdditions.joinToString("\n"),
                instructionsText = recipe.instructions.joinToString("\n"),
                prepTimeMinutes = recipe.prepTimeMinutes,
                calories = recipe.calories,
                proteinGrams = recipe.proteinGrams,
                carbsGrams = recipe.carbsGrams,
                fatGrams = recipe.fatGrams,
                isPersian = isPersian
            )
        )
    }

    suspend fun deleteSavedRecipe(id: Long) {
        savedRecipeDao.deleteSavedRecipeById(id)
    }

    val achievements: Flow<List<AchievementEntity>> = achievementDao.getAllAchievements()

    suspend fun refreshAchievements(todayIso: String = JalaliCalendarHelper.getTodayIsoDate()) {
        achievementDao.insertAchievementsIgnore(InitialFoodData.getInitialAchievements())
        val profile = userProfileDao.getUserProfileOnce()
        val logs = foodLogDao.getAllLogsOnce()
        val water = waterDao.getAllWaterRecordsOnce()
        val weights = weightDao.getAllWeightRecordsOnce()
        val existing = achievementDao.getAllAchievementsOnce()

        val evaluations = StreakAndAchievementCalculator.evaluateAll(
            profile = profile,
            allFoodLogs = logs,
            allWaterRecords = water,
            allWeightRecords = weights,
            existingAchievements = existing,
            todayIso = todayIso
        )

        evaluations.forEach { eval ->
            achievementDao.syncAchievementProgress(
                code = eval.code,
                tier = eval.tier,
                titleEn = eval.titleEn,
                titleFa = eval.titleFa,
                descriptionEn = eval.descriptionEn,
                descriptionFa = eval.descriptionFa,
                progressCurrent = eval.progressCurrent,
                progressTarget = eval.progressTarget,
                isUnlocked = eval.shouldUnlock,
                unlockedDate = eval.unlockedDate
            )
        }
    }

    suspend fun restoreFromBackup(payload: ValidatedBackupPayload) {
        val applyBlock: suspend () -> Unit = {
            foodLogDao.clearAll()
            weightDao.clearAll()
            waterDao.clearAll()
            activityDao.clearAll()
            foodDao.clearCustomFoods()
            savedRecipeDao.clearAll()

            payload.profile?.let { userProfileDao.saveUserProfile(it) }
            if (payload.customFoods.isNotEmpty()) {
                foodDao.insertFoods(payload.customFoods)
            }
            payload.foodLogs.forEach { foodLogDao.insertLog(it) }
            payload.weightHistory.forEach { weightDao.insertWeight(it) }
            payload.waterHistory.forEach { waterDao.insertWater(it) }
            payload.activityHistory.forEach { activityDao.insertActivity(it) }
            payload.savedRecipes.forEach { savedRecipeDao.insertSavedRecipe(it) }

            achievementDao.insertAchievementsIgnore(InitialFoodData.getInitialAchievements())
            payload.unlockedAchievementDates.forEach { (code, dateIso) ->
                if (dateIso.isNotBlank()) {
                    achievementDao.unlockAchievement(code, dateIso)
                }
            }
        }
        if (database != null) {
            database.withTransaction { applyBlock() }
        } else {
            applyBlock()
        }
        refreshAchievements()
    }
}
