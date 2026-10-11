package com.echo.inbox.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.echo.inbox.ui.components.AdaptiveMessageItem
import com.echo.inbox.ui.components.AppIcon
import com.echo.inbox.ui.components.ContactAvatar
import com.echo.inbox.ui.components.DateSeparatorPill
import com.echo.inbox.ui.components.EmptyState
import com.echo.inbox.ui.components.formatDateHeader
import com.echo.inbox.ui.theme.EchoTheme
import com.echo.inbox.ui.viewmodel.ConversationViewModel
import kotlinx.coroutines.launch

@Composable
fun ConversationScreen(
    viewModel: ConversationViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var confirmDelete by remember { mutableStateOf(false) }

    val conversation = state.conversation
    val displayName = conversation?.displayName ?: "Conversation"
    val isGroup = displayName.contains(",") || (conversation?.subtitle != null && conversation.subtitle != displayName)

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this conversation?", fontWeight = FontWeight.Bold) },
            text = { Text("All snapshots and captured message events in this conversation will be permanently removed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        viewModel.delete()
                        onBack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = EchoTheme.colors.danger),
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
            containerColor = EchoTheme.colors.surface,
            titleContentColor = EchoTheme.colors.primaryText,
            textContentColor = EchoTheme.colors.secondaryText,
            shape = RoundedCornerShape(EchoTheme.radius.lg),
        )
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

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
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
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
                        Spacer(Modifier.width(EchoTheme.spacing.xs))

                        Box(contentAlignment = Alignment.BottomEnd) {
                            ContactAvatar(displayName = displayName, size = 40.dp)
                            conversation?.let {
                                Surface(
                                    shape = CircleShape,
                                    color = EchoTheme.colors.surface,
                                    modifier = Modifier.size(16.dp),
                                    border = BorderStroke(1.dp, EchoTheme.colors.surface),
                                ) {
                                    AppIcon(it.appPackage, state.appName, size = 14.dp)
                                }
                            }
                        }

                        Spacer(Modifier.width(EchoTheme.spacing.md))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = EchoTheme.colors.primaryText,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = listOfNotNull(state.appName, "${state.messages.size} messages")
                                    .joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = EchoTheme.colors.secondaryText,
                                maxLines = 1,
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = viewModel::togglePinned,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape),
                        ) {
                            Icon(
                                imageVector = if (conversation?.isPinned == true) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = "Pin conversation",
                                tint = if (conversation?.isPinned == true) EchoTheme.colors.signalBlue else EchoTheme.colors.secondaryText,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        IconButton(
                            onClick = { confirmDelete = true },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Delete conversation",
                                tint = EchoTheme.colors.secondaryText,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.messages.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                EmptyState(
                    title = "No message events",
                    body = "A raw notification snapshot may exist, but no readable message text was exposed.",
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = EchoTheme.spacing.lg, vertical = EchoTheme.spacing.md),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                var lastDateHeader = ""
                state.messages.forEachIndexed { index, message ->
                    val timestamp = message.sourceTimestamp ?: message.capturedAt
                    val dateHeader = formatDateHeader(timestamp)

                    if (dateHeader != lastDateHeader) {
                        lastDateHeader = dateHeader
                        item(key = "header_$dateHeader-$index") {
                            DateSeparatorPill(dateText = dateHeader)
                        }
                    }

                    item(key = message.id) {
                        AdaptiveMessageItem(
                            message = message,
                            appCategory = state.appCategory,
                            isGroupChat = isGroup,
                            onCopy = { text ->
                                clipboard.setText(AnnotatedString(text))
                                scope.launch {
                                    snackbarHostState.showSnackbar("Message copied")
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
