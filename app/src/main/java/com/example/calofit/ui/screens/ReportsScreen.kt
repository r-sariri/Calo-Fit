package com.example.calofit.ui.screens

import android.content.Intent
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calofit.data.model.FoodLogEntryEntity
import com.example.calofit.data.model.UserProfileEntity
import com.example.calofit.data.model.WaterRecordEntity
import com.example.calofit.data.model.WeightRecordEntity
import com.example.calofit.domain.calendar.JalaliCalendarHelper
import com.example.calofit.localization.StringsProvider
import com.example.calofit.ui.components.CaloFitButton
import com.example.calofit.ui.components.CaloFitCard
import com.example.calofit.ui.components.CaloFitTextField
import com.example.calofit.ui.theme.CaloFitError
import com.example.calofit.ui.theme.CaloFitMacroCarbs
import com.example.calofit.ui.theme.CaloFitMacroFat
import com.example.calofit.ui.theme.CaloFitMacroProtein
import com.example.calofit.ui.theme.CaloFitSuccess
import com.example.calofit.ui.theme.CaloFitWaterBlue
import com.example.calofit.ui.theme.LocalCaloFitColors
import kotlin.math.roundToInt

@Composable
fun ReportsScreen(
    userProfile: UserProfileEntity?,
    selectedDateIso: String,
    selectedDateLogs: List<FoodLogEntryEntity>,
    selectedDateWaterMl: Int,
    selectedDateBurnedKcal: Int,
    allFoodLogs: List<FoodLogEntryEntity>,
    allWaterRecords: List<WaterRecordEntity>,
    weightRecords: List<WeightRecordEntity>,
    isPersian: Boolean,
    onSelectDate: (String) -> Unit,
    onShiftDate: (Int) -> Unit,
    onBackClick: () -> Unit,
    onUpdateLogQuantity: (entry: FoodLogEntryEntity, newGrams: Double) -> Unit = { _, _ -> },
    onDeleteLog: (id: Long) -> Unit = {},
    isEmbeddedInTab: Boolean = false
) {
    val palette = LocalCaloFitColors.current
    val context = LocalContext.current
    fun s(key: String): String = StringsProvider.get(key, isPersian)
    fun n(v: Number): String = StringsProvider.formatNumber(v, isPersian)

    var chartRangeDays by remember { mutableIntStateOf(7) }
    var editingEntry by remember { mutableStateOf<FoodLogEntryEntity?>(null) }
    val todayIso = JalaliCalendarHelper.getTodayIsoDate()
    val targetKcal = userProfile?.targetCalories ?: 2000
    val targetWater = userProfile?.waterMl ?: 2500

    val dayCalories = selectedDateLogs.sumOf { it.calories }
    val dayProtein = selectedDateLogs.sumOf { it.protein }
    val dayCarbs = selectedDateLogs.sumOf { it.carbs }
    val dayFat = selectedDateLogs.sumOf { it.fat }
    val dayRemaining = (targetKcal - dayCalories + selectedDateBurnedKcal).coerceAtLeast(0)

    val rangeDates = remember(chartRangeDays, todayIso) {
        JalaliCalendarHelper.getLastNDaysIso(chartRangeDays, todayIso)
    }
    val logsByDate = remember(allFoodLogs) { allFoodLogs.groupBy { it.dateIso } }
    val waterByDate = remember(allWaterRecords) {
        allWaterRecords.groupBy { it.dateIso }.mapValues { (_, list) -> list.sumOf { it.amountMl } }
    }

    val rangeCaloriePoints = remember(rangeDates, logsByDate) {
        rangeDates.map { date -> date to (logsByDate[date]?.sumOf { it.calories } ?: 0) }
    }
    val loggedDaysInRange = rangeCaloriePoints.filter { it.second > 0 }
    val avgCalories = if (loggedDaysInRange.isNotEmpty()) {
        (loggedDaysInRange.sumOf { it.second }.toDouble() / loggedDaysInRange.size).roundToInt()
    } else 0

    val rangeLogs = remember(rangeDates, logsByDate) {
        rangeDates.flatMap { logsByDate[it].orEmpty() }
    }
    val sumProtein = rangeLogs.sumOf { it.protein }
    val sumCarbs = rangeLogs.sumOf { it.carbs }
    val sumFat = rangeLogs.sumOf { it.fat }
    val macroTotal = (sumProtein + sumCarbs + sumFat).coerceAtLeast(0.0)
    val proteinPct = if (macroTotal > 0) ((sumProtein / macroTotal) * 100).roundToInt() else 0
    val carbsPct = if (macroTotal > 0) ((sumCarbs / macroTotal) * 100).roundToInt() else 0
    val fatPct = if (macroTotal > 0) (100 - proteinPct - carbsPct).coerceAtLeast(0) else 0

    val rangeMacroPoints = remember(rangeDates, logsByDate) {
        rangeDates.map { date ->
            val dayLogs = logsByDate[date].orEmpty()
            Triple(
                dayLogs.sumOf { it.protein }.toFloat(),
                dayLogs.sumOf { it.carbs }.toFloat(),
                dayLogs.sumOf { it.fat }.toFloat()
            )
        }
    }

    val waterDaysWithData = rangeDates.mapNotNull { d -> waterByDate[d]?.takeIf { it > 0 } }
    val avgWater = if (waterDaysWithData.isNotEmpty()) {
        (waterDaysWithData.sum().toDouble() / waterDaysWithData.size).roundToInt()
    } else 0

    val containerMod = if (isEmbeddedInTab) {
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(palette.mainBackground, palette.secondaryBackground)))
            .statusBarsPadding()
    } else {
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(palette.mainBackground, palette.secondaryBackground)))
            .statusBarsPadding()
            .navigationBarsPadding()
    }

    LazyColumn(
        modifier = containerMod,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isEmbeddedInTab) {
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
                }
                Text(
                    text = s("reports_title"),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryText
                )
            }
        }

        // Daily History Date Navigation Card
        item {
            CaloFitCard(modifier = Modifier.fillMaxWidth(), isAccent = true) {
                Column {
                    Text(
                        text = s("daily_history_title"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onShiftDate(-1) },
                            modifier = Modifier
                                .testTag("btn_history_prev_day")
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(palette.cardSurface)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = if (isPersian) "روز قبل" else "Previous day",
                                tint = palette.primaryText
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { onSelectDate(todayIso) }
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = JalaliCalendarHelper.formatIsoForDisplay(selectedDateIso, isPersian),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.primaryText
                            )
                            if (selectedDateIso != todayIso) {
                                Text(
                                    text = if (isPersian) "بازگشت به امروز" else "Tap for Today",
                                    fontSize = 10.sp,
                                    color = palette.accentTextOnLight
                                )
                            }
                        }

                        IconButton(
                            onClick = { onShiftDate(1) },
                            enabled = selectedDateIso < todayIso,
                            modifier = Modifier
                                .testTag("btn_history_next_day")
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(palette.cardSurface.copy(alpha = if (selectedDateIso < todayIso) 1f else 0.4f))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = if (isPersian) "روز بعد" else "Next day",
                                tint = if (selectedDateIso < todayIso) palette.primaryText else palette.secondaryText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        ReportMetricPill(s("calories_consumed"), "${n(dayCalories)} ${s("unit_calories")}", palette.accentTextOnLight)
                        ReportMetricPill(s("calories_remaining"), "${n(dayRemaining)} ${s("unit_calories")}", palette.primaryText)
                        ReportMetricPill(s("water_tracker"), "${n(selectedDateWaterMl)} ${s("unit_ml")}", CaloFitWaterBlue)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        val unitG = s("unit_grams")
                        Text(
                            text = "${s("carbs")}: ${StringsProvider.formatDecimal(dayCarbs, isPersian)} $unitG",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CaloFitMacroCarbs
                        )
                        Text(
                            text = "${s("protein")}: ${StringsProvider.formatDecimal(dayProtein, isPersian)} $unitG",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CaloFitMacroProtein
                        )
                        Text(
                            text = "${s("fat")}: ${StringsProvider.formatDecimal(dayFat, isPersian)} $unitG",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CaloFitMacroFat
                        )
                    }
                }
            }
        }

        // Meals recorded on selected date
        item {
            CaloFitCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    if (selectedDateLogs.isEmpty()) {
                        Text(
                            text = s("no_logs_for_day"),
                            fontSize = 13.sp,
                            color = palette.secondaryText
                        )
                    } else {
                        val mealBuckets = listOf(
                            "BREAKFAST" to s("meal_breakfast"),
                            "MORNING_SNACK" to s("meal_morning_snack"),
                            "LUNCH" to s("meal_lunch"),
                            "SNACK" to s("meal_snack"),
                            "DINNER" to s("meal_dinner"),
                            "EVENING_SNACK" to s("meal_evening_snack")
                        )
                        mealBuckets.forEach { (type, title) ->
                            val entries = selectedDateLogs.filter {
                                if (type == "SNACK") it.mealType == "SNACK" || it.mealType == "AFTERNOON_SNACK"
                                else it.mealType == type
                            }
                            if (entries.isNotEmpty()) {
                                Text(
                                    text = "$title (${n(entries.sumOf { it.calories })} ${s("unit_calories")})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.accentTextOnLight
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                entries.forEach { entry ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${entry.localizedName(isPersian)} (${n(entry.grams.toInt())} ${s("unit_grams")})",
                                            fontSize = 12.sp,
                                            color = palette.primaryText,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "${n(entry.calories)} ${s("unit_calories")}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = palette.secondaryText
                                        )
                                        IconButton(
                                            onClick = { editingEntry = entry },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("btn_history_edit_log_${entry.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = s("edit"),
                                                tint = palette.accentTextOnLight,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onDeleteLog(entry.id) },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("btn_history_delete_log_${entry.id}")
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
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }

        // 7-Day / 30-Day Calorie Trend Bar Chart
        item {
            CaloFitCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (chartRangeDays == 7) s("weekly_trend_title") else s("monthly_trend_title"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryText
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            RangeChip(
                                label = if (isPersian) "۷ روز" else "7D",
                                selected = chartRangeDays == 7,
                                onClick = { chartRangeDays = 7 }
                            )
                            RangeChip(
                                label = if (isPersian) "۳۰ روز" else "30D",
                                selected = chartRangeDays == 30,
                                onClick = { chartRangeDays = 30 }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${s("weekly_average")}: ${n(avgCalories)} ${s("unit_calories")} / ${s("daily_target")}: ${n(targetKcal)}",
                        fontSize = 12.sp,
                        color = palette.secondaryText
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    CalorieBarChart(
                        points = rangeCaloriePoints,
                        targetCalories = targetKcal,
                        barColor = palette.primaryAccent,
                        targetLineColor = Color(0xFFFF9500),
                        trackColor = palette.cardSurfaceSubtle
                    )
                }
            }
        }

        // Macro Distribution & Water Avg over selected range
        item {
            CaloFitCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = s("macros"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryText
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        MacroReportItem(s("carbs"), "${n(carbsPct)}%", CaloFitMacroCarbs)
                        MacroReportItem(s("protein"), "${n(proteinPct)}%", CaloFitMacroProtein)
                        MacroReportItem(s("fat"), "${n(fatPct)}%", CaloFitMacroFat)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    val waterPct = if (targetWater > 0) ((avgWater.toFloat() / targetWater) * 100).roundToInt() else 0
                    Text(
                        text = "${s("water_trend_title")}: ${n(avgWater)} ${s("unit_ml")} (${n(waterPct)}% ${s("water_goal")})",
                        fontSize = 12.sp,
                        color = palette.secondaryText
                    )
                }
            }
        }

        // Macronutrient Trends (Protein, Carbs, Fat) grouped bar chart
        item {
            CaloFitCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = s("macro_trends_title"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MacroLegendDot(label = s("protein"), color = CaloFitMacroProtein)
                        MacroLegendDot(label = s("carbs"), color = CaloFitMacroCarbs)
                        MacroLegendDot(label = s("fat"), color = CaloFitMacroFat)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    MacroTrendBarChart(
                        points = rangeMacroPoints,
                        proteinColor = CaloFitMacroProtein,
                        carbsColor = CaloFitMacroCarbs,
                        fatColor = CaloFitMacroFat,
                        trackColor = palette.cardSurfaceSubtle
                    )
                }
            }
        }

        // Weight Trend Line Chart
        item {
            CaloFitCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = s("weight_trend_title"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryText
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    if (weightRecords.size < 2) {
                        val latestText = weightRecords.lastOrNull()?.let {
                            "${StringsProvider.formatDecimal(it.weightKg, isPersian)} ${s("unit_kg")}"
                        } ?: "-"
                        Text(
                            text = if (isPersian) {
                                "آخرین وزن ثبت‌شده: $latestText. با ثبت ۲ وزن یا بیشتر، نمودار روند نمایش داده می‌شود."
                            } else {
                                "Latest weight: $latestText. Log at least 2 entries to view the trend line."
                            },
                            fontSize = 12.sp,
                            color = palette.secondaryText
                        )
                    } else {
                        WeightLineChart(
                            records = weightRecords.takeLast(14),
                            lineColor = CaloFitSuccess
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val firstW = weightRecords.first().weightKg
                        val lastW = weightRecords.last().weightKg
                        val delta = lastW - firstW
                        val deltaStr = (if (delta > 0) "+" else "") + StringsProvider.formatDecimal(delta, isPersian)
                        Text(
                            text = "${s("starting_weight")}: ${StringsProvider.formatDecimal(firstW, isPersian)} ${s("unit_kg")}  |  ${s("current_weight")}: ${StringsProvider.formatDecimal(lastW, isPersian)} ${s("unit_kg")} ($deltaStr)",
                            fontSize = 11.sp,
                            color = palette.secondaryText
                        )
                    }
                }
            }
        }

        // Real Share Button
        item {
            CaloFitButton(
                text = s("share_report"),
                icon = Icons.Default.Share,
                onClick = {
                    val shareText = buildReportShareText(
                        dateIso = selectedDateIso,
                        dayCalories = dayCalories,
                        targetCalories = targetKcal,
                        dayProtein = dayProtein,
                        dayCarbs = dayCarbs,
                        dayFat = dayFat,
                        waterMl = selectedDateWaterMl,
                        avgCalories = avgCalories,
                        isPersian = isPersian
                    )
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, s("share_report")))
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "btn_share_progress_report"
            )
            Spacer(modifier = Modifier.height(8.dp))
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
                        testTag = "input_history_edit_log_grams"
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
private fun ReportMetricPill(label: String, value: String, color: Color) {
    val palette = LocalCaloFitColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 10.sp, color = palette.secondaryText)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun RangeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalCaloFitColors.current
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) palette.primaryAccent else palette.cardSurfaceSubtle)
            .border(1.dp, if (selected) palette.primaryAccent else palette.cardBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) palette.onPrimaryAccent else palette.primaryText
        )
    }
}

@Composable
private fun CalorieBarChart(
    points: List<Pair<String, Int>>,
    targetCalories: Int,
    barColor: Color,
    targetLineColor: Color,
    trackColor: Color
) {
    val maxVal = maxOf(targetCalories, points.maxOfOrNull { it.second } ?: 0, 500).toFloat()

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        val count = points.size.coerceAtLeast(1)
        val slotWidth = size.width / count
        val barWidth = (slotWidth * 0.6f).coerceIn(4.dp.toPx(), 24.dp.toPx())

        points.forEachIndexed { idx, (_, kcal) ->
            val left = idx * slotWidth + (slotWidth - barWidth) / 2f
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(left, 0f),
                size = Size(barWidth, size.height),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
            if (kcal > 0) {
                val h = ((kcal / maxVal) * size.height).coerceIn(4.dp.toPx(), size.height)
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(left, size.height - h),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
            }
        }

        val targetY = size.height - ((targetCalories / maxVal) * size.height).coerceIn(0f, size.height)
        drawLine(
            color = targetLineColor,
            start = Offset(0f, targetY),
            end = Offset(size.width, targetY),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
        )
    }
}

@Composable
private fun MacroLegendDot(label: String, color: Color) {
    val palette = LocalCaloFitColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(text = label, fontSize = 11.sp, color = palette.secondaryText, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MacroTrendBarChart(
    points: List<Triple<Float, Float, Float>>,
    proteinColor: Color,
    carbsColor: Color,
    fatColor: Color,
    trackColor: Color
) {
    val maxMacro = maxOf(
        points.maxOfOrNull { maxOf(it.first, it.second, it.third) } ?: 0f,
        40f
    )
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
    ) {
        val count = points.size.coerceAtLeast(1)
        val groupWidth = size.width / count
        val innerGap = (groupWidth * 0.06f).coerceAtLeast(1.dp.toPx())
        val subBarWidth = ((groupWidth * 0.72f) / 3f).coerceIn(2.dp.toPx(), 10.dp.toPx())
        val totalClusterWidth = subBarWidth * 3f + innerGap * 2f

        points.forEachIndexed { idx, (protein, carbs, fat) ->
            val startX = idx * groupWidth + (groupWidth - totalClusterWidth) / 2f
            listOf(
                protein to proteinColor,
                carbs to carbsColor,
                fat to fatColor
            ).forEachIndexed { subIdx, (value, color) ->
                val x = startX + subIdx * (subBarWidth + innerGap)
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(x, 0f),
                    size = Size(subBarWidth, size.height),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
                if (value > 0f) {
                    val h = ((value / maxMacro) * size.height).coerceIn(3.dp.toPx(), size.height)
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, size.height - h),
                        size = Size(subBarWidth, h),
                        cornerRadius = CornerRadius(2.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
private fun WeightLineChart(
    records: List<WeightRecordEntity>,
    lineColor: Color
) {
    val minW = (records.minOf { it.weightKg } - 1.0).toFloat()
    val maxW = (records.maxOf { it.weightKg } + 1.0).toFloat()
    val span = (maxW - minW).coerceAtLeast(1f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        val stepX = if (records.size > 1) size.width / (records.size - 1) else size.width
        val path = Path()
        records.forEachIndexed { idx, rec ->
            val x = idx * stepX
            val normalizedY = ((rec.weightKg.toFloat() - minW) / span).coerceIn(0f, 1f)
            val y = size.height - normalizedY * (size.height - 12.dp.toPx()) - 6.dp.toPx()
            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(
                color = lineColor,
                radius = 4.dp.toPx(),
                center = Offset(x, y)
            )
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun MacroReportItem(label: String, percentage: String, color: Color) {
    val palette = LocalCaloFitColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = percentage,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = palette.primaryText,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun buildReportShareText(
    dateIso: String,
    dayCalories: Int,
    targetCalories: Int,
    dayProtein: Double,
    dayCarbs: Double,
    dayFat: Double,
    waterMl: Int,
    avgCalories: Int,
    isPersian: Boolean
): String {
    val dateLabel = JalaliCalendarHelper.formatIsoForDisplay(dateIso, isPersian)
    return if (isPersian) {
        """
        گزارش تغذیه کالوفیت ($dateLabel)
        • کالری دریافتی: ${StringsProvider.formatNumber(dayCalories, true)} از ${StringsProvider.formatNumber(targetCalories, true)} کالری
        • پروتئین: ${StringsProvider.formatDecimal(dayProtein, true)} گرم | کربوهیدرات: ${StringsProvider.formatDecimal(dayCarbs, true)} گرم | چربی: ${StringsProvider.formatDecimal(dayFat, true)} گرم
        • آب مصرفی: ${StringsProvider.formatNumber(waterMl, true)} میلی‌لیتر
        • میانگین کالری روزهای فعال: ${StringsProvider.formatNumber(avgCalories, true)} کالری
        """.trimIndent()
    } else {
        """
        KaloFit Progress Report ($dateLabel)
        • Calories: $dayCalories / $targetCalories Calories
        • Protein: ${StringsProvider.formatDecimal(dayProtein, false)} grams | Carbohydrates: ${StringsProvider.formatDecimal(dayCarbs, false)} grams | Fat: ${StringsProvider.formatDecimal(dayFat, false)} grams
        • Water: $waterMl ml
        • Active days average: $avgCalories Calories
        """.trimIndent()
    }
}
