package com.example.calofit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
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
import com.example.calofit.data.model.FoodLogEntryEntity
import com.example.calofit.domain.calendar.JalaliCalendarHelper
import com.example.calofit.localization.StringsProvider
import com.example.calofit.ui.components.CaloFitCard
import com.example.calofit.ui.components.CaloFitTextField
import com.example.calofit.ui.theme.CaloFitError
import com.example.calofit.ui.theme.CaloFitMacroCarbs
import com.example.calofit.ui.theme.CaloFitMacroFat
import com.example.calofit.ui.theme.CaloFitMacroProtein
import com.example.calofit.ui.theme.LocalCaloFitColors

@Composable
fun FoodLogScreen(
    todayLogs: List<FoodLogEntryEntity>,
    totalCalories: Int,
    targetCalories: Int,
    isPersian: Boolean,
    onAddFoodClick: (mealType: String) -> Unit,
    onUpdateLogQuantity: (entry: FoodLogEntryEntity, newGrams: Double) -> Unit = { _, _ -> },
    onDeleteLog: (id: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalCaloFitColors.current
    fun s(key: String): String = StringsProvider.get(key, isPersian)
    fun n(v: Number): String = StringsProvider.formatNumber(v, isPersian)

    var editingEntry by remember { mutableStateOf<FoodLogEntryEntity?>(null) }

    val mealTypes = listOf(
        "BREAKFAST" to s("meal_breakfast"),
        "MORNING_SNACK" to s("meal_morning_snack"),
        "LUNCH" to s("meal_lunch"),
        "SNACK" to s("meal_snack"),
        "DINNER" to s("meal_dinner"),
        "EVENING_SNACK" to s("meal_evening_snack")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(palette.mainBackground, palette.secondaryBackground)
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        CaloFitCard(modifier = Modifier.fillMaxWidth(), isAccent = true) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = JalaliCalendarHelper.getFormattedDate(isPersian),
                        fontSize = 13.sp,
                        color = palette.secondaryText,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = s("nav_food_log"),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryText
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    val summaryFraction = if (isPersian) {
                        "${n(totalCalories)} از ${n(targetCalories)}"
                    } else {
                        "${n(totalCalories)} / ${n(targetCalories)}"
                    }
                    Text(
                        text = summaryFraction,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.accentTextOnLight
                    )
                    Text(
                        text = s("unit_calories"),
                        fontSize = 11.sp,
                        color = palette.secondaryText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 20.dp)
        ) {
            items(mealTypes) { (type, title) ->
                val logsForMeal = todayLogs.filter {
                    if (type == "SNACK") {
                        it.mealType == "SNACK" || it.mealType == "AFTERNOON_SNACK"
                    } else {
                        it.mealType == type
                    }
                }
                val mealKcal = logsForMeal.sumOf { it.calories }

                CaloFitCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primaryText
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${n(mealKcal)} ${s("unit_calories")}",
                                    fontSize = 12.sp,
                                    color = palette.secondaryText,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(palette.primaryAccent.copy(alpha = if (palette.isDark) 0.25f else 0.35f))
                                    .clickable { onAddFoodClick(type) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = s("add_food"),
                                    tint = palette.accentTextOnLight,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (logsForMeal.isEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isPersian) "غذایی در این وعده ثبت نشده است" else "No food logged yet",
                                fontSize = 12.sp,
                                color = palette.secondaryText.copy(alpha = 0.7f)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(10.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                logsForMeal.forEach { entry ->
                                    FoodEntryRow(
                                        entry = entry,
                                        isPersian = isPersian,
                                        onEdit = { editingEntry = entry },
                                        onDelete = { onDeleteLog(entry.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingEntry?.let { entry ->
        var gramsStr by remember(entry) { mutableStateOf(entry.grams.toInt().toString()) }
        var errorText by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { editingEntry = null },
            title = { Text(text = s("edit_quantity"), fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = entry.localizedName(isPersian),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.primaryText
                    )
                    Text(
                        text = s("custom_grams"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.secondaryText
                    )
                    CaloFitTextField(
                        value = gramsStr,
                        onValueChange = { gramsStr = it; errorText = null },
                        placeholder = s("serving_amount_placeholder"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        testTag = "input_edit_log_grams"
                    )
                    if (errorText != null) {
                        Text(text = errorText.orEmpty(), fontSize = 12.sp, color = CaloFitError)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsed = JalaliCalendarHelper.toAsciiDigits(gramsStr).trim().toDoubleOrNull()
                        if (parsed == null || parsed <= 0.0 || parsed > 10000.0) {
                            errorText = s("err_serving_invalid")
                        } else {
                            onUpdateLogQuantity(entry, parsed)
                            editingEntry = null
                        }
                    }
                ) {
                    Text(text = s("save"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingEntry = null }) {
                    Text(text = s("cancel"))
                }
            }
        )
    }
}

@Composable
private fun FoodEntryRow(
    entry: FoodLogEntryEntity,
    isPersian: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = LocalCaloFitColors.current
    fun s(key: String): String = StringsProvider.get(key, isPersian)
    fun n(v: Number): String = StringsProvider.formatNumber(v, isPersian)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.cardSurfaceSubtle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.localizedName(isPersian),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.primaryText
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${n(entry.grams.toInt())} ${s("unit_grams")}",
                    fontSize = 10.sp,
                    color = palette.secondaryText
                )
                Text(
                    text = "${s("carbs")}: ${n(entry.carbs.toInt())} ${s("unit_grams")}",
                    fontSize = 10.sp,
                    color = CaloFitMacroCarbs
                )
                Text(
                    text = "${s("protein")}: ${n(entry.protein.toInt())} ${s("unit_grams")}",
                    fontSize = 10.sp,
                    color = CaloFitMacroProtein
                )
                Text(
                    text = "${s("fat")}: ${n(entry.fat.toInt())} ${s("unit_grams")}",
                    fontSize = 10.sp,
                    color = CaloFitMacroFat
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${n(entry.calories)} ${s("unit_calories")}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText
            )
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = s("edit"),
                    tint = palette.accentTextOnLight,
                    modifier = Modifier.size(17.dp)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = s("delete"),
                    tint = palette.secondaryText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
