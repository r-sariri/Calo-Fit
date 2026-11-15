package com.example.calofit

import com.example.calofit.data.ai.AiChatTurn
import com.example.calofit.data.ai.AiErrorKind
import com.example.calofit.data.ai.AiProvider
import com.example.calofit.data.ai.AiProviderCapability
import com.example.calofit.data.ai.AiResult
import com.example.calofit.data.ai.AiUserContext
import com.example.calofit.data.ai.FoodImageAnalysisResult
import com.example.calofit.data.ai.GeneratedRecipe
import com.example.calofit.data.ai.MultiProviderAiOrchestrator
import com.example.calofit.data.ai.SmartChefFilters
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiProviderAiOrchestratorTest {

    private val context = AiUserContext(
        languageCode = "en",
        goal = "LOSE_WEIGHT",
        targetCalories = 2000,
        proteinGrams = 130,
        carbsGrams = 220,
        fatGrams = 60,
        consumedCaloriesToday = 850,
        recentFoodsSummary = listOf("Oatmeal (250 kcal)")
    )

    private class FakeProvider(
        override val id: String,
        override val isConfigured: Boolean = true,
        override val priority: Int = 100,
        override val capabilities: Set<AiProviderCapability> = setOf(
            AiProviderCapability.TEXT_GENERATION,
            AiProviderCapability.VISION_ANALYSIS,
            AiProviderCapability.STRUCTURED_JSON
        ),
        private val chatResults: MutableList<AiResult<String>> = mutableListOf(AiResult.Failure(AiErrorKind.PROVIDER_UNAVAILABLE)),
        private val recipesResults: MutableList<AiResult<List<GeneratedRecipe>>> = mutableListOf(AiResult.Failure(AiErrorKind.PROVIDER_UNAVAILABLE)),
        private val visionResults: MutableList<AiResult<FoodImageAnalysisResult>> = mutableListOf(AiResult.Failure(AiErrorKind.PROVIDER_UNAVAILABLE)),
        private val callRecorder: MutableList<String>? = null
    ) : AiProvider {
        var calls = 0
        var lastFilters: SmartChefFilters? = null

        override suspend fun chatCoach(history: List<AiChatTurn>, message: String, context: AiUserContext): AiResult<String> {
            calls++
            callRecorder?.add(id)
            return if (chatResults.size > 1) chatResults.removeAt(0) else chatResults.last()
        }

        override suspend fun generateRecipes(
            ingredients: List<String>,
            isPersian: Boolean,
            filters: SmartChefFilters
        ): AiResult<List<GeneratedRecipe>> {
            calls++
            callRecorder?.add(id)
            lastFilters = filters
            return if (recipesResults.size > 1) recipesResults.removeAt(0) else recipesResults.last()
        }

        override suspend fun analyzeFoodImage(jpegBytes: ByteArray, isPersian: Boolean): AiResult<FoodImageAnalysisResult> {
            calls++
            callRecorder?.add(id)
            return if (visionResults.size > 1) visionResults.removeAt(0) else visionResults.last()
        }
    }

    @Test
    fun offlineCheck_returnsNoInternet_withoutCallingProviders() = runTest {
        val p1 = FakeProvider(id = "primary", chatResults = mutableListOf(AiResult.Success("ok", "primary")))
        val orchestrator = MultiProviderAiOrchestrator(
            connectivityChecker = { false },
            providers = listOf(p1),
            retryDelayMs = 0L
        )

        val result = orchestrator.chatCoach(emptyList(), "Hi", context)
        assertTrue(result is AiResult.Failure)
        assertEquals(AiErrorKind.NO_INTERNET, (result as AiResult.Failure).kind)
        assertEquals(0, p1.calls)
    }

    @Test
    fun retriesTransientOnceOnSameProvider_beforeFallingBack() = runTest {
        val transientRecovery = FakeProvider(
            id = "primary",
            chatResults = mutableListOf(
                AiResult.Failure(AiErrorKind.TIMEOUT, "timeout"),
                AiResult.Success("Recovered on retry!", "primary")
            )
        )
        val orchestrator = MultiProviderAiOrchestrator(
            connectivityChecker = { true },
            providers = listOf(transientRecovery),
            retryDelayMs = 0L
        )

        val result = orchestrator.chatCoach(emptyList(), "Hi", context)
        assertTrue(result is AiResult.Success)
        assertEquals("Recovered on retry!", (result as AiResult.Success).data)
        assertEquals(2, transientRecovery.calls)
    }

    @Test
    fun skipsUnconfiguredAndFallsBackOnFailure() = runTest {
        val unconfigured = FakeProvider(id = "unconfigured", isConfigured = false)
        val failingPrimary = FakeProvider(
            id = "primary",
            chatResults = mutableListOf(AiResult.Failure(AiErrorKind.RATE_LIMITED, "429")),
            recipesResults = mutableListOf(AiResult.Failure(AiErrorKind.TIMEOUT, "timeout")),
            visionResults = mutableListOf(AiResult.Failure(AiErrorKind.PROVIDER_UNAVAILABLE, "503"))
        )
        val sampleRecipe = GeneratedRecipe(
            name = "Herbed Chicken Rice",
            description = "Balanced meal",
            ingredients = listOf("Chicken", "Rice"),
            optionalAdditions = listOf("Saffron"),
            instructions = listOf("Cook chicken", "Serve with rice"),
            prepTimeMinutes = 25,
            calories = 460,
            proteinGrams = 38.0,
            carbsGrams = 48.0,
            fatGrams = 10.0
        )
        val sampleVision = FoodImageAnalysisResult(
            foodNameEn = "Ghormeh Sabzi",
            foodNameFa = "قورمه‌سبزی",
            estimatedGrams = 280.0,
            caloriesForPortion = 420,
            proteinGrams = 24.0,
            carbsGrams = 22.0,
            fatGrams = 26.0,
            fiberGrams = 5.5,
            possibleIngredients = listOf("Herbs", "Kidney beans"),
            confidenceNote = "Visual estimate"
        )
        val healthyFallback = FakeProvider(
            id = "firebase_ai_logic",
            chatResults = mutableListOf(AiResult.Success("Drink 500ml water and add lean protein.", "firebase_ai_logic")),
            recipesResults = mutableListOf(AiResult.Success(listOf(sampleRecipe), "firebase_ai_logic")),
            visionResults = mutableListOf(AiResult.Success(sampleVision, "firebase_ai_logic"))
        )

        val orchestrator = MultiProviderAiOrchestrator(
            connectivityChecker = { true },
            providers = listOf(unconfigured, failingPrimary, healthyFallback),
            retryDelayMs = 0L
        )

        val chat = orchestrator.chatCoach(emptyList(), "Need dinner tip", context)
        assertTrue(chat is AiResult.Success)
        assertEquals("firebase_ai_logic", (chat as AiResult.Success).providerId)
        assertEquals(0, unconfigured.calls)
        // RATE_LIMITED is non-retryable on same provider
        assertEquals(1, failingPrimary.calls)

        val customFilters = SmartChefFilters(cookingTime = "UNDER_20_MIN", dietaryPreference = "HIGH_PROTEIN")
        val recipes = orchestrator.generateRecipes(listOf("Chicken", "Rice"), isPersian = false, filters = customFilters)
        assertTrue(recipes is AiResult.Success)
        assertEquals(1, (recipes as AiResult.Success).data.size)
        assertEquals(customFilters, healthyFallback.lastFilters)

        val vision = orchestrator.analyzeFoodImage(byteArrayOf(1, 2, 3), isPersian = true)
        assertTrue(vision is AiResult.Success)
        assertEquals("قورمه‌سبزی", (vision as AiResult.Success).data.foodNameFa)
        assertEquals(5.5, vision.data.fiberGrams, 0.01)
    }

    @Test
    fun allProvidersFail_returnsTruthfulFailure_noFakeFallback() = runTest {
        val p1 = FakeProvider(id = "p1", chatResults = mutableListOf(AiResult.Failure(AiErrorKind.TIMEOUT)))
        val p2 = FakeProvider(id = "p2", chatResults = mutableListOf(AiResult.Failure(AiErrorKind.AUTH_FAILED)))
        val orchestrator = MultiProviderAiOrchestrator(
            connectivityChecker = { true },
            providers = listOf(p1, p2),
            retryDelayMs = 0L
        )

        val result = orchestrator.chatCoach(emptyList(), "Hello", context)
        assertTrue(result is AiResult.Failure)
        assertEquals(AiErrorKind.AUTH_FAILED, (result as AiResult.Failure).kind)
        // TIMEOUT triggers 1 bounded retry on p1 (2 calls), AUTH_FAILED triggers no retry on p2 (1 call)
        assertEquals(2, p1.calls)
        assertEquals(1, p2.calls)
    }

    @Test
    fun priorityOrder_executesSequentiallyOneThroughFive_andRecordsRedactedDiagnostics() = runTest {
        val sequence = mutableListOf<String>()
        val p5 = FakeProvider(
            id = "gemini",
            priority = 5,
            chatResults = mutableListOf(AiResult.Success("Gemini response", "gemini")),
            callRecorder = sequence
        )
        val p3 = FakeProvider(
            id = "grok",
            priority = 3,
            chatResults = mutableListOf(AiResult.Failure(AiErrorKind.RATE_LIMITED, "HTTP 429")),
            callRecorder = sequence
        )
        val p1 = FakeProvider(
            id = "openai",
            priority = 1,
            chatResults = mutableListOf(AiResult.Failure(AiErrorKind.RATE_LIMITED, "HTTP 429")),
            callRecorder = sequence
        )
        val p4 = FakeProvider(
            id = "claude",
            priority = 4,
            chatResults = mutableListOf(AiResult.Failure(AiErrorKind.AUTH_FAILED, "HTTP 403")),
            callRecorder = sequence
        )
        val p2 = FakeProvider(
            id = "deepseek",
            priority = 2,
            chatResults = mutableListOf(AiResult.Failure(AiErrorKind.RATE_LIMITED, "HTTP 429")),
            callRecorder = sequence
        )

        val orchestrator = MultiProviderAiOrchestrator(
            connectivityChecker = { true },
            providers = listOf(p5, p3, p1, p4, p2),
            retryDelayMs = 0L
        )

        val result = orchestrator.chatCoach(emptyList(), "Protein meal ideas", context)
        assertTrue(result is AiResult.Success)
        assertEquals("gemini", (result as AiResult.Success).providerId)
        assertEquals(listOf("openai", "deepseek", "grok", "claude", "gemini"), sequence)

        val events = orchestrator.getDiagnosticEvents()
        assertEquals(5, events.size)
        assertEquals("429", events[0].httpStatusCategory)
        assertEquals("401_403", events[3].httpStatusCategory)
        assertEquals("2XX", events[4].httpStatusCategory)
        assertEquals("NONE", events[4].errorCategory)
    }

    @Test
    fun visionRequests_skipProvidersWithoutVisionCapability() = runTest {
        val textOnlyProvider = FakeProvider(
            id = "deepseek_text_only",
            priority = 2,
            capabilities = setOf(AiProviderCapability.TEXT_GENERATION, AiProviderCapability.STRUCTURED_JSON)
        )
        val visionProvider = FakeProvider(
            id = "gemini_vision",
            priority = 5,
            visionResults = mutableListOf(
                AiResult.Success(
                    FoodImageAnalysisResult(
                        foodNameEn = "Mirza Ghasemi",
                        foodNameFa = "میرزا قاسمی",
                        estimatedGrams = 240.0,
                        caloriesForPortion = 310,
                        proteinGrams = 9.0,
                        carbsGrams = 18.0,
                        fatGrams = 22.0,
                        possibleIngredients = listOf("Eggplant", "Tomato", "Egg"),
                        confidenceNote = "Visual estimate"
                    ),
                    "gemini_vision"
                )
            )
        )

        val orchestrator = MultiProviderAiOrchestrator(
            connectivityChecker = { true },
            providers = listOf(textOnlyProvider, visionProvider),
            retryDelayMs = 0L
        )

        val visionResult = orchestrator.analyzeFoodImage(byteArrayOf(1, 2, 3), isPersian = false)
        assertTrue(visionResult is AiResult.Success)
        assertEquals(0, textOnlyProvider.calls)
        assertEquals(1, visionProvider.calls)
    }
}
