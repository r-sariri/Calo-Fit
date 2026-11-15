package com.example.calofit.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calofit.localization.StringsProvider
import com.example.calofit.ui.theme.LocalCaloFitColors

@Composable
fun CaloFitCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    isAccent: Boolean = false,
    content: @Composable () -> Unit
) {
    val palette = LocalCaloFitColors.current

    val backgroundColor = if (isAccent) {
        palette.primaryAccent.copy(alpha = if (palette.isDark) 0.14f else 0.18f)
    } else {
        palette.cardSurface
    }

    val borderColor = if (isAccent) {
        palette.primaryAccent.copy(alpha = 0.45f)
    } else {
        palette.cardBorder
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun CaloFitButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    testTag: String = "calofit_button"
) {
    val palette = LocalCaloFitColors.current

    val backgroundColor = when {
        !enabled -> palette.cardBorder.copy(alpha = 0.6f)
        isPrimary -> palette.primaryAccent
        else -> palette.cardSurface
    }

    val contentColor = when {
        !enabled -> palette.secondaryText
        isPrimary -> palette.onPrimaryAccent
        else -> palette.primaryText
    }

    val borderColor = when {
        !enabled -> palette.cardBorder
        isPrimary -> palette.primaryAccent
        else -> palette.cardBorder
    }

    Box(
        modifier = modifier
            .testTag(testTag)
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = contentColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CaloFitTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isError: Boolean = false,
    testTag: String = "calofit_textfield"
) {
    val palette = LocalCaloFitColors.current

    Row(
        modifier = modifier
            .testTag(testTag)
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(palette.cardSurfaceSubtle)
            .border(
                width = 1.dp,
                color = if (isError) Color(0xFFE46C79) else palette.cardBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = palette.secondaryText,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = palette.secondaryText.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = palette.primaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(palette.primaryAccent),
                singleLine = singleLine,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun CircularCalorieMeter(
    remainingKcal: Int,
    targetKcal: Int,
    consumedKcal: Int,
    isPersian: Boolean,
    modifier: Modifier = Modifier,
    remainingLabel: String = "Remaining",
    kcalLabel: String = "Calories"
) {
    val palette = LocalCaloFitColors.current
    val progress = if (targetKcal > 0) {
        (consumedKcal.toFloat() / targetKcal.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600),
        label = "calorie_progress"
    )

    Box(
        modifier = modifier.size(184.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(174.dp)) {
            val strokeWidth = 14.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val radius = diameter / 2f

            drawCircle(
                color = palette.primaryAccent.copy(alpha = if (palette.isDark) 0.22f else 0.16f),
                radius = radius,
                style = Stroke(width = strokeWidth)
            )

            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        palette.primaryAccent,
                        palette.secondaryAccent,
                        palette.primaryAccent
                    )
                ),
                startAngle = -90f,
                sweepAngle = animatedProgress * 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = StringsProvider.formatNumber(remainingKcal, isPersian),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = remainingLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = palette.secondaryText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.primaryAccent.copy(alpha = 0.22f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = kcalLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.accentTextOnLight
                )
            }
        }
    }
}

@Composable
fun MacroProgressBar(
    label: String,
    currentGrams: Double,
    targetGrams: Int,
    color: Color,
    modifier: Modifier = Modifier,
    unitLabel: String = "grams",
    isPersian: Boolean = false
) {
    val palette = LocalCaloFitColors.current
    val progress = if (targetGrams > 0) {
        (currentGrams / targetGrams).toFloat().coerceIn(0f, 1f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 500),
        label = "macro_progress"
    )

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.primaryText
                )
            }
            val currentStr = StringsProvider.formatNumber(currentGrams.toInt(), isPersian)
            val targetStr = StringsProvider.formatNumber(targetGrams, isPersian)
            val formattedAmount = if (isPersian) {
                "$currentStr از $targetStr $unitLabel"
            } else {
                "$currentStr / $targetStr $unitLabel"
            }
            Text(
                text = formattedAmount,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = palette.secondaryText
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (palette.isDark) Color(0x33283833) else Color(0x1F000000))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}
