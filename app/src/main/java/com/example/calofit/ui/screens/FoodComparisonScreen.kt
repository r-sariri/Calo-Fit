package com.example.calofit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calofit.data.model.FoodItemEntity
import com.example.calofit.domain.calculator.ComparisonGoal
import com.example.calofit.domain.calculator.ComparisonMode
import com.example.calofit.domain.calculator.ComparisonWinner
import com.example.calofit.domain.calculator.FoodComparisonEvaluation
import com.example.calofit.domain.calculator.FoodComparisonEvaluator
import com.example.calofit.domain.calculator.NormalizedNutritionMetrics
import com.example.calofit.localization.StringsProvider
import com.example.calofit.ui.components.CaloFitCard
import com.example.calofit.ui.components.CaloFitTextField
import com.example.calofit.ui.theme.CaloFitMacroCarbs
import com.example.calofit.ui.theme.CaloFitMacroFat
import com.example.calofit.ui.theme.CaloFitMacroProtein
import com.example.calofit.ui.theme.CaloFitSuccess
import com.example.calofit.ui.theme.CaloFitWarning
import com.example.calofit.ui.theme.LocalCaloFitColors
import kotlin.math.abs

private enum class ComparisonSlot { LEFT, RIGHT }

@Composable
fun FoodComparisonScreen(
    allFoods: List<FoodItemEntity>,
    isPersian: Boolean,
    onBackClick: () -> Unit
) {
    val palette = LocalCaloFitColors.current
    fun s(key: String): String = StringsProvider.get(key, isPersian)

    val uniqueFoods = remember(allFoods) {
        allFoods
            .distinctBy { it.id }
            .distinctBy {
                Triple(
                    it.nameEn.trim().lowercase(),
                    it.nameFa.trim().lowercase(),
                    Pair(it.servingSizeGrams, it.calories)
                )
            }
    }

    var leftFoodId by remember { mutableStateOf<Long?>(null) }
    var rightFoodId by remember { mutableStateOf<Long?>(null) }
    var activePickerSlot by remember { mutableStateOf<ComparisonSlot?>(null) }
    var comparisonMode by remember { mutableStateOf(ComparisonMode.PER_SERVING) }
    var comparisonGoal by remember { mutableStateOf(ComparisonGoal.OVERALL) }

    LaunchedEffect(uniqueFoods) {
        if (uniqueFoods.isNotEmpty()) {
            if (leftFoodId == null || uniqueFoods.none { it.id == leftFoodId }) {
                leftFoodId = (uniqueFoods.find { it.nameEn.contains("Zereshk", ignoreCase = true) }
                    ?: uniqueFoods.first()).id
            }
            if (rightFoodId == null || uniqueFoods.none { it.id == rightFoodId }) {
                rightFoodId = (uniqueFoods.find { it.nameEn.contains("Koobideh", ignoreCase = true) }
                    ?: uniqueFoods.getOrNull(1)
                    ?: uniqueFoods.first()).id
            }
        }
    }

    val leftFood = uniqueFoods.find { it.id == leftFoodId } ?: uniqueFoods.firstOrNull()
    val rightFood = uniqueFoods.find { it.id == rightFoodId } ?: uniqueFoods.getOrNull(1) ?: leftFood

    val evaluation = remember(leftFood, rightFood, comparisonMode, comparisonGoal) {
        if (leftFood != null && rightFood != null) {
            FoodComparisonEvaluator.evaluate(leftFood, rightFood, comparisonMode, comparisonGoal)
        } else null
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
                text = s("food_comparison_title"),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Comparison Mode Toggle (Per Serving vs Per 100g)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(palette.cardSurface)
                .border(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                ComparisonMode.PER_SERVING to s("compare_mode_serving"),
                ComparisonMode.PER_100G to s("compare_mode_100g")
            ).forEach { (mode, label) ->
                val selected = comparisonMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_compare_mode_${mode.name.lowercase()}")
                        .defaultMinSize(minHeight = 44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) palette.primaryAccent else Color.Transparent)
                        .clickable { comparisonMode = mode }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) palette.onPrimaryAccent else palette.primaryText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = s("compare_goal_label"),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.secondaryText
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                ComparisonGoal.OVERALL to s("compare_goal_overall"),
                ComparisonGoal.LOWER_CALORIES to s("compare_goal_low_cal"),
                ComparisonGoal.HIGHER_PROTEIN to s("compare_goal_high_protein")
            ).forEach { (goalOption, goalLabel) ->
                val selected = comparisonGoal == goalOption
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_compare_goal_${goalOption.name.lowercase()}")
                        .defaultMinSize(minHeight = 42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) palette.primaryAccent else palette.cardSurface)
                        .border(
                            1.dp,
                            if (selected) palette.primaryAccent else palette.cardBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { comparisonGoal = goalOption }
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = goalLabel,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) palette.onPrimaryAccent else palette.primaryText,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (leftFood != null && rightFood != null && evaluation != null) {
            val leftBadges = remember(evaluation, isPersian) {
                buildSideBadges(ComparisonWinner.LEFT, evaluation, isPersian)
            }
            val rightBadges = remember(evaluation, isPersian) {
                buildSideBadges(ComparisonWinner.RIGHT, evaluation, isPersian)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ComparisonFoodCard(
                    food = leftFood,
                    metrics = evaluation.leftMetrics,
                    badges = leftBadges,
                    mode = comparisonMode,
                    isPersian = isPersian,
                    isAccent = evaluation.greenHighlightSide == ComparisonWinner.LEFT,
                    showSodium = evaluation.leftMetrics.sodiumMg > 0.0 || evaluation.rightMetrics.sodiumMg > 0.0,
                    onClick = { activePickerSlot = ComparisonSlot.LEFT },
                    testTag = "card_compare_left",
                    modifier = Modifier.weight(1f)
                )
                ComparisonFoodCard(
                    food = rightFood,
                    metrics = evaluation.rightMetrics,
                    badges = rightBadges,
                    mode = comparisonMode,
                    isPersian = isPersian,
                    isAccent = evaluation.greenHighlightSide == ComparisonWinner.RIGHT,
                    showSodium = evaluation.leftMetrics.sodiumMg > 0.0 || evaluation.rightMetrics.sodiumMg > 0.0,
                    onClick = { activePickerSlot = ComparisonSlot.RIGHT },
                    testTag = "card_compare_right",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            CaloFitCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                            contentDescription = null,
                            tint = palette.accentTextOnLight,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = s("compare_verdict"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryText
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val analysis = remember(leftFood, rightFood, evaluation, comparisonMode, isPersian) {
                        buildDynamicComparisonSummary(leftFood, rightFood, evaluation, comparisonMode, isPersian)
                    }

                    Text(
                        text = analysis,
                        fontSize = 12.sp,
                        color = palette.secondaryText,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }

    activePickerSlot?.let { slot ->
        FoodPickerDialog(
            foods = uniqueFoods,
            isPersian = isPersian,
            title = if (slot == ComparisonSlot.LEFT) s("select_food_1") else s("select_food_2"),
            onDismiss = { activePickerSlot = null },
            onSelect = { chosen ->
                if (slot == ComparisonSlot.LEFT) {
                    leftFoodId = chosen.id
                } else {
                    rightFoodId = chosen.id
                }
                activePickerSlot = null
            }
        )
    }
}

private data class ComparisonBadge(
    val text: String,
    val isOverallWinner: Boolean = false
)

private fun buildSideBadges(
    side: ComparisonWinner,
    eval: FoodComparisonEvaluation,
    isPersian: Boolean
): List<ComparisonBadge> {
    if (eval.isSameFood || eval.hasInsufficientData) return emptyList()
    fun s(key: String) = StringsProvider.get(key, isPersian)
    val badges = mutableListOf<ComparisonBadge>()
    if (eval.greenHighlightSide == side) {
        badges.add(ComparisonBadge(s("badge_healthier_choice"), isOverallWinner = true))
    }
    if (eval.lowerCaloriesSide == side) badges.add(ComparisonBadge(s("badge_lower_calories")))
    if (eval.higherProteinSide == side) badges.add(ComparisonBadge(s("badge_higher_protein")))
    if (eval.higherFiberSide == side) badges.add(ComparisonBadge(s("badge_higher_fiber")))
    if (eval.lowerSugarSide == side) badges.add(ComparisonBadge(s("badge_lower_sugar")))
    if (eval.lowerSodiumSide == side) badges.add(ComparisonBadge(s("badge_lower_sodium")))
    return badges
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ComparisonFoodCard(
    food: FoodItemEntity,
    metrics: NormalizedNutritionMetrics,
    badges: List<ComparisonBadge>,
    mode: ComparisonMode,
    isPersian: Boolean,
    isAccent: Boolean,
    showSodium: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalCaloFitColors.current
    fun s(key: String): String = StringsProvider.get(key, isPersian)
    fun n(v: Number): String = StringsProvider.formatNumber(v, isPersian)

    CaloFitCard(
        modifier = modifier
            .testTag(testTag)
            .clickable(onClick = onClick),
        isAccent = isAccent
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            val foodTitle = if (isPersian) {
                food.nameFa.ifBlank { food.nameEn }
            } else {
                food.nameEn.ifBlank { food.nameFa }
            }

            Text(
                text = foodTitle,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            val servingText = if (mode == ComparisonMode.PER_100G) {
                "${n(100)} ${s("unit_grams")}"
            } else {
                val raw = if (isPersian) food.servingUnitFa else food.servingUnitEn
                StringsProvider.localizeDigits(raw, isPersian)
            }
            Text(
                text = servingText,
                fontSize = 10.sp,
                color = palette.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = n(metrics.calories),
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isAccent) palette.accentTextOnLight else Color(0xFFFF9500)
            )
            Text(text = s("unit_calories"), fontSize = 11.sp, color = palette.secondaryText)

            if (badges.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    badges.take(3).forEach { badge ->
                        val bg = if (badge.isOverallWinner) CaloFitSuccess.copy(alpha = 0.22f)
                        else palette.cardSurfaceSubtle
                        val txtColor = if (badge.isOverallWinner) CaloFitSuccess else palette.accentTextOnLight
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(bg)
                                .border(
                                    1.dp,
                                    if (badge.isOverallWinner) CaloFitSuccess.copy(alpha = 0.5f) else palette.cardBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = badge.text,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = txtColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            ComparisonMetricRow(
                s("protein"),
                "${StringsProvider.formatDecimal(metrics.protein, isPersian)} ${s("unit_grams")}",
                CaloFitMacroProtein
            )
            ComparisonMetricRow(
                s("carbs"),
                "${StringsProvider.formatDecimal(metrics.carbs, isPersian)} ${s("unit_grams")}",
                CaloFitMacroCarbs
            )
            ComparisonMetricRow(
                s("fat"),
                "${StringsProvider.formatDecimal(metrics.fat, isPersian)} ${s("unit_grams")}",
                CaloFitMacroFat
            )
            ComparisonMetricRow(
                s("fiber"),
                "${StringsProvider.formatDecimal(metrics.fiber, isPersian)} ${s("unit_grams")}",
                CaloFitSuccess
            )
            ComparisonMetricRow(
                s("sugar"),
                "${StringsProvider.formatDecimal(metrics.sugar, isPersian)} ${s("unit_grams")}",
                CaloFitWarning
            )
            if (showSodium) {
                ComparisonMetricRow(
                    s("sodium"),
                    "${n(metrics.sodiumMg.toInt())} ${s("unit_mg")}",
                    palette.secondaryText
                )
            }
            ComparisonMetricRow(
                s("quality_score"),
                "${n(metrics.qualityScore.toInt())}/100",
                palette.accentTextOnLight
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.cardSurfaceSubtle)
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = palette.accentTextOnLight,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = s("tap_to_change_food"),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.primaryText
                )
            }
        }
    }
}

@Composable
private fun FoodPickerDialog(
    foods: List<FoodItemEntity>,
    isPersian: Boolean,
    title: String,
    onDismiss: () -> Unit,
    onSelect: (FoodItemEntity) -> Unit
) {
    val palette = LocalCaloFitColors.current
    fun s(key: String): String = StringsProvider.get(key, isPersian)
    var query by remember { mutableStateOf("") }

    val filtered by remember(query, foods) {
        derivedStateOf {
            val q = query.trim()
            if (q.isBlank()) foods
            else foods.filter {
                it.nameFa.contains(q, ignoreCase = true) || it.nameEn.contains(q, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                CaloFitTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = s("search_food_hint"),
                    leadingIcon = Icons.Default.Search,
                    testTag = "input_compare_food_search"
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(filtered, key = { it.id }) { item ->
                        val itemName = if (isPersian) item.nameFa.ifBlank { item.nameEn } else item.nameEn.ifBlank { item.nameFa }
                        val kcalStr = StringsProvider.formatNumber(item.calories, isPersian)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(palette.cardSurfaceSubtle)
                                .border(1.dp, palette.cardBorder, RoundedCornerShape(12.dp))
                                .clickable { onSelect(item) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = itemName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = palette.primaryText
                                )
                                Text(
                                    text = StringsProvider.localizeDigits(
                                        if (isPersian) item.servingUnitFa else item.servingUnitEn,
                                        isPersian
                                    ),
                                    fontSize = 10.sp,
                                    color = palette.secondaryText
                                )
                            }
                            Text(
                                text = "$kcalStr ${s("unit_calories")}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.accentTextOnLight
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = s("close"))
            }
        }
    )
}

@Composable
private fun ComparisonMetricRow(label: String, value: String, color: Color) {
    val palette = LocalCaloFitColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 10.sp, color = palette.secondaryText)
        Text(text = value, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

fun buildDynamicComparisonSummary(
    left: FoodItemEntity,
    right: FoodItemEntity,
    eval: FoodComparisonEvaluation = FoodComparisonEvaluator.evaluate(left, right, ComparisonMode.PER_SERVING),
    mode: ComparisonMode = ComparisonMode.PER_SERVING,
    isPersian: Boolean
): String {
    if (eval.isSameFood) {
        return StringsProvider.get("same_food_selected", isPersian)
    }
    if (eval.hasInsufficientData) {
        return StringsProvider.get("insufficient_nutrition_data", isPersian)
    }
    val leftName = if (isPersian) left.nameFa.ifBlank { left.nameEn } else left.nameEn.ifBlank { left.nameFa }
    val rightName = if (isPersian) right.nameFa.ifBlank { right.nameEn } else right.nameEn.ifBlank { right.nameFa }
    val unitScope = if (mode == ComparisonMode.PER_100G) {
        if (isPersian) "در ۱۰۰ گرم" else "per 100g"
    } else {
        if (isPersian) "در هر سهم" else "per serving"
    }

    val lm = eval.leftMetrics
    val rm = eval.rightMetrics
    val calDiff = abs(lm.calories - rm.calories)
    val protDiff = abs(lm.protein - rm.protein)
    val fiberDiff = abs(lm.fiber - rm.fiber)
    val sugarDiff = abs(lm.sugar - rm.sugar)

    val calLeader = if (lm.calories <= rm.calories) leftName else rightName
    val calHigher = if (lm.calories > rm.calories) leftName else rightName
    val protLeader = if (lm.protein >= rm.protein) leftName else rightName
    val fiberLeader = if (lm.fiber >= rm.fiber) leftName else rightName
    val sugarLeader = if (lm.sugar <= rm.sugar) leftName else rightName

    return if (isPersian) {
        buildString {
            if (calDiff > 0) {
                append("• انرژی ($unitScope): «$calLeader» به اندازه ${StringsProvider.formatNumber(calDiff, true)} کالری سبک‌تر از «$calHigher» است.\n")
            } else {
                append("• انرژی ($unitScope): هر دو غذا کالری یکسانی دارند.\n")
            }
            if (protDiff >= 0.8) {
                append("• پروتئین: «$protLeader» با ${StringsProvider.formatDecimal(protDiff, true)} گرم پروتئین بیشتر، برای سیری و حفظ عضله قوی‌تر است.\n")
            }
            if (fiberDiff >= 0.5) {
                append("• فیبر: «$fiberLeader» به اندازه ${StringsProvider.formatDecimal(fiberDiff, true)} گرم فیبر بیشتری دارد.\n")
            }
            if (sugarDiff >= 0.5) {
                append("• قند: «$sugarLeader» با ${StringsProvider.formatDecimal(sugarDiff, true)} گرم قند کمتر گزینه متعادل‌تری است.\n")
            }
            if (eval.isCalorieVsNutritionConflict) {
                append(StringsProvider.get("compare_tradeoff_note", true))
            } else if (eval.greenHighlightSide != ComparisonWinner.NONE) {
                val winnerName = if (eval.greenHighlightSide == ComparisonWinner.LEFT) leftName else rightName
                append("• جمع‌بندی: «$winnerName» هم کالری کمتری دارد و هم از نظر ترکیب تغذیه‌ای امتیاز بالاتری می‌گیرد.")
            } else {
                append("هر دو گزینه تعادل مشابهی دارند؛ بر اساس هدف روزانه خود انتخاب کنید.")
            }
        }
    } else {
        buildString {
            if (calDiff > 0) {
                append("• Energy ($unitScope): $calLeader has $calDiff fewer calories than $calHigher.\n")
            } else {
                append("• Energy ($unitScope): Both options provide equal calories.\n")
            }
            if (protDiff >= 0.8) {
                append("• Protein: $protLeader delivers ${StringsProvider.formatDecimal(protDiff, false)}g more protein.\n")
            }
            if (fiberDiff >= 0.5) {
                append("• Fiber: $fiberLeader supplies ${StringsProvider.formatDecimal(fiberDiff, false)}g more dietary fiber.\n")
            }
            if (sugarDiff >= 0.5) {
                append("• Sugar: $sugarLeader has ${StringsProvider.formatDecimal(sugarDiff, false)}g less sugar.\n")
            }
            if (eval.isCalorieVsNutritionConflict) {
                append(StringsProvider.get("compare_tradeoff_note", false))
            } else if (eval.greenHighlightSide != ComparisonWinner.NONE) {
                val winnerName = if (eval.greenHighlightSide == ComparisonWinner.LEFT) leftName else rightName
                append("• Summary: $winnerName offers both lower calories and a stronger overall nutrient profile.")
            } else {
                append("Both options are closely balanced; choose according to your macro targets.")
            }
        }
    }
}

