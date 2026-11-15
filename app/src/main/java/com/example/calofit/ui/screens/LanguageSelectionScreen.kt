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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calofit.localization.StringsProvider
import com.example.calofit.ui.components.CaloFitButton
import com.example.calofit.ui.components.CaloFitCard
import com.example.calofit.ui.theme.LocalCaloFitColors

@Composable
fun LanguageSelectionScreen(
    onLanguageSelected: (String) -> Unit
) {
    val palette = LocalCaloFitColors.current
    var selectedLang by remember { mutableStateOf("fa") }
    val isPersian = selectedLang == "fa"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(palette.mainBackground, palette.secondaryBackground)
                )
            )
            .safeDrawingPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(palette.primaryAccent.copy(alpha = 0.22f))
                    .border(1.5.dp, palette.primaryAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = StringsProvider.get("choose_language", isPersian),
                    tint = palette.accentTextOnLight,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = StringsProvider.get("welcome_title", isPersian),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = StringsProvider.get("choose_language", isPersian),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = palette.secondaryText,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            CaloFitCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LanguageOptionRow(
                        label = StringsProvider.get("lang_persian_label", true),
                        isSelected = selectedLang == "fa",
                        onClick = { selectedLang = "fa" },
                        testTag = "btn_lang_fa"
                    )
                    LanguageOptionRow(
                        label = StringsProvider.get("lang_english_label", false),
                        isSelected = selectedLang == "en",
                        onClick = { selectedLang = "en" },
                        testTag = "btn_lang_en"
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            CaloFitButton(
                text = StringsProvider.get("continue_btn", isPersian),
                onClick = { onLanguageSelected(selectedLang) },
                modifier = Modifier.fillMaxWidth(),
                testTag = "btn_language_continue"
            )
        }
    }
}

@Composable
private fun LanguageOptionRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val palette = LocalCaloFitColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .defaultMinSize(minHeight = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) palette.primaryAccent.copy(alpha = if (palette.isDark) 0.22f else 0.26f)
                else palette.cardSurfaceSubtle
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) palette.primaryAccent else palette.cardBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = palette.primaryText
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isSelected) palette.primaryAccent else Color.Transparent)
                .border(1.5.dp, if (isSelected) palette.primaryAccent else palette.cardBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = palette.onPrimaryAccent,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}
