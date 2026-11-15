package com.example.calofit.ui.components

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calofit.localization.StringsProvider
import com.example.calofit.ui.navigation.NavDestination
import com.example.calofit.ui.theme.LocalCaloFitColors

@Composable
fun CaloFitBottomBar(
    currentDestination: NavDestination,
    onNavigate: (NavDestination) -> Unit,
    labels: Map<NavDestination, String>,
    modifier: Modifier = Modifier
) {
    val palette = LocalCaloFitColors.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = palette.cardSurface,
        tonalElevation = 2.dp,
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 0.5.dp, color = palette.cardBorder)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavDestination.entries.forEach { destination ->
                    val isSelected = currentDestination == destination
                    val icon = when (destination) {
                        NavDestination.HOME -> Icons.Default.Home
                        NavDestination.FOOD_LOG -> Icons.Default.Restaurant
                        NavDestination.PROGRESS -> Icons.Default.Timeline
                        NavDestination.PROFILE -> Icons.Default.Person
                    }

                    val tint = if (isSelected) {
                        palette.accentTextOnLight
                    } else {
                        palette.secondaryText
                    }

                    val pillBg = if (isSelected) {
                        palette.primaryAccent.copy(alpha = if (palette.isDark) 0.22f else 0.35f)
                    } else {
                        Color.Transparent
                    }

                    Row(
                        modifier = Modifier
                            .testTag("nav_tab_${destination.name.lowercase()}")
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(pillBg)
                            .clickable { onNavigate(destination) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = labels[destination],
                            tint = tint,
                            modifier = Modifier.size(22.dp)
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = labels[destination] ?: "",
                                color = tint,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CaloFitTopHeader(
    greeting: String,
    userName: String,
    dateString: String,
    streakDays: Int,
    isPersian: Boolean,
    onStreakClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = LocalCaloFitColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$greeting ${userName.trim()}".trim(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = palette.primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = palette.secondaryText,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = dateString,
                    fontSize = 12.sp,
                    color = palette.secondaryText,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .defaultMinSize(minHeight = 40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFFF9500).copy(alpha = 0.16f))
                .border(1.dp, Color(0xFFFF9500).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                .clickable(onClick = onStreakClick)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = StringsProvider.get("streak_days_label", isPersian),
                    tint = Color(0xFFFF9500),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = StringsProvider.formatNumber(streakDays, isPersian),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryText
                )
            }
        }
    }
}
