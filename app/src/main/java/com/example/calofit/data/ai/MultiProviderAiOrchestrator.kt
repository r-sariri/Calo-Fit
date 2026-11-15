package com.example.calofit.data.ai

import com.example.BuildConfig
import kotlinx.coroutines.delay

class MultiProviderAiOrchestrator(
    private val connectivityChecker: () -> Boolean,
    private val providers: List<AiProvider> = defaultProviders(),
    private val retryDelayMs: Long = 600L,
    private val nowMs: () -> Long = { System.currentTimeMillis() }
) {

    private val diagnosticBuffer = ArrayDeque<AiDiagnosticEvent>()
    private val diagnosticLock = Any()

    fun getDiagnosticEvents(): List<AiDiagnosticEvent> = synchronized(diagnosticLock) {
        diagnosticBuffer.toList()
    }

    suspend fun chatCoach(
        history: List<AiChatTurn>,
        message: String,
        context: AiUserContext
    ): AiResult<String> {
        return executeWithFallback(
            stage = "coach",
            requiredCapabilities = setOf(AiProviderCapability.TEXT_GENERATION)
        ) { provider ->
            provider.chatCoach(history, message, context)
        }
    }

    suspend fun generateRecipes(
        ingredients: List<String>,
        isPersian: Boolean,
        filters: SmartChefFilters = SmartChefFilters()
    ): AiResult<List<GeneratedRecipe>> {
        return executeWithFallback(
            stage = "recipes",
            requiredCapabilities = setOf(
                AiProviderCapability.TEXT_GENERATION,
                AiProviderCapability.STRUCTURED_JSON
            )
        ) { provider ->
            provider.generateRecipes(ingredients, isPersian, filters)
        }
    }

    suspend fun analyzeFoodImage(
        jpegBytes: ByteArray,
        isPersian: Boolean
    ): AiResult<FoodImageAnalysisResult> {
        return executeWithFallback(
            stage = "vision",
            requiredCapabilities = setOf(
                AiProviderCapability.VISION_ANALYSIS,
                AiProviderCapability.STRUCTURED_JSON
            )
        ) { provider ->
            provider.analyzeFoodImage(jpegBytes, isPersian)
        }
    }

    private suspend fun <T> executeWithFallback(
        stage: String,
        requiredCapabilities: Set<AiProviderCapability>,
        block: suspend (AiProvider) -> AiResult<T>
    ): AiResult<T> {
        if (!connectivityChecker()) {
            recordEvent("connectivity", stage, AiErrorKind.NO_INTERNET.name, "OFFLINE", 0L)
            return AiResult.Failure(AiErrorKind.NO_INTERNET, "No active internet connection")
        }

        val activeProviders = providers
            .asSequence()
            .filter { it.isConfigured }
            .filter { requiredCapabilities.all { req -> req in it.capabilities } }
            .sortedBy { it.priority }
            .toList()

        if (activeProviders.isEmpty()) {
            recordEvent("orchestrator", stage, AiErrorKind.PROVIDER_UNAVAILABLE.name, "NO_MATCHING_PROVIDER", 0L)
            return AiResult.Failure(AiErrorKind.PROVIDER_UNAVAILABLE, "No AI providers configured")
        }

        val unreachableProxyHosts = mutableSetOf<String>()
        var lastFailure: AiResult.Failure = AiResult.Failure(AiErrorKind.PROVIDER_UNAVAILABLE, "All providers failed")

        for (provider in activeProviders) {
            val host = (provider as? BackendProxyAiProvider)?.proxyHost
            if (host != null && host in unreachableProxyHosts) {
                continue
            }

            val start = nowMs()
            var attemptResult = block(provider)
            if (attemptResult is AiResult.Failure &&
                (attemptResult.kind == AiErrorKind.TIMEOUT || attemptResult.kind == AiErrorKind.PROVIDER_UNAVAILABLE) &&
                connectivityChecker()
            ) {
                if (retryDelayMs > 0L) {
                    delay(retryDelayMs)
                }
                if (connectivityChecker()) {
                    attemptResult = block(provider)
                }
            }
            val elapsed = (nowMs() - start).coerceAtLeast(0L)

            when (attemptResult) {
                is AiResult.Success -> {
                    recordEvent(provider.id, stage, "NONE", "2XX", elapsed)
                    return attemptResult
                }
                is AiResult.Failure -> {
                    recordEvent(
                        providerId = provider.id,
                        stage = stage,
                        errorCategory = attemptResult.kind.name,
                        httpStatusCategory = redactStatusCategory(attemptResult.detail),
                        durationMs = elapsed
                    )
                    if (host != null && attemptResult.detail.equals("Host unreachable", ignoreCase = true)) {
                        unreachableProxyHosts.add(host)
                    }
                    lastFailure = attemptResult
                }
            }
        }
        return lastFailure
    }

    private fun redactStatusCategory(detail: String): String {
        val code = Regex("\\b(\\d{3})\\b").find(detail)?.groupValues?.getOrNull(1)?.toIntOrNull()
        return when {
            code == null && detail.contains("Timeout", ignoreCase = true) -> "TIMEOUT"
            code == null && detail.contains("unreachable", ignoreCase = true) -> "NETWORK_UNREACHABLE"
            code == null -> "CLIENT_OR_SDK"
            code in 200..299 -> "2XX"
            code == 401 || code == 403 -> "401_403"
            code == 429 -> "429"
            code in 400..499 -> "4XX"
            code in 500..599 -> "5XX"
            else -> "OTHER"
        }
    }

    private fun recordEvent(
        providerId: String,
        stage: String,
        errorCategory: String,
        httpStatusCategory: String,
        durationMs: Long
    ) {
        synchronized(diagnosticLock) {
            if (diagnosticBuffer.size >= MAX_DIAGNOSTIC_EVENTS) {
                diagnosticBuffer.removeFirst()
            }
            diagnosticBuffer.addLast(
                AiDiagnosticEvent(
                    providerId = providerId,
                    stage = stage,
                    errorCategory = errorCategory,
                    httpStatusCategory = httpStatusCategory,
                    durationMs = durationMs,
                    timestampMs = nowMs()
                )
            )
        }
    }

    companion object {
        private const val MAX_DIAGNOSTIC_EVENTS = 50

        /**
         * Canonical 5-tier sequential AI fallback registry (never parallel):
         * 1. OpenAI (via HTTPS proxy relay when configured)
         * 2. DeepSeek (via HTTPS proxy relay when configured — text & JSON only)
         * 3. Grok (via HTTPS proxy relay when configured)
         * 4. Claude (via HTTPS proxy relay when configured)
         * 5. Gemini (Firebase AI Logic + App Check)
         */
        fun providerRegistry(): List<AiProviderConfig> = listOf(
            AiProviderConfig(
                id = "openai_proxy",
                displayName = "OpenAI",
                priority = 1,
                modelId = "gpt-4o-mini",
                capabilities = setOf(
                    AiProviderCapability.TEXT_GENERATION,
                    AiProviderCapability.VISION_ANALYSIS,
                    AiProviderCapability.STRUCTURED_JSON
                )
            ),
            AiProviderConfig(
                id = "deepseek_proxy",
                displayName = "DeepSeek",
                priority = 2,
                modelId = "deepseek-chat",
                capabilities = setOf(
                    AiProviderCapability.TEXT_GENERATION,
                    AiProviderCapability.STRUCTURED_JSON
                )
            ),
            AiProviderConfig(
                id = "grok_proxy",
                displayName = "Grok",
                priority = 3,
                modelId = "grok-2-vision",
                capabilities = setOf(
                    AiProviderCapability.TEXT_GENERATION,
                    AiProviderCapability.VISION_ANALYSIS,
                    AiProviderCapability.STRUCTURED_JSON
                )
            ),
            AiProviderConfig(
                id = "claude_proxy",
                displayName = "Claude",
                priority = 4,
                modelId = "claude-sonnet",
                capabilities = setOf(
                    AiProviderCapability.TEXT_GENERATION,
                    AiProviderCapability.VISION_ANALYSIS,
                    AiProviderCapability.STRUCTURED_JSON
                )
            ),
            AiProviderConfig(
                id = "firebase_ai_logic",
                displayName = "Gemini (Firebase AI Logic)",
                priority = 5,
                modelId = "gemini-3.8-flash",
                capabilities = setOf(
                    AiProviderCapability.TEXT_GENERATION,
                    AiProviderCapability.VISION_ANALYSIS,
                    AiProviderCapability.STRUCTURED_JSON
                ),
                textTimeoutMs = 18_000L,
                visionTimeoutMs = 35_000L
            )
        )

        fun defaultProviders(): List<AiProvider> {
            val primaryProxy = BuildConfig.AI_PROXY_PRIMARY_URL
            val secondaryProxy = BuildConfig.AI_PROXY_SECONDARY_URL
            val registry = providerRegistry().associateBy { it.id }

            return listOf(
                BackendProxyAiProvider(
                    id = "openai_proxy",
                    baseUrl = primaryProxy,
                    priority = registry.getValue("openai_proxy").priority,
                    capabilities = registry.getValue("openai_proxy").capabilities,
                    providerHint = "openai"
                ),
                BackendProxyAiProvider(
                    id = "deepseek_proxy",
                    baseUrl = primaryProxy,
                    priority = registry.getValue("deepseek_proxy").priority,
                    capabilities = registry.getValue("deepseek_proxy").capabilities,
                    providerHint = "deepseek"
                ),
                BackendProxyAiProvider(
                    id = "grok_proxy",
                    baseUrl = primaryProxy,
                    priority = registry.getValue("grok_proxy").priority,
                    capabilities = registry.getValue("grok_proxy").capabilities,
                    providerHint = "grok"
                ),
                BackendProxyAiProvider(
                    id = "claude_proxy",
                    baseUrl = primaryProxy,
                    priority = registry.getValue("claude_proxy").priority,
                    capabilities = registry.getValue("claude_proxy").capabilities,
                    providerHint = "claude"
                ),
                FirebaseAiLogicProvider(
                    priority = registry.getValue("firebase_ai_logic").priority
                ),
                BackendProxyAiProvider(
                    id = "secondary_https_proxy",
                    baseUrl = secondaryProxy,
                    priority = 6,
                    providerHint = "fallback"
                )
            )
        }
    }
}
