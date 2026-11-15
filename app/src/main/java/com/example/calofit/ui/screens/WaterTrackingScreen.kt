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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calofit.localization.StringsProvider
import com.example.calofit.ui.components.CaloFitCard
import com.example.calofit.ui.theme.CaloFitWaterBlue
import com.example.calofit.ui.theme.LocalCaloFitColors

@Composable
fun WaterTrackingScreen(
    currentWaterMl: Int,
    targetWaterMl: Int,
    isPersian: Boolean,
    onAddWater: (Int) -> Unit,
    onBackClick: () -> Unit
) {
    val palette = LocalCaloFitColors.current
    fun s(key: String): String = StringsProvider.get(key, isPersian)
    fun n(v: Number): String = StringsProvider.formatNumber(v, isPersian)

    val percentage = if (targetWaterMl > 0) {
        ((currentWaterMl.toFloat() / targetWaterMl) * 100).toInt().coerceIn(0, 100)
    } else 0

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
                text = s("water_tracker"),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        CaloFitCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(CaloFitWaterBlue.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Opacity,
                        contentDescription = s("water_tracker"),
                        tint = CaloFitWaterBlue,
                        modifier = Modifier.size(50.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = n(currentWaterMl),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = CaloFitWaterBlue
                )
                Text(
                    text = "${s("water_goal")}: ${n(targetWaterMl)} ${s("unit_ml")} (${n(percentage)}%)",
                    fontSize = 14.sp,
                    color = palette.secondaryText,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(palette.cardSurfaceSubtle)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(percentage / 100f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(CaloFitWaterBlue)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = s("quick_add"),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = palette.primaryText
        )

        Spacer(modifier = Modifier.height(12.dp))

        val waterPresets = listOf(150, 250, 500, 750)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            waterPresets.forEach { amount ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_water_$amount")
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.cardSurface)
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                        .clickable { onAddWater(amount) }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.LocalDrink,
                            contentDescription = null,
                            tint = CaloFitWaterBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "+${n(amount)}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.primaryText
                        )
                        Text(
                            text = s("unit_ml"),
                            fontSize = 10.sp,
                            color = palette.secondaryText
                        )
                    }
                }
            }
        }
    }
}
