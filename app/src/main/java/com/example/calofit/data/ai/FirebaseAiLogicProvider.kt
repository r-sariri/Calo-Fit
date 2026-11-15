package com.example.calofit.data.ai

import android.graphics.BitmapFactory
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class FirebaseAiLogicProvider(
    override val priority: Int = 5,
    private val textTimeoutMs: Long = DEFAULT_TEXT_TIMEOUT_MS,
    private val visionTimeoutMs: Long = DEFAULT_VISION_TIMEOUT_MS
) : AiProvider {

    override val id: String = "firebase_ai_logic"
    override val isConfigured: Boolean = true
    override val capabilities: Set<AiProviderCapability> = setOf(
        AiProviderCapability.TEXT_GENERATION,
        AiProviderCapability.VISION_ANALYSIS,
        AiProviderCapability.STRUCTURED_JSON
    )

    private val coachModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME
        )
    }

    private val recipeSchema = Schema.obj(
        properties = mapOf(
            "recipes" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "name" to Schema.string(description = "Recipe title"),
                        "description" to Schema.string(description = "Short 1-sentence appetizing summary"),
                        "ingredients" to Schema.array(
                            items = Schema.string(),
                            description = "Ingredients strictly selected by the user plus basic water/salt if needed"
                        ),
                        "optionalAdditions" to Schema.array(
                            items = Schema.string(),
                            description = "Optional extra ingredients NOT in the user's list, clearly separated"
                        ),
                        "instructions" to Schema.array(
                            items = Schema.string(),
                            description = "Clear step-by-step cooking instructions"
                        ),
                        "prepTimeMinutes" to Schema.integer(description = "Preparation time in minutes"),
                        "cookTimeMinutes" to Schema.integer(description = "Active cooking time in minutes"),
                        "servings" to Schema.integer(description = "Number of servings produced"),
                        "calories" to Schema.integer(description = "Estimated total calories per serving"),
                        "proteinGrams" to Schema.double(description = "Estimated protein in grams"),
                        "carbsGrams" to Schema.double(description = "Estimated carbohydrates in grams"),
                        "fatGrams" to Schema.double(description = "Estimated fat in grams")
                    ),
                    optionalProperties = listOf("optionalAdditions", "cookTimeMinutes", "servings")
                )
            )
        )
    )

    private val recipeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = recipeSchema
            }
        )
    }

    private val visionSchema = Schema.obj(
        properties = mapOf(
            "foodNameEn" to Schema.string(description = "Concise English name of the identified food"),
            "foodNameFa" to Schema.string(description = "Concise Persian name of the identified food"),
            "estimatedGrams" to Schema.double(description = "Estimated edible serving weight in grams"),
            "caloriesForPortion" to Schema.integer(description = "Estimated calories for the estimated portion"),
            "proteinGrams" to Schema.double(description = "Estimated protein grams for the portion"),
            "carbsGrams" to Schema.double(description = "Estimated carbohydrates grams for the portion"),
            "fatGrams" to Schema.double(description = "Estimated fat grams for the portion"),
            "fiberGrams" to Schema.double(description = "Estimated dietary fiber grams for the portion"),
            "possibleIngredients" to Schema.array(
                items = Schema.string(),
                description = "Visible or likely ingredients in the dish"
            ),
            "confidenceNote" to Schema.string(description = "Short note about estimation uncertainty or visual cues")
        ),
        optionalProperties = listOf("fiberGrams")
    )

    private val visionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = visionSchema
            }
        )
    }

    override suspend fun chatCoach(
        history: List<AiChatTurn>,
        message: String,
        context: AiUserContext
    ): AiResult<String> {
        return try {
            val isPersian = context.languageCode == "fa"
            val languageDirective = if (isPersian) {
                "Respond exclusively in natural, clear, friendly Persian (Farsi) with Persian digits where appropriate."
            } else {
                "Respond exclusively in natural, clear, friendly English."
            }
            val recentFoodsLine = if (context.recentFoodsSummary.isNotEmpty()) {
                "- Foods logged today: ${context.recentFoodsSummary.joinToString(", ")}"
            } else {
                ""
            }
            val sysPrompt = """
                You are KaloFit's nutrition & healthy lifestyle coach.
                $languageDirective
                User nutrition context (non-identifying summary only):
                - Goal: ${context.goal}
                - Daily calorie target: ${context.targetCalories} kcal
                - Macro targets: Protein ${context.proteinGrams}g, Carbs ${context.carbsGrams}g, Fat ${context.fatGrams}g
                - Calories logged today: ${context.consumedCaloriesToday} kcal
                $recentFoodsLine
                ${context.age?.let { "- Age: $it" } ?: ""}
                ${context.gender?.let { "- Gender: $it" } ?: ""}
                Rules:
                1. Be practical, culturally aware (including Iranian & international dishes), concise, and supportive.
                2. You are NOT a doctor. Never diagnose medical conditions or prescribe treatments. When asked about medical conditions, pregnancy, or medications, advise consulting a qualified physician or registered dietitian.
                3. Never claim exact clinical certainty; frame estimates honestly.
            """.trimIndent()

            val chatHistory = buildList {
                add(content("user") { text(sysPrompt) })
                add(
                    content("model") {
                        text(
                            if (isPersian) "متوجه شدم. با توجه به اهداف شما پاسخ می‌دهم و توصیه پزشکی ارائه نمی‌کنم."
                            else "Understood. I will respond in English tailored to your nutrition goals without offering medical diagnoses."
                        )
                    }
                )
                history.takeLast(12).forEach { turn ->
                    add(content(if (turn.isUser) "user" else "model") { text(turn.text) })
                }
            }

            val response = withTimeout(textTimeoutMs) {
                val chat = coachModel.startChat(history = chatHistory)
                chat.sendMessage(message)
            }
            val text = response.text?.trim()
            if (text.isNullOrBlank()) {
                AiResult.Failure(AiErrorKind.INVALID_RESPONSE, "Empty AI response")
            } else {
                AiResult.Success(text, id)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Coach request timed out")
            AiResult.Failure(AiErrorKind.TIMEOUT, "Timeout")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            mapException(e)
        }
    }

    override suspend fun generateRecipes(
        ingredients: List<String>,
        isPersian: Boolean,
        filters: SmartChefFilters
    ): AiResult<List<GeneratedRecipe>> {
        if (ingredients.isEmpty()) {
            return AiResult.Failure(AiErrorKind.INVALID_RESPONSE, "No ingredients provided")
        }
        return try {
            val langInstruction = if (isPersian) {
                "All recipe names, descriptions, ingredients, optional additions, and instructions MUST be written in natural Persian (Farsi)."
            } else {
                "All recipe names, descriptions, ingredients, optional additions, and instructions MUST be written in natural English."
            }
            val filterLines = buildList {
                if (filters.cookingTime.isNotBlank()) add("Maximum cooking time preference: ${filters.cookingTime}")
                if (filters.mealType.isNotBlank()) add("Meal type preference: ${filters.mealType}")
                if (filters.dietaryPreference.isNotBlank()) add("Dietary style preference: ${filters.dietaryPreference}")
                if (filters.equipment.isNotBlank()) add("Preferred cooking equipment: ${filters.equipment}")
            }.joinToString("\n                ")
            val prompt = """
                Create 2 practical, nutritious home-cooked recipes using ONLY the user's selected ingredients below (plus basic water or salt if essential).
                Selected ingredients: ${ingredients.joinToString(", ")}
                ${if (filterLines.isNotBlank()) "Optional user preferences:\n                $filterLines" else ""}
                STRICT RULES:
                1. Do NOT silently add unrequested main ingredients into the `ingredients` array. Any extra seasoning, oil, or optional item NOT in the selected list MUST go into `optionalAdditions` only.
                2. $langInstruction
                3. Provide honest estimated nutrition per serving, prepTimeMinutes, cookTimeMinutes, and servings count.
            """.trimIndent()

            val response = withTimeout(textTimeoutMs) {
                recipeModel.generateContent(prompt)
            }
            val jsonText = response.text ?: return AiResult.Failure(AiErrorKind.INVALID_RESPONSE, "Empty recipe JSON")
            val parsed = parseRecipesJson(jsonText)
            if (parsed.isEmpty()) {
                AiResult.Failure(AiErrorKind.INVALID_RESPONSE, "No recipes parsed")
            } else {
                AiResult.Success(parsed, id)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Recipe generation timed out")
            AiResult.Failure(AiErrorKind.TIMEOUT, "Timeout")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            mapException(e)
        }
    }

    override suspend fun analyzeFoodImage(
        jpegBytes: ByteArray,
        isPersian: Boolean
    ): AiResult<FoodImageAnalysisResult> {
        val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
            ?: return AiResult.Failure(AiErrorKind.INVALID_RESPONSE, "Invalid image bytes")
        return try {
            val langNote = if (isPersian) {
                "Write `possibleIngredients` and `confidenceNote` in natural Persian. Still populate both `foodNameEn` and `foodNameFa` accurately."
            } else {
                "Write `possibleIngredients` and `confidenceNote` in natural English. Still populate both `foodNameEn` and `foodNameFa` accurately."
            }
            val prompt = """
                Analyze this food image carefully. Identify the dish or meal (supporting Iranian/Persian and international foods), estimate a realistic portion weight in grams, and estimate calories, protein, carbs, and fat for that portion.
                $langNote
                If multiple foods are visible, summarize the primary plate as one cohesive entry and list visible components in `possibleIngredients`. Keep nutrition values realistic and clearly treat them as visual estimates.
            """.trimIndent()

            val response = withTimeout(visionTimeoutMs) {
                visionModel.generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                )
            }
            val jsonText = response.text ?: return AiResult.Failure(AiErrorKind.INVALID_RESPONSE, "Empty vision JSON")
            val result = parseFoodAnalysisJson(jsonText)
            AiResult.Success(result, id)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Vision request timed out")
            AiResult.Failure(AiErrorKind.TIMEOUT, "Timeout")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            mapException(e)
        } finally {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    private fun parseRecipesJson(jsonText: String): List<GeneratedRecipe> {
        val root = JSONObject(jsonText)
        val array = root.optJSONArray("recipes") ?: return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val ingArr = obj.optJSONArray("ingredients")
                val optArr = obj.optJSONArray("optionalAdditions")
                val stepsArr = obj.optJSONArray("instructions")
                add(
                    GeneratedRecipe(
                        name = obj.optString("name").trim(),
                        description = obj.optString("description").trim(),
                        ingredients = (0 until (ingArr?.length() ?: 0)).map { ingArr!!.getString(it) },
                        optionalAdditions = (0 until (optArr?.length() ?: 0)).map { optArr!!.getString(it) },
                        instructions = (0 until (stepsArr?.length() ?: 0)).map { stepsArr!!.getString(it) },
                        prepTimeMinutes = obj.optInt("prepTimeMinutes", 15).coerceIn(1, 300),
                        calories = obj.optInt("calories", 250).coerceAtLeast(0),
                        proteinGrams = obj.optDouble("proteinGrams", 10.0).coerceAtLeast(0.0),
                        carbsGrams = obj.optDouble("carbsGrams", 25.0).coerceAtLeast(0.0),
                        fatGrams = obj.optDouble("fatGrams", 8.0).coerceAtLeast(0.0),
                        isEstimate = true,
                        cookTimeMinutes = obj.optInt("cookTimeMinutes", 15).coerceIn(1, 300),
                        servings = obj.optInt("servings", 2).coerceIn(1, 24)
                    )
                )
            }
        }.filter { it.name.isNotBlank() }
    }

    private fun parseFoodAnalysisJson(jsonText: String): FoodImageAnalysisResult {
        val obj = JSONObject(jsonText)
        val ingArr = obj.optJSONArray("possibleIngredients")
        val ingredients = (0 until (ingArr?.length() ?: 0)).map { ingArr!!.getString(it) }
        val enName = obj.optString("foodNameEn").trim().ifBlank { obj.optString("foodNameFa").trim() }
        val faName = obj.optString("foodNameFa").trim().ifBlank { enName }
        return FoodImageAnalysisResult(
            foodNameEn = enName,
            foodNameFa = faName,
            estimatedGrams = obj.optDouble("estimatedGrams", 200.0).coerceIn(10.0, 2000.0),
            caloriesForPortion = obj.optInt("caloriesForPortion", 250).coerceAtLeast(0),
            proteinGrams = obj.optDouble("proteinGrams", 12.0).coerceAtLeast(0.0),
            carbsGrams = obj.optDouble("carbsGrams", 25.0).coerceAtLeast(0.0),
            fatGrams = obj.optDouble("fatGrams", 10.0).coerceAtLeast(0.0),
            fiberGrams = obj.optDouble("fiberGrams", 0.0).coerceAtLeast(0.0),
            possibleIngredients = ingredients,
            confidenceNote = obj.optString("confidenceNote").trim(),
            isEstimate = true
        )
    }

    private fun mapException(e: Exception): AiResult.Failure {
        Log.e(TAG, "Firebase AI error: ${e.javaClass.simpleName}")
        val msg = (e.message ?: "").lowercase()
        val kind = when {
            msg.contains("location") || msg.contains("region") || msg.contains("country") || msg.contains("not supported") ->
                AiErrorKind.PROVIDER_UNAVAILABLE
            msg.contains("429") || msg.contains("quota") || msg.contains("resource_exhausted") ->
                AiErrorKind.RATE_LIMITED
            msg.contains("app check") || msg.contains("unauthenticated") || msg.contains("401") || msg.contains("403") ->
                AiErrorKind.AUTH_FAILED
            msg.contains("timeout") || msg.contains("deadline") ->
                AiErrorKind.TIMEOUT
            else -> AiErrorKind.PROVIDER_UNAVAILABLE
        }
        return AiResult.Failure(kind, kind.name)
    }

    companion object {
        private const val TAG = "FirebaseAiProvider"
        private const val MODEL_NAME = "gemini-3.8-flash"
        private const val DEFAULT_TEXT_TIMEOUT_MS = 18_000L
        private const val DEFAULT_VISION_TIMEOUT_MS = 35_000L
    }
}
