package com.example.calofit.data.ai

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * Connects strictly over HTTPS to a server-side AI relay endpoint holding provider API keys
 * securely on the backend. Never embeds provider API keys in client code.
 */
class BackendProxyAiProvider(
    override val id: String,
    private val baseUrl: String,
    override val priority: Int = 10,
    override val capabilities: Set<AiProviderCapability> = setOf(
        AiProviderCapability.TEXT_GENERATION,
        AiProviderCapability.VISION_ANALYSIS,
        AiProviderCapability.STRUCTURED_JSON
    ),
    private val providerHint: String = "",
    private val textTimeoutMs: Long = 6_000L,
    private val visionTimeoutMs: Long = 35_000L,
    private val connectTimeoutMs: Long = 5_000L,
    private val client: OkHttpClient = defaultClient(connectTimeoutMs, textTimeoutMs)
) : AiProvider {

    private val normalizedBaseUrl: String = baseUrl.trim().trimEnd('/')

    internal val proxyHost: String?
        get() = normalizedBaseUrl.toHttpUrlOrNull()?.host

    override val isConfigured: Boolean
        get() = normalizedBaseUrl.startsWith("https://", ignoreCase = true)

    override suspend fun chatCoach(
        history: List<AiChatTurn>,
        message: String,
        context: AiUserContext
    ): AiResult<String> {
        if (!isConfigured) {
            return AiResult.Failure(AiErrorKind.PROVIDER_UNAVAILABLE, "Proxy URL not configured")
        }
        val payload = JSONObject().apply {
            if (providerHint.isNotBlank()) put("preferredProvider", providerHint)
            put("language", context.languageCode)
            put("message", message)
            put(
                "history",
                JSONArray().apply {
                    history.takeLast(12).forEach { turn ->
                        put(
                            JSONObject().apply {
                                put("role", if (turn.isUser) "user" else "assistant")
                                put("text", turn.text)
                            }
                        )
                    }
                }
            )
            put(
                "context",
                JSONObject().apply {
                    put("goal", context.goal)
                    put("targetCalories", context.targetCalories)
                    put("proteinGrams", context.proteinGrams)
                    put("carbsGrams", context.carbsGrams)
                    put("fatGrams", context.fatGrams)
                    put("consumedCaloriesToday", context.consumedCaloriesToday)
                    if (context.recentFoodsSummary.isNotEmpty()) {
                        put("recentFoodsSummary", JSONArray(context.recentFoodsSummary))
                    }
                }
            )
        }
        return postJson("$normalizedBaseUrl/v1/coach", payload, textTimeoutMs) { body ->
            val json = JSONObject(body)
            val reply = json.optString("reply").trim()
            if (reply.isBlank()) {
                AiResult.Failure(AiErrorKind.INVALID_RESPONSE, "Missing reply field")
            } else {
                AiResult.Success(reply, id)
            }
        }
    }

    override suspend fun generateRecipes(
        ingredients: List<String>,
        isPersian: Boolean,
        filters: SmartChefFilters
    ): AiResult<List<GeneratedRecipe>> {
        if (!isConfigured) {
            return AiResult.Failure(AiErrorKind.PROVIDER_UNAVAILABLE, "Proxy URL not configured")
        }
        val payload = JSONObject().apply {
            if (providerHint.isNotBlank()) put("preferredProvider", providerHint)
            put("language", if (isPersian) "fa" else "en")
            put("ingredients", JSONArray(ingredients))
            if (filters.hasAnyFilter()) {
                put(
                    "filters",
                    JSONObject().apply {
                        if (filters.cookingTime.isNotBlank()) put("cookingTime", filters.cookingTime)
                        if (filters.mealType.isNotBlank()) put("mealType", filters.mealType)
                        if (filters.dietaryPreference.isNotBlank()) put("dietaryPreference", filters.dietaryPreference)
                        if (filters.equipment.isNotBlank()) put("equipment", filters.equipment)
                    }
                )
            }
        }
        return postJson("$normalizedBaseUrl/v1/recipes", payload, textTimeoutMs) { body ->
            val root = JSONObject(body)
            val arr = root.optJSONArray("recipes")
                ?: return@postJson AiResult.Failure(AiErrorKind.INVALID_RESPONSE, "Missing recipes")
            val list = buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val ingArr = obj.optJSONArray("ingredients")
                    val optArr = obj.optJSONArray("optionalAdditions")
                    val stepArr = obj.optJSONArray("instructions")
                    add(
                        GeneratedRecipe(
                            name = obj.optString("name").trim(),
                            description = obj.optString("description").trim(),
                            ingredients = (0 until (ingArr?.length() ?: 0)).map { ingArr!!.getString(it) },
                            optionalAdditions = (0 until (optArr?.length() ?: 0)).map { optArr!!.getString(it) },
                            instructions = (0 until (stepsOrEmpty(stepArr))).map { stepArr!!.getString(it) },
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
            if (list.isEmpty()) {
                AiResult.Failure(AiErrorKind.INVALID_RESPONSE, "Empty recipe list")
            } else {
                AiResult.Success(list, id)
            }
        }
    }

    private fun stepsOrEmpty(arr: JSONArray?): Int = arr?.length() ?: 0

    override suspend fun analyzeFoodImage(
        jpegBytes: ByteArray,
        isPersian: Boolean
    ): AiResult<FoodImageAnalysisResult> {
        if (!isConfigured) {
            return AiResult.Failure(AiErrorKind.PROVIDER_UNAVAILABLE, "Proxy URL not configured")
        }
        val payload = JSONObject().apply {
            if (providerHint.isNotBlank()) put("preferredProvider", providerHint)
            put("language", if (isPersian) "fa" else "en")
            put("mimeType", "image/jpeg")
            put("imageBase64", Base64.encodeToString(jpegBytes, Base64.NO_WRAP))
        }
        return postJson("$normalizedBaseUrl/v1/food-vision", payload, visionTimeoutMs) { body ->
            val obj = JSONObject(body)
            val ingArr = obj.optJSONArray("possibleIngredients")
            val enName = obj.optString("foodNameEn").trim().ifBlank { obj.optString("foodNameFa").trim() }
            val faName = obj.optString("foodNameFa").trim().ifBlank { enName }
            if (enName.isBlank()) {
                return@postJson AiResult.Failure(AiErrorKind.INVALID_RESPONSE, "Missing food name")
            }
            AiResult.Success(
                FoodImageAnalysisResult(
                    foodNameEn = enName,
                    foodNameFa = faName,
                    estimatedGrams = obj.optDouble("estimatedGrams", 200.0).coerceIn(10.0, 2000.0),
                    caloriesForPortion = obj.optInt("caloriesForPortion", 250).coerceAtLeast(0),
                    proteinGrams = obj.optDouble("proteinGrams", 12.0).coerceAtLeast(0.0),
                    carbsGrams = obj.optDouble("carbsGrams", 25.0).coerceAtLeast(0.0),
                    fatGrams = obj.optDouble("fatGrams", 10.0).coerceAtLeast(0.0),
                    fiberGrams = obj.optDouble("fiberGrams", 0.0).coerceAtLeast(0.0),
                    possibleIngredients = (0 until (ingArr?.length() ?: 0)).map { ingArr!!.getString(it) },
                    confidenceNote = obj.optString("confidenceNote").trim(),
                    isEstimate = true
                ),
                id
            )
        }
    }

    private suspend fun <T> postJson(
        url: String,
        payload: JSONObject,
        readTimeoutMs: Long,
        parser: (String) -> AiResult<T>
    ): AiResult<T> = withContext(Dispatchers.IO) {
        try {
            val callClient = if (readTimeoutMs != textTimeoutMs) {
                client.newBuilder()
                    .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
                    .writeTimeout(readTimeoutMs.coerceAtLeast(connectTimeoutMs), TimeUnit.MILLISECONDS)
                    .build()
            } else {
                client
            }
            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("Accept", "application/json")
                .build()

            callClient.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val kind = when (response.code) {
                        429 -> AiErrorKind.RATE_LIMITED
                        401, 403 -> AiErrorKind.AUTH_FAILED
                        408, 504 -> AiErrorKind.TIMEOUT
                        else -> AiErrorKind.PROVIDER_UNAVAILABLE
                    }
                    return@withContext AiResult.Failure(kind, "HTTP ${response.code}")
                }
                parser(rawBody)
            }
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Backend proxy timed out ($id)")
            AiResult.Failure(AiErrorKind.TIMEOUT, "Timeout")
        } catch (e: IOException) {
            Log.e(TAG, "Backend proxy network error ($id)")
            AiResult.Failure(AiErrorKind.PROVIDER_UNAVAILABLE, "Host unreachable")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Backend proxy parse error ($id)")
            AiResult.Failure(AiErrorKind.INVALID_RESPONSE, "Malformed response")
        }
    }

    companion object {
        private const val TAG = "BackendProxyAiProvider"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        private fun defaultClient(connectTimeoutMs: Long, readTimeoutMs: Long): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(readTimeoutMs.coerceAtLeast(10_000L), TimeUnit.MILLISECONDS)
                .build()
        }
    }
}
