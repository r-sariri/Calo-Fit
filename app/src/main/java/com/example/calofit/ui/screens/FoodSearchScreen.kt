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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calofit.data.model.FoodItemEntity
import com.example.calofit.domain.calendar.JalaliCalendarHelper
import com.example.calofit.localization.StringsProvider
import com.example.calofit.ui.components.CaloFitCard
import com.example.calofit.ui.components.CaloFitTextField
import com.example.calofit.ui.theme.CaloFitError
import com.example.calofit.ui.theme.CaloFitMacroCarbs
import com.example.calofit.ui.theme.CaloFitMacroFat
import com.example.calofit.ui.theme.CaloFitMacroProtein
import com.example.calofit.ui.theme.CaloFitSuccess
import com.example.calofit.ui.theme.LocalCaloFitColors

@Composable
fun FoodSearchScreen(
    allFoods: List<FoodItemEntity>,
    mealType: String,
    isPersian: Boolean,
    onFoodSelected: (FoodItemEntity) -> Unit,
    onAddCustomFood: (
        nameEn: String,
        nameFa: String,
        category: String,
        servingGrams: Double,
        calories: Int,
        protein: Double,
        carbs: Double,
        fat: Double,
        fiber: Double,
        sugar: Double,
        isIranian: Boolean
    ) -> Unit,
    onUpdateCustomFood: (
        existing: FoodItemEntity,
        name: String,
        servingGrams: Double,
        calories: Int,
        protein: Double,
        carbs: Double,
        fat: Double,
        fiber: Double,
        sugar: Double
    ) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onDeleteCustomFood: (Long) -> Unit = {},
    onBackClick: () -> Unit
) {
    val palette = LocalCaloFitColors.current
    fun s(key: String): String = StringsProvider.get(key, isPersian)

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingCustomFood by remember { mutableStateOf<FoodItemEntity?>(null) }
    var deletingCustomFood by remember { mutableStateOf<FoodItemEntity?>(null) }

    val categories = if (isPersian) listOf(
        "ALL" to s("all_foods"),
        "IRANIAN_MAINS" to s("iranian_foods"),
        "MEAT_PROTEIN" to "پروتئین و گوشت",
        "BREAD_GRAINS" to "نان و غلات",
        "DAIRY" to "لبنیات",
        "FRUITS" to "میوه و سبزیجات",
        "SNACKS" to "میان‌وعده و مغزها"
    ) else listOf(
        "ALL" to "All Foods",
        "IRANIAN_MAINS" to "Persian Cuisine",
        "MEAT_PROTEIN" to "Meat & Protein",
        "BREAD_GRAINS" to "Bread & Grains",
        "DAIRY" to "Dairy",
        "FRUITS" to "Fruits & Veggies",
        "SNACKS" to "Snacks & Nuts"
    )

    val filteredFoods by remember(searchQuery, selectedCategory, allFoods) {
        derivedStateOf {
            allFoods.filter { food ->
                val matchesCategory = selectedCategory == "ALL" || food.category == selectedCategory
                val q = searchQuery.trim()
                val matchesQuery = q.isBlank() ||
                    food.nameFa.contains(q, ignoreCase = true) ||
                    food.nameEn.contains(q, ignoreCase = true)
                matchesCategory && matchesQuery
            }
        }
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
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
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
                text = s("add_food"),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .testTag("btn_open_create_custom_food")
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.primaryAccent.copy(alpha = 0.25f))
                    .clickable { showCreateDialog = true }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = palette.accentTextOnLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = s("create_custom_food"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = s("search_label"),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = palette.primaryText
        )
        Spacer(modifier = Modifier.height(6.dp))
        CaloFitTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = s("search_food_hint"),
            leadingIcon = Icons.Default.Search,
            testTag = "input_food_search"
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { (code, title) ->
                val isSelected = selectedCategory == code
                Box(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 40.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) palette.primaryAccent.copy(alpha = if (palette.isDark) 0.28f else 0.42f)
                            else palette.cardSurface
                        )
                        .border(
                            1.dp,
                            if (isSelected) palette.primaryAccent else palette.cardBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { selectedCategory = code }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) palette.accentTextOnLight else palette.primaryText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredFoods.isEmpty()) {
            CaloFitCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = s("no_foods_found"),
                    fontSize = 13.sp,
                    color = palette.secondaryText
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredFoods, key = { it.id }) { food ->
                    FoodSearchCard(
                        food = food,
                        isPersian = isPersian,
                        onClick = { onFoodSelected(food) },
                        onEditClick = if (!food.isVerified) {
                            { editingCustomFood = food }
                        } else null,
                        onDeleteClick = if (!food.isVerified) {
                            { deletingCustomFood = food }
                        } else null
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CustomFoodDialog(
            initialFood = null,
            isPersian = isPersian,
            onDismiss = { showCreateDialog = false },
            onSave = { nameEn, nameFa, category, grams, kcal, p, c, f, fiber, sugar, isIr ->
                onAddCustomFood(nameEn, nameFa, category, grams, kcal, p, c, f, fiber, sugar, isIr)
                showCreateDialog = false
            }
        )
    }

    editingCustomFood?.let { target ->
        CustomFoodDialog(
            initialFood = target,
            isPersian = isPersian,
            onDismiss = { editingCustomFood = null },
            onSave = { nameEn, _, _, grams, kcal, p, c, f, fiber, sugar, _ ->
                onUpdateCustomFood(target, nameEn, grams, kcal, p, c, f, fiber, sugar)
                editingCustomFood = null
            }
        )
    }

    deletingCustomFood?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingCustomFood = null },
            title = { Text(text = s("delete"), fontWeight = FontWeight.Bold) },
            text = { Text(text = s("delete_food_confirm"), fontSize = 13.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCustomFood(target.id)
                        deletingCustomFood = null
                    }
                ) {
                    Text(text = s("delete"), color = CaloFitError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingCustomFood = null }) {
                    Text(text = s("cancel"))
                }
            }
        )
    }
}

@Composable
private fun FoodSearchCard(
    food: FoodItemEntity,
    isPersian: Boolean,
    onClick: () -> Unit,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null
) {
    val palette = LocalCaloFitColors.current
    fun s(key: String): String = StringsProvider.get(key, isPersian)
    fun n(v: Number): String = StringsProvider.formatNumber(v, isPersian)

    CaloFitCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isPersian) food.nameFa.ifBlank { food.nameEn } else food.nameEn.ifBlank { food.nameFa },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryText
                    )
                    if (food.isIranian) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CaloFitSuccess.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isPersian) "ایرانی" else "Persian",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = CaloFitSuccess
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                val servingLabel = if (isPersian) {
                    StringsProvider.localizeDigits(food.servingUnitFa, true)
                } else {
                    StringsProvider.localizeDigits(food.servingUnitEn, false)
                }
                Text(
                    text = servingLabel,
                    fontSize = 11.sp,
                    color = palette.secondaryText
                )

                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val unitG = s("unit_grams")
                    Text(
                        text = "${s("carbs")}: ${StringsProvider.formatDecimal(food.carbs, isPersian)} $unitG",
                        fontSize = 10.sp,
                        color = CaloFitMacroCarbs,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${s("protein")}: ${StringsProvider.formatDecimal(food.protein, isPersian)} $unitG",
                        fontSize = 10.sp,
                        color = CaloFitMacroProtein,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${s("fat")}: ${StringsProvider.formatDecimal(food.fat, isPersian)} $unitG",
                        fontSize = 10.sp,
                        color = CaloFitMacroFat,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onEditClick != null) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = s("edit"),
                            tint = palette.accentTextOnLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (onDeleteClick != null) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = s("delete"),
                            tint = CaloFitError,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = n(food.calories),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accentTextOnLight
                    )
                    Text(
                        text = s("unit_calories"),
                        fontSize = 10.sp,
                        color = palette.secondaryText
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomFoodDialog(
    initialFood: FoodItemEntity?,
    isPersian: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        nameEn: String,
        nameFa: String,
        category: String,
        grams: Double,
        calories: Int,
        protein: Double,
        carbs: Double,
        fat: Double,
        fiber: Double,
        sugar: Double,
        isIranian: Boolean
    ) -> Unit
) {
    val palette = LocalCaloFitColors.current
    fun s(key: String): String = StringsProvider.get(key, isPersian)

    var foodName by remember(initialFood) {
        mutableStateOf(
            if (initialFood == null) ""
            else if (isPersian) initialFood.nameFa.ifBlank { initialFood.nameEn }
            else initialFood.nameEn.ifBlank { initialFood.nameFa }
        )
    }
    var servingGramsStr by remember(initialFood) {
        mutableStateOf(initialFood?.let { StringsProvider.formatDecimal(it.servingSizeGrams, false, 0) } ?: "")
    }
    var caloriesStr by remember(initialFood) {
        mutableStateOf(initialFood?.calories?.toString() ?: "")
    }
    var carbsStr by remember(initialFood) {
        mutableStateOf(initialFood?.let { StringsProvider.formatDecimal(it.carbs, false) } ?: "")
    }
    var proteinStr by remember(initialFood) {
        mutableStateOf(initialFood?.let { StringsProvider.formatDecimal(it.protein, false) } ?: "")
    }
    var fatStr by remember(initialFood) {
        mutableStateOf(initialFood?.let { StringsProvider.formatDecimal(it.fat, false) } ?: "")
    }
    var fiberStr by remember(initialFood) {
        mutableStateOf(initialFood?.fiber?.takeIf { it > 0.0 }?.let { StringsProvider.formatDecimal(it, false) } ?: "")
    }
    var sugarStr by remember(initialFood) {
        mutableStateOf(initialFood?.sugar?.takeIf { it > 0.0 }?.let { StringsProvider.formatDecimal(it, false) } ?: "")
    }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialFood != null) s("edit_custom_food") else s("create_custom_food"),
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column {
                    Text(
                        text = s("food_name_label"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    CaloFitTextField(
                        value = foodName,
                        onValueChange = {
                            foodName = it
                            errorText = null
                        },
                        placeholder = s("food_name_placeholder"),
                        testTag = "input_custom_food_name"
                    )
                }

                Column {
                    Text(
                        text = s("serving_amount_label"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    CaloFitTextField(
                        value = servingGramsStr,
                        onValueChange = {
                            servingGramsStr = it
                            errorText = null
                        },
                        placeholder = s("serving_amount_placeholder"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        testTag = "input_custom_food_grams"
                    )
                }

                Column {
                    Text(
                        text = s("calories_label"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    CaloFitTextField(
                        value = caloriesStr,
                        onValueChange = {
                            caloriesStr = it
                            errorText = null
                        },
                        placeholder = s("calories_placeholder"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        testTag = "input_custom_food_calories"
                    )
                }

                Column {
                    Text(
                        text = s("carbs_label"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CaloFitMacroCarbs
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    CaloFitTextField(
                        value = carbsStr,
                        onValueChange = {
                            carbsStr = it
                            errorText = null
                        },
                        placeholder = s("carbs_placeholder"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        testTag = "input_custom_food_carbs"
                    )
                }

                Column {
                    Text(
                        text = s("protein_label"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CaloFitMacroProtein
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    CaloFitTextField(
                        value = proteinStr,
                        onValueChange = {
                            proteinStr = it
                            errorText = null
                        },
                        placeholder = s("protein_placeholder"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        testTag = "input_custom_food_protein"
                    )
                }

                Column {
                    Text(
                        text = s("fat_label"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CaloFitMacroFat
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    CaloFitTextField(
                        value = fatStr,
                        onValueChange = {
                            fatStr = it
                            errorText = null
                        },
                        placeholder = s("fat_placeholder"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        testTag = "input_custom_food_fat"
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = s("fiber_label"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CaloFitSuccess
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        CaloFitTextField(
                            value = fiberStr,
                            onValueChange = { fiberStr = it; errorText = null },
                            placeholder = "0",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            testTag = "input_custom_food_fiber"
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = s("sugar_label"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.secondaryText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        CaloFitTextField(
                            value = sugarStr,
                            onValueChange = { sugarStr = it; errorText = null },
                            placeholder = "0",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            testTag = "input_custom_food_sugar"
                        )
                    }
                }

                if (errorText != null) {
                    Text(
                        text = errorText.orEmpty(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CaloFitError
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmedName = foodName.trim()
                    val grams = JalaliCalendarHelper.toAsciiDigits(servingGramsStr).trim().toDoubleOrNull()
                    val kcal = JalaliCalendarHelper.toAsciiDigits(caloriesStr).trim().toIntOrNull()
                    val carbs = JalaliCalendarHelper.toAsciiDigits(carbsStr).trim().toDoubleOrNull()
                    val protein = JalaliCalendarHelper.toAsciiDigits(proteinStr).trim().toDoubleOrNull()
                    val fat = JalaliCalendarHelper.toAsciiDigits(fatStr).trim().toDoubleOrNull()
                    val fiber = if (fiberStr.isBlank()) 0.0 else JalaliCalendarHelper.toAsciiDigits(fiberStr).trim().toDoubleOrNull()
                    val sugar = if (sugarStr.isBlank()) 0.0 else JalaliCalendarHelper.toAsciiDigits(sugarStr).trim().toDoubleOrNull()

                    when {
                        trimmedName.isBlank() -> errorText = s("err_food_name_required")
                        grams == null || grams <= 0.0 -> errorText = s("err_serving_invalid")
                        kcal == null || kcal < 0 -> errorText = s("err_calories_invalid")
                        carbs == null || carbs < 0.0 ||
                            protein == null || protein < 0.0 ||
                            fat == null || fat < 0.0 ||
                            fiber == null || fiber < 0.0 ||
                            sugar == null || sugar < 0.0 -> errorText = s("err_macros_invalid")
                        else -> onSave(
                            trimmedName,
                            trimmedName,
                            initialFood?.category ?: if (isPersian) "IRANIAN_MAINS" else "SNACKS",
                            grams,
                            kcal,
                            protein,
                            carbs,
                            fat,
                            fiber,
                            sugar,
                            initialFood?.isIranian ?: isPersian
                        )
                    }
                }
            ) {
                Text(
                    text = if (initialFood != null) s("update_food") else s("save"),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = s("cancel"))
            }
        }
    )
}
