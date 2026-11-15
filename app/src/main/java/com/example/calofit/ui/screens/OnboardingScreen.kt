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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calofit.domain.calculator.NutritionCalculator
import com.example.calofit.domain.calendar.JalaliCalendarHelper
import com.example.calofit.localization.StringsProvider
import com.example.calofit.ui.components.CaloFitButton
import com.example.calofit.ui.components.CaloFitCard
import com.example.calofit.ui.components.CaloFitTextField
import com.example.calofit.ui.theme.CaloFitError
import com.example.calofit.ui.theme.CaloFitMacroCarbs
import com.example.calofit.ui.theme.CaloFitMacroFat
import com.example.calofit.ui.theme.CaloFitMacroProtein
import com.example.calofit.ui.theme.LocalCaloFitColors

@Composable
fun OnboardingScreen(
    isPersian: Boolean,
    onComplete: (
        name: String,
        age: Int,
        gender: NutritionCalculator.Gender,
        heightCm: Double,
        weightKg: Double,
        targetWeightKg: Double,
        activityLevel: NutritionCalculator.ActivityLevel,
        goal: NutritionCalculator.WeightGoal
    ) -> Unit
) {
    val palette = LocalCaloFitColors.current
    var currentStep by remember { mutableIntStateOf(1) }

    var name by remember { mutableStateOf("") }
    var ageInput by remember { mutableStateOf(if (isPersian) "۲۶" else "26") }
    var step1Error by remember { mutableStateOf<String?>(null) }
    var gender by remember { mutableStateOf(NutritionCalculator.Gender.MALE) }
    var heightCm by remember { mutableDoubleStateOf(175.0) }
    var weightKg by remember { mutableDoubleStateOf(74.0) }
    var targetWeightKg by remember { mutableDoubleStateOf(68.0) }
    var activityLevel by remember { mutableStateOf(NutritionCalculator.ActivityLevel.MODERATELY_ACTIVE) }
    var goal by remember { mutableStateOf(NutritionCalculator.WeightGoal.LOSE_WEIGHT) }

    fun s(key: String): String = StringsProvider.get(key, isPersian)
    fun n(value: Number): String = StringsProvider.formatNumber(value, isPersian)

    val parsedAge = JalaliCalendarHelper.toAsciiDigits(ageInput).trim().toIntOrNull()

    val previewPlan by remember(weightKg, heightCm, parsedAge, gender, activityLevel, goal) {
        derivedStateOf {
            NutritionCalculator.calculateFullPlan(
                weightKg = weightKg,
                heightCm = heightCm,
                age = (parsedAge ?: 26).coerceIn(12, 100),
                gender = gender,
                activityLevel = activityLevel,
                goal = goal
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(palette.mainBackground, palette.secondaryBackground)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentStep > 1) {
                    IconButton(
                        onClick = { currentStep-- },
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
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (step in 1..4) {
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (step == currentStep) 26.dp else 10.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    when {
                                        step == currentStep -> palette.primaryAccent
                                        step < currentStep -> palette.primaryAccent.copy(alpha = 0.55f)
                                        else -> palette.cardBorder
                                    }
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.size(48.dp))
            }

            if (currentStep == 1) {
                Text(
                    text = s("onboarding_title_1"),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryText
                )
                Spacer(modifier = Modifier.height(18.dp))

                CaloFitCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            text = s("name_label"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.secondaryText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        CaloFitTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                step1Error = null
                            },
                            placeholder = s("name_placeholder"),
                            testTag = "input_onboarding_name"
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = s("age_label"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.secondaryText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        CaloFitTextField(
                            value = ageInput,
                            onValueChange = {
                                ageInput = it
                                step1Error = null
                            },
                            placeholder = s("age_placeholder"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = step1Error != null && (parsedAge == null || !NutritionCalculator.isValidAge(parsedAge)),
                            testTag = "input_onboarding_age"
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = s("gender_label"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.secondaryText
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            GenderSelectCard(
                                title = s("gender_male"),
                                isSelected = gender == NutritionCalculator.Gender.MALE,
                                onClick = { gender = NutritionCalculator.Gender.MALE },
                                modifier = Modifier.weight(1f)
                            )
                            GenderSelectCard(
                                title = s("gender_female"),
                                isSelected = gender == NutritionCalculator.Gender.FEMALE,
                                onClick = { gender = NutritionCalculator.Gender.FEMALE },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (step1Error != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = step1Error.orEmpty(),
                                fontSize = 12.sp,
                                color = CaloFitError,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                CaloFitButton(
                    text = s("continue_btn"),
                    onClick = {
                        val ageVal = parsedAge
                        when {
                            ageVal == null || !NutritionCalculator.isValidAge(ageVal) -> {
                                step1Error = s("err_age_invalid")
                            }
                            else -> {
                                step1Error = null
                                currentStep = 2
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "btn_step1_next"
                )
            }

            if (currentStep == 2) {
                Text(
                    text = s("weight_current_target_title"),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryText
                )
                Spacer(modifier = Modifier.height(18.dp))

                CaloFitCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = s("height_label"),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.primaryText
                            )
                            Text(
                                text = "${n(heightCm.toInt())} ${s("unit_cm")}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.accentTextOnLight
                            )
                        }
                        Slider(
                            value = heightCm.toFloat(),
                            onValueChange = { heightCm = kotlin.math.round(it).toDouble() },
                            valueRange = 130f..220f,
                            colors = SliderDefaults.colors(
                                thumbColor = palette.primaryAccent,
                                activeTrackColor = palette.primaryAccent
                            )
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = s("current_weight"),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.primaryText
                            )
                            Text(
                                text = "${n(weightKg.toInt())} ${s("unit_kg")}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.accentTextOnLight
                            )
                        }
                        Slider(
                            value = weightKg.toFloat(),
                            onValueChange = { weightKg = kotlin.math.round(it).toDouble() },
                            valueRange = 35f..180f,
                            colors = SliderDefaults.colors(
                                thumbColor = palette.primaryAccent,
                                activeTrackColor = palette.primaryAccent
                            )
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = s("target_weight"),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.primaryText
                            )
                            Text(
                                text = "${n(targetWeightKg.toInt())} ${s("unit_kg")}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.secondaryAccent
                            )
                        }
                        Slider(
                            value = targetWeightKg.toFloat(),
                            onValueChange = { targetWeightKg = kotlin.math.round(it).toDouble() },
                            valueRange = 35f..180f,
                            colors = SliderDefaults.colors(
                                thumbColor = palette.secondaryAccent,
                                activeTrackColor = palette.secondaryAccent
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                CaloFitButton(
                    text = s("continue_btn"),
                    onClick = { currentStep = 3 },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "btn_step2_next"
                )
            }

            if (currentStep == 3) {
                Text(
                    text = s("onboarding_title_2"),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryText
                )
                Spacer(modifier = Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val activityOptions = listOf(
                        Triple(NutritionCalculator.ActivityLevel.SEDENTARY, s("act_sedentary"), "1.2"),
                        Triple(NutritionCalculator.ActivityLevel.LIGHTLY_ACTIVE, s("act_light"), "1.375"),
                        Triple(NutritionCalculator.ActivityLevel.MODERATELY_ACTIVE, s("act_moderate"), "1.55"),
                        Triple(NutritionCalculator.ActivityLevel.VERY_ACTIVE, s("act_heavy"), "1.725"),
                        Triple(NutritionCalculator.ActivityLevel.EXTRA_ACTIVE, s("act_extra_active"), "1.9")
                    )
                    activityOptions.forEach { (level, title, factor) ->
                        val factorText = if (isPersian) {
                            "ضریب فعالیت ${StringsProvider.localizeDigits(factor, true)}"
                        } else {
                            "Activity factor $factor"
                        }
                        ActivityLevelCard(
                            title = title,
                            subtitle = factorText,
                            isSelected = activityLevel == level,
                            onClick = { activityLevel = level }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                CaloFitButton(
                    text = s("continue_btn"),
                    onClick = { currentStep = 4 },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "btn_step3_next"
                )
            }

            if (currentStep == 4) {
                Text(
                    text = s("onboarding_title_4"),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryText
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GoalChip(
                        title = s("goal_lose"),
                        isSelected = goal == NutritionCalculator.WeightGoal.LOSE_WEIGHT,
                        onClick = { goal = NutritionCalculator.WeightGoal.LOSE_WEIGHT },
                        modifier = Modifier.weight(1f)
                    )
                    GoalChip(
                        title = s("goal_maintain"),
                        isSelected = goal == NutritionCalculator.WeightGoal.MAINTAIN_WEIGHT,
                        onClick = { goal = NutritionCalculator.WeightGoal.MAINTAIN_WEIGHT },
                        modifier = Modifier.weight(1f)
                    )
                    GoalChip(
                        title = s("goal_gain"),
                        isSelected = goal == NutritionCalculator.WeightGoal.GAIN_WEIGHT,
                        onClick = { goal = NutritionCalculator.WeightGoal.GAIN_WEIGHT },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                CaloFitCard(
                    modifier = Modifier.fillMaxWidth(),
                    isAccent = true
                ) {
                    Column {
                        Text(
                            text = "${s("calculated_tdee")}: ${n(previewPlan.tdee)} ${s("unit_calories")}",
                            fontSize = 13.sp,
                            color = palette.secondaryText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = n(previewPlan.targetCalories),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.accentTextOnLight
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${s("unit_calories")} / ${s("daily_target")}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.primaryText,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MacroPreviewPill(s("carbs"), "${n(previewPlan.carbsGrams)} ${s("unit_grams")}", CaloFitMacroCarbs)
                            MacroPreviewPill(s("protein"), "${n(previewPlan.proteinGrams)} ${s("unit_grams")}", CaloFitMacroProtein)
                            MacroPreviewPill(s("fat"), "${n(previewPlan.fatGrams)} ${s("unit_grams")}", CaloFitMacroFat)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "${s("water_goal")}: ${n(previewPlan.waterMl)} ${s("unit_ml")} | ${s("bmi_label")}: ${StringsProvider.formatDecimal(previewPlan.bmi, isPersian)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.secondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                CaloFitButton(
                    text = s("start_journey"),
                    onClick = {
                        val safeAge = (parsedAge ?: 26).coerceIn(12, 100)
                        onComplete(
                            if (name.isBlank()) (if (isPersian) "کاربر کالوفیت" else "KaloFit User") else name.trim(),
                            safeAge,
                            gender,
                            heightCm,
                            weightKg,
                            targetWeightKg,
                            activityLevel,
                            goal
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "btn_complete_onboarding"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GenderSelectCard(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalCaloFitColors.current
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) palette.primaryAccent.copy(alpha = 0.25f) else palette.cardSurfaceSubtle)
            .border(
                if (isSelected) 2.dp else 1.dp,
                if (isSelected) palette.primaryAccent else palette.cardBorder,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) palette.accentTextOnLight else palette.primaryText
        )
    }
}

@Composable
private fun ActivityLevelCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalCaloFitColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (isSelected) palette.primaryAccent.copy(alpha = 0.2f) else palette.cardSurface)
            .border(
                if (isSelected) 1.8.dp else 1.dp,
                if (isSelected) palette.primaryAccent else palette.cardBorder,
                RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = palette.primaryText)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, fontSize = 12.sp, color = palette.secondaryText)
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(palette.primaryAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = palette.onPrimaryAccent,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalCaloFitColors.current
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) palette.primaryAccent else palette.cardSurface)
            .border(1.dp, if (isSelected) palette.primaryAccent else palette.cardBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) palette.onPrimaryAccent else palette.primaryText
        )
    }
}

@Composable
private fun MacroPreviewPill(title: String, amount: String, color: Color) {
    val palette = LocalCaloFitColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(palette.cardSurface)
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, fontSize = 11.sp, color = palette.secondaryText)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = amount, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
