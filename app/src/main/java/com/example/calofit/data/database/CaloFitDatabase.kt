package com.example.calofit.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.calofit.data.dao.AchievementDao
import com.example.calofit.data.dao.ActivityDao
import com.example.calofit.data.dao.FoodDao
import com.example.calofit.data.dao.FoodLogDao
import com.example.calofit.data.dao.MealPlanDao
import com.example.calofit.data.dao.SavedRecipeDao
import com.example.calofit.data.dao.UserProfileDao
import com.example.calofit.data.dao.WaterDao
import com.example.calofit.data.dao.WeightDao
import com.example.calofit.data.model.AchievementEntity
import com.example.calofit.data.model.ActivityRecordEntity
import com.example.calofit.data.model.FoodItemEntity
import com.example.calofit.data.model.FoodLogEntryEntity
import com.example.calofit.data.model.MealPlanItemEntity
import com.example.calofit.data.model.SavedRecipeEntity
import com.example.calofit.data.model.UserProfileEntity
import com.example.calofit.data.model.WaterRecordEntity
import com.example.calofit.data.model.WeightRecordEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Database(
    entities = [
        UserProfileEntity::class,
        FoodItemEntity::class,
        FoodLogEntryEntity::class,
        WeightRecordEntity::class,
        WaterRecordEntity::class,
        ActivityRecordEntity::class,
        MealPlanItemEntity::class,
        AchievementEntity::class,
        SavedRecipeEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class CaloFitDatabase : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileDao
    abstract fun foodDao(): FoodDao
    abstract fun foodLogDao(): FoodLogDao
    abstract fun weightDao(): WeightDao
    abstract fun waterDao(): WaterDao
    abstract fun activityDao(): ActivityDao
    abstract fun mealPlanDao(): MealPlanDao
    abstract fun achievementDao(): AchievementDao
    abstract fun savedRecipeDao(): SavedRecipeDao

    companion object {
        @Volatile
        private var INSTANCE: CaloFitDatabase? = null
        private val seedMutex = Mutex()

        /**
         * Non-destructive migration from v1 to v2:
         * - Preserves all user_profile, food_items (including custom foods), food_logs, weight_records,
         *   water_records, activity_records, and unlocked achievements.
         * - Adds foodNameEn / foodNameFa on food_logs for bilingual log display.
         * - Adds tier / progressCurrent / progressTarget on achievements.
         * - Standardizes AFTERNOON_SNACK -> SNACK.
         * - Repoints any duplicate seeded food references and deduplicates food_items safely.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE food_logs ADD COLUMN foodNameEn TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE food_logs ADD COLUMN foodNameFa TEXT NOT NULL DEFAULT ''")

                db.execSQL("ALTER TABLE achievements ADD COLUMN tier TEXT NOT NULL DEFAULT 'BRONZE'")
                db.execSQL("ALTER TABLE achievements ADD COLUMN progressCurrent INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE achievements ADD COLUMN progressTarget INTEGER NOT NULL DEFAULT 1")

                db.execSQL("UPDATE food_logs SET mealType = 'SNACK' WHERE mealType = 'AFTERNOON_SNACK'")
                db.execSQL("UPDATE meal_plans SET mealType = 'SNACK' WHERE mealType = 'AFTERNOON_SNACK'")

                db.execSQL("UPDATE food_logs SET foodNameFa = foodName WHERE foodNameFa = '' AND foodName != ''")
                db.execSQL(
                    """
                    UPDATE food_logs
                    SET foodNameEn = COALESCE(
                        (SELECT nameEn FROM food_items WHERE food_items.id = food_logs.foodId LIMIT 1),
                        foodName
                    )
                    WHERE foodNameEn = ''
                    """.trimIndent()
                )

                // Repoint food_logs.foodId to the lowest matching id before removing exact duplicates
                db.execSQL(
                    """
                    UPDATE food_logs
                    SET foodId = (
                        SELECT MIN(f2.id)
                        FROM food_items f1
                        JOIN food_items f2
                          ON f1.nameEn = f2.nameEn
                         AND f1.nameFa = f2.nameFa
                         AND f1.servingSizeGrams = f2.servingSizeGrams
                         AND f1.calories = f2.calories
                        WHERE f1.id = food_logs.foodId
                    )
                    WHERE foodId IS NOT NULL
                      AND EXISTS (SELECT 1 FROM food_items WHERE food_items.id = food_logs.foodId)
                    """.trimIndent()
                )

                // Deduplicate identical food_items rows while keeping all unique items and custom entries
                db.execSQL(
                    """
                    DELETE FROM food_items
                    WHERE id NOT IN (
                        SELECT MIN(id)
                        FROM food_items
                        GROUP BY nameEn, nameFa, servingSizeGrams, calories
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Non-destructive migration from v2 to v3:
         * - Adds sugar and sodiumMg to food_items.
         * - Creates saved_recipes table for locally saved Smart Chef recipes.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE food_items ADD COLUMN sugar REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE food_items ADD COLUMN sodiumMg REAL NOT NULL DEFAULT 0.0")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS saved_recipes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        ingredientsText TEXT NOT NULL,
                        optionalAdditionsText TEXT NOT NULL,
                        instructionsText TEXT NOT NULL,
                        prepTimeMinutes INTEGER NOT NULL,
                        calories INTEGER NOT NULL,
                        proteinGrams REAL NOT NULL,
                        carbsGrams REAL NOT NULL,
                        fatGrams REAL NOT NULL,
                        isPersian INTEGER NOT NULL,
                        savedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): CaloFitDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CaloFitDatabase::class.java,
                    "calofit_local_database.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialDatabase(database)
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialDatabase(database)
                    }
                }
            }
        }

        suspend fun populateInitialDatabase(database: CaloFitDatabase) {
            seedMutex.withLock {
                database.withTransaction {
                    val existingFoods = database.foodDao().getAllFoodsOnce()
                    val seedFoods = InitialFoodData.getInitialFoods()
                    if (existingFoods.isEmpty()) {
                        database.foodDao().insertFoods(seedFoods)
                    } else {
                        // Backfill sugar/sodiumMg only for verified seeded items that still have 0.0/0.0
                        val seedByName = seedFoods.associateBy { it.nameEn }
                        existingFoods.forEach { existing ->
                            if (existing.isVerified && existing.sugar == 0.0 && existing.sodiumMg == 0.0) {
                                val updated = seedByName[existing.nameEn]
                                if (updated != null && (updated.sugar > 0.0 || updated.sodiumMg > 0.0)) {
                                    database.foodDao().updateFood(
                                        existing.copy(sugar = updated.sugar, sodiumMg = updated.sodiumMg)
                                    )
                                }
                            }
                        }
                    }
                    // Insert any newly added achievements without overwriting existing unlocked progress
                    database.achievementDao().insertAchievementsIgnore(
                        InitialFoodData.getInitialAchievements()
                    )
                    // Ensure all 7 days of meal plans exist without erasing if already populated for 7 days
                    val existingPlanCount = database.mealPlanDao().getCount()
                    val initialPlans = InitialFoodData.getInitialMealPlans()
                    if (existingPlanCount < initialPlans.size) {
                        database.mealPlanDao().clearAll()
                        database.mealPlanDao().insertPlans(initialPlans)
                    }
                }
            }
        }
    }
}
