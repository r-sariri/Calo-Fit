package com.example.calofit.data.ai

interface AiProvider {
    val id: String
    val isConfigured: Boolean
    val priority: Int
        get() = 100
    val capabilities: Set<AiProviderCapability>
        get() = setOf(
            AiProviderCapability.TEXT_GENERATION,
            AiProviderCapability.VISION_ANALYSIS,
            AiProviderCapability.STRUCTURED_JSON
        )

    suspend fun chatCoach(
        history: List<AiChatTurn>,
        message: String,
        context: AiUserContext
    ): AiResult<String>

    suspend fun generateRecipes(
        ingredients: List<String>,
        isPersian: Boolean,
        filters: SmartChefFilters = SmartChefFilters()
    ): AiResult<List<GeneratedRecipe>>

    suspend fun analyzeFoodImage(
        jpegBytes: ByteArray,
        isPersian: Boolean
    ): AiResult<FoodImageAnalysisResult>
}
