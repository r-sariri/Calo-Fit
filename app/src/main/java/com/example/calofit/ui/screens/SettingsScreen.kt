package com.example.calofit.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.UploadFile
import com.example.calofit.data.model.UserProfileEntity
import com.example.calofit.data.repository.BackupValidationResult
import com.example.calofit.data.repository.ValidatedBackupPayload
import com.example.calofit.domain.calculator.NutritionCalculator
import com.example.calofit.domain.calendar.JalaliCalendarHelper
import com.example.calofit.localization.StringsProvider
import com.example.calofit.ui.components.CaloFitButton
import com.example.calofit.ui.components.CaloFitCard
import com.example.calofit.ui.components.CaloFitTextField
import com.example.calofit.ui.theme.CaloFitError
import com.example.calofit.ui.theme.CaloFitSuccess
import com.example.calofit.ui.theme.LocalCaloFitColors

@Composable
fun SettingsScreen(
    userProfile: UserProfileEntity?,
    currentLanguage: String,
    currentThemeMode: String,
    isPersian: Boolean,
    onSaveProfile: (
        name: String,
        age: Int,
        gender: NutritionCalculator.Gender,
        heightCm: Double,
        currentWeightKg: Double,
        targetWeightKg: Double,
        activityLevel: NutritionCalculator.ActivityLevel,
        goal: NutritionCalculator.WeightGoal,
        onComplete: () -> Unit
    ) -> Unit,
    onLanguageChange: (String) -> Unit,
    onThemeModeChange: (String) -> Unit,
    onExportDataJson: () -> String,
    onValidateBackupJson: (String) -> BackupValidationResult = { BackupValidationResult.Invalid("NOT_WIRED") },
    onRestoreFromBackup: (ValidatedBackupPayload, () -> Unit) -> Unit = { _, _ -> },
    onBackClick: () -> Unit,
    isEmbeddedInTab: Boolean = false
) {
    val palette = LocalCaloFitColors.current
    val context = LocalContext.current
    fun s(key: String): String = StringsProvider.get(key, isPersian)

    var exportedJsonPreview by remember { mutableStateOf<String?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonInput by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf<String?>(null) }
    var pendingRestorePayload by remember { mutableStateOf<ValidatedBackupPayload?>(null) }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val loaded = runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val maxBytes = 2 * 1024 * 1024
                    val buffer = ByteArray(maxBytes + 1)
                    var totalRead = 0
                    while (totalRead < buffer.size) {
                        val read = stream.read(buffer, totalRead, buffer.size - totalRead)
                        if (read <= 0) break
                        totalRead += read
                    }
                    if (totalRead > maxBytes) null else String(buffer, 0, totalRead, Charsets.UTF_8)
                }
            }.getOrNull()
            if (loaded != null) {
                importJsonInput = loaded
                importError = null
            } else {
                importError = s("err_invalid_backup_json")
            }
        }
    }

    // Editable personal profile state
    var nameInput by remember { mutableStateOf(userProfile?.name ?: "") }
    var ageInput by remember { mutableStateOf(StringsProvider.formatNumber(userProfile?.age ?: 26, isPersian)) }
    var gender by remember {
        mutableStateOf(
            runCatching { NutritionCalculator.Gender.valueOf(userProfile?.gender ?: "MALE") }
                .getOrDefault(NutritionCalculator.Gender.MALE)
        )
    }
    var heightInput by remember { mutableStateOf(StringsProvider.formatNumber((userProfile?.heightCm ?: 175.0).toInt(), isPersian)) }
    var currentWeightInput by remember { mutableStateOf(StringsProvider.formatDecimal(userProfile?.currentWeightKg ?: 70.0, isPersian)) }
    var targetWeightInput by remember { mutableStateOf(StringsProvider.formatDecimal(userProfile?.targetWeightKg ?: 68.0, isPersian)) }
    var activityLevel by remember {
        mutableStateOf(
            runCatching { NutritionCalculator.ActivityLevel.valueOf(userProfile?.activityLevel ?: "MODERATELY_ACTIVE") }
                .getOrDefault(NutritionCalculator.ActivityLevel.MODERATELY_ACTIVE)
        )
    }
    var goal by remember {
        mutableStateOf(
            runCatching { NutritionCalculator.WeightGoal.valueOf(userProfile?.goal ?: "LOSE_WEIGHT") }
                .getOrDefault(NutritionCalculator.WeightGoal.LOSE_WEIGHT)
        )
    }
    var profileError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userProfile, isPersian) {
        if (userProfile != null) {
            nameInput = userProfile.name
            ageInput = StringsProvider.formatNumber(userProfile.age, isPersian)
            gender = runCatching { NutritionCalculator.Gender.valueOf(userProfile.gender) }
                .getOrDefault(NutritionCalculator.Gender.MALE)
            heightInput = StringsProvider.formatNumber(userProfile.heightCm.toInt(), isPersian)
            currentWeightInput = StringsProvider.formatDecimal(userProfile.currentWeightKg, isPersian)
            targetWeightInput = StringsProvider.formatDecimal(userProfile.targetWeightKg, isPersian)
            activityLevel = runCatching { NutritionCalculator.ActivityLevel.valueOf(userProfile.activityLevel) }
                .getOrDefault(NutritionCalculator.ActivityLevel.MODERATELY_ACTIVE)
            goal = runCatching { NutritionCalculator.WeightGoal.valueOf(userProfile.goal) }
                .getOrDefault(NutritionCalculator.WeightGoal.LOSE_WEIGHT)
        }
    }

    val baseModifier = if (isEmbeddedInTab) {
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(palette.mainBackground, palette.secondaryBackground)))
            .statusBarsPadding()
            .imePadding()
    } else {
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(palette.mainBackground, palette.secondaryBackground)))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    }

    Column(
        modifier = baseModifier
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
    ) {
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
                text = s("settings_title"),
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 100% Free App Badge
        CaloFitCard(modifier = Modifier.fillMaxWidth(), isAccent = true) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = s("free_app_badge"),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryText,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = s("app_version_value"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.accentTextOnLight
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Editable Personal Information & Goal Section
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = palette.accentTextOnLight,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = s("personal_info_title"),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        CaloFitCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column {
                    Text(text = s("name_label"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = palette.secondaryText)
                    Spacer(modifier = Modifier.height(4.dp))
                    CaloFitTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it; profileError = null },
                        placeholder = s("name_placeholder"),
                        testTag = "input_profile_name"
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = s("age_label"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = palette.secondaryText)
                        Spacer(modifier = Modifier.height(4.dp))
                        CaloFitTextField(
                            value = ageInput,
                            onValueChange = { ageInput = it; profileError = null },
                            placeholder = s("age_placeholder"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            testTag = "input_profile_age"
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = s("height_label"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = palette.secondaryText)
                        Spacer(modifier = Modifier.height(4.dp))
                        CaloFitTextField(
                            value = heightInput,
                            onValueChange = { heightInput = it; profileError = null },
                            placeholder = s("height_placeholder"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            testTag = "input_profile_height"
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = s("weight_label"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = palette.secondaryText)
                        Spacer(modifier = Modifier.height(4.dp))
                        CaloFitTextField(
                            value = currentWeightInput,
                            onValueChange = { currentWeightInput = it; profileError = null },
                            placeholder = s("weight_placeholder"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            testTag = "input_profile_weight"
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = s("target_weight_label"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = palette.secondaryText)
                        Spacer(modifier = Modifier.height(4.dp))
                        CaloFitTextField(
                            value = targetWeightInput,
                            onValueChange = { targetWeightInput = it; profileError = null },
                            placeholder = s("target_weight_placeholder"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            testTag = "input_profile_target_weight"
                        )
                    }
                }

                Text(text = s("gender_label"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = palette.secondaryText)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        NutritionCalculator.Gender.MALE to s("gender_male"),
                        NutritionCalculator.Gender.FEMALE to s("gender_female")
                    ).forEach { (g, label) ->
                        val selected = gender == g
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) palette.primaryAccent else palette.cardSurfaceSubtle)
                                .border(1.dp, if (selected) palette.primaryAccent else palette.cardBorder, RoundedCornerShape(12.dp))
                                .clickable { gender = g }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) palette.onPrimaryAccent else palette.primaryText
                            )
                        }
                    }
                }

                Text(text = s("activity_level_label"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = palette.secondaryText)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        NutritionCalculator.ActivityLevel.SEDENTARY to s("act_sedentary"),
                        NutritionCalculator.ActivityLevel.LIGHTLY_ACTIVE to s("act_light"),
                        NutritionCalculator.ActivityLevel.MODERATELY_ACTIVE to s("act_moderate"),
                        NutritionCalculator.ActivityLevel.VERY_ACTIVE to s("act_heavy"),
                        NutritionCalculator.ActivityLevel.EXTRA_ACTIVE to s("act_extra_active")
                    ).forEach { (level, title) ->
                        ThemeRadioRow(
                            title = title,
                            isSelected = activityLevel == level,
                            onClick = { activityLevel = level },
                            testTag = "opt_activity_${level.name.lowercase()}"
                        )
                    }
                }

                Text(text = s("goal_label"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = palette.secondaryText)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        NutritionCalculator.WeightGoal.LOSE_WEIGHT to s("goal_lose"),
                        NutritionCalculator.WeightGoal.MAINTAIN_WEIGHT to s("goal_maintain"),
                        NutritionCalculator.WeightGoal.GAIN_WEIGHT to s("goal_gain")
                    ).forEach { (gOption, title) ->
                        val selected = goal == gOption
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) palette.primaryAccent else palette.cardSurfaceSubtle)
                                .border(1.dp, if (selected) palette.primaryAccent else palette.cardBorder, RoundedCornerShape(12.dp))
                                .clickable { goal = gOption }
                                .padding(horizontal = 6.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) palette.onPrimaryAccent else palette.primaryText
                            )
                        }
                    }
                }

                if (profileError != null) {
                    Text(
                        text = profileError.orEmpty(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CaloFitError
                    )
                }

                CaloFitButton(
                    text = s("save_profile"),
                    icon = Icons.Default.Check,
                    onClick = {
                        val trimmedName = nameInput.trim()
                        val ageVal = JalaliCalendarHelper.toAsciiDigits(ageInput).trim().toIntOrNull()
                        val heightVal = JalaliCalendarHelper.toAsciiDigits(heightInput).replace('٫', '.').trim().toDoubleOrNull()
                        val weightVal = JalaliCalendarHelper.toAsciiDigits(currentWeightInput).replace('٫', '.').trim().toDoubleOrNull()
                        val targetWeightVal = JalaliCalendarHelper.toAsciiDigits(targetWeightInput).replace('٫', '.').trim().toDoubleOrNull()

                        when {
                            trimmedName.isBlank() -> profileError = s("err_name_required")
                            ageVal == null || !NutritionCalculator.isValidAge(ageVal) -> profileError = s("err_age_invalid")
                            heightVal == null || !NutritionCalculator.isValidHeightCm(heightVal) -> profileError = s("err_height_invalid")
                            weightVal == null || !NutritionCalculator.isValidWeightKg(weightVal) -> profileError = s("err_weight_invalid")
                            targetWeightVal == null || !NutritionCalculator.isValidWeightKg(targetWeightVal) -> profileError = s("err_target_weight_invalid")
                            else -> {
                                profileError = null
                                onSaveProfile(
                                    trimmedName,
                                    ageVal,
                                    gender,
                                    heightVal,
                                    weightVal,
                                    targetWeightVal,
                                    activityLevel,
                                    goal
                                ) {
                                    Toast.makeText(context, s("saved_successfully"), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "btn_save_personal_profile"
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Appearance Settings (ONLY Light / Dark / System Default)
        Text(
            text = s("appearance"),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = palette.primaryText
        )
        Spacer(modifier = Modifier.height(8.dp))

        CaloFitCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeRadioRow(
                    title = s("theme_light"),
                    isSelected = currentThemeMode == "LIGHT",
                    onClick = { onThemeModeChange("LIGHT") },
                    testTag = "opt_theme_light"
                )
                ThemeRadioRow(
                    title = s("theme_dark"),
                    isSelected = currentThemeMode == "DARK",
                    onClick = { onThemeModeChange("DARK") },
                    testTag = "opt_theme_dark"
                )
                ThemeRadioRow(
                    title = s("theme_system"),
                    isSelected = currentThemeMode == "SYSTEM",
                    onClick = { onThemeModeChange("SYSTEM") },
                    testTag = "opt_theme_system"
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Language Settings (strictly Persian | English; digits adapt automatically)
        Text(
            text = s("regional_settings"),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = palette.primaryText
        )
        Spacer(modifier = Modifier.height(8.dp))

        CaloFitCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeRadioRow(
                    title = s("lang_persian_label"),
                    isSelected = currentLanguage == "fa",
                    onClick = { onLanguageChange("fa") },
                    testTag = "opt_lang_fa"
                )
                ThemeRadioRow(
                    title = s("lang_english_label"),
                    isSelected = currentLanguage == "en",
                    onClick = { onLanguageChange("en") },
                    testTag = "opt_lang_en"
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Backup & JSON Export
        Text(
            text = s("backup_export"),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = palette.primaryText
        )
        Spacer(modifier = Modifier.height(8.dp))

        CaloFitCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = palette.accentTextOnLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = s("export_json"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryText
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isPersian) {
                        "تهیه خروجی کامل JSON از پروفایل، تاریخچه غذا، وزن، آب و فعالیت‌ها."
                    } else {
                        "Generate a complete JSON backup of your profile, food logs, weight history, hydration, and activity records."
                    },
                    fontSize = 12.sp,
                    color = palette.secondaryText,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                CaloFitButton(
                    text = s("export_json"),
                    onClick = { exportedJsonPreview = onExportDataJson() },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "btn_export_json"
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = s("import_json_desc"),
                    fontSize = 12.sp,
                    color = palette.secondaryText,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                CaloFitButton(
                    text = s("import_json"),
                    icon = Icons.Default.UploadFile,
                    isPrimary = false,
                    onClick = {
                        importError = null
                        showImportDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "btn_open_import_json"
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        CaloFitCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = CaloFitSuccess,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = s("privacy_policy"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryText
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = s("privacy_desc"),
                    fontSize = 12.sp,
                    color = palette.secondaryText,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(palette.cardSurfaceSubtle)
                .border(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFE29828),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = s("medical_disclaimer"),
                    fontSize = 11.sp,
                    color = palette.secondaryText,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    if (exportedJsonPreview != null) {
        val jsonText = exportedJsonPreview.orEmpty()
        AlertDialog(
            onDismissRequest = { exportedJsonPreview = null },
            title = { Text(text = s("export_json"), fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = jsonText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = palette.primaryText
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("KaloFit JSON", jsonText))
                            Toast.makeText(context, s("copied"), Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text(text = s("copy"))
                    }
                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_TEXT, jsonText)
                            }
                            context.startActivity(Intent.createChooser(intent, s("export_json")))
                        }
                    ) {
                        Text(text = s("share"))
                    }
                    TextButton(onClick = { exportedJsonPreview = null }) {
                        Text(text = s("close"))
                    }
                }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(text = s("import_json"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CaloFitButton(
                        text = s("choose_json_file"),
                        icon = Icons.Default.UploadFile,
                        isPrimary = false,
                        onClick = {
                            openDocumentLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "btn_choose_json_file"
                    )
                    Text(
                        text = s("paste_json_label"),
                        fontSize = 12.sp,
                        color = palette.secondaryText
                    )
                    CaloFitTextField(
                        value = importJsonInput,
                        onValueChange = { importJsonInput = it; importError = null },
                        placeholder = s("paste_json_placeholder"),
                        singleLine = false,
                        testTag = "input_import_json"
                    )
                    if (importError != null) {
                        Text(
                            text = importError.orEmpty(),
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
                        when (val validation = onValidateBackupJson(importJsonInput)) {
                            is BackupValidationResult.Valid -> {
                                importError = null
                                showImportDialog = false
                                pendingRestorePayload = validation.payload
                            }
                            is BackupValidationResult.Invalid -> {
                                importError = s("err_invalid_backup_json")
                            }
                        }
                    },
                    modifier = Modifier.testTag("btn_validate_import_json")
                ) {
                    Text(text = s("validate_and_restore"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text(text = s("close"))
                }
            }
        )
    }

    val toRestore = pendingRestorePayload
    if (toRestore != null) {
        AlertDialog(
            onDismissRequest = { pendingRestorePayload = null },
            title = { Text(text = s("confirm_restore_title"), fontWeight = FontWeight.Bold) },
            text = { Text(text = s("confirm_restore_message")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRestorePayload = null
                        onRestoreFromBackup(toRestore) {
                            importJsonInput = ""
                            Toast.makeText(context, s("restore_success"), Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("btn_confirm_restore_overwrite")
                ) {
                    Text(text = s("confirm_restore_btn"), color = CaloFitError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestorePayload = null }) {
                    Text(text = s("close"))
                }
            }
        )
    }
}

@Composable
private fun ThemeRadioRow(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val palette = LocalCaloFitColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .defaultMinSize(minHeight = 44.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = palette.primaryText,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(palette.primaryAccent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = palette.onPrimaryAccent,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
