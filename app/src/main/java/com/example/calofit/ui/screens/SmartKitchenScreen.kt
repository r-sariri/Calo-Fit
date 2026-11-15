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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calofit.data.ai.GeneratedRecipe
import com.example.calofit.data.ai.SmartChefFilters
import com.example.calofit.data.model.SavedRecipeEntity
import com.example.calofit.localization.StringsProvider
import com.example.calofit.ui.components.CaloFitButton
import com.example.calofit.ui.components.CaloFitCard
import com.example.calofit.ui.components.CaloFitTextField
import com.example.calofit.ui.theme.CaloFitError
import com.example.calofit.ui.theme.CaloFitMacroCarbs
import com.example.calofit.ui.theme.CaloFitMacroFat
import com.example.calofit.ui.theme.CaloFitMacroProtein
import com.example.calofit.ui.theme.CaloFitSuccess
import com.example.calofit.ui.theme.CaloFitWarning
import com.example.calofit.ui.theme.LocalCaloFitColors
import com.example.calofit.ui.viewmodel.SmartChefUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartKitchenScreen(
    state: SmartChefUiState,
    savedRecipes: List<SavedRecipeEntity> = emptyList(),
    isPersian: Boolean,
    onToggleIngredient: (String) -> Unit,
    onAddCustomIngredient: (String) -> Unit,
    onFiltersChange: (SmartChefFilters) -> Unit = {},
    onGenerateRecipes: () -> Unit,
    onSaveRecipe: (GeneratedRecipe) -> Unit = {},
    onDeleteSavedRecipe: (Long) -> Unit = {},
    onCancelRequest: () -> Unit,
    onBackClick: () -> Unit
) {
    val palette = LocalCaloFitColors.current
    fun s(key: String): String = StringsProvider.get(key, isPersian)
    fun n(v: Number): String = StringsProvider.formatNumber(v, isPersian)

    var customIngredientInput by remember { mutableStateOf("") }
    var filtersExpanded by remember { mutableStateOf(false) }

    val pantryChips = if (isPersian) listOf(
        "برنج", "مرغ", "گوشت", "عدس", "لپه", "تخم‌مرغ", "سیب‌زمینی",
        "بادمجان", "گوجه‌فرنگی", "پیاز", "سبزی قورمه", "اسفناج", "لوبیا چیتی", "پنیر", "گردو"
    ) else listOf(
        "Rice", "Chicken", "Beef", "Lentils", "Split peas", "Eggs", "Potato",
        "Eggplant", "Tomato", "Onion", "Fresh herbs", "Spinach", "Kidney beans", "Cheese", "Walnut"
    )

    val allChips = (pantryChips + state.selectedIngredients.toList()).distinct()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(palette.mainBackground, palette.secondaryBackground)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                Column {
                    Text(
                        text = s("smart_kitchen_title"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryText
                    )
                    Text(
                        text = s("smart_kitchen_subtitle"),
                        fontSize = 11.sp,
                        color = palette.secondaryText
                    )
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CaloFitWarning.copy(alpha = 0.14f))
                    .border(1.dp, CaloFitWarning.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFE29828),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = s("smart_chef_internet_notice"),
                        fontSize = 11.sp,
                        color = palette.primaryText,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        item {
            Text(
                text = s("available_ingredients_label"),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                allChips.forEach { item ->
                    val isSelected = state.selectedIngredients.contains(item)
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) palette.primaryAccent else palette.cardSurface)
                            .border(
                                1.dp,
                                if (isSelected) palette.primaryAccent else palette.cardBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onToggleIngredient(item) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = palette.onPrimaryAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = item,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) palette.onPrimaryAccent else palette.primaryText
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(5.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = s("delete"),
                                    tint = palette.onPrimaryAccent.copy(alpha = 0.85f),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = s("custom_ingredient_label"),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.secondaryText
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CaloFitTextField(
                    value = customIngredientInput,
                    onValueChange = { customIngredientInput = it },
                    placeholder = s("custom_ingredient_placeholder"),
                    modifier = Modifier.weight(1f),
                    testTag = "input_custom_ingredient"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .testTag("btn_add_custom_ingredient")
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.primaryAccent)
                        .clickable {
                            if (customIngredientInput.isNotBlank()) {
                                onAddCustomIngredient(customIngredientInput)
                                customIngredientInput = ""
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = s("add"),
                        tint = palette.onPrimaryAccent
                    )
                }
            }
        }

        if (state.selectedIngredients.isEmpty()) {
            item {
                CaloFitCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = s("select_ingredients_empty_msg"),
                        fontSize = 13.sp,
                        color = palette.secondaryText,
                        lineHeight = 19.sp
                    )
                }
            }
        }

        // Collapsible Optional Filters
        item {
            CaloFitCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 40.dp)
                            .clickable { filtersExpanded = !filtersExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = s("chef_filters"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryText
                        )
                        Icon(
                            imageVector = if (filtersExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = s("chef_filters"),
                            tint = palette.accentTextOnLight
                        )
                    }
                    if (filtersExpanded) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val timeOpts = if (isPersian) listOf("", "زیر ۲۰ دقیقه", "زیر ۴۵ دقیقه")
                        else listOf("", "Under 20 min", "Under 45 min")
                        val mealOpts = if (isPersian) listOf("", "صبحانه", "ناهار", "شام", "میان‌وعده")
                        else listOf("", "Breakfast", "Lunch", "Dinner", "Snack")
                        val dietOpts = if (isPersian) listOf("", "پروتئین بالا", "گیاهی", "کم‌کالری")
                        else listOf("", "High Protein", "Vegetarian", "Low Calorie")
                        val equipOpts = if (isPersian) listOf("", "تابه", "قابلمه", "فر")
                        else listOf("", "Pan", "Pot", "Oven")

                        FilterChipGroup(
                            label = s("filter_cooking_time"),
                            options = timeOpts,
                            selected = state.filters.cookingTime,
                            anyLabel = s("filter_any"),
                            onSelect = { onFiltersChange(state.filters.copy(cookingTime = it)) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FilterChipGroup(
                            label = s("filter_meal_type"),
                            options = mealOpts,
                            selected = state.filters.mealType,
                            anyLabel = s("filter_any"),
                            onSelect = { onFiltersChange(state.filters.copy(mealType = it)) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FilterChipGroup(
                            label = s("filter_diet"),
                            options = dietOpts,
                            selected = state.filters.dietaryPreference,
                            anyLabel = s("filter_any"),
                            onSelect = { onFiltersChange(state.filters.copy(dietaryPreference = it)) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FilterChipGroup(
                            label = s("filter_equipment"),
                            options = equipOpts,
                            selected = state.filters.equipment,
                            anyLabel = s("filter_any"),
                            onSelect = { onFiltersChange(state.filters.copy(equipment = it)) }
                        )
                    }
                }
            }
        }

        if (state.validationError != null) {
            item {
                Text(
                    text = state.validationError,
                    fontSize = 12.sp,
                    color = CaloFitError,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        item {
            if (state.isLoading) {
                CaloFitCard(modifier = Modifier.fillMaxWidth(), isAccent = true) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = palette.accentTextOnLight)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = s("connecting_ai"),
                            fontSize = 11.sp,
                            color = palette.secondaryText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = s("generating_recipes"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.primaryText
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        CaloFitButton(
                            text = s("cancel_request"),
                            icon = Icons.Default.Close,
                            isPrimary = false,
                            onClick = onCancelRequest,
                            testTag = "btn_cancel_smart_chef"
                        )
                    }
                }
            } else {
                CaloFitButton(
                    text = s("find_recipes"),
                    icon = Icons.Default.AutoAwesome,
                    enabled = state.selectedIngredients.isNotEmpty(),
                    onClick = onGenerateRecipes,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "btn_generate_recipes"
                )
            }
        }

        if (state.errorMessage != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CaloFitError.copy(alpha = 0.14f))
                        .border(1.dp, CaloFitError.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = state.errorMessage,
                            fontSize = 12.sp,
                            color = palette.primaryText,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CaloFitButton(
                            text = s("retry"),
                            isPrimary = false,
                            onClick = onGenerateRecipes,
                            testTag = "btn_retry_smart_chef"
                        )
                    }
                }
            }
        }

        if (state.recipeSavedMessage != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CaloFitSuccess.copy(alpha = 0.16f))
                        .border(1.dp, CaloFitSuccess.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = state.recipeSavedMessage,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CaloFitSuccess
                    )
                }
            }
        }

        if (state.recipes.isNotEmpty()) {
            item {
                Text(
                    text = s("suggested_dishes"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryText
                )
            }

            items(state.recipes) { recipe ->
                AiRecipeCard(
                    recipe = recipe,
                    isPersian = isPersian,
                    s = ::s,
                    n = ::n,
                    onSaveClick = { onSaveRecipe(recipe) }
                )
            }
        }

        if (savedRecipes.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = s("saved_recipes_title"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryText
                )
            }

            items(savedRecipes, key = { it.id }) { saved ->
                SavedRecipeCard(
                    saved = saved,
                    s = ::s,
                    n = ::n,
                    onDeleteClick = { onDeleteSavedRecipe(saved.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterChipGroup(
    label: String,
    options: List<String>,
    selected: String,
    anyLabel: String,
    onSelect: (String) -> Unit
) {
    val palette = LocalCaloFitColors.current
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.secondaryText
        )
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { opt ->
                val isSelected = selected == opt
                val display = opt.ifBlank { anyLabel }
                Box(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) palette.primaryAccent else palette.cardSurfaceSubtle)
                        .border(
                            1.dp,
                            if (isSelected) palette.primaryAccent else palette.cardBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelect(opt) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = display,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) palette.onPrimaryAccent else palette.primaryText
                    )
                }
            }
        }
    }
}

@Composable
private fun AiRecipeCard(
    recipe: GeneratedRecipe,
    isPersian: Boolean,
    s: (String) -> String,
    n: (Number) -> String,
    onSaveClick: () -> Unit
) {
    val palette = LocalCaloFitColors.current

    CaloFitCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = recipe.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryText,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = palette.secondaryText,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${n(recipe.prepTimeMinutes + recipe.cookTimeMinutes)} ${s("unit_minutes")} • ${n(recipe.servings)} ${s("serving_count_label")}",
                        fontSize = 11.sp,
                        color = palette.secondaryText
                    )
                }
            }

            if (recipe.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = recipe.description,
                    fontSize = 12.sp,
                    color = palette.secondaryText
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${s("ingredients_used")}: ${recipe.ingredients.joinToString(", ")}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = palette.primaryText
            )

            if (recipe.optionalAdditions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${s("optional_additions")}: ${recipe.optionalAdditions.joinToString(", ")}",
                    fontSize = 11.sp,
                    color = palette.secondaryText
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = s("preparation_steps"),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText
            )
            recipe.instructions.forEachIndexed { idx, step ->
                Text(
                    text = "${n(idx + 1)}. $step",
                    fontSize = 12.sp,
                    color = palette.secondaryText,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${n(recipe.calories)} ${s("unit_calories")}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.accentTextOnLight
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val unitG = s("unit_grams")
                    Text(
                        text = "${s("carbs")}: ${n(recipe.carbsGrams.toInt())} $unitG",
                        fontSize = 10.sp,
                        color = CaloFitMacroCarbs,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${s("protein")}: ${n(recipe.proteinGrams.toInt())} $unitG",
                        fontSize = 10.sp,
                        color = CaloFitMacroProtein,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${s("fat")}: ${n(recipe.fatGrams.toInt())} $unitG",
                        fontSize = 10.sp,
                        color = CaloFitMacroFat,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${s("estimate_badge")} — ${s("nutrition_estimate_note")}",
                fontSize = 10.sp,
                color = palette.secondaryText
            )

            Spacer(modifier = Modifier.height(10.dp))
            CaloFitButton(
                text = s("save_recipe"),
                icon = Icons.Default.BookmarkAdded,
                isPrimary = false,
                onClick = onSaveClick,
                modifier = Modifier.fillMaxWidth(),
                testTag = "btn_save_recipe"
            )
        }
    }
}

@Composable
private fun SavedRecipeCard(
    saved: SavedRecipeEntity,
    s: (String) -> String,
    n: (Number) -> String,
    onDeleteClick: () -> Unit
) {
    val palette = LocalCaloFitColors.current
    CaloFitCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = saved.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryText,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = s("delete_recipe"),
                        tint = CaloFitError,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (saved.description.isNotBlank()) {
                Text(text = saved.description, fontSize = 12.sp, color = palette.secondaryText)
                Spacer(modifier = Modifier.height(6.dp))
            }
            Text(
                text = "${s("ingredients_used")}: ${saved.ingredientsText.replace("\n", ", ")}",
                fontSize = 11.sp,
                color = palette.primaryText
            )
            Spacer(modifier = Modifier.height(6.dp))
            saved.instructionsText.split("\n").filter { it.isNotBlank() }.forEachIndexed { idx, step ->
                Text(
                    text = "${n(idx + 1)}. $step",
                    fontSize = 11.5.sp,
                    color = palette.secondaryText,
                    lineHeight = 17.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${n(saved.calories)} ${s("unit_calories")} • ${n(saved.prepTimeMinutes)} ${s("unit_minutes")}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = palette.accentTextOnLight
            )
        }
    }
}
