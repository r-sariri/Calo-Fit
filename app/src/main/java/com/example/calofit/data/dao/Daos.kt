package com.example.calofit.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.calofit.data.model.AchievementEntity
import com.example.calofit.data.model.ActivityRecordEntity
import com.example.calofit.data.model.FoodItemEntity
import com.example.calofit.data.model.FoodLogEntryEntity
import com.example.calofit.data.model.MealPlanItemEntity
import com.example.calofit.data.model.SavedRecipeEntity
import com.example.calofit.data.model.UserProfileEntity
import com.example.calofit.data.model.WaterRecordEntity
import com.example.calofit.data.model.WeightRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfileOnce(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(profile: UserProfileEntity)

    @Update
    suspend fun updateUserProfile(profile: UserProfileEntity)
}

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_items ORDER BY isIranian DESC, nameFa ASC")
    fun getAllFoods(): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items ORDER BY isIranian DESC, nameFa ASC")
    suspend fun getAllFoodsOnce(): List<FoodItemEntity>

    @Query(
        """
        SELECT * FROM food_items
        WHERE nameFa LIKE '%' || :query || '%'
           OR nameEn LIKE '%' || :query || '%'
        ORDER BY isIranian DESC, calories ASC
        """
    )
    fun searchFoods(query: String): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE category = :category ORDER BY nameFa ASC")
    fun getFoodsByCategory(category: String): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE id = :id LIMIT 1")
    suspend fun getFoodById(id: Long): FoodItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoods(foods: List<FoodItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: FoodItemEntity): Long

    @Update
    suspend fun updateFood(food: FoodItemEntity)

    @Query("DELETE FROM food_items WHERE id = :id AND isVerified = 0")
    suspend fun deleteCustomFoodById(id: Long)

    @Query("DELETE FROM food_items WHERE isVerified = 0")
    suspend fun clearCustomFoods()

    @Query("SELECT COUNT(*) FROM food_items")
    suspend fun getCount(): Int
}

@Dao
interface FoodLogDao {
    @Query("SELECT * FROM food_logs WHERE dateIso = :dateIso ORDER BY timestamp DESC")
    fun getLogsForDate(dateIso: String): Flow<List<FoodLogEntryEntity>>

    @Query("SELECT * FROM food_logs ORDER BY dateIso DESC, timestamp DESC")
    fun getAllLogs(): Flow<List<FoodLogEntryEntity>>

    @Query("SELECT * FROM food_logs ORDER BY dateIso DESC, timestamp DESC")
    suspend fun getAllLogsOnce(): List<FoodLogEntryEntity>

    @Query("SELECT * FROM food_logs WHERE id = :id LIMIT 1")
    suspend fun getLogById(id: Long): FoodLogEntryEntity?

    @Query("SELECT * FROM food_logs WHERE dateIso BETWEEN :startDateIso AND :endDateIso ORDER BY dateIso ASC, timestamp ASC")
    fun getLogsInRange(startDateIso: String, endDateIso: String): Flow<List<FoodLogEntryEntity>>

    @Query("SELECT DISTINCT dateIso FROM food_logs ORDER BY dateIso DESC")
    suspend fun getDistinctLoggedDatesOnce(): List<String>

    @Query("SELECT * FROM food_logs WHERE dateIso = :dateIso AND mealType = :mealType ORDER BY timestamp DESC")
    fun getLogsForMeal(dateIso: String, mealType: String): Flow<List<FoodLogEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(entry: FoodLogEntryEntity): Long

    @Update
    suspend fun updateLog(entry: FoodLogEntryEntity)

    @Query("DELETE FROM food_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("DELETE FROM food_logs")
    suspend fun clearAll()

    @Query("SELECT SUM(calories) FROM food_logs WHERE dateIso = :dateIso")
    fun getTotalCaloriesForDate(dateIso: String): Flow<Int?>

    @Query("SELECT * FROM food_logs ORDER BY timestamp DESC LIMIT 10")
    fun getRecentLogs(): Flow<List<FoodLogEntryEntity>>
}

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_records ORDER BY dateIso ASC")
    fun getAllWeightRecords(): Flow<List<WeightRecordEntity>>

    @Query("SELECT * FROM weight_records ORDER BY dateIso ASC")
    suspend fun getAllWeightRecordsOnce(): List<WeightRecordEntity>

    @Query("SELECT * FROM weight_records ORDER BY dateIso DESC LIMIT 1")
    fun getLatestWeight(): Flow<WeightRecordEntity?>

    @Query("SELECT COUNT(*) FROM weight_records")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(record: WeightRecordEntity)

    @Query("DELETE FROM weight_records WHERE id = :id")
    suspend fun deleteWeightById(id: Long)

    @Query("DELETE FROM weight_records")
    suspend fun clearAll()
}

@Dao
interface WaterDao {
    @Query("SELECT * FROM water_records WHERE dateIso = :dateIso ORDER BY timestamp DESC")
    fun getWaterRecordsForDate(dateIso: String): Flow<List<WaterRecordEntity>>

    @Query("SELECT * FROM water_records ORDER BY dateIso ASC")
    fun getAllWaterRecords(): Flow<List<WaterRecordEntity>>

    @Query("SELECT * FROM water_records ORDER BY dateIso ASC")
    suspend fun getAllWaterRecordsOnce(): List<WaterRecordEntity>

    @Query("SELECT COALESCE(SUM(amountMl), 0) FROM water_records WHERE dateIso = :dateIso")
    fun getTotalWaterForDate(dateIso: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWater(record: WaterRecordEntity)

    @Query("DELETE FROM water_records WHERE dateIso = :dateIso")
    suspend fun clearWaterForDate(dateIso: String)

    @Query("DELETE FROM water_records")
    suspend fun clearAll()
}

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activity_records WHERE dateIso = :dateIso ORDER BY timestamp DESC")
    fun getActivitiesForDate(dateIso: String): Flow<List<ActivityRecordEntity>>

    @Query("SELECT * FROM activity_records ORDER BY dateIso DESC, timestamp DESC")
    fun getAllActivities(): Flow<List<ActivityRecordEntity>>

    @Query("SELECT * FROM activity_records ORDER BY dateIso DESC, timestamp DESC")
    suspend fun getAllActivitiesOnce(): List<ActivityRecordEntity>

    @Query("SELECT COALESCE(SUM(burnedCalories), 0) FROM activity_records WHERE dateIso = :dateIso")
    fun getTotalBurnedCaloriesForDate(dateIso: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(record: ActivityRecordEntity)

    @Query("DELETE FROM activity_records WHERE id = :id")
    suspend fun deleteActivityById(id: Long)

    @Query("DELETE FROM activity_records")
    suspend fun clearAll()
}

@Dao
interface MealPlanDao {
    @Query("SELECT * FROM meal_plans WHERE dayOfWeek = :dayOfWeek ORDER BY id ASC")
    fun getPlansForDay(dayOfWeek: Int): Flow<List<MealPlanItemEntity>>

    @Query("SELECT * FROM meal_plans ORDER BY dayOfWeek ASC, id ASC")
    fun getAllPlans(): Flow<List<MealPlanItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlans(plans: List<MealPlanItemEntity>)

    @Query("DELETE FROM meal_plans")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM meal_plans")
    suspend fun getCount(): Int
}

@Dao
interface SavedRecipeDao {
    @Query("SELECT * FROM saved_recipes ORDER BY savedAt DESC")
    fun getAllSavedRecipes(): Flow<List<SavedRecipeEntity>>

    @Query("SELECT * FROM saved_recipes ORDER BY savedAt DESC")
    suspend fun getAllSavedRecipesOnce(): List<SavedRecipeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedRecipe(recipe: SavedRecipeEntity): Long

    @Query("DELETE FROM saved_recipes WHERE id = :id")
    suspend fun deleteSavedRecipeById(id: Long)

    @Query("DELETE FROM saved_recipes")
    suspend fun clearAll()
}

@Dao
interface AchievementDao {
    @Query(
        """
        SELECT * FROM achievements
        ORDER BY CASE tier WHEN 'BRONZE' THEN 1 WHEN 'SILVER' THEN 2 WHEN 'GOLD' THEN 3 ELSE 4 END ASC,
        isUnlocked DESC, id ASC
        """
    )
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements")
    suspend fun getAllAchievementsOnce(): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievementsIgnore(achievements: List<AchievementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<AchievementEntity>)

    @Query("UPDATE achievements SET isUnlocked = 1, unlockedDate = :dateIso WHERE code = :code")
    suspend fun unlockAchievement(code: String, dateIso: String)

    @Query(
        """
        UPDATE achievements
        SET tier = :tier,
            titleEn = :titleEn,
            titleFa = :titleFa,
            descriptionEn = :descriptionEn,
            descriptionFa = :descriptionFa,
            progressCurrent = :progressCurrent,
            progressTarget = :progressTarget,
            isUnlocked = CASE WHEN isUnlocked = 1 THEN 1 ELSE :isUnlocked END,
            unlockedDate = CASE WHEN unlockedDate IS NOT NULL THEN unlockedDate ELSE :unlockedDate END
        WHERE code = :code
        """
    )
    suspend fun syncAchievementProgress(
        code: String,
        tier: String,
        titleEn: String,
        titleFa: String,
        descriptionEn: String,
        descriptionFa: String,
        progressCurrent: Int,
        progressTarget: Int,
        isUnlocked: Boolean,
        unlockedDate: String?
    )

    @Query("SELECT COUNT(*) FROM achievements")
    suspend fun getCount(): Int
}
