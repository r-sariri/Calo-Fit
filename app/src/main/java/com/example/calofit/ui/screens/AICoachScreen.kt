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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calofit.localization.StringsProvider
import com.example.calofit.ui.components.CaloFitButton
import com.example.calofit.ui.components.CaloFitTextField
import com.example.calofit.ui.theme.CaloFitError
import com.example.calofit.ui.theme.CaloFitSuccess
import com.example.calofit.ui.theme.CaloFitWarning
import com.example.calofit.ui.theme.LocalCaloFitColors
import com.example.calofit.ui.viewmodel.AiCoachUiState

@Composable
fun AICoachScreen(
    state: AiCoachUiState,
    isPersian: Boolean,
    onSendMessage: (String) -> Unit,
    onRetryLastMessage: () -> Unit = {},
    onCancelRequest: () -> Unit,
    onClearChat: () -> Unit,
    onRefreshStatus: () -> Unit = {},
    onBackClick: () -> Unit
) {
    val palette = LocalCaloFitColors.current
    fun s(key: String): String = StringsProvider.get(key, isPersian)

    LaunchedEffect(Unit) {
        onRefreshStatus()
    }

    var inputPrompt by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val quickPrompts = if (isPersian) listOf(
        "پیشنهاد ناهار رژیمی ایرانی",
        "کالری قورمه سبزی با کته چقدر است؟",
        "بهترین منابع پروتئین در غذاهای ایرانی",
        "کنترل هوس شیرینی عصرگاهی"
    ) else listOf(
        "Healthy Persian lunch ideas",
        "Calories in Ghormeh Sabzi?",
        "High-protein everyday foods",
        "How to manage sugar cravings"
    )

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = s("ai_coach_title"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.primaryText
                )
                Text(
                    text = s("ai_coach_subtitle"),
                    fontSize = 11.sp,
                    color = palette.secondaryText
                )
            }
            if (state.messages.isNotEmpty()) {
                IconButton(
                    onClick = onClearChat,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(palette.cardSurface)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = s("clear_chat"),
                        tint = palette.primaryText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Medical & Internet Safety Notice
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CaloFitWarning.copy(alpha = 0.14f))
                .border(1.dp, CaloFitWarning.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                .padding(10.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFE29828),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = s("ai_coach_medical_notice"),
                    fontSize = 11.sp,
                    color = palette.primaryText,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val statusColor = if (state.isOffline) CaloFitWarning else CaloFitSuccess
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_coach_connectivity_pill")
                .clip(RoundedCornerShape(10.dp))
                .background(statusColor.copy(alpha = 0.14f))
                .border(1.dp, statusColor.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Text(
                    text = if (state.isOffline) s("ai_status_offline") else s("ai_status_online"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.primaryText
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(quickPrompts) { prompt ->
                Box(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.cardSurface)
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(12.dp))
                        .clickable(enabled = !state.isLoading) { onSendMessage(prompt) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = prompt,
                        fontSize = 11.sp,
                        color = palette.primaryText,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (state.messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(palette.cardSurface)
                            .border(1.dp, palette.cardBorder, RoundedCornerShape(18.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = s("ai_coach_internet_notice"),
                            fontSize = 13.sp,
                            color = palette.secondaryText,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            items(state.messages, key = { it.id }) { msg ->
                val bubbleAlignment = if (msg.isUser) Alignment.End else Alignment.Start
                val bubbleBackground = if (msg.isUser) {
                    palette.primaryAccent.copy(alpha = if (palette.isDark) 0.28f else 0.42f)
                } else {
                    palette.cardSurface
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = bubbleAlignment
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.86f)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 18.dp,
                                    topEnd = 18.dp,
                                    bottomStart = if (msg.isUser) 18.dp else 4.dp,
                                    bottomEnd = if (msg.isUser) 4.dp else 18.dp
                                )
                            )
                            .background(bubbleBackground)
                            .border(
                                1.dp,
                                if (msg.isUser) palette.primaryAccent.copy(alpha = 0.5f) else palette.cardBorder,
                                RoundedCornerShape(18.dp)
                            )
                            .padding(14.dp)
                    ) {
                        Text(
                            text = msg.text,
                            fontSize = 13.sp,
                            color = palette.primaryText,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            if (state.isLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = palette.accentTextOnLight,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = s("connecting_ai"),
                            fontSize = 12.sp,
                            color = palette.secondaryText
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        CaloFitButton(
                            text = s("cancel_request"),
                            icon = Icons.Default.Close,
                            isPrimary = false,
                            onClick = onCancelRequest,
                            testTag = "btn_cancel_coach"
                        )
                    }
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
                            if (state.lastFailedMessage != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                CaloFitButton(
                                    text = s("retry"),
                                    isPrimary = false,
                                    onClick = onRetryLastMessage,
                                    testTag = "btn_retry_coach"
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = s("coach_input_label"),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = palette.secondaryText,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CaloFitTextField(
                value = inputPrompt,
                onValueChange = { inputPrompt = it },
                placeholder = s("ask_coach_hint"),
                modifier = Modifier.weight(1f),
                testTag = "input_ai_coach"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .testTag("btn_send_ai_coach")
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (inputPrompt.isNotBlank() && !state.isLoading) palette.primaryAccent
                        else palette.cardBorder
                    )
                    .clickable(enabled = inputPrompt.isNotBlank() && !state.isLoading) {
                        val toSend = inputPrompt
                        inputPrompt = ""
                        onSendMessage(toSend)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = s("send"),
                    tint = if (inputPrompt.isNotBlank() && !state.isLoading) palette.onPrimaryAccent else palette.secondaryText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
