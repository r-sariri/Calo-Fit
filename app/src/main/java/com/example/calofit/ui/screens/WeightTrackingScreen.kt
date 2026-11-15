package com.example.calofit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calofit.data.model.UserProfileEntity
import com.example.calofit.data.model.WeightRecordEntity
import com.example.calofit.domain.calculator.NutritionCalculator
import com.example.calofit.domain.calendar.JalaliCalendarHelper
import com.example.calofit.localization.StringsProvider
import com.example.calofit.ui.components.CaloFitCard
import com.example.calofit.ui.components.CaloFitTextField
import com.example.calofit.ui.theme.CaloFitError
import com.example.calofit.ui.theme.CaloFitSuccess
import com.example.calofit.ui.theme.CaloFitWarning
import com.example.calofit.ui.theme.LocalCaloFitColors
import kotlin.math.abs

@Composable
fun WeightTrackingScreen(
    userProfile: UserProfileEntity?,
    weightRecords: List<WeightRecordEntity>,
    isPersian: Boolean,
    onSaveWeight: (weightKg: Double, note: String) -> Unit,
    onBackClick: () -> Unit
) {
    val palette = LocalCaloFitColors.current
    fun s(key: String): String = StringsProvider.get(key, isPersian)

    var showAddDialog by remember { mutableStateOf(false) }

    val currentWeight = weightRecords.lastOrNull()?.weightKg ?: userProfile?.currentWeightKg ?: 70.0
    val startingWeight = weightRecords.firstOrNull()?.weightKg ?: currentWeight
    val targetWeight = userProfile?.targetWeightKg ?: 68.0
    val height = userProfile?.heightCm ?: 175.0
    val bmi = NutritionCalculator.calculateBmi(currentWeight, height)
    val bmiCategory = NutritionCalculator.getBmiCategory(bmi)

    val bmiCategoryLabel = when (bmiCategory) {
        NutritionCalculator.BmiCategory.UNDERWEIGHT -> s("bmi_underweight")
        NutritionCalculator.BmiCategory.NORMAL -> s("bmi_normal")
        NutritionCalculator.BmiCategory.OVERWEIGHT -> s("bmi_overweight")
        NutritionCalculator.BmiCategory.OBESE -> s("bmi_obese")
    }

    val bmiColor = when (bmiCategory) {
        NutritionCalculator.BmiCategory.NORMAL -> CaloFitSuccess
        NutritionCalculator.BmiCategory.OVERWEIGHT -> CaloFitWarning
        else -> CaloFitError
    }

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
                text = s("weight_current_target_title"),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(palette.primaryAccent.copy(alpha = 0.25f))
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = s("log_new_weight"),
                    tint = palette.accentTextOnLight
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        CaloFitCard(modifier = Modifier.fillMaxWidth(), isAccent = true) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = s("current_weight"),
                        fontSize = 13.sp,
                        color = palette.secondaryText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = StringsProvider.formatDecimal(currentWeight, isPersian),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.primaryText
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = s("unit_kg"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.secondaryText,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    val sinceStart = currentWeight - startingWeight
                    if (weightRecords.size > 1) {
                        val sign = if (sinceStart > 0) "+" else ""
                        Text(
                            text = "${s("starting_weight")}: ${StringsProvider.formatDecimal(startingWeight, isPersian)} ($sign${StringsProvider.formatDecimal(sinceStart, isPersian)})",
                            fontSize = 11.sp,
                            color = palette.secondaryText
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = s("target_weight"),
                        fontSize = 13.sp,
                        color = palette.secondaryText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${StringsProvider.formatDecimal(targetWeight, isPersian)} ${s("unit_kg")}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accentTextOnLight
                    )
                    val diff = abs(currentWeight - targetWeight)
                    val diffStr = if (diff <= 0.3) {
                        s("weight_goal_reached")
                    } else {
                        "${StringsProvider.formatDecimal(diff, isPersian)} ${s("unit_kg")}"
                    }
                    Text(
                        text = diffStr,
                        fontSize = 12.sp,
                        color = palette.secondaryText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        CaloFitCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = s("bmi_label"),
                        fontSize = 13.sp,
                        color = palette.secondaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = StringsProvider.formatDecimal(bmi, isPersian),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.primaryText
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(bmiColor.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = bmiCategoryLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = bmiColor
                            )
                        }
                    }
                }

                Icon(
                    imageVector = Icons.Default.MonitorWeight,
                    contentDescription = null,
                    tint = palette.secondaryText.copy(alpha = 0.5f),
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = s("weight_history"),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = palette.primaryText
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(weightRecords.reversed(), key = { it.id }) { record ->
                CaloFitCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = JalaliCalendarHelper.formatIsoForDisplay(record.dateIso, isPersian),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.primaryText
                            )
                            if (record.note.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = record.note,
                                    fontSize = 11.sp,
                                    color = palette.secondaryText
                                )
                            }
                        }
                        Text(
                            text = "${StringsProvider.formatDecimal(record.weightKg, isPersian)} ${s("unit_kg")}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accentTextOnLight
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var inputWeight by remember { mutableStateOf(StringsProvider.formatDecimal(currentWeight, isPersian)) }
        var inputNote by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(text = s("log_new_weight"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = s("weight_input_label"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.primaryText
                    )
                    CaloFitTextField(
                        value = inputWeight,
                        onValueChange = {
                            inputWeight = it
                            errorMessage = null
                        },
                        placeholder = if (isPersian) "مثال: ۷۲.۵" else "e.g. 72.5",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        testTag = "input_weight_val"
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = s("note_input_label"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.secondaryText
                    )
                    CaloFitTextField(
                        value = inputNote,
                        onValueChange = { inputNote = it },
                        placeholder = if (isPersian) "مثال: وزن‌کشی صبحگاهی" else "e.g. Morning fasting weight",
                        testTag = "input_weight_note"
                    )
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage.orEmpty(),
                            fontSize = 12.sp,
                            color = CaloFitError,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val normalized = JalaliCalendarHelper.toAsciiDigits(inputWeight).replace('٫', '.').trim()
                        val w = normalized.toDoubleOrNull()
                        if (w == null || !NutritionCalculator.isValidWeightKg(w)) {
                            errorMessage = s("err_weight_invalid")
                        } else {
                            onSaveWeight(w, inputNote.trim())
                            showAddDialog = false
                        }
                    }
                ) {
                    Text(text = s("save"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(text = s("cancel"))
                }
            }
        )
    }
}
