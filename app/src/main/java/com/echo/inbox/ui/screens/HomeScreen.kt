package com.echo.inbox.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.echo.inbox.ui.components.AppSourceChip
import com.echo.inbox.ui.components.CaptureHealthPill
import com.echo.inbox.ui.components.ConversationCard
import com.echo.inbox.ui.components.EchoFilterChip
import com.echo.inbox.ui.components.EmptyState
import com.echo.inbox.ui.components.SectionHeader
import com.echo.inbox.ui.theme.EchoTheme
import com.echo.inbox.ui.viewmodel.HomeFilter
import com.echo.inbox.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onConversationClick: (Long) -> Unit,
    onAppClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    openNotificationAccessSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshAccess()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isHealthy = state.accessGranted && state.health.listenerConnected

    Scaffold(
        containerColor = EchoTheme.colors.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = EchoTheme.spacing.lg, vertical = EchoTheme.spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Inbox",
                        style = MaterialTheme.typography.titleLarge,
                        color = EchoTheme.colors.primaryText,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Private notification archive",
                        style = MaterialTheme.typography.bodySmall,
                        color = EchoTheme.colors.secondaryText,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(EchoTheme.spacing.sm),
                ) {
                    CaptureHealthPill(status = if (isHealthy) "HEALTHY" else "DEGRADED")
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = EchoTheme.colors.primaryText,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(EchoTheme.spacing.md),
        ) {
            if (!state.accessGranted) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = EchoTheme.spacing.lg)
                            .clip(RoundedCornerShape(EchoTheme.radius.md))
                            .clickable(onClick = openNotificationAccessSettings),
                        shape = RoundedCornerShape(EchoTheme.radius.md),
                        colors = CardDefaults.cardColors(containerColor = EchoTheme.colors.dangerContainer),
                        border = BorderStroke(1.dp, EchoTheme.colors.danger.copy(alpha = 0.5f)),
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(EchoTheme.spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.NotificationsOff,
                                contentDescription = null,
                                tint = EchoTheme.colors.danger,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(Modifier.width(EchoTheme.spacing.md))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Notification access is off",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = EchoTheme.colors.danger,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "Capture is paused. Tap to enable Android notification access.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EchoTheme.colors.secondaryText,
                                )
                            }
                        }
                    }
                }
            }

            // Quick Search Box
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = EchoTheme.spacing.lg)
                        .clip(RoundedCornerShape(EchoTheme.radius.md))
                        .clickable(onClick = onSearchClick),
                    shape = RoundedCornerShape(EchoTheme.radius.md),
                    colors = CardDefaults.cardColors(containerColor = EchoTheme.colors.surface),
                    border = BorderStroke(1.dp, EchoTheme.colors.border),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = EchoTheme.spacing.md, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = EchoTheme.colors.secondaryText,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(EchoTheme.spacing.md))
                        Text(
                            text = "Search messages...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = EchoTheme.colors.secondaryText,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = "Filters",
                            tint = EchoTheme.colors.secondaryText,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            // Category Filter Chips
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = EchoTheme.spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(EchoTheme.spacing.sm),
                ) {
                    items(HomeFilter.entries) { filter ->
                        EchoFilterChip(
                            selected = state.filter == filter,
                            onClick = { viewModel.setFilter(filter) },
                            label = filter.label,
                        )
                    }
                }
            }

            // Applications Horizontal Carousel
            if (state.apps.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Applications",
                        subtitle = "Captured sources",
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = EchoTheme.spacing.lg),
                        horizontalArrangement = Arrangement.spacedBy(EchoTheme.spacing.sm),
                    ) {
                        items(state.apps, key = { it.packageName }) { app ->
                            AppSourceChip(
                                packageName = app.packageName,
                                displayName = app.displayName,
                                eventCount = app.eventCount,
                                onClick = { onAppClick(app.packageName) },
                            )
                        }
                    }
                }
            }

            // Conversations Section
            item {
                SectionHeader(
                    title = "Conversations",
                    subtitle = if (state.conversations.isNotEmpty()) "${state.conversations.size} active threads" else null,
                )
            }

            if (state.conversations.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                    ) {
                        EmptyState(
                            title = "No captured conversations yet",
                            body = "Send a notification from a selected app, or use Capture Health to run a diagnostic fixture.",
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            } else {
                items(state.conversations, key = { it.id }) { conversation ->
                    ConversationCard(
                        conversation = conversation,
                        onClick = { onConversationClick(conversation.id) },
                        modifier = Modifier.padding(horizontal = EchoTheme.spacing.lg),
                    )
                }
            }
        }
    }
}
