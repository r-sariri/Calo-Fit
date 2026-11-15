package com.example.calofit.data.ai

enum class AiErrorKind {
    NO_INTERNET,
    RATE_LIMITED,
    AUTH_FAILED,
    TIMEOUT,
    PROVIDER_UNAVAILABLE,
    INVALID_RESPONSE
}

enum class AiProviderCapability {
    TEXT_GENERATION,
    VISION_ANALYSIS,
    STRUCTURED_JSON
}

data class AiProviderConfig(
    val id: String,
    val displayName: String,
    val priority: Int,
    val modelId: String,
    val capabilities: Set<AiProviderCapability>,
    val textTimeoutMs: Long = 6_000L,
    val visionTimeoutMs: Long = 35_000L,
    val connectTimeoutMs: Long = 5_000L
)

data class AiDiagnosticEvent(
    val providerId: String,
    val stage: String,
    val errorCategory: String,
    val httpStatusCategory: String,
    val durationMs: Long,
    val timestampMs: Long
)

sealed class AiResult<out T> {
    data class Success<out T>(
        val data: T,
        val providerId: String
    ) : AiResult<T>()

    data class Failure(
        val kind: AiErrorKind,
        val detail: String = ""
    ) : AiResult<Nothing>()
}

data class AiUserContext(
    val languageCode: String,
    val goal: String,
    val targetCalories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
    val consumedCaloriesToday: Int,
    val recentFoodsSummary: List<String> = emptyList(),
    val age: Int? = null,
    val gender: String? = null
)

data class AiChatTurn(
    val isUser: Boolean,
    val text: String
)

data class SmartChefFilters(
    val cookingTime: String = "",
    val mealType: String = "",
    val dietaryPreference: String = "",
    val equipment: String = ""
) {
    fun hasAnyFilter(): Boolean =
        cookingTime.isNotBlank() || mealType.isNotBlank() || dietaryPreference.isNotBlank() || equipment.isNotBlank()
}

data class GeneratedRecipe(
    val name: String,
    val description: String,
    val ingredients: List<String>,
    val optionalAdditions: List<String>,
    val instructions: List<String>,
    val prepTimeMinutes: Int,
    val calories: Int,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val isEstimate: Boolean = true,
    val cookTimeMinutes: Int = 15,
    val servings: Int = 2
)

data class FoodImageAnalysisResult(
    val foodNameEn: String,
    val foodNameFa: String,
    val estimatedGrams: Double,
    val caloriesForPortion: Int,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double,
    val fiberGrams: Double = 0.0,
    val possibleIngredients: List<String>,
    val confidenceNote: String,
    val isEstimate: Boolean = true
)
