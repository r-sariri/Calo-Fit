package com.example.calofit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calofit.data.model.FoodLogEntryEntity
import com.example.calofit.data.model.UserProfileEntity
import com.example.calofit.data.model.WeightRecordEntity
import com.example.calofit.domain.calculator.NutritionCalculator
import com.example.calofit.domain.calendar.JalaliCalendarHelper
import com.example.calofit.localization.StringsProvider
import com.example.calofit.ui.components.CaloFitCard
import com.example.calofit.ui.components.CaloFitTopHeader
import com.example.calofit.ui.components.CircularCalorieMeter
import com.example.calofit.ui.components.MacroProgressBar
import com.example.calofit.ui.theme.CaloFitMacroCarbs
import com.example.calofit.ui.theme.CaloFitMacroFat
import com.example.calofit.ui.theme.CaloFitMacroProtein
import com.example.calofit.ui.theme.CaloFitWaterBlue
import com.example.calofit.ui.theme.LocalCaloFitColors

@Composable
fun DashboardScreen(
    userProfile: UserProfileEntity?,
    todayLogs: List<FoodLogEntryEntity>,
    consumedCalories: Int,
    consumedProtein: Double,
    consumedCarbs: Double,
    consumedFat: Double,
    waterTotalMl: Int,
    burnedCalories: Int,
    latestWeight: WeightRecordEntity?,
    streakDays: Int,
    isPersian: Boolean,
    onAddFoodClick: (mealType: String) -> Unit,
    onQuickWaterClick: (Int) -> Unit,
    onWaterDetailClick: () -> Unit,
    onWeightClick: () -> Unit,
    onActivityClick: () -> Unit,
    onAIScanClick: () -> Unit,
    onAICoachClick: () -> Unit,
    onSmartKitchenClick: () -> Unit,
    onFoodComparisonClick: () -> Unit,
    onMealPlannerClick: () -> Unit,
    onAchievementsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalCaloFitColors.current
    fun s(key: String): String = StringsProvider.get(key, isPersian)
    fun n(value: Number): String = StringsProvider.formatNumber(value, isPersian)

    val targetCalories = userProfile?.targetCalories ?: 2000
    val targetProtein = userProfile?.proteinGrams ?: 125
    val targetCarbs = userProfile?.carbsGrams ?: 250
    val targetFat = userProfile?.fatGrams ?: 55
    val targetWater = userProfile?.waterMl ?: 2500

    val remainingCalories = (targetCalories - consumedCalories + burnedCalories).coerceAtLeast(0)
    val greeting = s(JalaliCalendarHelper.getGreetingKeyForCurrentHour())
    val dateString = JalaliCalendarHelper.getFormattedDate(isPersian)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(palette.mainBackground, palette.secondaryBackground)
                )
            )
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp)
    ) {
        CaloFitTopHeader(
            greeting = greeting,
            userName = userProfile?.name ?: "",
            dateString = dateString,
            streakDays = streakDays,
            isPersian = isPersian,
            onStreakClick = onAchievementsClick
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Hero Circular Calorie Meter Card
        CaloFitCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularCalorieMeter(
                    remainingKcal = remainingCalories,
                    targetKcal = targetCalories,
                    consumedKcal = consumedCalories,
                    isPersian = isPersian,
                    remainingLabel = s("calories_remaining"),
                    kcalLabel = s("unit_calories")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CalorieStatItem(
                        label = s("daily_target"),
                        value = n(targetCalories),
                        unit = s("unit_calories"),
                        color = palette.primaryText
                    )
                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .width(1.dp)
                            .background(palette.cardBorder)
                    )
                    CalorieStatItem(
                        label = s("calories_consumed"),
                        value = n(consumedCalories),
                        unit = s("unit_calories"),
                        color = palette.accentTextOnLight
                    )
                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .width(1.dp)
                            .background(palette.cardBorder)
                    )
                    CalorieStatItem(
                        label = s("calories_burned"),
                        value = n(burnedCalories),
                        unit = s("unit_calories"),
                        color = Color(0xFFFF9500)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Smart & Utility Quick Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionButton(
                icon = Icons.Default.SoupKitchen,
                label = s("smart_kitchen_title"),
                isAccent = true,
                onClick = onSmartKitchenClick,
                modifier = Modifier.weight(1f),
                testTag = "btn_quick_smartkitchen"
            )
            QuickActionButton(
                icon = Icons.AutoMirrored.Filled.CompareArrows,
                label = s("food_comparison_title"),
                isAccent = false,
                onClick = onFoodComparisonClick,
                modifier = Modifier.weight(1f),
                testTag = "btn_quick_comparison"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionButton(
                icon = Icons.Default.CameraAlt,
                label = s("ai_food_scanner"),
                isAccent = false,
                onClick = onAIScanClick,
                modifier = Modifier.weight(1f),
                testTag = "btn_quick_aiscan"
            )
            QuickActionButton(
                icon = Icons.Default.SmartToy,
                label = s("ai_coach_title"),
                isAccent = false,
                onClick = onAICoachClick,
                modifier = Modifier.weight(1f),
                testTag = "btn_quick_aicoach"
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionButton(
                icon = Icons.Default.CalendarMonth,
                label = s("meal_planner_title"),
                isAccent = false,
                onClick = onMealPlannerClick,
                modifier = Modifier.weight(1f),
                testTag = "btn_quick_mealplanner"
            )
            QuickActionButton(
                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                label = s("activity_tracking"),
                isAccent = false,
                onClick = onActivityClick,
                modifier = Modifier.weight(1f),
                testTag = "btn_quick_activity"
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Macronutrients Breakdown Card
        CaloFitCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column {
                Text(
                    text = s("macros"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryText
                )

                Spacer(modifier = Modifier.height(14.dp))

                MacroProgressBar(
                    label = s("carbs"),
                    currentGrams = consumedCarbs,
                    targetGrams = targetCarbs,
                    color = CaloFitMacroCarbs,
                    unitLabel = s("unit_grams"),
                    isPersian = isPersian
                )

                Spacer(modifier = Modifier.height(10.dp))

                MacroProgressBar(
                    label = s("protein"),
                    currentGrams = consumedProtein,
                    targetGrams = targetProtein,
                    color = CaloFitMacroProtein,
                    unitLabel = s("unit_grams"),
                    isPersian = isPersian
                )

                Spacer(modifier = Modifier.height(10.dp))

                MacroProgressBar(
                    label = s("fat"),
                    currentGrams = consumedFat,
                    targetGrams = targetFat,
                    color = CaloFitMacroFat,
                    unitLabel = s("unit_grams"),
                    isPersian = isPersian
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Water Tracking Card
        CaloFitCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable(onClick = onWaterDetailClick)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(CaloFitWaterBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Opacity,
                            contentDescription = s("water_tracker"),
                            tint = CaloFitWaterBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = s("water_tracker"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val waterDisplay = if (isPersian) {
                            "${n(waterTotalMl)} از ${n(targetWater)} ${s("unit_ml")}"
                        } else {
                            "${n(waterTotalMl)} / ${n(targetWater)} ${s("unit_ml")}"
                        }
                        Text(
                            text = waterDisplay,
                            fontSize = 12.sp,
                            color = palette.secondaryText,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .testTag("btn_quick_water_250")
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CaloFitWaterBlue.copy(alpha = 0.2f))
                        .border(1.dp, CaloFitWaterBlue.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .clickable { onQuickWaterClick(250) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = s("add_water_quick"),
                        color = CaloFitWaterBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = s("meals_today"),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = palette.primaryText,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        val mealCategories = listOf(
            "BREAKFAST" to s("meal_breakfast"),
            "LUNCH" to s("meal_lunch"),
            "DINNER" to s("meal_dinner"),
            "SNACK" to s("meal_snack")
        )

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            mealCategories.forEach { (type, label) ->
                val logsForMeal = todayLogs.filter {
                    if (type == "SNACK") {
                        it.mealType == "SNACK" || it.mealType == "AFTERNOON_SNACK"
                    } else {
                        it.mealType == type
                    }
                }
                val mealKcal = logsForMeal.sumOf { it.calories }

                MealSummaryRow(
                    mealName = label,
                    calories = mealKcal,
                    itemsCount = logsForMeal.size,
                    kcalLabel = s("unit_calories"),
                    isPersian = isPersian,
                    onAddClick = { onAddFoodClick(type) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        val weight = latestWeight?.weightKg ?: userProfile?.currentWeightKg ?: 70.0
        val targetW = userProfile?.targetWeightKg ?: 68.0
        val height = userProfile?.heightCm ?: 175.0
        val bmi = NutritionCalculator.calculateBmi(weight, height)

        CaloFitCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable(onClick = onWeightClick)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(palette.primaryAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonitorWeight,
                            contentDescription = s("weight_current_target_title"),
                            tint = palette.accentTextOnLight,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = s("weight_current_target_title"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val weightFormatted = StringsProvider.formatDecimal(weight, isPersian)
                        val bmiFormatted = StringsProvider.formatDecimal(bmi, isPersian)
                        Text(
                            text = "$weightFormatted ${s("unit_kg")} | ${s("bmi_label")}: $bmiFormatted",
                            fontSize = 12.sp,
                            color = palette.secondaryText
                        )
                    }
                }

                Text(
                    text = "${s("target_weight")}: ${StringsProvider.formatDecimal(targetW, isPersian)} ${s("unit_kg")}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.secondaryText
                )
            }
        }
    }
}

@Composable
private fun CalorieStatItem(
    label: String,
    value: String,
    unit: String,
    color: Color
) {
    val palette = LocalCaloFitColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 11.sp, color = palette.secondaryText)
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = unit,
                fontSize = 10.sp,
                color = palette.secondaryText,
                modifier = Modifier.padding(bottom = 1.dp)
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    isAccent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    val palette = LocalCaloFitColors.current
    Box(
        modifier = modifier
            .testTag(testTag)
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isAccent) {
                    palette.primaryAccent.copy(alpha = if (palette.isDark) 0.22f else 0.30f)
                } else {
                    palette.cardSurface
                }
            )
            .border(
                1.dp,
                if (isAccent) palette.primaryAccent.copy(alpha = 0.5f) else palette.cardBorder,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isAccent) palette.accentTextOnLight else palette.primaryText,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MealSummaryRow(
    mealName: String,
    calories: Int,
    itemsCount: Int,
    kcalLabel: String,
    isPersian: Boolean,
    onAddClick: () -> Unit
) {
    val palette = LocalCaloFitColors.current

    CaloFitCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mealName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryText
                )
                Spacer(modifier = Modifier.height(2.dp))
                val summaryText = if (itemsCount > 0) {
                    val cStr = StringsProvider.formatNumber(calories, isPersian)
                    val iStr = StringsProvider.formatNumber(itemsCount, isPersian)
                    if (isPersian) "$cStr $kcalLabel ($iStr مورد)" else "$cStr $kcalLabel ($iStr items)"
                } else {
                    if (isPersian) "هنوز ثبت نشده است" else "Not logged yet"
                }
                Text(
                    text = summaryText,
                    fontSize = 12.sp,
                    color = if (itemsCount > 0) palette.accentTextOnLight else palette.secondaryText,
                    fontWeight = if (itemsCount > 0) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(palette.primaryAccent.copy(alpha = if (palette.isDark) 0.25f else 0.35f))
                    .clickable(onClick = onAddClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = StringsProvider.get("add_food", isPersian),
                    tint = palette.accentTextOnLight,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
