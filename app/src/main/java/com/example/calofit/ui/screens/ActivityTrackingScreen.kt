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
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calofit.data.model.ActivityRecordEntity
import com.example.calofit.domain.calculator.NutritionCalculator
import com.example.calofit.localization.StringsProvider
import com.example.calofit.ui.components.CaloFitButton
import com.example.calofit.ui.components.CaloFitCard
import com.example.calofit.ui.theme.LocalCaloFitColors

@Composable
fun ActivityTrackingScreen(
    userWeightKg: Double,
    todayActivities: List<ActivityRecordEntity>,
    totalBurnedKcal: Int,
    isPersian: Boolean,
    onSaveActivity: (activityType: String, durationMinutes: Int) -> Unit,
    onBackClick: () -> Unit
) {
    val palette = LocalCaloFitColors.current
    fun s(key: String): String = StringsProvider.get(key, isPersian)
    fun n(v: Number): String = StringsProvider.formatNumber(v, isPersian)

    val activities = listOf(
        Triple("Walking", s("act_walking"), Icons.AutoMirrored.Filled.DirectionsWalk),
        Triple("Running", s("act_running"), Icons.AutoMirrored.Filled.DirectionsRun),
        Triple("Cycling", s("act_cycling"), Icons.AutoMirrored.Filled.DirectionsBike),
        Triple("Gym", s("act_gym"), Icons.Default.FitnessCenter),
        Triple("Swimming", s("act_swimming"), Icons.Default.Pool),
        Triple("Yoga", s("act_yoga"), Icons.Default.SelfImprovement),
        Triple("Home Workout", s("act_home_workout"), Icons.Default.Home)
    )

    var selectedActivityKey by remember { mutableStateOf(activities[0].first) }
    var durationMinutes by remember { mutableFloatStateOf(30f) }

    val selectedActivity = activities.find { it.first == selectedActivityKey } ?: activities[0]
    val estimatedBurned = NutritionCalculator.calculateBurnedCalories(
        selectedActivity.first,
        durationMinutes.toInt(),
        userWeightKg
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(palette.mainBackground, palette.secondaryBackground)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
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
                    text = s("activity_tracking"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryText
                )
            }
        }

        item {
            CaloFitCard(modifier = Modifier.fillMaxWidth(), isAccent = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = s("calories_burned_est"),
                            fontSize = 13.sp,
                            color = palette.secondaryText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = n(estimatedBurned),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFF9500)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = s("unit_calories"),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.secondaryText,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isPersian) "مجموع امروز" else "Today's Total",
                            fontSize = 12.sp,
                            color = palette.secondaryText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${n(totalBurnedKcal)} ${s("unit_calories")}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryText
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = s("activity_type_label"),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                activities.take(4).forEach { act ->
                    ActivitySelectButton(
                        act = act,
                        isSelected = selectedActivityKey == act.first,
                        onClick = { selectedActivityKey = act.first },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                activities.drop(4).forEach { act ->
                    ActivitySelectButton(
                        act = act,
                        isSelected = selectedActivityKey == act.first,
                        onClick = { selectedActivityKey = act.first },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            CaloFitCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = s("duration_label"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryText
                        )
                        Text(
                            text = "${n(durationMinutes.toInt())} ${s("unit_minutes")}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9500)
                        )
                    }

                    Slider(
                        value = durationMinutes,
                        onValueChange = { durationMinutes = kotlin.math.round(it) },
                        valueRange = 5f..180f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFF9500),
                            activeTrackColor = Color(0xFFFF9500)
                        )
                    )
                }
            }
        }

        item {
            CaloFitButton(
                text = s("log_activity"),
                onClick = { onSaveActivity(selectedActivity.first, durationMinutes.toInt()) },
                modifier = Modifier.fillMaxWidth(),
                testTag = "btn_save_activity"
            )
        }

        if (todayActivities.isNotEmpty()) {
            items(todayActivities, key = { it.id }) { item ->
                val localizedTitle = when (item.activityType) {
                    "Walking" -> s("act_walking")
                    "Running" -> s("act_running")
                    "Cycling" -> s("act_cycling")
                    "Gym" -> s("act_gym")
                    "Swimming" -> s("act_swimming")
                    "Yoga" -> s("act_yoga")
                    "Home Workout" -> s("act_home_workout")
                    else -> item.activityType
                }
                CaloFitCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$localizedTitle (${n(item.durationMinutes)} ${s("unit_minutes")})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.primaryText
                        )
                        Text(
                            text = "${n(item.burnedCalories)} ${s("unit_calories")}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9500)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivitySelectButton(
    act: Triple<String, String, ImageVector>,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalCaloFitColors.current
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) Color(0xFFFF9500).copy(alpha = 0.22f) else palette.cardSurface)
            .border(
                1.dp,
                if (isSelected) Color(0xFFFF9500) else palette.cardBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = act.third,
                contentDescription = null,
                tint = if (isSelected) Color(0xFFFF9500) else palette.secondaryText,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = act.second,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = palette.primaryText,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}
