package com.example.calofit.data.repository

import com.example.calofit.data.model.AchievementEntity
import com.example.calofit.data.model.ActivityRecordEntity
import com.example.calofit.data.model.FoodItemEntity
import com.example.calofit.data.model.FoodLogEntryEntity
import com.example.calofit.data.model.SavedRecipeEntity
import com.example.calofit.data.model.UserProfileEntity
import com.example.calofit.data.model.WaterRecordEntity
import com.example.calofit.data.model.WeightRecordEntity
import com.example.calofit.domain.calculator.NutritionCalculator
import org.json.JSONArray
import org.json.JSONObject

data class ValidatedBackupPayload(
    val profile: UserProfileEntity?,
    val customFoods: List<FoodItemEntity>,
    val foodLogs: List<FoodLogEntryEntity>,
    val weightHistory: List<WeightRecordEntity>,
    val waterHistory: List<WaterRecordEntity>,
    val activityHistory: List<ActivityRecordEntity>,
    val savedRecipes: List<SavedRecipeEntity>,
    val unlockedAchievementDates: Map<String, String>
)

sealed class BackupValidationResult {
    data class Valid(val payload: ValidatedBackupPayload) : BackupValidationResult()
    data class Invalid(val errorCode: String) : BackupValidationResult()
}

object BackupJsonCodec {

    private const val MAX_JSON_BYTES = 2 * 1024 * 1024
    private const val MAX_ITEMS_PER_COLLECTION = 2500
    private val ISO_DATE_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}$")

    fun encode(
        profile: UserProfileEntity?,
        customFoods: List<FoodItemEntity>,
        foodLogs: List<FoodLogEntryEntity>,
        weightRecords: List<WeightRecordEntity>,
        waterRecords: List<WaterRecordEntity>,
        activities: List<ActivityRecordEntity>,
        savedRecipes: List<SavedRecipeEntity>,
        achievements: List<AchievementEntity>
    ): String {
        val root = JSONObject()
        root.put("appName", "KaloFit")
        root.put("version", "1.0")
        root.put("schemaVersion", 3)
        root.put("exportTimestamp", System.currentTimeMillis())

        val profileJson = JSONObject()
        profile?.let { p ->
            profileJson.put("name", p.name)
            profileJson.put("age", p.age)
            profileJson.put("gender", p.gender)
            profileJson.put("heightCm", p.heightCm)
            profileJson.put("currentWeightKg", p.currentWeightKg)
            profileJson.put("targetWeightKg", p.targetWeightKg)
            profileJson.put("activityLevel", p.activityLevel)
            profileJson.put("goal", p.goal)
            profileJson.put("targetCalories", p.targetCalories)
            profileJson.put("proteinGrams", p.proteinGrams)
            profileJson.put("carbsGrams", p.carbsGrams)
            profileJson.put("fatGrams", p.fatGrams)
            profileJson.put("waterMl", p.waterMl)
        }
        root.put("profile", profileJson)

        val customFoodsArray = JSONArray()
        customFoods.filter { !it.isVerified }.forEach { f ->
            customFoodsArray.put(
                JSONObject().apply {
                    put("nameEn", f.nameEn)
                    put("nameFa", f.nameFa)
                    put("category", f.category)
                    put("servingSizeGrams", f.servingSizeGrams)
                    put("servingUnitEn", f.servingUnitEn)
                    put("servingUnitFa", f.servingUnitFa)
                    put("calories", f.calories)
                    put("protein", f.protein)
                    put("carbs", f.carbs)
                    put("fat", f.fat)
                    put("fiber", f.fiber)
                    put("sugar", f.sugar)
                    put("sodiumMg", f.sodiumMg)
                    put("isIranian", f.isIranian)
                }
            )
        }
        root.put("customFoods", customFoodsArray)

        val logsArray = JSONArray()
        foodLogs.forEach { entry ->
            logsArray.put(
                JSONObject().apply {
                    put("date", entry.dateIso)
                    put("mealType", entry.mealType)
                    put("foodNameEn", entry.foodNameEn)
                    put("foodNameFa", entry.foodNameFa.ifBlank { entry.foodName })
                    put("grams", entry.grams)
                    put("calories", entry.calories)
                    put("protein", entry.protein)
                    put("carbs", entry.carbs)
                    put("fat", entry.fat)
                }
            )
        }
        root.put("foodLogs", logsArray)

        val weightArray = JSONArray()
        weightRecords.forEach { w ->
            weightArray.put(
                JSONObject().apply {
                    put("date", w.dateIso)
                    put("weightKg", w.weightKg)
                    put("note", w.note)
                }
            )
        }
        root.put("weightHistory", weightArray)

        val waterArray = JSONArray()
        waterRecords.forEach { w ->
            waterArray.put(
                JSONObject().apply {
                    put("date", w.dateIso)
                    put("amountMl", w.amountMl)
                }
            )
        }
        root.put("waterHistory", waterArray)

        val activityArray = JSONArray()
        activities.forEach { a ->
            activityArray.put(
                JSONObject().apply {
                    put("date", a.dateIso)
                    put("activityType", a.activityType)
                    put("durationMinutes", a.durationMinutes)
                    put("burnedCalories", a.burnedCalories)
                }
            )
        }
        root.put("activityHistory", activityArray)

        val recipesArray = JSONArray()
        savedRecipes.forEach { r ->
            recipesArray.put(
                JSONObject().apply {
                    put("name", r.name)
                    put("description", r.description)
                    put("ingredientsText", r.ingredientsText)
                    put("optionalAdditionsText", r.optionalAdditionsText)
                    put("instructionsText", r.instructionsText)
                    put("prepTimeMinutes", r.prepTimeMinutes)
                    put("calories", r.calories)
                    put("proteinGrams", r.proteinGrams)
                    put("carbsGrams", r.carbsGrams)
                    put("fatGrams", r.fatGrams)
                    put("isPersian", r.isPersian)
                }
            )
        }
        root.put("savedRecipes", recipesArray)

        val achArray = JSONArray()
        achievements.filter { it.isUnlocked }.forEach { a ->
            achArray.put(
                JSONObject().apply {
                    put("code", a.code)
                    put("unlockedDate", a.unlockedDate ?: "")
                }
            )
        }
        root.put("achievements", achArray)

        return root.toString(2)
    }

    fun parseAndValidate(rawJson: String): BackupValidationResult {
        val trimmed = rawJson.trim()
        if (trimmed.isBlank() || trimmed.length > MAX_JSON_BYTES) {
            return BackupValidationResult.Invalid("INVALID_SIZE_OR_EMPTY")
        }

        val root = try {
            JSONObject(trimmed)
        } catch (_: Exception) {
            return BackupValidationResult.Invalid("MALFORMED_JSON")
        }

        val appName = root.optString("appName", "").trim()
        val acceptedNames = setOf("kalofit", "kalo fit", "calo fit", "calofit")
        if (appName.lowercase() !in acceptedNames) {
            return BackupValidationResult.Invalid("UNRECOGNIZED_BACKUP_FORMAT")
        }

        val profile = parseProfile(root.optJSONObject("profile"))
            ?: return BackupValidationResult.Invalid("INVALID_PROFILE_FIELDS")
        val resolvedProfile = if (profile.isEmptySentinel) null else profile.entity

        val customFoods = parseCustomFoods(root.optJSONArray("customFoods"))
            ?: return BackupValidationResult.Invalid("INVALID_CUSTOM_FOODS")
        val foodLogs = parseFoodLogs(root.optJSONArray("foodLogs"))
            ?: return BackupValidationResult.Invalid("INVALID_FOOD_LOGS")
        val weightHistory = parseWeights(root.optJSONArray("weightHistory"))
            ?: return BackupValidationResult.Invalid("INVALID_WEIGHT_HISTORY")
        val waterHistory = parseWater(root.optJSONArray("waterHistory"))
            ?: return BackupValidationResult.Invalid("INVALID_WATER_HISTORY")
        val activityHistory = parseActivities(root.optJSONArray("activityHistory"))
            ?: return BackupValidationResult.Invalid("INVALID_ACTIVITY_HISTORY")
        val savedRecipes = parseSavedRecipes(root.optJSONArray("savedRecipes"))
            ?: return BackupValidationResult.Invalid("INVALID_SAVED_RECIPES")
        val achievements = parseAchievements(root.optJSONArray("achievements"))
            ?: return BackupValidationResult.Invalid("INVALID_ACHIEVEMENTS")

        return BackupValidationResult.Valid(
            ValidatedBackupPayload(
                profile = resolvedProfile,
                customFoods = customFoods,
                foodLogs = foodLogs,
                weightHistory = weightHistory,
                waterHistory = waterHistory,
                activityHistory = activityHistory,
                savedRecipes = savedRecipes,
                unlockedAchievementDates = achievements
            )
        )
    }

    private data class ProfileParseOutcome(
        val entity: UserProfileEntity?,
        val isEmptySentinel: Boolean
    )

    private fun parseProfile(obj: JSONObject?): ProfileParseOutcome? {
        if (obj == null || obj.length() == 0) {
            return ProfileParseOutcome(entity = null, isEmptySentinel = true)
        }
        val name = obj.optString("name", "").trim().take(120)
        val age = obj.optInt("age", -1)
        val height = obj.optDouble("heightCm", Double.NaN)
        val currentWeight = obj.optDouble("currentWeightKg", Double.NaN)
        val targetWeight = obj.optDouble("targetWeightKg", Double.NaN)
        if (!NutritionCalculator.isValidAge(age) ||
            !NutritionCalculator.isValidHeightCm(height) ||
            !NutritionCalculator.isValidWeightKg(currentWeight) ||
            !NutritionCalculator.isValidWeightKg(targetWeight)
        ) {
            return null
        }

        val gender = runCatching { NutritionCalculator.Gender.valueOf(obj.optString("gender", "")) }
            .getOrNull() ?: return null
        val activity = runCatching { NutritionCalculator.ActivityLevel.valueOf(obj.optString("activityLevel", "")) }
            .getOrNull() ?: return null
        val goal = runCatching { NutritionCalculator.WeightGoal.valueOf(obj.optString("goal", "")) }
            .getOrNull() ?: return null

        val computed = NutritionCalculator.calculateFullPlan(
            weightKg = currentWeight,
            heightCm = height,
            age = age,
            gender = gender,
            activityLevel = activity,
            goal = goal
        )
        val targetCal = obj.optInt("targetCalories", computed.targetCalories).coerceIn(1000, 8000)
        val protG = obj.optInt("proteinGrams", computed.proteinGrams).coerceIn(20, 600)
        val carbsG = obj.optInt("carbsGrams", computed.carbsGrams).coerceIn(20, 1200)
        val fatG = obj.optInt("fatGrams", computed.fatGrams).coerceIn(15, 400)
        val waterMl = obj.optInt("waterMl", computed.waterMl).coerceIn(800, 8000)

        return ProfileParseOutcome(
            entity = UserProfileEntity(
                id = 1,
                name = name.ifBlank { "KaloFit User" },
                age = age,
                gender = gender.name,
                heightCm = height,
                currentWeightKg = currentWeight,
                targetWeightKg = targetWeight,
                activityLevel = activity.name,
                goal = goal.name,
                targetCalories = targetCal,
                proteinGrams = protG,
                carbsGrams = carbsG,
                fatGrams = fatG,
                waterMl = waterMl,
                isSetupComplete = true
            ),
            isEmptySentinel = false
        )
    }

    private fun parseCustomFoods(arr: JSONArray?): List<FoodItemEntity>? {
        if (arr == null) return emptyList()
        if (arr.length() > MAX_ITEMS_PER_COLLECTION) return null
        val out = mutableListOf<FoodItemEntity>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: return null
            val nameEn = obj.optString("nameEn", "").trim().take(140)
            val nameFa = obj.optString("nameFa", "").trim().take(140)
            if (nameEn.isBlank() && nameFa.isBlank()) return null
            val serving = obj.optDouble("servingSizeGrams", Double.NaN)
            val calories = obj.optInt("calories", -1)
            val protein = obj.optDouble("protein", Double.NaN)
            val carbs = obj.optDouble("carbs", Double.NaN)
            val fat = obj.optDouble("fat", Double.NaN)
            val fiber = obj.optDouble("fiber", 0.0)
            val sugar = obj.optDouble("sugar", 0.0)
            val sodium = obj.optDouble("sodiumMg", 0.0)
            if (!serving.isFinite() || serving <= 0.0 || serving > 10000.0) return null
            if (calories !in 0..15000) return null
            if (!isValidNonNeg(protein, 2000.0) ||
                !isValidNonNeg(carbs, 2000.0) ||
                !isValidNonNeg(fat, 2000.0) ||
                !isValidNonNeg(fiber, 500.0) ||
                !isValidNonNeg(sugar, 1000.0) ||
                !isValidNonNeg(sodium, 50000.0)
            ) return null

            out.add(
                FoodItemEntity(
                    nameEn = nameEn.ifBlank { nameFa },
                    nameFa = nameFa.ifBlank { nameEn },
                    category = obj.optString("category", "SNACKS").take(40),
                    servingSizeGrams = serving,
                    servingUnitEn = obj.optString("servingUnitEn", "${serving.toInt()}g").take(60),
                    servingUnitFa = obj.optString("servingUnitFa", "${serving.toInt()} گرم").take(60),
                    calories = calories,
                    protein = protein,
                    carbs = carbs,
                    fat = fat,
                    fiber = fiber,
                    sugar = sugar,
                    sodiumMg = sodium,
                    isIranian = obj.optBoolean("isIranian", false),
                    isVerified = false
                )
            )
        }
        return out
    }

    private fun parseFoodLogs(arr: JSONArray?): List<FoodLogEntryEntity>? {
        if (arr == null) return emptyList()
        if (arr.length() > MAX_ITEMS_PER_COLLECTION) return null
        val allowedMeals = setOf("BREAKFAST", "MORNING_SNACK", "LUNCH", "SNACK", "AFTERNOON_SNACK", "DINNER", "EVENING_SNACK")
        val out = mutableListOf<FoodLogEntryEntity>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: return null
            val dateIso = obj.optString("date", "").trim()
            if (!ISO_DATE_REGEX.matches(dateIso)) return null
            val rawMeal = obj.optString("mealType", "").trim()
            if (rawMeal !in allowedMeals) return null
            val mealType = if (rawMeal == "AFTERNOON_SNACK") "SNACK" else rawMeal
            val nameEn = obj.optString("foodNameEn", "").trim().take(140)
            val nameFa = obj.optString("foodNameFa", "").trim().take(140)
            if (nameEn.isBlank() && nameFa.isBlank()) return null

            val grams = obj.optDouble("grams", Double.NaN)
            val calories = obj.optInt("calories", -1)
            val protein = obj.optDouble("protein", Double.NaN)
            val carbs = obj.optDouble("carbs", Double.NaN)
            val fat = obj.optDouble("fat", Double.NaN)
            if (!grams.isFinite() || grams <= 0.0 || grams > 10000.0) return null
            if (calories !in 0..15000) return null
            if (!isValidNonNeg(protein, 2000.0) || !isValidNonNeg(carbs, 2000.0) || !isValidNonNeg(fat, 2000.0)) {
                return null
            }
            out.add(
                FoodLogEntryEntity(
                    dateIso = dateIso,
                    mealType = mealType,
                    foodId = null,
                    foodName = nameFa.ifBlank { nameEn },
                    grams = grams,
                    calories = calories,
                    protein = protein,
                    carbs = carbs,
                    fat = fat,
                    foodNameEn = nameEn.ifBlank { nameFa },
                    foodNameFa = nameFa.ifBlank { nameEn }
                )
            )
        }
        return out
    }

    private fun parseWeights(arr: JSONArray?): List<WeightRecordEntity>? {
        if (arr == null) return emptyList()
        if (arr.length() > MAX_ITEMS_PER_COLLECTION) return null
        val out = mutableListOf<WeightRecordEntity>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: return null
            val dateIso = obj.optString("date", "").trim()
            if (!ISO_DATE_REGEX.matches(dateIso)) return null
            val w = obj.optDouble("weightKg", Double.NaN)
            if (!NutritionCalculator.isValidWeightKg(w)) return null
            out.add(
                WeightRecordEntity(
                    dateIso = dateIso,
                    weightKg = w,
                    note = obj.optString("note", "").trim().take(200)
                )
            )
        }
        return out.distinctBy { it.dateIso }
    }

    private fun parseWater(arr: JSONArray?): List<WaterRecordEntity>? {
        if (arr == null) return emptyList()
        if (arr.length() > MAX_ITEMS_PER_COLLECTION) return null
        val out = mutableListOf<WaterRecordEntity>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: return null
            val dateIso = obj.optString("date", "").trim()
            if (!ISO_DATE_REGEX.matches(dateIso)) return null
            val amount = obj.optInt("amountMl", -1)
            if (amount !in 1..10000) return null
            out.add(WaterRecordEntity(dateIso = dateIso, amountMl = amount))
        }
        return out
    }

    private fun parseActivities(arr: JSONArray?): List<ActivityRecordEntity>? {
        if (arr == null) return emptyList()
        if (arr.length() > MAX_ITEMS_PER_COLLECTION) return null
        val out = mutableListOf<ActivityRecordEntity>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: return null
            val dateIso = obj.optString("date", "").trim()
            if (!ISO_DATE_REGEX.matches(dateIso)) return null
            val type = obj.optString("activityType", "").trim().take(40)
            if (type.isBlank()) return null
            val duration = obj.optInt("durationMinutes", -1)
            val burned = obj.optInt("burnedCalories", -1)
            if (duration !in 1..1440 || burned !in 0..10000) return null
            out.add(
                ActivityRecordEntity(
                    dateIso = dateIso,
                    activityType = type,
                    durationMinutes = duration,
                    burnedCalories = burned
                )
            )
        }
        return out
    }

    private fun parseSavedRecipes(arr: JSONArray?): List<SavedRecipeEntity>? {
        if (arr == null) return emptyList()
        if (arr.length() > MAX_ITEMS_PER_COLLECTION) return null
        val out = mutableListOf<SavedRecipeEntity>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: return null
            val name = obj.optString("name", "").trim().take(160)
            val ingredients = obj.optString("ingredientsText", "").trim().take(1500)
            val instructions = obj.optString("instructionsText", "").trim().take(4000)
            if (name.isBlank() || ingredients.isBlank() || instructions.isBlank()) return null
            val calories = obj.optInt("calories", 0).coerceIn(0, 10000)
            val prepTime = obj.optInt("prepTimeMinutes", 20).coerceIn(1, 600)
            val p = obj.optDouble("proteinGrams", 0.0).coerceIn(0.0, 1000.0)
            val c = obj.optDouble("carbsGrams", 0.0).coerceIn(0.0, 1000.0)
            val f = obj.optDouble("fatGrams", 0.0).coerceIn(0.0, 1000.0)
            out.add(
                SavedRecipeEntity(
                    name = name,
                    description = obj.optString("description", "").trim().take(400),
                    ingredientsText = ingredients,
                    optionalAdditionsText = obj.optString("optionalAdditionsText", "").trim().take(500),
                    instructionsText = instructions,
                    prepTimeMinutes = prepTime,
                    calories = calories,
                    proteinGrams = p,
                    carbsGrams = c,
                    fatGrams = f,
                    isPersian = obj.optBoolean("isPersian", true)
                )
            )
        }
        return out
    }

    private fun parseAchievements(arr: JSONArray?): Map<String, String>? {
        if (arr == null) return emptyMap()
        if (arr.length() > 200) return null
        val out = mutableMapOf<String, String>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: return null
            val code = obj.optString("code", "").trim().take(60)
            if (code.isBlank()) return null
            val unlockedDate = obj.optString("unlockedDate", "").trim()
            if (unlockedDate.isNotBlank() && !ISO_DATE_REGEX.matches(unlockedDate)) return null
            out[code] = unlockedDate
        }
        return out
    }

    private fun isValidNonNeg(v: Double, max: Double): Boolean = v.isFinite() && v >= 0.0 && v <= max
}
