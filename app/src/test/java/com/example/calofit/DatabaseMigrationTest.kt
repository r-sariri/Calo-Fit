package com.example.calofit

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.calofit.data.database.CaloFitDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DatabaseMigrationTest {

    private lateinit var context: Context
    private val dbName = "migration_test_v1_to_v2.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun migrate1To2_preservesUserAndCustomFoodData_fixesSnackAndDuplicates() = runBlocking {
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        // Create authentic Room v1 schema and seed user data + duplicate food + AFTERNOON_SNACK
        val v1Db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        v1Db.execSQL("PRAGMA user_version = 1")
        v1Db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS user_profile (
                id INTEGER NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                age INTEGER NOT NULL,
                gender TEXT NOT NULL,
                heightCm REAL NOT NULL,
                currentWeightKg REAL NOT NULL,
                targetWeightKg REAL NOT NULL,
                activityLevel TEXT NOT NULL,
                goal TEXT NOT NULL,
                targetCalories INTEGER NOT NULL,
                proteinGrams INTEGER NOT NULL,
                carbsGrams INTEGER NOT NULL,
                fatGrams INTEGER NOT NULL,
                waterMl INTEGER NOT NULL,
                dietaryPreference TEXT NOT NULL,
                allergies TEXT NOT NULL,
                isSetupComplete INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        v1Db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS food_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                nameEn TEXT NOT NULL,
                nameFa TEXT NOT NULL,
                category TEXT NOT NULL,
                servingSizeGrams REAL NOT NULL,
                servingUnitEn TEXT NOT NULL,
                servingUnitFa TEXT NOT NULL,
                calories INTEGER NOT NULL,
                protein REAL NOT NULL,
                carbs REAL NOT NULL,
                fat REAL NOT NULL,
                fiber REAL NOT NULL,
                isIranian INTEGER NOT NULL,
                isVerified INTEGER NOT NULL
            )
            """.trimIndent()
        )
        v1Db.execSQL("CREATE INDEX IF NOT EXISTS index_food_items_nameFa ON food_items (nameFa)")
        v1Db.execSQL("CREATE INDEX IF NOT EXISTS index_food_items_nameEn ON food_items (nameEn)")
        v1Db.execSQL("CREATE INDEX IF NOT EXISTS index_food_items_category ON food_items (category)")

        v1Db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS food_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                dateIso TEXT NOT NULL,
                mealType TEXT NOT NULL,
                foodId INTEGER,
                foodName TEXT NOT NULL,
                grams REAL NOT NULL,
                calories INTEGER NOT NULL,
                protein REAL NOT NULL,
                carbs REAL NOT NULL,
                fat REAL NOT NULL,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
        v1Db.execSQL("CREATE INDEX IF NOT EXISTS index_food_logs_dateIso ON food_logs (dateIso)")
        v1Db.execSQL("CREATE INDEX IF NOT EXISTS index_food_logs_mealType ON food_logs (mealType)")

        v1Db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS weight_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                dateIso TEXT NOT NULL,
                weightKg REAL NOT NULL,
                note TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
        v1Db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_weight_records_dateIso ON weight_records (dateIso)")

        v1Db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS water_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                dateIso TEXT NOT NULL,
                amountMl INTEGER NOT NULL,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
        v1Db.execSQL("CREATE INDEX IF NOT EXISTS index_water_records_dateIso ON water_records (dateIso)")

        v1Db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS activity_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                dateIso TEXT NOT NULL,
                activityType TEXT NOT NULL,
                durationMinutes INTEGER NOT NULL,
                burnedCalories INTEGER NOT NULL,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
        v1Db.execSQL("CREATE INDEX IF NOT EXISTS index_activity_records_dateIso ON activity_records (dateIso)")

        v1Db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS meal_plans (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                dayOfWeek INTEGER NOT NULL,
                mealType TEXT NOT NULL,
                titleEn TEXT NOT NULL,
                titleFa TEXT NOT NULL,
                recipeSummaryEn TEXT NOT NULL,
                recipeSummaryFa TEXT NOT NULL,
                calories INTEGER NOT NULL,
                protein REAL NOT NULL,
                carbs REAL NOT NULL,
                fat REAL NOT NULL
            )
            """.trimIndent()
        )

        v1Db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS achievements (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                code TEXT NOT NULL,
                titleEn TEXT NOT NULL,
                titleFa TEXT NOT NULL,
                descriptionEn TEXT NOT NULL,
                descriptionFa TEXT NOT NULL,
                iconName TEXT NOT NULL,
                isUnlocked INTEGER NOT NULL,
                unlockedDate TEXT
            )
            """.trimIndent()
        )
        v1Db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_achievements_code ON achievements (code)")

        // Insert v1 records
        v1Db.execSQL(
            """
            INSERT INTO user_profile VALUES (
                1, 'Sara', 29, 'FEMALE', 166.0, 64.5, 59.0, 'MODERATELY_ACTIVE',
                'LOSE_WEIGHT', 1750, 110, 210, 48, 2250, 'STANDARD', '', 1, 1700000000000
            )
            """.trimIndent()
        )
        // Duplicate seeded foods (id 1 and id 2) + custom food (id 3)
        v1Db.execSQL(
            """
            INSERT INTO food_items (id, nameEn, nameFa, category, servingSizeGrams, servingUnitEn, servingUnitFa, calories, protein, carbs, fat, fiber, isIranian, isVerified)
            VALUES
                (1, 'Steamed Saffron Rice', 'برنج ساده زعفرانی', 'BREAD_GRAINS', 100.0, '100g', '۱۰۰ گرم', 130, 2.7, 28.0, 0.3, 0.4, 1, 1),
                (2, 'Steamed Saffron Rice', 'برنج ساده زعفرانی', 'BREAD_GRAINS', 100.0, '100g', '۱۰۰ گرم', 130, 2.7, 28.0, 0.3, 0.4, 1, 1),
                (3, 'Custom Homemade Soup', 'سوپ خانگی اختصاصی', 'IRANIAN_MAINS', 250.0, '1 bowl', '۱ کاسه', 280, 14.0, 32.0, 9.0, 4.0, 1, 0)
            """.trimIndent()
        )
        // Food logs: one referencing duplicate id=2 with AFTERNOON_SNACK, one referencing custom food id=3
        v1Db.execSQL(
            """
            INSERT INTO food_logs (id, dateIso, mealType, foodId, foodName, grams, calories, protein, carbs, fat, timestamp)
            VALUES
                (10, '2026-10-01', 'AFTERNOON_SNACK', 2, 'برنج ساده زعفرانی', 150.0, 195, 4.05, 42.0, 0.45, 1700000001000),
                (11, '2026-10-01', 'DINNER', 3, 'سوپ خانگی اختصاصی', 250.0, 280, 14.0, 32.0, 9.0, 1700000002000)
            """.trimIndent()
        )
        v1Db.execSQL("INSERT INTO weight_records (dateIso, weightKg, note, timestamp) VALUES ('2026-10-01', 64.5, 'Morning', 1700000003000)")
        v1Db.execSQL("INSERT INTO meal_plans (dayOfWeek, mealType, titleEn, titleFa, recipeSummaryEn, recipeSummaryFa, calories, protein, carbs, fat) VALUES (1, 'AFTERNOON_SNACK', 'Walnuts', 'گردو', 'Snack', 'میان‌وعده', 160, 4.0, 3.0, 15.0)")
        v1Db.execSQL("INSERT INTO achievements (code, titleEn, titleFa, descriptionEn, descriptionFa, iconName, isUnlocked, unlockedDate) VALUES ('FIRST_LOG', 'First Step', 'اولین قدم', 'Log first meal', 'اولین وعده', 'star', 1, '2026-10-01')")
        v1Db.close()

        // Open with Room v3 + MIGRATION_1_2 + MIGRATION_2_3
        val migratedDb = Room.databaseBuilder(context, CaloFitDatabase::class.java, dbName)
            .addMigrations(CaloFitDatabase.MIGRATION_1_2, CaloFitDatabase.MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()

        // Verify schema + data integrity after migration
        val profile = migratedDb.userProfileDao().getUserProfileOnce()
        assertNotNull(profile)
        assertEquals("Sara", profile!!.name)
        assertEquals(1750, profile.targetCalories)
        assertTrue(profile.isSetupComplete)

        val foods = migratedDb.foodDao().getAllFoodsOnce()
        assertEquals(2, foods.size)
        assertTrue(foods.any { it.id == 1L && it.nameEn == "Steamed Saffron Rice" && it.sugar == 0.0 && it.sodiumMg == 0.0 })
        assertTrue(foods.any { it.id == 3L && it.nameEn == "Custom Homemade Soup" })

        val logs = migratedDb.foodLogDao().getAllLogsOnce()
        assertEquals(2, logs.size)

        val migratedSnackLog = logs.first { it.id == 10L }
        assertEquals("SNACK", migratedSnackLog.mealType)
        assertEquals(1L, migratedSnackLog.foodId)
        assertEquals("Steamed Saffron Rice", migratedSnackLog.foodNameEn)
        assertEquals("برنج ساده زعفرانی", migratedSnackLog.foodNameFa)

        val customLog = logs.first { it.id == 11L }
        assertEquals(3L, customLog.foodId)
        assertEquals("Custom Homemade Soup", customLog.foodNameEn)

        val weights = migratedDb.weightDao().getAllWeightRecordsOnce()
        assertEquals(1, weights.size)
        assertEquals(64.5, weights.first().weightKg, 0.01)

        val plans = migratedDb.mealPlanDao().getPlansForDay(1).first()
        assertEquals(1, plans.size)
        assertEquals("SNACK", plans.first().mealType)

        val achievements = migratedDb.achievementDao().getAllAchievementsOnce()
        val firstLogAch = achievements.first { it.code == "FIRST_LOG" }
        assertTrue(firstLogAch.isUnlocked)
        assertEquals("2026-10-01", firstLogAch.unlockedDate)
        assertEquals("BRONZE", firstLogAch.tier)

        // Verify v3 saved_recipes table is functional after v1 -> v3 migration
        val insertedRecipeId = migratedDb.savedRecipeDao().insertSavedRecipe(
            com.example.calofit.data.model.SavedRecipeEntity(
                name = "Tahchin",
                description = "Baked saffron rice",
                ingredientsText = "Rice, Chicken",
                optionalAdditionsText = "",
                instructionsText = "Bake slowly",
                prepTimeMinutes = 40,
                calories = 480,
                proteinGrams = 30.0,
                carbsGrams = 55.0,
                fatGrams = 14.0,
                isPersian = false
            )
        )
        assertTrue(insertedRecipeId > 0L)
        assertEquals(1, migratedDb.savedRecipeDao().getAllSavedRecipes().first().size)

        migratedDb.close()
    }

    @Test
    fun migrate2To3_addsSugarSodiumAndSavedRecipes_preservesData(): Unit = runBlocking {
        val v2DbName = "migration_test_v2_to_v3.db"
        context.deleteDatabase(v2DbName)
        val dbFile = context.getDatabasePath(v2DbName)
        dbFile.parentFile?.mkdirs()

        val v2Db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        v2Db.execSQL("PRAGMA user_version = 2")
        v2Db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS user_profile (
                id INTEGER NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                age INTEGER NOT NULL,
                gender TEXT NOT NULL,
                heightCm REAL NOT NULL,
                currentWeightKg REAL NOT NULL,
                targetWeightKg REAL NOT NULL,
                activityLevel TEXT NOT NULL,
                goal TEXT NOT NULL,
                targetCalories INTEGER NOT NULL,
                proteinGrams INTEGER NOT NULL,
                carbsGrams INTEGER NOT NULL,
                fatGrams INTEGER NOT NULL,
                waterMl INTEGER NOT NULL,
                dietaryPreference TEXT NOT NULL,
                allergies TEXT NOT NULL,
                isSetupComplete INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        v2Db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS food_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                nameEn TEXT NOT NULL,
                nameFa TEXT NOT NULL,
                category TEXT NOT NULL,
                servingSizeGrams REAL NOT NULL,
                servingUnitEn TEXT NOT NULL,
                servingUnitFa TEXT NOT NULL,
                calories INTEGER NOT NULL,
                protein REAL NOT NULL,
                carbs REAL NOT NULL,
                fat REAL NOT NULL,
                fiber REAL NOT NULL,
                isIranian INTEGER NOT NULL,
                isVerified INTEGER NOT NULL
            )
            """.trimIndent()
        )
        v2Db.execSQL("CREATE INDEX IF NOT EXISTS index_food_items_nameFa ON food_items (nameFa)")
        v2Db.execSQL("CREATE INDEX IF NOT EXISTS index_food_items_nameEn ON food_items (nameEn)")
        v2Db.execSQL("CREATE INDEX IF NOT EXISTS index_food_items_category ON food_items (category)")
        v2Db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS food_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                dateIso TEXT NOT NULL,
                mealType TEXT NOT NULL,
                foodId INTEGER,
                foodName TEXT NOT NULL,
                grams REAL NOT NULL,
                calories INTEGER NOT NULL,
                protein REAL NOT NULL,
                carbs REAL NOT NULL,
                fat REAL NOT NULL,
                foodNameEn TEXT NOT NULL,
                foodNameFa TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
        v2Db.execSQL("CREATE INDEX IF NOT EXISTS index_food_logs_dateIso ON food_logs (dateIso)")
        v2Db.execSQL("CREATE INDEX IF NOT EXISTS index_food_logs_mealType ON food_logs (mealType)")
        v2Db.execSQL("CREATE TABLE IF NOT EXISTS weight_records (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, dateIso TEXT NOT NULL, weightKg REAL NOT NULL, note TEXT NOT NULL, timestamp INTEGER NOT NULL)")
        v2Db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_weight_records_dateIso ON weight_records (dateIso)")
        v2Db.execSQL("CREATE TABLE IF NOT EXISTS water_records (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, dateIso TEXT NOT NULL, amountMl INTEGER NOT NULL, timestamp INTEGER NOT NULL)")
        v2Db.execSQL("CREATE INDEX IF NOT EXISTS index_water_records_dateIso ON water_records (dateIso)")
        v2Db.execSQL("CREATE TABLE IF NOT EXISTS activity_records (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, dateIso TEXT NOT NULL, activityType TEXT NOT NULL, durationMinutes INTEGER NOT NULL, burnedCalories INTEGER NOT NULL, timestamp INTEGER NOT NULL)")
        v2Db.execSQL("CREATE INDEX IF NOT EXISTS index_activity_records_dateIso ON activity_records (dateIso)")
        v2Db.execSQL("CREATE TABLE IF NOT EXISTS meal_plans (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, dayOfWeek INTEGER NOT NULL, mealType TEXT NOT NULL, titleEn TEXT NOT NULL, titleFa TEXT NOT NULL, recipeSummaryEn TEXT NOT NULL, recipeSummaryFa TEXT NOT NULL, calories INTEGER NOT NULL, protein REAL NOT NULL, carbs REAL NOT NULL, fat REAL NOT NULL)")
        v2Db.execSQL("CREATE TABLE IF NOT EXISTS achievements (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, code TEXT NOT NULL, tier TEXT NOT NULL, titleEn TEXT NOT NULL, titleFa TEXT NOT NULL, descriptionEn TEXT NOT NULL, descriptionFa TEXT NOT NULL, iconName TEXT NOT NULL, isUnlocked INTEGER NOT NULL, unlockedDate TEXT, progressCurrent INTEGER NOT NULL, progressTarget INTEGER NOT NULL)")
        v2Db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_achievements_code ON achievements (code)")

        v2Db.execSQL("INSERT INTO food_items (id, nameEn, nameFa, category, servingSizeGrams, servingUnitEn, servingUnitFa, calories, protein, carbs, fat, fiber, isIranian, isVerified) VALUES (5, 'Custom Oats', 'جو دوسر', 'BREAKFAST', 60.0, '60g', '۶۰ گرم', 220, 8.0, 38.0, 4.0, 6.0, 0, 0)")
        v2Db.close()

        val db = Room.databaseBuilder(context, CaloFitDatabase::class.java, v2DbName)
            .addMigrations(CaloFitDatabase.MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()

        val foods = db.foodDao().getAllFoodsOnce()
        assertEquals(1, foods.size)
        assertEquals("Custom Oats", foods.first().nameEn)
        assertEquals(0.0, foods.first().sugar, 0.01)
        assertEquals(0.0, foods.first().sodiumMg, 0.01)

        val id = db.savedRecipeDao().insertSavedRecipe(
            com.example.calofit.data.model.SavedRecipeEntity(
                name = "Omelette",
                description = "Quick protein breakfast",
                ingredientsText = "Eggs, Spinach",
                optionalAdditionsText = "",
                instructionsText = "Cook eggs with spinach",
                prepTimeMinutes = 10,
                calories = 240,
                proteinGrams = 18.0,
                carbsGrams = 3.0,
                fatGrams = 17.0,
                isPersian = false
            )
        )
        assertTrue(id > 0L)
        assertEquals(1, db.savedRecipeDao().getAllSavedRecipesOnce().size)

        db.close()
        context.deleteDatabase(v2DbName)
    }
}
