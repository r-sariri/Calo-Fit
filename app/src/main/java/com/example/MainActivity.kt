package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.calofit.data.ai.configureAppCheck
import com.example.calofit.localization.StringsProvider
import com.example.calofit.ui.components.CaloFitBottomBar
import com.example.calofit.ui.navigation.NavDestination
import com.example.calofit.ui.navigation.SubScreen
import com.example.calofit.ui.screens.AICoachScreen
import com.example.calofit.ui.screens.AIFoodScanScreen
import com.example.calofit.ui.screens.AchievementsScreen
import com.example.calofit.ui.screens.ActivityTrackingScreen
import com.example.calofit.ui.screens.DashboardScreen
import com.example.calofit.ui.screens.FoodComparisonScreen
import com.example.calofit.ui.screens.FoodDetailScreen
import com.example.calofit.ui.screens.FoodLogScreen
import com.example.calofit.ui.screens.FoodSearchScreen
import com.example.calofit.ui.screens.LanguageSelectionScreen
import com.example.calofit.ui.screens.MealPlannerScreen
import com.example.calofit.ui.screens.OnboardingScreen
import com.example.calofit.ui.screens.ReportsScreen
import com.example.calofit.ui.screens.SettingsScreen
import com.example.calofit.ui.screens.SmartKitchenScreen
import com.example.calofit.ui.screens.WaterTrackingScreen
import com.example.calofit.ui.screens.WeightTrackingScreen
import com.example.calofit.ui.theme.CaloFitTheme
import com.example.calofit.ui.viewmodel.CaloFitViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: CaloFitViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure Firebase App Check before edge-to-edge UI, ViewModel, or Firebase AI access.
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        setContent {
            val language by viewModel.language.collectAsState()
            val themeMode by viewModel.themeMode.collectAsState()

            val isPersian = language == "fa"
            val layoutDirection = if (isPersian) LayoutDirection.Rtl else LayoutDirection.Ltr

            CaloFitTheme(themeMode = themeMode, isPersian = isPersian) {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    CaloFitApp(
                        viewModel = viewModel,
                        isPersian = isPersian
                    )
                }
            }
        }
    }
}

@Composable
fun CaloFitApp(
    viewModel: CaloFitViewModel,
    isPersian: Boolean
) {
    val navDestination by viewModel.navDestination.collectAsState()
    val subScreen by viewModel.subScreen.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val foodLogsToday by viewModel.foodLogsToday.collectAsState()
    val allFoodLogs by viewModel.allFoodLogs.collectAsState()
    val selectedHistoryDate by viewModel.selectedDate.collectAsState()
    val historyDateLogs by viewModel.historyDateLogs.collectAsState()
    val historyDateWaterMl by viewModel.historyDateWaterMl.collectAsState()
    val historyDateBurnedKcal by viewModel.historyDateBurnedKcal.collectAsState()
    val totalCaloriesConsumed by viewModel.totalCaloriesConsumed.collectAsState()
    val totalProteinConsumed by viewModel.totalProteinConsumed.collectAsState()
    val totalCarbsConsumed by viewModel.totalCarbsConsumed.collectAsState()
    val totalFatConsumed by viewModel.totalFatConsumed.collectAsState()
    val waterTotalToday by viewModel.waterTotalToday.collectAsState()
    val allWaterRecords by viewModel.allWaterRecords.collectAsState()
    val burnedCaloriesToday by viewModel.burnedCaloriesToday.collectAsState()
    val activitiesToday by viewModel.activitiesToday.collectAsState()
    val latestWeight by viewModel.latestWeight.collectAsState()
    val weightRecords by viewModel.weightRecords.collectAsState()
    val allFoods by viewModel.allFoods.collectAsState()
    val achievements by viewModel.achievements.collectAsState()
    val savedRecipes by viewModel.savedRecipes.collectAsState()
    val selectedMealPlanDay by viewModel.selectedMealPlanDay.collectAsState()
    val mealPlans by viewModel.mealPlans.collectAsState()
    val currentThemeMode by viewModel.themeMode.collectAsState()
    val currentLanguage by viewModel.language.collectAsState()
    val streakDays by viewModel.currentStreakDays.collectAsState()
    val longestStreakDays by viewModel.longestStreakDays.collectAsState()

    val coachState by viewModel.coachState.collectAsState()
    val smartChefState by viewModel.smartChefState.collectAsState()
    val foodScanState by viewModel.foodScanState.collectAsState()

    fun s(key: String): String = StringsProvider.get(key, isPersian)

    val navLabels = mapOf(
        NavDestination.HOME to s("nav_home"),
        NavDestination.FOOD_LOG to s("nav_food_log"),
        NavDestination.PROGRESS to s("nav_progress"),
        NavDestination.PROFILE to s("nav_profile")
    )

    if (subScreen != SubScreen.None &&
        subScreen !is SubScreen.LanguageSelection &&
        subScreen !is SubScreen.Onboarding
    ) {
        BackHandler {
            viewModel.closeSubScreen()
        }
    }

    when (val screen = subScreen) {
        is SubScreen.LanguageSelection -> {
            LanguageSelectionScreen(
                onLanguageSelected = { lang ->
                    viewModel.completeLanguageSelection(lang)
                }
            )
        }

        is SubScreen.Onboarding -> {
            OnboardingScreen(
                isPersian = isPersian,
                onComplete = { name, age, gender, heightCm, weightKg, targetWeightKg, activityLevel, goal ->
                    viewModel.completeOnboarding(
                        name, age, gender, heightCm, weightKg, targetWeightKg, activityLevel, goal
                    )
                }
            )
        }

        is SubScreen.FoodSearch -> {
            FoodSearchScreen(
                allFoods = allFoods,
                mealType = screen.mealType,
                isPersian = isPersian,
                onFoodSelected = { food ->
                    viewModel.openSubScreen(SubScreen.FoodDetail(food.id, screen.mealType))
                },
                onAddCustomFood = { nameEn, nameFa, category, grams, kcal, p, c, f, fiber, sugar, isIr ->
                    viewModel.addCustomFood(nameEn, nameFa, category, grams, kcal, p, c, f, fiber, sugar, isIr)
                },
                onUpdateCustomFood = { existing, name, grams, kcal, p, c, f, fiber, sugar ->
                    viewModel.updateCustomFood(existing, name, grams, kcal, p, c, f, fiber, sugar)
                },
                onDeleteCustomFood = { id -> viewModel.deleteCustomFood(id) },
                onBackClick = { viewModel.closeSubScreen() }
            )
        }

        is SubScreen.FoodDetail -> {
            val food = allFoods.find { it.id == screen.foodId }
            if (food != null) {
                FoodDetailScreen(
                    food = food,
                    initialMealType = screen.mealType,
                    isPersian = isPersian,
                    onConfirmLog = { mealType, grams ->
                        viewModel.logFood(mealType, food, grams)
                    },
                    onBackClick = { viewModel.openSubScreen(SubScreen.FoodSearch(screen.mealType)) }
                )
            } else {
                viewModel.closeSubScreen()
            }
        }

        is SubScreen.WaterTracking -> {
            WaterTrackingScreen(
                currentWaterMl = waterTotalToday,
                targetWaterMl = userProfile?.waterMl ?: 2500,
                isPersian = isPersian,
                onAddWater = { amount -> viewModel.logWater(amount) },
                onBackClick = { viewModel.closeSubScreen() }
            )
        }

        is SubScreen.WeightTracking -> {
            WeightTrackingScreen(
                userProfile = userProfile,
                weightRecords = weightRecords,
                isPersian = isPersian,
                onSaveWeight = { weight, note -> viewModel.logWeight(weight, note) },
                onBackClick = { viewModel.closeSubScreen() }
            )
        }

        is SubScreen.ActivityTracking -> {
            ActivityTrackingScreen(
                userWeightKg = userProfile?.currentWeightKg ?: 70.0,
                todayActivities = activitiesToday,
                totalBurnedKcal = burnedCaloriesToday,
                isPersian = isPersian,
                onSaveActivity = { type, duration -> viewModel.logActivity(type, duration) },
                onBackClick = { viewModel.closeSubScreen() }
            )
        }

        is SubScreen.AIScan -> {
            AIFoodScanScreen(
                state = foodScanState,
                isPersian = isPersian,
                onImagePicked = { uri -> viewModel.loadScanImageFromUri(uri) },
                onAnalyzeClick = { viewModel.analyzeSelectedFoodImage() },
                onCancelRequest = { viewModel.cancelFoodScanRequest() },
                onClearImage = { viewModel.clearScanImage() },
                onConfirmLog = { foodItem, grams, mealType ->
                    viewModel.logFood(mealType, foodItem, grams)
                },
                onBackClick = { viewModel.closeSubScreen() }
            )
        }

        is SubScreen.AICoach -> {
            AICoachScreen(
                state = coachState,
                isPersian = isPersian,
                onSendMessage = { msg -> viewModel.sendCoachMessage(msg) },
                onRetryLastMessage = { viewModel.retryLastCoachMessage() },
                onCancelRequest = { viewModel.cancelCoachRequest() },
                onClearChat = { viewModel.clearCoachChat() },
                onRefreshStatus = { viewModel.refreshAiConnectivityStatus() },
                onBackClick = { viewModel.closeSubScreen() }
            )
        }

        is SubScreen.MealPlanner -> {
            MealPlannerScreen(
                selectedDay = selectedMealPlanDay,
                mealPlans = mealPlans,
                isPersian = isPersian,
                onSelectDay = { day -> viewModel.selectMealPlanDay(day) },
                onBackClick = { viewModel.closeSubScreen() }
            )
        }

        is SubScreen.Reports -> {
            ReportsScreen(
                userProfile = userProfile,
                selectedDateIso = selectedHistoryDate,
                selectedDateLogs = historyDateLogs,
                selectedDateWaterMl = historyDateWaterMl,
                selectedDateBurnedKcal = historyDateBurnedKcal,
                allFoodLogs = allFoodLogs,
                allWaterRecords = allWaterRecords,
                weightRecords = weightRecords,
                isPersian = isPersian,
                onSelectDate = { date -> viewModel.selectHistoryDate(date) },
                onShiftDate = { delta -> viewModel.shiftHistoryDate(delta) },
                onUpdateLogQuantity = { entry, newGrams ->
                    viewModel.updateFoodLogQuantity(entry, newGrams)
                },
                onDeleteLog = { id -> viewModel.deleteFoodLog(id) },
                onBackClick = { viewModel.closeSubScreen() }
            )
        }

        is SubScreen.Achievements -> {
            AchievementsScreen(
                achievements = achievements,
                currentStreakDays = streakDays,
                longestStreakDays = longestStreakDays,
                isPersian = isPersian,
                onBackClick = { viewModel.closeSubScreen() }
            )
        }

        is SubScreen.SmartKitchen -> {
            SmartKitchenScreen(
                state = smartChefState,
                savedRecipes = savedRecipes,
                isPersian = isPersian,
                onToggleIngredient = { viewModel.toggleSmartChefIngredient(it) },
                onAddCustomIngredient = { viewModel.addCustomSmartChefIngredient(it) },
                onFiltersChange = { viewModel.updateSmartChefFilters(it) },
                onGenerateRecipes = { viewModel.generateSmartChefRecipes() },
                onSaveRecipe = { viewModel.saveRecipeLocally(it) },
                onDeleteSavedRecipe = { viewModel.deleteSavedRecipe(it) },
                onCancelRequest = { viewModel.cancelSmartChefRequest() },
                onBackClick = { viewModel.closeSubScreen() }
            )
        }

        is SubScreen.FoodComparison -> {
            FoodComparisonScreen(
                allFoods = allFoods,
                isPersian = isPersian,
                onBackClick = { viewModel.closeSubScreen() }
            )
        }

        is SubScreen.Settings -> {
            SettingsScreen(
                userProfile = userProfile,
                currentLanguage = currentLanguage,
                currentThemeMode = currentThemeMode,
                isPersian = isPersian,
                onSaveProfile = { name, age, gender, heightCm, currentWeightKg, targetWeightKg, activity, goal, onSaved ->
                    viewModel.updatePersonalProfile(
                        name, age, gender, heightCm, currentWeightKg, targetWeightKg, activity, goal, onSaved
                    )
                },
                onLanguageChange = { viewModel.setLanguage(it) },
                onThemeModeChange = { viewModel.setThemeMode(it) },
                onExportDataJson = { viewModel.exportDataAsJson() },
                onValidateBackupJson = { viewModel.validateBackupJson(it) },
                onRestoreFromBackup = { payload, onDone ->
                    viewModel.restoreFromValidatedBackup(payload, onDone)
                },
                onBackClick = { viewModel.closeSubScreen() }
            )
        }

        SubScreen.None -> {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    CaloFitBottomBar(
                        currentDestination = navDestination,
                        onNavigate = { dest -> viewModel.navigateTo(dest) },
                        labels = navLabels
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (navDestination) {
                        NavDestination.HOME -> {
                            DashboardScreen(
                                userProfile = userProfile,
                                todayLogs = foodLogsToday,
                                consumedCalories = totalCaloriesConsumed,
                                consumedProtein = totalProteinConsumed,
                                consumedCarbs = totalCarbsConsumed,
                                consumedFat = totalFatConsumed,
                                waterTotalMl = waterTotalToday,
                                burnedCalories = burnedCaloriesToday,
                                latestWeight = latestWeight,
                                streakDays = streakDays,
                                isPersian = isPersian,
                                onAddFoodClick = { mealType ->
                                    viewModel.openSubScreen(SubScreen.FoodSearch(mealType))
                                },
                                onQuickWaterClick = { ml -> viewModel.logWater(ml) },
                                onWaterDetailClick = { viewModel.openSubScreen(SubScreen.WaterTracking) },
                                onWeightClick = { viewModel.openSubScreen(SubScreen.WeightTracking) },
                                onActivityClick = { viewModel.openSubScreen(SubScreen.ActivityTracking) },
                                onAIScanClick = { viewModel.openSubScreen(SubScreen.AIScan) },
                                onAICoachClick = { viewModel.openSubScreen(SubScreen.AICoach) },
                                onSmartKitchenClick = { viewModel.openSubScreen(SubScreen.SmartKitchen) },
                                onFoodComparisonClick = { viewModel.openSubScreen(SubScreen.FoodComparison) },
                                onMealPlannerClick = { viewModel.openSubScreen(SubScreen.MealPlanner) },
                                onAchievementsClick = { viewModel.openSubScreen(SubScreen.Achievements) }
                            )
                        }

                        NavDestination.FOOD_LOG -> {
                            FoodLogScreen(
                                todayLogs = foodLogsToday,
                                totalCalories = totalCaloriesConsumed,
                                targetCalories = userProfile?.targetCalories ?: 2000,
                                isPersian = isPersian,
                                onAddFoodClick = { mealType ->
                                    viewModel.openSubScreen(SubScreen.FoodSearch(mealType))
                                },
                                onUpdateLogQuantity = { entry, newGrams ->
                                    viewModel.updateFoodLogQuantity(entry, newGrams)
                                },
                                onDeleteLog = { id -> viewModel.deleteFoodLog(id) }
                            )
                        }

                        NavDestination.PROGRESS -> {
                            ReportsScreen(
                                userProfile = userProfile,
                                selectedDateIso = selectedHistoryDate,
                                selectedDateLogs = historyDateLogs,
                                selectedDateWaterMl = historyDateWaterMl,
                                selectedDateBurnedKcal = historyDateBurnedKcal,
                                allFoodLogs = allFoodLogs,
                                allWaterRecords = allWaterRecords,
                                weightRecords = weightRecords,
                                isPersian = isPersian,
                                onSelectDate = { date -> viewModel.selectHistoryDate(date) },
                                onShiftDate = { delta -> viewModel.shiftHistoryDate(delta) },
                                onUpdateLogQuantity = { entry, newGrams ->
                                    viewModel.updateFoodLogQuantity(entry, newGrams)
                                },
                                onDeleteLog = { id -> viewModel.deleteFoodLog(id) },
                                onBackClick = { viewModel.navigateTo(NavDestination.HOME) },
                                isEmbeddedInTab = true
                            )
                        }

                        NavDestination.PROFILE -> {
                            SettingsScreen(
                                userProfile = userProfile,
                                currentLanguage = currentLanguage,
                                currentThemeMode = currentThemeMode,
                                isPersian = isPersian,
                                onSaveProfile = { name, age, gender, heightCm, currentWeightKg, targetWeightKg, activity, goal, onSaved ->
                                    viewModel.updatePersonalProfile(
                                        name, age, gender, heightCm, currentWeightKg, targetWeightKg, activity, goal, onSaved
                                    )
                                },
                                onLanguageChange = { viewModel.setLanguage(it) },
                                onThemeModeChange = { viewModel.setThemeMode(it) },
                                onExportDataJson = { viewModel.exportDataAsJson() },
                                onValidateBackupJson = { viewModel.validateBackupJson(it) },
                                onRestoreFromBackup = { payload, onDone ->
                                    viewModel.restoreFromValidatedBackup(payload, onDone)
                                },
                                onBackClick = { viewModel.navigateTo(NavDestination.HOME) },
                                isEmbeddedInTab = true
                            )
                        }
                    }
                }
            }
        }
    }
}
