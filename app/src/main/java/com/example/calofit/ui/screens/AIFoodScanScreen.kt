package com.example.calofit.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.calofit.data.model.FoodItemEntity
import com.example.calofit.localization.StringsProvider
import com.example.calofit.ui.components.CaloFitButton
import com.example.calofit.ui.components.CaloFitCard
import com.example.calofit.ui.components.CaloFitTextField
import com.example.calofit.ui.theme.CaloFitError
import com.example.calofit.ui.theme.CaloFitMacroCarbs
import com.example.calofit.ui.theme.CaloFitMacroFat
import com.example.calofit.ui.theme.CaloFitMacroProtein
import com.example.calofit.ui.theme.CaloFitWarning
import com.example.calofit.ui.theme.LocalCaloFitColors
import com.example.calofit.ui.viewmodel.FoodScanUiState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import com.example.calofit.domain.calendar.JalaliCalendarHelper
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AIFoodScanScreen(
    state: FoodScanUiState,
    isPersian: Boolean,
    onImagePicked: (Uri) -> Unit,
    onAnalyzeClick: () -> Unit,
    onCancelRequest: () -> Unit,
    onClearImage: () -> Unit,
    onConfirmLog: (foodItem: FoodItemEntity, grams: Double, mealType: String) -> Unit,
    onBackClick: () -> Unit
) {
    val palette = LocalCaloFitColors.current
    val context = LocalContext.current
    fun s(key: String): String = StringsProvider.get(key, isPersian)
    fun n(v: Number): String = StringsProvider.formatNumber(v, isPersian)

    var permissionDeniedNote by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            permissionDeniedNote = false
            onImagePicked(uri)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDeniedNote = false
            val uri = createTempCameraUri(context)
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        } else {
            permissionDeniedNote = true
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            permissionDeniedNote = false
            onImagePicked(uri)
        }
    }

    fun launchCameraFlow() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            permissionDeniedNote = false
            val uri = createTempCameraUri(context)
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var editableFoodName by remember { mutableStateOf("") }
    var editableGrams by remember { mutableDoubleStateOf(200.0) }
    var editableCaloriesStr by remember { mutableStateOf("") }
    var editableProteinStr by remember { mutableStateOf("") }
    var editableCarbsStr by remember { mutableStateOf("") }
    var editableFatStr by remember { mutableStateOf("") }
    var editableFiberStr by remember { mutableStateOf("") }
    var macrosManuallyEdited by remember { mutableStateOf(false) }
    var selectedMealType by remember { mutableStateOf("LUNCH") }

    fun syncScaledEstimates(res: com.example.calofit.data.ai.FoodImageAnalysisResult, grams: Double) {
        val ratio = grams / res.estimatedGrams.coerceAtLeast(10.0)
        editableCaloriesStr = ((res.caloriesForPortion * ratio).roundToInt()).toString()
        editableProteinStr = (((res.proteinGrams * ratio) * 10.0).roundToInt() / 10.0).toString()
        editableCarbsStr = (((res.carbsGrams * ratio) * 10.0).roundToInt() / 10.0).toString()
        editableFatStr = (((res.fatGrams * ratio) * 10.0).roundToInt() / 10.0).toString()
        editableFiberStr = (((res.fiberGrams * ratio) * 10.0).roundToInt() / 10.0).toString()
    }

    LaunchedEffect(state.result) {
        val res = state.result
        if (res != null) {
            editableFoodName = if (isPersian) {
                res.foodNameFa.ifBlank { res.foodNameEn }
            } else {
                res.foodNameEn.ifBlank { res.foodNameFa }
            }
            val initialGrams = res.estimatedGrams.coerceIn(20.0, 1000.0)
            editableGrams = initialGrams
            macrosManuallyEdited = false
            syncScaledEstimates(res, initialGrams)
        }
    }

    val mealOptions = listOf(
        "BREAKFAST" to s("meal_breakfast"),
        "LUNCH" to s("meal_lunch"),
        "DINNER" to s("meal_dinner"),
        "SNACK" to s("meal_snack")
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
            .imePadding()
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
            Column {
                Text(
                    text = s("ai_food_scanner"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryText
                )
                Text(
                    text = s("ai_scan_internet_notice"),
                    fontSize = 11.sp,
                    color = palette.secondaryText
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        CaloFitCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    state.previewBitmap != null -> {
                        Image(
                            bitmap = state.previewBitmap.asImageBitmap(),
                            contentDescription = s("ai_food_scanner"),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(14.dp))
                        )
                        if (state.isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.Black.copy(alpha = 0.55f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = palette.primaryAccent)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = s("analyzing_image"),
                                        fontSize = 13.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(palette.primaryAccent.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = palette.accentTextOnLight,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = s("ai_scan_subtitle"),
                                fontSize = 13.sp,
                                color = palette.secondaryText,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (permissionDeniedNote) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CaloFitError.copy(alpha = 0.14f))
                    .border(1.dp, CaloFitError.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = s("camera_permission_denied"),
                    fontSize = 12.sp,
                    color = palette.primaryText
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (!state.isLoading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CaloFitButton(
                    text = if (state.previewBitmap == null) s("take_photo") else s("replace_image"),
                    icon = Icons.Default.CameraAlt,
                    isPrimary = state.previewBitmap == null,
                    onClick = { launchCameraFlow() },
                    modifier = Modifier.weight(1f),
                    testTag = "btn_camera_scan"
                )
                CaloFitButton(
                    text = s("choose_gallery"),
                    icon = Icons.Default.PhotoLibrary,
                    isPrimary = false,
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    testTag = "btn_gallery_scan"
                )
            }
        }

        if (state.previewBitmap != null && !state.isLoading) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CaloFitButton(
                    text = s("analyze_image_btn"),
                    icon = Icons.Default.AutoAwesome,
                    onClick = onAnalyzeClick,
                    modifier = Modifier.weight(2f),
                    testTag = "btn_analyze_food_image"
                )
                CaloFitButton(
                    text = s("remove_image"),
                    icon = Icons.Default.Close,
                    isPrimary = false,
                    onClick = onClearImage,
                    modifier = Modifier.weight(1f),
                    testTag = "btn_clear_scan_image"
                )
            }
        } else if (state.isLoading) {
            Spacer(modifier = Modifier.height(10.dp))
            CaloFitButton(
                text = s("cancel_request"),
                icon = Icons.Default.Close,
                isPrimary = false,
                onClick = onCancelRequest,
                modifier = Modifier.fillMaxWidth(),
                testTag = "btn_cancel_food_scan"
            )
        }

        if (state.errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
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
                    if (state.selectedImageBytes != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        CaloFitButton(
                            text = s("retry"),
                            isPrimary = false,
                            onClick = onAnalyzeClick,
                            testTag = "btn_retry_food_scan"
                        )
                    }
                }
            }
        }

        val result = state.result
        if (result != null) {
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CaloFitWarning.copy(alpha = 0.15f))
                    .border(1.dp, CaloFitWarning.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFE29828),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = s("estimate_badge"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryText
                        )
                        Text(
                            text = s("ai_estimate_disclaimer"),
                            fontSize = 11.sp,
                            color = palette.primaryText,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val ratio = editableGrams / result.estimatedGrams.coerceAtLeast(10.0)
            val fallbackKcal = (result.caloriesForPortion * ratio).roundToInt()
            val fallbackProtein = ((result.proteinGrams * ratio) * 10).roundToInt() / 10.0
            val fallbackCarbs = ((result.carbsGrams * ratio) * 10).roundToInt() / 10.0
            val fallbackFat = ((result.fatGrams * ratio) * 10).roundToInt() / 10.0
            val fallbackFiber = ((result.fiberGrams * ratio) * 10).roundToInt() / 10.0

            val parsedKcal = JalaliCalendarHelper.toAsciiDigits(editableCaloriesStr).trim().toIntOrNull()?.coerceIn(0, 15000) ?: fallbackKcal
            val parsedProtein = JalaliCalendarHelper.toAsciiDigits(editableProteinStr).replace('٫', '.').trim().toDoubleOrNull()?.coerceIn(0.0, 1000.0) ?: fallbackProtein
            val parsedCarbs = JalaliCalendarHelper.toAsciiDigits(editableCarbsStr).replace('٫', '.').trim().toDoubleOrNull()?.coerceIn(0.0, 1000.0) ?: fallbackCarbs
            val parsedFat = JalaliCalendarHelper.toAsciiDigits(editableFatStr).replace('٫', '.').trim().toDoubleOrNull()?.coerceIn(0.0, 1000.0) ?: fallbackFat
            val parsedFiber = JalaliCalendarHelper.toAsciiDigits(editableFiberStr).replace('٫', '.').trim().toDoubleOrNull()?.coerceIn(0.0, 500.0) ?: fallbackFiber

            CaloFitCard(modifier = Modifier.fillMaxWidth(), isAccent = true) {
                Column {
                    Text(
                        text = s("identified_food_label"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryText
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    CaloFitTextField(
                        value = editableFoodName,
                        onValueChange = { editableFoodName = it },
                        placeholder = s("food_name_placeholder"),
                        testTag = "input_ai_scan_food_name"
                    )

                    if (result.possibleIngredients.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = s("possible_ingredients_label"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.secondaryText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            result.possibleIngredients.forEach { ing ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(palette.cardSurface)
                                        .border(1.dp, palette.cardBorder, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(text = ing, fontSize = 11.sp, color = palette.primaryText)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = s("portion_size"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryText
                        )
                        Text(
                            text = "${n(editableGrams.toInt())} ${s("unit_grams")}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accentTextOnLight
                        )
                    }

                    Slider(
                        value = editableGrams.toFloat(),
                        onValueChange = {
                            val newG = kotlin.math.round(it).toDouble()
                            editableGrams = newG
                            if (!macrosManuallyEdited) {
                                syncScaledEstimates(result, newG)
                            }
                        },
                        valueRange = 20f..1000f,
                        colors = SliderDefaults.colors(
                            thumbColor = palette.primaryAccent,
                            activeTrackColor = palette.primaryAccent
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = s("unit_calories"), fontSize = 11.sp, color = palette.secondaryText)
                            Text(
                                text = n(parsedKcal),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.primaryText
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = s("carbs"), fontSize = 11.sp, color = palette.secondaryText)
                            Text(
                                text = "${StringsProvider.formatDecimal(parsedCarbs, isPersian)} ${s("unit_grams")}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CaloFitMacroCarbs
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = s("protein"), fontSize = 11.sp, color = palette.secondaryText)
                            Text(
                                text = "${StringsProvider.formatDecimal(parsedProtein, isPersian)} ${s("unit_grams")}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CaloFitMacroProtein
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = s("fat"), fontSize = 11.sp, color = palette.secondaryText)
                            Text(
                                text = "${StringsProvider.formatDecimal(parsedFat, isPersian)} ${s("unit_grams")}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CaloFitMacroFat
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = s("editable_nutrition_estimates"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryText
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = s("unit_calories"), fontSize = 11.sp, color = palette.secondaryText)
                            Spacer(modifier = Modifier.height(2.dp))
                            CaloFitTextField(
                                value = editableCaloriesStr,
                                onValueChange = { editableCaloriesStr = it; macrosManuallyEdited = true },
                                placeholder = "0",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                testTag = "input_ai_scan_calories"
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = s("protein_label"), fontSize = 11.sp, color = palette.secondaryText)
                            Spacer(modifier = Modifier.height(2.dp))
                            CaloFitTextField(
                                value = editableProteinStr,
                                onValueChange = { editableProteinStr = it; macrosManuallyEdited = true },
                                placeholder = "0",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                testTag = "input_ai_scan_protein"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = s("carbs_label"), fontSize = 11.sp, color = palette.secondaryText)
                            Spacer(modifier = Modifier.height(2.dp))
                            CaloFitTextField(
                                value = editableCarbsStr,
                                onValueChange = { editableCarbsStr = it; macrosManuallyEdited = true },
                                placeholder = "0",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                testTag = "input_ai_scan_carbs"
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = s("fat_label"), fontSize = 11.sp, color = palette.secondaryText)
                            Spacer(modifier = Modifier.height(2.dp))
                            CaloFitTextField(
                                value = editableFatStr,
                                onValueChange = { editableFatStr = it; macrosManuallyEdited = true },
                                placeholder = "0",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                testTag = "input_ai_scan_fat"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = s("vision_fiber_label"), fontSize = 11.sp, color = palette.secondaryText)
                    Spacer(modifier = Modifier.height(2.dp))
                    CaloFitTextField(
                        value = editableFiberStr,
                        onValueChange = { editableFiberStr = it; macrosManuallyEdited = true },
                        placeholder = "0",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        testTag = "input_ai_scan_fiber"
                    )

                    if (result.confidenceNote.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = result.confidenceNote,
                            fontSize = 11.sp,
                            color = palette.secondaryText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = s("select_meal"),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                mealOptions.forEach { (type, label) ->
                    val isSelected = selectedMealType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) palette.primaryAccent else palette.cardSurface)
                            .border(1.dp, if (isSelected) palette.primaryAccent else palette.cardBorder, RoundedCornerShape(12.dp))
                            .clickable { selectedMealType = type }
                            .padding(vertical = 8.dp),
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

            CaloFitButton(
                text = s("add_to_meal"),
                icon = Icons.Default.Check,
                enabled = editableFoodName.isNotBlank(),
                onClick = {
                    val loggedPortion = editableGrams.coerceAtLeast(10.0)
                    val finalEn = if (isPersian) result.foodNameEn.ifBlank { editableFoodName } else editableFoodName
                    val finalFa = if (isPersian) editableFoodName else result.foodNameFa.ifBlank { editableFoodName }
                    val food = FoodItemEntity(
                        nameEn = finalEn.trim(),
                        nameFa = finalFa.trim(),
                        category = "IRANIAN_MAINS",
                        servingSizeGrams = loggedPortion,
                        calories = parsedKcal,
                        protein = parsedProtein,
                        carbs = parsedCarbs,
                        fat = parsedFat,
                        fiber = parsedFiber,
                        isIranian = isPersian,
                        isVerified = false
                    )
                    onConfirmLog(food, loggedPortion, selectedMealType)
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "btn_confirm_ai_log"
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

private fun createTempCameraUri(context: Context): Uri {
    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
    val file = File(dir, "food_scan_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
