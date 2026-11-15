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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calofit.data.model.FoodItemEntity
import com.example.calofit.localization.StringsProvider
import com.example.calofit.ui.components.CaloFitButton
import com.example.calofit.ui.components.CaloFitCard
import com.example.calofit.ui.theme.CaloFitMacroCarbs
import com.example.calofit.ui.theme.CaloFitMacroFat
import com.example.calofit.ui.theme.CaloFitMacroProtein
import com.example.calofit.ui.theme.LocalCaloFitColors
import kotlin.math.roundToInt

@Composable
fun FoodDetailScreen(
    food: FoodItemEntity,
    initialMealType: String,
    isPersian: Boolean,
    onConfirmLog: (mealType: String, grams: Double) -> Unit,
    onBackClick: () -> Unit
) {
    val palette = LocalCaloFitColors.current
    fun s(key: String): String = StringsProvider.get(key, isPersian)
    fun n(v: Number): String = StringsProvider.formatNumber(v, isPersian)

    val normalizedInitialMeal = if (initialMealType == "AFTERNOON_SNACK") "SNACK" else initialMealType
    var currentMealType by remember { mutableStateOf(normalizedInitialMeal) }
    var grams by remember { mutableDoubleStateOf(food.servingSizeGrams.coerceAtLeast(10.0)) }

    val safeServing = food.servingSizeGrams.coerceAtLeast(1.0)
    val multiplier = grams / safeServing
    val calculatedKcal = (food.calories * multiplier).roundToInt()
    val calculatedProtein = ((food.protein * multiplier) * 10).roundToInt() / 10.0
    val calculatedCarbs = ((food.carbs * multiplier) * 10).roundToInt() / 10.0
    val calculatedFat = ((food.fat * multiplier) * 10).roundToInt() / 10.0

    val mealOptions = listOf(
        "BREAKFAST" to s("meal_breakfast"),
        "LUNCH" to s("meal_lunch"),
        "DINNER" to s("meal_dinner"),
        "SNACK" to s("meal_snack")
    )

    val householdUnits = if (isPersian) listOf(
        "۱ سهم استاندارد" to food.servingSizeGrams,
        "۱ کفگیر" to 150.0,
        "۱ ملاقه" to 180.0,
        "۱ کف دست نان" to 35.0,
        "۱ قاشق غذاخوری" to 15.0,
        "۱ لیوان" to 240.0
    ) else listOf(
        "1 Serving" to food.servingSizeGrams,
        "1 Spatula" to 150.0,
        "1 Ladle" to 180.0,
        "1 Palm bread" to 35.0,
        "1 Tablespoon" to 15.0,
        "1 Glass" to 240.0
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(palette.mainBackground, palette.secondaryBackground)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(palette.cardSurface)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = s("back"),
                    tint = palette.primaryText
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (isPersian) food.nameFa.ifBlank { food.nameEn } else food.nameEn.ifBlank { food.nameFa },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        CaloFitCard(
            modifier = Modifier.fillMaxWidth(),
            isAccent = true
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${s("portion_size")}: ${n(grams.toInt())} ${s("unit_grams")}",
                    fontSize = 14.sp,
                    color = palette.secondaryText,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = n(calculatedKcal),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.accentTextOnLight
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = s("unit_calories"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    MacroBox(
                        s("carbs"),
                        "${StringsProvider.formatDecimal(calculatedCarbs, isPersian)} ${s("unit_grams")}",
                        CaloFitMacroCarbs
                    )
                    MacroBox(
                        s("protein"),
                        "${StringsProvider.formatDecimal(calculatedProtein, isPersian)} ${s("unit_grams")}",
                        CaloFitMacroProtein
                    )
                    MacroBox(
                        s("fat"),
                        "${StringsProvider.formatDecimal(calculatedFat, isPersian)} ${s("unit_grams")}",
                        CaloFitMacroFat
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = s("household_unit"),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = palette.primaryText
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            householdUnits.take(3).forEach { (label, g) ->
                HouseholdPill(
                    label = label,
                    isSelected = grams.toInt() == g.toInt(),
                    onClick = { grams = g },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            householdUnits.drop(3).forEach { (label, g) ->
                HouseholdPill(
                    label = label,
                    isSelected = grams.toInt() == g.toInt(),
                    onClick = { grams = g },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        CaloFitCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = s("custom_grams"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryText
                    )
                    Text(
                        text = "${n(grams.toInt())} ${s("unit_grams")}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accentTextOnLight
                    )
                }

                Slider(
                    value = grams.toFloat(),
                    onValueChange = { grams = kotlin.math.round(it).toDouble() },
                    valueRange = 10f..600f,
                    colors = SliderDefaults.colors(
                        thumbColor = palette.primaryAccent,
                        activeTrackColor = palette.primaryAccent
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = s("select_meal"),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = palette.primaryText
        )
        Spacer(modifier = Modifier.height(6.dp))

        CaloFitCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                mealOptions.forEach { (type, label) ->
                    val isSelected = currentMealType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) palette.primaryAccent else palette.cardSurfaceSubtle)
                            .clickable { currentMealType = type }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) palette.onPrimaryAccent else palette.primaryText
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        CaloFitButton(
            text = s("add_to_meal"),
            icon = Icons.Default.Check,
            onClick = { onConfirmLog(currentMealType, grams) },
            modifier = Modifier.fillMaxWidth(),
            testTag = "btn_confirm_add_food"
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun HouseholdPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalCaloFitColors.current
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) palette.primaryAccent else palette.cardSurface)
            .border(1.dp, if (isSelected) palette.primaryAccent else palette.cardBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) palette.onPrimaryAccent else palette.primaryText,
            maxLines = 1
        )
    }
}

@Composable
private fun MacroBox(title: String, amount: String, color: Color) {
    val palette = LocalCaloFitColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, fontSize = 12.sp, color = palette.secondaryText, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = amount, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
