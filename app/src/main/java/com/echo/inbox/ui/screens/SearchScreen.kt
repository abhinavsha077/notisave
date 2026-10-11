package com.echo.inbox.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.echo.inbox.ui.components.AppIcon
import com.echo.inbox.ui.components.ContactAvatar
import com.echo.inbox.ui.components.EchoFilterChip
import com.echo.inbox.ui.components.EchoSearchField
import com.echo.inbox.ui.components.EmptyState
import com.echo.inbox.ui.components.fullDateTime
import com.echo.inbox.ui.theme.EchoTheme
import com.echo.inbox.ui.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onConversationClick: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var appMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = EchoTheme.colors.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = EchoTheme.spacing.lg, vertical = EchoTheme.spacing.md),
            ) {
                Text(
                    text = "Search",
                    style = MaterialTheme.typography.titleLarge,
                    color = EchoTheme.colors.primaryText,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "100% on-device full-text index",
                    style = MaterialTheme.typography.bodySmall,
                    color = EchoTheme.colors.secondaryText,
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Search Bar
            Box(Modifier.padding(horizontal = EchoTheme.spacing.lg)) {
                EchoSearchField(
                    query = state.query,
                    onQueryChange = viewModel::setQuery,
                    placeholder = "Search messages, senders, or keywords...",
                )
            }

            // Filters Scrollable Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = EchoTheme.spacing.lg, vertical = EchoTheme.spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(EchoTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    val currentAppLabel = state.apps.firstOrNull { it.packageName == state.selectedPackage }?.displayName ?: "All apps"
                    EchoFilterChip(
                        selected = state.selectedPackage != null,
                        onClick = { appMenuExpanded = true },
                        label = "$currentAppLabel ▾",
                    )
                    DropdownMenu(
                        expanded = appMenuExpanded,
                        onDismissRequest = { appMenuExpanded = false },
                        modifier = Modifier.background(EchoTheme.colors.surface),
                    ) {
                        DropdownMenuItem(
                            text = { Text("All apps", color = EchoTheme.colors.primaryText) },
                            onClick = {
                                viewModel.setPackage(null)
                                appMenuExpanded = false
                            },
                        )
                        state.apps.forEach { app ->
                            DropdownMenuItem(
                                text = { Text(app.displayName, color = EchoTheme.colors.primaryText) },
                                onClick = {
                                    viewModel.setPackage(app.packageName)
                                    appMenuExpanded = false
                                },
                            )
                        }
                    }
                }

                listOf(null to "Any time", 1 to "Today", 7 to "7 days", 30 to "30 days").forEach { (days, label) ->
                    EchoFilterChip(
                        selected = state.periodDays == days,
                        onClick = { viewModel.setPeriodDays(days) },
                        label = label,
                    )
                }
            }

            // Results Counter
            if (state.results.isNotEmpty()) {
                Text(
                    text = "Results (${state.results.size})",
                    modifier = Modifier.padding(horizontal = EchoTheme.spacing.lg, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = EchoTheme.colors.secondaryText,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (state.results.isEmpty()) {
                EmptyState(
                    title = if (state.query.isBlank()) "Search notification history" else "No matching messages",
                    body = if (state.query.isBlank()) {
                        "Enter a query to search across all captured notifications instantly."
                    } else {
                        "Try fewer keywords, select another app, or broaden the date range."
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = EchoTheme.spacing.lg, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(EchoTheme.spacing.md),
                ) {
                    items(state.results, key = { it.messageId }) { result ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(EchoTheme.radius.md))
                                .clickable { onConversationClick(result.conversationId) },
                            shape = RoundedCornerShape(EchoTheme.radius.md),
                            colors = CardDefaults.cardColors(containerColor = EchoTheme.colors.surface),
                            border = BorderStroke(1.dp, EchoTheme.colors.border),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(EchoTheme.spacing.md),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    ContactAvatar(displayName = result.conversationName, size = 42.dp)
                                    Surface(
                                        shape = CircleShape,
                                        color = EchoTheme.colors.surface,
                                        modifier = Modifier.size(16.dp),
                                        border = BorderStroke(1.dp, EchoTheme.colors.surface),
                                    ) {
                                        AppIcon(result.appPackage, result.appName, size = 14.dp)
                                    }
                                }
                                Spacer(Modifier.width(EchoTheme.spacing.md))
                                Column(Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = result.conversationName,
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = EchoTheme.colors.primaryText,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = fullDateTime(result.sourceTimestamp ?: result.capturedAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EchoTheme.colors.secondaryText,
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 2.dp),
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = EchoTheme.colors.blueContainer,
                                        ) {
                                            Text(
                                                text = result.appName,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                color = EchoTheme.colors.signalBlue,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        }
                                        if (!result.sender.isNullOrBlank() && result.sender != result.conversationName) {
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = result.sender,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = EchoTheme.colors.secondaryText,
                                                maxLines = 1,
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(6.dp))

                                    // Highlight matching snippet
                                    HighlightedText(
                                        fullText = result.body,
                                        query = state.query,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HighlightedText(
    fullText: String,
    query: String,
) {
    if (query.isBlank()) {
        Text(
            text = fullText,
            style = MaterialTheme.typography.bodyMedium,
            color = EchoTheme.colors.primaryText,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        return
    }

    val annotated = buildAnnotatedString {
        val lowerText = fullText.lowercase()
        val lowerQuery = query.lowercase().trim()
        var startIndex = 0

        while (startIndex < fullText.length) {
            val matchIndex = lowerText.indexOf(lowerQuery, startIndex)
            if (matchIndex == -1) {
                append(fullText.substring(startIndex))
                break
            }
            if (matchIndex > startIndex) {
                append(fullText.substring(startIndex, matchIndex))
            }
            withStyle(
                SpanStyle(
                    background = EchoTheme.colors.mintContainer,
                    color = EchoTheme.colors.onMintContainer,
                    fontWeight = FontWeight.Bold,
                ),
            ) {
                append(fullText.substring(matchIndex, matchIndex + lowerQuery.length))
            }
            startIndex = matchIndex + lowerQuery.length
        }
    }

    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium,
        color = EchoTheme.colors.primaryText,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )
}
