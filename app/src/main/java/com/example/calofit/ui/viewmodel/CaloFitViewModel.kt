package com.example.calofit.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calofit.data.ai.AiChatTurn
import com.example.calofit.data.ai.AiErrorKind
import com.example.calofit.data.ai.AiResult
import com.example.calofit.data.ai.AiUserContext
import com.example.calofit.data.ai.FoodImageAnalysisResult
import com.example.calofit.data.ai.GeneratedRecipe
import com.example.calofit.data.ai.MultiProviderAiOrchestrator
import com.example.calofit.data.ai.NetworkConnectivityHelper
import com.example.calofit.data.ai.SmartChefFilters
import com.example.calofit.data.database.CaloFitDatabase
import com.example.calofit.data.model.AchievementEntity
import com.example.calofit.data.model.ActivityRecordEntity
import com.example.calofit.data.model.FoodItemEntity
import com.example.calofit.data.model.FoodLogEntryEntity
import com.example.calofit.data.model.MealPlanItemEntity
import com.example.calofit.data.model.SavedRecipeEntity
import com.example.calofit.data.model.UserProfileEntity
import com.example.calofit.data.model.WaterRecordEntity
import com.example.calofit.data.model.WeightRecordEntity
import com.example.calofit.data.preferences.UserPreferencesManager
import com.example.calofit.data.repository.BackupJsonCodec
import com.example.calofit.data.repository.BackupValidationResult
import com.example.calofit.data.repository.CaloFitRepository
import com.example.calofit.data.repository.ValidatedBackupPayload
import com.example.calofit.domain.calculator.NutritionCalculator
import com.example.calofit.domain.calculator.StreakAndAchievementCalculator
import com.example.calofit.domain.calendar.JalaliCalendarHelper
import com.example.calofit.localization.StringsProvider
import com.example.calofit.ui.navigation.NavDestination
import com.example.calofit.ui.navigation.SubScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class AiCoachMessage(
    val id: Long,
    val isUser: Boolean,
    val text: String
)

data class AiCoachUiState(
    val messages: List<AiCoachMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isOffline: Boolean = false,
    val activeProviderId: String? = null,
    val errorMessage: String? = null,
    val lastFailedMessage: String? = null
)

data class SmartChefUiState(
    val selectedIngredients: Set<String> = emptySet(),
    val filters: SmartChefFilters = SmartChefFilters(),
    val recipes: List<GeneratedRecipe> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val validationError: String? = null,
    val recipeSavedMessage: String? = null
)

data class FoodScanUiState(
    val selectedImageBytes: ByteArray? = null,
    val previewBitmap: Bitmap? = null,
    val isLoading: Boolean = false,
    val result: FoodImageAnalysisResult? = null,
    val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class CaloFitViewModel(application: Application) : AndroidViewModel(application) {

    private val database = CaloFitDatabase.getDatabase(application, viewModelScope)
    private val preferencesManager = UserPreferencesManager(application)
    private val connectivityHelper = NetworkConnectivityHelper(application)
    private val aiOrchestrator = MultiProviderAiOrchestrator(
        connectivityChecker = { connectivityHelper.isInternetAvailable() }
    )

    val repository = CaloFitRepository(
        userProfileDao = database.userProfileDao(),
        foodDao = database.foodDao(),
        foodLogDao = database.foodLogDao(),
        weightDao = database.weightDao(),
        waterDao = database.waterDao(),
        activityDao = database.activityDao(),
        mealPlanDao = database.mealPlanDao(),
        achievementDao = database.achievementDao(),
        savedRecipeDao = database.savedRecipeDao(),
        preferencesManager = preferencesManager,
        database = database
    )

    val language: StateFlow<String> = preferencesManager.appLanguage
        .stateIn(viewModelScope, SharingStarted.Eagerly, "fa")

    val themeMode: StateFlow<String> = preferencesManager.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, "LIGHT")

    val firstLaunchCompleted: StateFlow<Boolean> = preferencesManager.firstLaunchCompleted
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedDate = MutableStateFlow(JalaliCalendarHelper.getTodayIsoDate())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    val foodLogsToday: StateFlow<List<FoodLogEntryEntity>> =
        repository.getLogsForDate(JalaliCalendarHelper.getTodayIsoDate())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFoodLogs: StateFlow<List<FoodLogEntryEntity>> = repository.allFoodLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historyDateLogs: StateFlow<List<FoodLogEntryEntity>> = _selectedDate
        .flatMapLatest { dateIso -> repository.getLogsForDate(dateIso) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historyDateWaterMl: StateFlow<Int> = _selectedDate
        .flatMapLatest { dateIso -> repository.getWaterTotalForDate(dateIso) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val historyDateBurnedKcal: StateFlow<Int> = _selectedDate
        .flatMapLatest { dateIso -> repository.getTotalBurnedCaloriesForDate(dateIso) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalCaloriesConsumed: StateFlow<Int> = foodLogsToday.map { logs ->
        logs.sumOf { it.calories }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalProteinConsumed: StateFlow<Double> = foodLogsToday.map { logs ->
        logs.sumOf { it.protein }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalCarbsConsumed: StateFlow<Double> = foodLogsToday.map { logs ->
        logs.sumOf { it.carbs }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalFatConsumed: StateFlow<Double> = foodLogsToday.map { logs ->
        logs.sumOf { it.fat }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val waterTotalToday: StateFlow<Int> = repository.getWaterTotalForDate(JalaliCalendarHelper.getTodayIsoDate())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allWaterRecords: StateFlow<List<WaterRecordEntity>> = repository.allWaterRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val burnedCaloriesToday: StateFlow<Int> = repository.getTotalBurnedCaloriesForDate(JalaliCalendarHelper.getTodayIsoDate())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val activitiesToday: StateFlow<List<ActivityRecordEntity>> = repository.getActivitiesForDate(JalaliCalendarHelper.getTodayIsoDate())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allActivities: StateFlow<List<ActivityRecordEntity>> = repository.allActivities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weightRecords: StateFlow<List<WeightRecordEntity>> = repository.weightRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestWeight: StateFlow<WeightRecordEntity?> = repository.latestWeight
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allFoods: StateFlow<List<FoodItemEntity>> = repository.getAllFoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val achievements: StateFlow<List<AchievementEntity>> = repository.achievements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedRecipes: StateFlow<List<SavedRecipeEntity>> = repository.savedRecipes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedMealPlanDay = MutableStateFlow(1)
    val selectedMealPlanDay: StateFlow<Int> = _selectedMealPlanDay.asStateFlow()

    val mealPlans: StateFlow<List<MealPlanItemEntity>> = _selectedMealPlanDay
        .flatMapLatest { day -> repository.getMealPlansForDay(day) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentStreakDays: StateFlow<Int> = allFoodLogs.map { logs ->
        StreakAndAchievementCalculator.calculateConsecutiveStreak(logs.map { it.dateIso })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val longestStreakDays: StateFlow<Int> = allFoodLogs.map { logs ->
        StreakAndAchievementCalculator.calculateLongestStreak(logs.map { it.dateIso })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Navigation State
    private val _navDestination = MutableStateFlow(NavDestination.HOME)
    val navDestination: StateFlow<NavDestination> = _navDestination.asStateFlow()

    private val _subScreen = MutableStateFlow<SubScreen>(SubScreen.None)
    val subScreen: StateFlow<SubScreen> = _subScreen.asStateFlow()

    // AI States
    private val _coachState = MutableStateFlow(AiCoachUiState())
    val coachState: StateFlow<AiCoachUiState> = _coachState.asStateFlow()

    private val _smartChefState = MutableStateFlow(SmartChefUiState())
    val smartChefState: StateFlow<SmartChefUiState> = _smartChefState.asStateFlow()

    private val _foodScanState = MutableStateFlow(FoodScanUiState())
    val foodScanState: StateFlow<FoodScanUiState> = _foodScanState.asStateFlow()

    private var coachJob: Job? = null
    private var smartChefJob: Job? = null
    private var foodScanJob: Job? = null

    init {
        viewModelScope.launch {
            preferencesManager.firstLaunchCompleted.collect { completed ->
                if (!completed && _subScreen.value == SubScreen.None) {
                    _subScreen.value = SubScreen.LanguageSelection
                }
            }
        }
        viewModelScope.launch {
            repository.refreshAchievements()
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            preferencesManager.setLanguage(lang)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(mode)
        }
    }

    fun selectHistoryDate(dateIso: String) {
        val today = JalaliCalendarHelper.getTodayIsoDate()
        _selectedDate.value = if (dateIso > today) today else dateIso
    }

    fun shiftHistoryDate(days: Int) {
        val next = JalaliCalendarHelper.addDays(_selectedDate.value, days)
        val today = JalaliCalendarHelper.getTodayIsoDate()
        if (next <= today) {
            _selectedDate.value = next
        }
    }

    fun selectMealPlanDay(day: Int) {
        _selectedMealPlanDay.value = day.coerceIn(1, 7)
    }

    fun navigateTo(destination: NavDestination) {
        _navDestination.value = destination
        _subScreen.value = SubScreen.None
    }

    fun openSubScreen(screen: SubScreen) {
        _subScreen.value = screen
    }

    fun closeSubScreen() {
        _subScreen.value = SubScreen.None
    }

    fun completeLanguageSelection(lang: String) {
        viewModelScope.launch {
            preferencesManager.setLanguage(lang)
            _subScreen.value = SubScreen.Onboarding
        }
    }

    fun completeOnboarding(
        name: String,
        age: Int,
        gender: NutritionCalculator.Gender,
        heightCm: Double,
        weightKg: Double,
        targetWeightKg: Double,
        activityLevel: NutritionCalculator.ActivityLevel,
        goal: NutritionCalculator.WeightGoal
    ) {
        viewModelScope.launch {
            val plan = NutritionCalculator.calculateFullPlan(
                weightKg = weightKg,
                heightCm = heightCm,
                age = age,
                gender = gender,
                activityLevel = activityLevel,
                goal = goal
            )

            val profile = UserProfileEntity(
                name = name.trim(),
                age = age,
                gender = gender.name,
                heightCm = heightCm,
                currentWeightKg = weightKg,
                targetWeightKg = targetWeightKg,
                activityLevel = activityLevel.name,
                goal = goal.name,
                targetCalories = plan.targetCalories,
                proteinGrams = plan.proteinGrams,
                carbsGrams = plan.carbsGrams,
                fatGrams = plan.fatGrams,
                waterMl = plan.waterMl,
                isSetupComplete = true
            )

            repository.saveUserProfile(profile)
            repository.logWeight(
                JalaliCalendarHelper.getTodayIsoDate(),
                weightKg,
                if (language.value == "fa") "وزن شروع برنامه" else "Initial Onboarding Weight"
            )
            preferencesManager.setFirstLaunchCompleted(true)
            _subScreen.value = SubScreen.None
            _navDestination.value = NavDestination.HOME
        }
    }

    fun updatePersonalProfile(
        name: String,
        age: Int,
        gender: NutritionCalculator.Gender,
        heightCm: Double,
        currentWeightKg: Double,
        targetWeightKg: Double,
        activityLevel: NutritionCalculator.ActivityLevel,
        goal: NutritionCalculator.WeightGoal,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val plan = NutritionCalculator.calculateFullPlan(
                weightKg = currentWeightKg,
                heightCm = heightCm,
                age = age,
                gender = gender,
                activityLevel = activityLevel,
                goal = goal
            )
            val existing = repository.getUserProfileOnce()
            val updated = (existing ?: UserProfileEntity()).copy(
                id = 1,
                name = name.trim(),
                age = age,
                gender = gender.name,
                heightCm = heightCm,
                currentWeightKg = currentWeightKg,
                targetWeightKg = targetWeightKg,
                activityLevel = activityLevel.name,
                goal = goal.name,
                targetCalories = plan.targetCalories,
                proteinGrams = plan.proteinGrams,
                carbsGrams = plan.carbsGrams,
                fatGrams = plan.fatGrams,
                waterMl = plan.waterMl,
                isSetupComplete = true,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateProfilePreservingWeightHistory(updated)
            onComplete()
        }
    }

    fun logFood(
        mealType: String,
        foodItem: FoodItemEntity,
        grams: Double,
        dateIso: String = JalaliCalendarHelper.getTodayIsoDate()
    ) {
        viewModelScope.launch {
            repository.addFoodLogEntry(
                dateIso = dateIso,
                mealType = mealType,
                foodItem = foodItem,
                grams = grams
            )
            _subScreen.value = SubScreen.None
        }
    }

    fun updateFoodLogQuantity(entry: FoodLogEntryEntity, newGrams: Double) {
        viewModelScope.launch {
            repository.updateFoodLogQuantity(entry, newGrams)
        }
    }

    fun deleteFoodLog(id: Long) {
        viewModelScope.launch {
            repository.deleteFoodLog(id)
        }
    }

    fun logWater(amountMl: Int) {
        viewModelScope.launch {
            repository.logWater(JalaliCalendarHelper.getTodayIsoDate(), amountMl)
        }
    }

    fun logWeight(weightKg: Double, note: String = "") {
        viewModelScope.launch {
            repository.logWeight(JalaliCalendarHelper.getTodayIsoDate(), weightKg, note)
            _subScreen.value = SubScreen.None
        }
    }

    fun logActivity(activityType: String, durationMinutes: Int) {
        viewModelScope.launch {
            val weight = userProfile.value?.currentWeightKg ?: 70.0
            val burned = NutritionCalculator.calculateBurnedCalories(activityType, durationMinutes, weight)
            repository.logActivity(
                dateIso = JalaliCalendarHelper.getTodayIsoDate(),
                activityType = activityType,
                durationMinutes = durationMinutes,
                burnedCalories = burned
            )
            _subScreen.value = SubScreen.None
        }
    }

    fun addCustomFood(
        nameEn: String,
        nameFa: String,
        category: String,
        servingGrams: Double,
        calories: Int,
        protein: Double,
        carbs: Double,
        fat: Double,
        fiber: Double = 0.0,
        sugar: Double = 0.0,
        isIranian: Boolean
    ) {
        viewModelScope.launch {
            val food = FoodItemEntity(
                nameEn = nameEn.trim(),
                nameFa = nameFa.trim().ifBlank { nameEn.trim() },
                category = category,
                servingSizeGrams = servingGrams,
                servingUnitEn = "${servingGrams.toInt()}g",
                servingUnitFa = "${JalaliCalendarHelper.toPersianDigits(servingGrams.toInt().toString())} گرم",
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat,
                fiber = fiber,
                sugar = sugar,
                isIranian = isIranian,
                isVerified = false
            )
            repository.insertCustomFood(food)
        }
    }

    fun updateCustomFood(
        existing: FoodItemEntity,
        name: String,
        servingGrams: Double,
        calories: Int,
        protein: Double,
        carbs: Double,
        fat: Double,
        fiber: Double,
        sugar: Double
    ) {
        viewModelScope.launch {
            val cleanName = name.trim()
            val updated = existing.copy(
                nameEn = if (existing.isIranian) existing.nameEn.ifBlank { cleanName } else cleanName,
                nameFa = if (existing.isIranian) cleanName else existing.nameFa.ifBlank { cleanName },
                servingSizeGrams = servingGrams,
                servingUnitEn = "${servingGrams.toInt()}g",
                servingUnitFa = "${JalaliCalendarHelper.toPersianDigits(servingGrams.toInt().toString())} گرم",
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat,
                fiber = fiber,
                sugar = sugar
            )
            repository.updateCustomFood(updated)
        }
    }

    fun deleteCustomFood(id: Long) {
        viewModelScope.launch {
            repository.deleteCustomFood(id)
        }
    }

    // --- AI Nutrition Coach ---

    fun refreshAiConnectivityStatus() {
        val online = connectivityHelper.isInternetAvailable()
        _coachState.update { it.copy(isOffline = !online) }
    }

    fun sendCoachMessage(rawMessage: String) {
        val message = rawMessage.trim()
        if (message.isBlank() || _coachState.value.isLoading) return

        val isPersian = language.value == "fa"
        if (!connectivityHelper.isInternetAvailable()) {
            _coachState.update {
                it.copy(
                    isLoading = false,
                    isOffline = true,
                    errorMessage = StringsProvider.get("err_coach_no_internet", isPersian),
                    lastFailedMessage = message
                )
            }
            return
        }

        val userTurn = AiCoachMessage(
            id = System.nanoTime(),
            isUser = true,
            text = message
        )
        val historyTurns = _coachState.value.messages.map { AiChatTurn(it.isUser, it.text) }

        _coachState.update {
            it.copy(
                messages = it.messages + userTurn,
                isLoading = true,
                isOffline = false,
                errorMessage = null,
                lastFailedMessage = null
            )
        }

        launchCoachRequest(historyTurns = historyTurns, message = message, isPersian = isPersian)
    }

    fun retryLastCoachMessage() {
        val failed = _coachState.value.lastFailedMessage?.trim().orEmpty()
        if (failed.isBlank() || _coachState.value.isLoading) return

        val isPersian = language.value == "fa"
        if (!connectivityHelper.isInternetAvailable()) {
            _coachState.update {
                it.copy(
                    isLoading = false,
                    isOffline = true,
                    errorMessage = StringsProvider.get("err_coach_no_internet", isPersian),
                    lastFailedMessage = failed
                )
            }
            return
        }

        val existing = _coachState.value.messages
        val lastMsg = existing.lastOrNull()
        val alreadyHasTrailingUserTurn = lastMsg != null && lastMsg.isUser && lastMsg.text == failed
        val historyTurns = (if (alreadyHasTrailingUserTurn) existing.dropLast(1) else existing)
            .map { AiChatTurn(it.isUser, it.text) }
        val updatedMessages = if (alreadyHasTrailingUserTurn) {
            existing
        } else {
            existing + AiCoachMessage(id = System.nanoTime(), isUser = true, text = failed)
        }

        _coachState.update {
            it.copy(
                messages = updatedMessages,
                isLoading = true,
                isOffline = false,
                errorMessage = null,
                lastFailedMessage = null
            )
        }

        launchCoachRequest(historyTurns = historyTurns, message = failed, isPersian = isPersian)
    }

    private fun launchCoachRequest(
        historyTurns: List<AiChatTurn>,
        message: String,
        isPersian: Boolean
    ) {
        val profile = userProfile.value
        val recentFoods = foodLogsToday.value.takeLast(8).map { entry ->
            "${entry.localizedName(isPersian)} (${entry.calories} kcal)"
        }
        val context = AiUserContext(
            languageCode = language.value,
            goal = profile?.goal ?: "MAINTAIN_WEIGHT",
            targetCalories = profile?.targetCalories ?: 2000,
            proteinGrams = profile?.proteinGrams ?: 120,
            carbsGrams = profile?.carbsGrams ?: 240,
            fatGrams = profile?.fatGrams ?: 60,
            consumedCaloriesToday = totalCaloriesConsumed.value,
            recentFoodsSummary = recentFoods,
            age = profile?.age,
            gender = profile?.gender
        )

        coachJob?.cancel()
        coachJob = viewModelScope.launch {
            when (val res = aiOrchestrator.chatCoach(historyTurns, message, context)) {
                is AiResult.Success -> {
                    val botMsg = AiCoachMessage(
                        id = System.nanoTime(),
                        isUser = false,
                        text = res.data
                    )
                    _coachState.update {
                        it.copy(
                            messages = it.messages + botMsg,
                            isLoading = false,
                            isOffline = false,
                            activeProviderId = res.providerId,
                            errorMessage = null,
                            lastFailedMessage = null
                        )
                    }
                }
                is AiResult.Failure -> {
                    val msg = if (res.kind == AiErrorKind.NO_INTERNET) {
                        StringsProvider.get("err_coach_no_internet", isPersian)
                    } else {
                        mapAiErrorToUserMessage(res.kind, isPersian)
                    }
                    _coachState.update {
                        it.copy(
                            isLoading = false,
                            isOffline = res.kind == AiErrorKind.NO_INTERNET,
                            errorMessage = msg,
                            lastFailedMessage = message
                        )
                    }
                }
            }
        }
    }

    fun cancelCoachRequest() {
        coachJob?.cancel()
        _coachState.update { it.copy(isLoading = false) }
    }

    fun clearCoachChat() {
        coachJob?.cancel()
        _coachState.value = AiCoachUiState(isOffline = !connectivityHelper.isInternetAvailable())
    }

    // --- Smart Chef ---

    fun toggleSmartChefIngredient(ingredient: String) {
        val clean = ingredient.trim()
        if (clean.isBlank()) return
        _smartChefState.update { state ->
            val updated = state.selectedIngredients.toMutableSet()
            if (!updated.add(clean)) {
                updated.remove(clean)
            }
            state.copy(selectedIngredients = updated, validationError = null, errorMessage = null, recipeSavedMessage = null)
        }
    }

    fun addCustomSmartChefIngredient(ingredient: String) {
        val clean = ingredient.trim()
        if (clean.isBlank()) return
        _smartChefState.update { state ->
            state.copy(
                selectedIngredients = state.selectedIngredients + clean,
                validationError = null,
                errorMessage = null,
                recipeSavedMessage = null
            )
        }
    }

    fun updateSmartChefFilters(filters: SmartChefFilters) {
        _smartChefState.update { it.copy(filters = filters, errorMessage = null) }
    }

    fun generateSmartChefRecipes() {
        val isPersian = language.value == "fa"
        val current = _smartChefState.value
        if (current.isLoading) return
        if (current.selectedIngredients.isEmpty()) {
            _smartChefState.update {
                it.copy(
                    validationError = StringsProvider.get("select_ingredients_empty_msg", isPersian),
                    recipes = emptyList()
                )
            }
            return
        }

        _smartChefState.update { it.copy(isLoading = true, errorMessage = null, validationError = null, recipeSavedMessage = null) }
        smartChefJob?.cancel()
        smartChefJob = viewModelScope.launch {
            when (val res = aiOrchestrator.generateRecipes(current.selectedIngredients.toList(), isPersian, current.filters)) {
                is AiResult.Success -> {
                    _smartChefState.update {
                        it.copy(isLoading = false, recipes = res.data, errorMessage = null)
                    }
                }
                is AiResult.Failure -> {
                    _smartChefState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = mapAiErrorToUserMessage(res.kind, isPersian)
                        )
                    }
                }
            }
        }
    }

    fun saveRecipeLocally(recipe: GeneratedRecipe) {
        val isPersian = language.value == "fa"
        viewModelScope.launch {
            repository.saveRecipe(recipe, isPersian)
            _smartChefState.update {
                it.copy(recipeSavedMessage = StringsProvider.get("recipe_saved", isPersian))
            }
        }
    }

    fun deleteSavedRecipe(id: Long) {
        viewModelScope.launch {
            repository.deleteSavedRecipe(id)
        }
    }

    fun cancelSmartChefRequest() {
        smartChefJob?.cancel()
        _smartChefState.update { it.copy(isLoading = false) }
    }

    // --- AI Food Image Scan ---

    fun loadScanImageFromUri(uri: Uri) {
        foodScanJob?.cancel()
        viewModelScope.launch {
            val isPersian = language.value == "fa"
            val decoded = withContext(Dispatchers.IO) {
                decodeAndCompressImage(uri)
            }
            if (decoded == null) {
                _foodScanState.update {
                    it.copy(errorMessage = StringsProvider.get("err_ai_unavailable", isPersian))
                }
            } else {
                _foodScanState.value = FoodScanUiState(
                    selectedImageBytes = decoded.second,
                    previewBitmap = decoded.first,
                    isLoading = false,
                    result = null,
                    errorMessage = null
                )
            }
        }
    }

    fun analyzeSelectedFoodImage() {
        val current = _foodScanState.value
        val bytes = current.selectedImageBytes ?: return
        if (current.isLoading) return

        val isPersian = language.value == "fa"
        _foodScanState.update { it.copy(isLoading = true, errorMessage = null) }

        foodScanJob?.cancel()
        foodScanJob = viewModelScope.launch {
            when (val res = aiOrchestrator.analyzeFoodImage(bytes, isPersian)) {
                is AiResult.Success -> {
                    _foodScanState.update {
                        it.copy(isLoading = false, result = res.data, errorMessage = null)
                    }
                }
                is AiResult.Failure -> {
                    _foodScanState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = mapAiErrorToUserMessage(res.kind, isPersian)
                        )
                    }
                }
            }
        }
    }

    fun cancelFoodScanRequest() {
        foodScanJob?.cancel()
        _foodScanState.update { it.copy(isLoading = false) }
    }

    fun clearScanImage() {
        foodScanJob?.cancel()
        _foodScanState.value = FoodScanUiState()
    }

    private fun decodeAndCompressImage(uri: Uri): Pair<Bitmap, ByteArray>? {
        val resolver = getApplication<Application>().contentResolver
        val rawBitmap = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return null

        val maxDim = 1024
        val width = rawBitmap.width
        val height = rawBitmap.height
        val scale = if (width > maxDim || height > maxDim) {
            minOf(maxDim.toFloat() / width, maxDim.toFloat() / height)
        } else 1f

        val finalBitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                rawBitmap,
                (width * scale).toInt().coerceAtLeast(1),
                (height * scale).toInt().coerceAtLeast(1),
                true
            ).also {
                if (it != rawBitmap) rawBitmap.recycle()
            }
        } else {
            rawBitmap
        }

        val out = ByteArrayOutputStream()
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return finalBitmap to out.toByteArray()
    }

    private fun mapAiErrorToUserMessage(kind: AiErrorKind, isPersian: Boolean): String {
        return when (kind) {
            AiErrorKind.NO_INTERNET -> StringsProvider.get("err_no_internet", isPersian)
            AiErrorKind.RATE_LIMITED -> StringsProvider.get("err_ai_rate_limit", isPersian)
            AiErrorKind.AUTH_FAILED -> StringsProvider.get("err_ai_auth", isPersian)
            AiErrorKind.TIMEOUT -> StringsProvider.get("err_ai_timeout", isPersian)
            AiErrorKind.PROVIDER_UNAVAILABLE,
            AiErrorKind.INVALID_RESPONSE -> StringsProvider.get("err_ai_unavailable", isPersian)
        }
    }

    fun exportDataAsJson(): String {
        return BackupJsonCodec.encode(
            profile = userProfile.value,
            customFoods = allFoods.value,
            foodLogs = allFoodLogs.value,
            weightRecords = weightRecords.value,
            waterRecords = allWaterRecords.value,
            activities = allActivities.value,
            savedRecipes = savedRecipes.value,
            achievements = achievements.value
        )
    }

    fun validateBackupJson(rawJson: String): BackupValidationResult {
        return BackupJsonCodec.parseAndValidate(rawJson)
    }

    fun restoreFromValidatedBackup(payload: ValidatedBackupPayload, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.restoreFromBackup(payload)
            if (payload.profile != null) {
                preferencesManager.setFirstLaunchCompleted(true)
            }
            onDone()
        }
    }
}
