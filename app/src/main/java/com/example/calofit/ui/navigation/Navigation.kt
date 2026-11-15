package com.example.calofit.ui.navigation

enum class NavDestination {
    HOME,
    FOOD_LOG,
    PROGRESS,
    PROFILE
}

sealed class SubScreen {
    object None : SubScreen()
    object LanguageSelection : SubScreen()
    object Onboarding : SubScreen()
    data class FoodSearch(val mealType: String = "LUNCH") : SubScreen()
    data class FoodDetail(val foodId: Long, val mealType: String = "LUNCH") : SubScreen()
    object WaterTracking : SubScreen()
    object WeightTracking : SubScreen()
    object ActivityTracking : SubScreen()
    object AIScan : SubScreen()
    object AICoach : SubScreen()
    object MealPlanner : SubScreen()
    object Reports : SubScreen()
    object Achievements : SubScreen()
    object Settings : SubScreen()
    object SmartKitchen : SubScreen()
    object FoodComparison : SubScreen()
}
