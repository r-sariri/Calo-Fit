package com.example.calofit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calofit.data.model.AchievementEntity
import com.example.calofit.domain.calendar.JalaliCalendarHelper
import com.example.calofit.localization.StringsProvider
import com.example.calofit.ui.components.CaloFitCard
import com.example.calofit.ui.theme.LocalCaloFitColors
import com.example.calofit.ui.theme.TierBronze
import com.example.calofit.ui.theme.TierGold
import com.example.calofit.ui.theme.TierSilver

@Composable
fun AchievementsScreen(
    achievements: List<AchievementEntity>,
    currentStreakDays: Int,
    longestStreakDays: Int,
    isPersian: Boolean,
    onBackClick: () -> Unit
) {
    val palette = LocalCaloFitColors.current
    fun s(key: String): String = StringsProvider.get(key, isPersian)
    fun n(v: Number): String = StringsProvider.formatNumber(v, isPersian)

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
                text = s("achievements_title"),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        CaloFitCard(modifier = Modifier.fillMaxWidth(), isAccent = true) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF9500).copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = s("streak_days_label"),
                        tint = Color(0xFFFF9500),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${n(currentStreakDays)} ${s("streak_days_label")}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.primaryText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${s("best_streak_label")}: ${n(longestStreakDays)} ${if (isPersian) "روز" else "days"}",
                        fontSize = 12.sp,
                        color = palette.secondaryText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(achievements, key = { it.code }) { item ->
                val icon = when (item.iconName) {
                    "flag" -> Icons.Default.Flag
                    "water_drop" -> Icons.Default.WaterDrop
                    "local_fire_department" -> Icons.Default.LocalFireDepartment
                    "emoji_events" -> Icons.Default.EmojiEvents
                    "monitor_weight" -> Icons.Default.MonitorWeight
                    else -> Icons.Default.PieChart
                }

                val (tierColor, tierLabel) = when (item.tier) {
                    "GOLD" -> TierGold to s("tier_gold")
                    "SILVER" -> TierSilver to s("tier_silver")
                    else -> TierBronze to s("tier_bronze")
                }

                val target = item.progressTarget.coerceAtLeast(1)
                val progressFrac = if (item.isUnlocked) 1f else (item.progressCurrent.toFloat() / target).coerceIn(0f, 1f)

                CaloFitCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 18.dp
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (item.isUnlocked) tierColor.copy(alpha = 0.22f)
                                        else palette.cardSurfaceSubtle
                                    )
                                    .border(
                                        1.dp,
                                        if (item.isUnlocked) tierColor else palette.cardBorder,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = tierLabel,
                                    tint = if (item.isUnlocked) tierColor else palette.secondaryText.copy(alpha = 0.55f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isPersian) item.titleFa else item.titleEn,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.primaryText,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(tierColor.copy(alpha = 0.16f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = tierLabel,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = tierColor
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (isPersian) item.descriptionFa else item.descriptionEn,
                                    fontSize = 11.sp,
                                    color = palette.secondaryText
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (item.isUnlocked) palette.primaryAccent.copy(alpha = 0.24f)
                                        else palette.cardSurfaceSubtle
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (item.isUnlocked) s("status_unlocked") else s("status_locked"),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.isUnlocked) palette.accentTextOnLight else palette.secondaryText
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(palette.cardSurfaceSubtle)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progressFrac)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (item.isUnlocked) tierColor else palette.primaryAccent)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            val rightText = if (item.isUnlocked && !item.unlockedDate.isNullOrBlank()) {
                                JalaliCalendarHelper.formatIsoShort(item.unlockedDate, isPersian)
                            } else {
                                "${n(item.progressCurrent.coerceAtMost(target))} / ${n(target)}"
                            }
                            Text(
                                text = rightText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.secondaryText
                            )
                        }
                    }
                }
            }
        }
    }
}
