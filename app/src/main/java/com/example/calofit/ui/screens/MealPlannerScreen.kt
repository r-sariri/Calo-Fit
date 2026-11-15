package com.example.calofit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calofit.data.model.MealPlanItemEntity
import com.example.calofit.localization.StringsProvider
import com.example.calofit.ui.components.CaloFitCard
import com.example.calofit.ui.theme.CaloFitMacroCarbs
import com.example.calofit.ui.theme.CaloFitMacroFat
import com.example.calofit.ui.theme.CaloFitMacroProtein
import com.example.calofit.ui.theme.LocalCaloFitColors

@Composable
fun MealPlannerScreen(
    selectedDay: Int,
    mealPlans: List<MealPlanItemEntity>,
    isPersian: Boolean,
    onSelectDay: (Int) -> Unit,
    onBackClick: () -> Unit
) {
    val palette = LocalCaloFitColors.current
    fun s(key: String): String = StringsProvider.get(key, isPersian)
    fun n(v: Number): String = StringsProvider.formatNumber(v, isPersian)

    val daysOfWeek = if (isPersian) listOf(
        1 to "شنبه",
        2 to "یکشنبه",
        3 to "دوشنبه",
        4 to "سه‌شنبه",
        5 to "چهارشنبه",
        6 to "پنجشنبه",
        7 to "جمعه"
    ) else listOf(
        1 to "Mon",
        2 to "Tue",
        3 to "Wed",
        4 to "Thu",
        5 to "Fri",
        6 to "Sat",
        7 to "Sun"
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
                text = s("meal_planner_title"),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            daysOfWeek.forEach { (day, label) ->
                val isSelected = selectedDay == day
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_meal_day_$day")
                        .defaultMinSize(minHeight = 44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) palette.primaryAccent else palette.cardSurface)
                        .border(
                            1.dp,
                            if (isSelected) palette.primaryAccent else palette.cardBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectDay(day) }
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

        Spacer(modifier = Modifier.height(14.dp))

        val dailyCalSum = mealPlans.sumOf { it.calories }
        CaloFitCard(modifier = Modifier.fillMaxWidth(), isAccent = true) {
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(palette.primaryAccent.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = palette.accentTextOnLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isPersian) "مجموع برنامه روز انتخاب‌شده" else "Daily Plan Summary",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryText
                        )
                        Text(
                            text = "${n(dailyCalSum)} ${s("unit_calories")}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.accentTextOnLight
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(mealPlans, key = { it.id }) { plan ->
                val mealTitle = when (plan.mealType) {
                    "BREAKFAST" -> s("meal_breakfast")
                    "LUNCH" -> s("meal_lunch")
                    "DINNER" -> s("meal_dinner")
                    else -> s("meal_snack")
                }

                CaloFitCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            text = mealTitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accentTextOnLight
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isPersian) plan.titleFa else plan.titleEn,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.primaryText,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${n(plan.calories)} ${s("unit_calories")}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.accentTextOnLight
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (isPersian) plan.recipeSummaryFa else plan.recipeSummaryEn,
                            fontSize = 12.sp,
                            color = palette.secondaryText,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val unitG = s("unit_grams")
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "${s("carbs")}: ${n(plan.carbs.toInt())} $unitG",
                                fontSize = 11.sp,
                                color = CaloFitMacroCarbs
                            )
                            Text(
                                text = "${s("protein")}: ${n(plan.protein.toInt())} $unitG",
                                fontSize = 11.sp,
                                color = CaloFitMacroProtein
                            )
                            Text(
                                text = "${s("fat")}: ${n(plan.fat.toInt())} $unitG",
                                fontSize = 11.sp,
                                color = CaloFitMacroFat
                            )
                        }
                    }
                }
            }
        }
    }
}
