package com.echo.inbox.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.echo.inbox.ui.components.AppIcon
import com.echo.inbox.ui.components.CaptureHealthPill
import com.echo.inbox.ui.components.ConversationCard
import com.echo.inbox.ui.components.EchoFilterChip
import com.echo.inbox.ui.components.EmptyState
import com.echo.inbox.ui.theme.EchoTheme
import com.echo.inbox.ui.viewmodel.AppDetailViewModel

@Composable
fun AppDetailScreen(
    viewModel: AppDetailViewModel,
    onBack: () -> Unit,
    onConversationClick: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val displayName = state.app?.displayName ?: state.packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
    val pkgName = state.app?.packageName ?: state.packageName
    val totalEvents = state.app?.eventCount ?: state.conversations.sumOf { it.eventCount }

    Scaffold(
        containerColor = EchoTheme.colors.background,
        topBar = {
            Surface(
                color = EchoTheme.colors.surface,
                border = BorderStroke(1.dp, EchoTheme.colors.border),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = EchoTheme.spacing.md, vertical = EchoTheme.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = EchoTheme.colors.primaryText,
                        )
                    }
                    Spacer(Modifier.width(EchoTheme.spacing.sm))
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = EchoTheme.colors.primaryText,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(EchoTheme.spacing.lg),
            verticalArrangement = Arrangement.spacedBy(EchoTheme.spacing.md),
        ) {
            // Hero App Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(EchoTheme.radius.lg),
                    colors = CardDefaults.cardColors(containerColor = EchoTheme.colors.surface),
                    border = BorderStroke(1.dp, EchoTheme.colors.border),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(EchoTheme.spacing.xl),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AppIcon(pkgName, displayName, size = 64.dp)
                        Spacer(Modifier.height(EchoTheme.spacing.md))
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.headlineMedium,
                            color = EchoTheme.colors.primaryText,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(EchoTheme.spacing.xs))
                        CaptureHealthPill(status = "ON_DEVICE")
                        Spacer(Modifier.height(EchoTheme.spacing.sm))
                        Text(
                            text = "Captured $totalEvents events",
                            style = MaterialTheme.typography.bodyMedium,
                            color = EchoTheme.colors.secondaryText,
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Showing all conversations",
                        style = MaterialTheme.typography.titleMedium,
                        color = EchoTheme.colors.primaryText,
                        fontWeight = FontWeight.Bold,
                    )
                    EchoFilterChip(
                        selected = false,
                        onClick = {},
                        label = "Filter",
                        count = state.conversations.size,
                    )
                }
            }

            if (state.conversations.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                    ) {
                        EmptyState(
                            title = "No conversations",
                            body = "This app has not produced any captured message events yet.",
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            } else {
                items(state.conversations, key = { it.id }) { conversation ->
                    ConversationCard(
                        conversation = conversation,
                        onClick = { onConversationClick(conversation.id) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
