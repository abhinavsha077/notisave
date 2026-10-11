package com.echo.inbox.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ScreenshotMonitor
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.echo.inbox.data.local.AppSourceEntity
import com.echo.inbox.ui.components.AppIcon
import com.echo.inbox.ui.components.DangerAction
import com.echo.inbox.ui.components.NavigationSettingRow
import com.echo.inbox.ui.components.SectionHeader
import com.echo.inbox.ui.components.StatCard
import com.echo.inbox.ui.components.SwitchSettingRow
import com.echo.inbox.ui.components.formatBytes
import com.echo.inbox.ui.theme.EchoTheme
import com.echo.inbox.ui.viewmodel.PrivacyViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PrivacyScreen(viewModel: PrivacyViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var retentionExpanded by remember { mutableStateOf(false) }
    var pendingSensitiveApp by remember { mutableStateOf<AppSourceEntity?>(null) }
    var pendingDeleteApp by remember { mutableStateOf<AppSourceEntity?>(null) }
    var confirmExport by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> if (uri != null) viewModel.export(uri) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    pendingSensitiveApp?.let { app ->
        AlertDialog(
            onDismissRequest = { pendingSensitiveApp = null },
            title = { Text("Capture sensitive notifications?", fontWeight = FontWeight.Bold) },
            text = {
                Text("${app.displayName} may expose verification codes, financial data, health information, or other highly sensitive content. Enable only on a device you control.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setCaptureEnabled(app.packageName, true)
                        pendingSensitiveApp = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = EchoTheme.colors.signalBlue),
                ) { Text("Enable", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { pendingSensitiveApp = null }) { Text("Cancel") } },
            containerColor = EchoTheme.colors.surface,
            titleContentColor = EchoTheme.colors.primaryText,
            textContentColor = EchoTheme.colors.secondaryText,
            shape = RoundedCornerShape(EchoTheme.radius.lg),
        )
    }

    pendingDeleteApp?.let { app ->
        AlertDialog(
            onDismissRequest = { pendingDeleteApp = null },
            title = { Text("Delete ${app.displayName} data?", fontWeight = FontWeight.Bold) },
            text = { Text("All captured conversations, message events, and raw snapshots for this app will be permanently removed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAppData(app.packageName)
                        pendingDeleteApp = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = EchoTheme.colors.danger),
                ) { Text("Delete", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteApp = null }) { Text("Cancel") } },
            containerColor = EchoTheme.colors.surface,
            titleContentColor = EchoTheme.colors.primaryText,
            textContentColor = EchoTheme.colors.secondaryText,
            shape = RoundedCornerShape(EchoTheme.radius.lg),
        )
    }

    if (confirmExport) {
        AlertDialog(
            onDismissRequest = { confirmExport = false },
            title = { Text("Create a readable export?", fontWeight = FontWeight.Bold) },
            text = {
                Text("The JSON export is intentionally readable and is not encrypted by Echo Inbox. Store it only in a trusted location.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        exportLauncher.launch("echo-inbox-$date.json")
                        confirmExport = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = EchoTheme.colors.signalBlue),
                ) { Text("Choose location", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmExport = false }) { Text("Cancel") } },
            containerColor = EchoTheme.colors.surface,
            titleContentColor = EchoTheme.colors.primaryText,
            textContentColor = EchoTheme.colors.secondaryText,
            shape = RoundedCornerShape(EchoTheme.radius.lg),
        )
    }

    Scaffold(
        containerColor = EchoTheme.colors.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = EchoTheme.spacing.lg, vertical = EchoTheme.spacing.md),
            ) {
                Text(
                    text = "Privacy Center",
                    style = MaterialTheme.typography.titleLarge,
                    color = EchoTheme.colors.primaryText,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Encrypted storage & controls",
                    style = MaterialTheme.typography.bodySmall,
                    color = EchoTheme.colors.secondaryText,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(EchoTheme.spacing.xs),
            ) {
                item {
                    SectionHeader(
                        title = "Encrypted storage",
                        subtitle = "100% on-device SQLCipher database. Zero network access.",
                    )
                }

                // 3-column stats
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = EchoTheme.spacing.lg, vertical = EchoTheme.spacing.xs),
                        horizontalArrangement = Arrangement.spacedBy(EchoTheme.spacing.sm),
                    ) {
                        StatCard(
                            value = state.stats.messageCount.toString(),
                            label = "Messages",
                            modifier = Modifier.weight(1f),
                        )
                        StatCard(
                            value = state.stats.conversationCount.toString(),
                            label = "Conversations",
                            modifier = Modifier.weight(1f),
                        )
                        StatCard(
                            value = formatBytes(state.databaseBytes),
                            label = "Database size",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(EchoTheme.spacing.sm))
                    SectionHeader(title = "Capture controls")
                }

                item {
                    SwitchSettingRow(
                        title = "Pause all capture",
                        body = "Keep listener connected but stop storing new notification snapshots.",
                        checked = state.settings.capturePaused,
                        onCheckedChange = viewModel::setCapturePaused,
                        icon = Icons.Outlined.PauseCircle,
                    )
                }
                item {
                    SwitchSettingRow(
                        title = "Capture newly discovered apps",
                        body = "Automatically capture notifications from newly installed non-sensitive apps.",
                        checked = state.settings.captureUnknownApps,
                        onCheckedChange = viewModel::setCaptureUnknownApps,
                        icon = Icons.Outlined.Apps,
                    )
                }

                item {
                    Spacer(Modifier.height(EchoTheme.spacing.sm))
                    SectionHeader(title = "Protection")
                }

                item {
                    SwitchSettingRow(
                        title = "Biometric app lock",
                        body = if (state.authenticationAvailable) {
                            "Require biometrics or device PIN/pattern when opening Echo Inbox."
                        } else {
                            "Unavailable until device screen lock is configured in system settings."
                        },
                        checked = state.settings.biometricLockEnabled && state.authenticationAvailable,
                        onCheckedChange = viewModel::setBiometricLock,
                        icon = Icons.Outlined.Fingerprint,
                    )
                }
                item {
                    SwitchSettingRow(
                        title = "Block screenshots",
                        body = "Apply Android FLAG_SECURE to prevent screenshotting and app preview leaking.",
                        checked = state.settings.screenshotProtectionEnabled,
                        onCheckedChange = viewModel::setScreenshotProtection,
                        icon = Icons.Outlined.ScreenshotMonitor,
                    )
                }
                item {
                    SwitchSettingRow(
                        title = "Redact verification codes",
                        body = "Mask 4–8 digit OTP codes with lock badges in notification previews.",
                        checked = state.settings.redactOtpEnabled,
                        onCheckedChange = viewModel::setOtpRedaction,
                        icon = Icons.Outlined.Password,
                    )
                }

                item {
                    Spacer(Modifier.height(EchoTheme.spacing.sm))
                    SectionHeader(title = "Retention & export")
                }

                item {
                    Box(Modifier.fillMaxWidth()) {
                        NavigationSettingRow(
                            title = "Retention policy",
                            body = "Auto-delete older snapshots",
                            trailingText = retentionLabel(state.settings.retentionDays),
                            onClick = { retentionExpanded = true },
                            icon = Icons.Outlined.Schedule,
                        )
                        DropdownMenu(
                            expanded = retentionExpanded,
                            onDismissRequest = { retentionExpanded = false },
                            modifier = Modifier.background(EchoTheme.colors.surface),
                        ) {
                            RETENTION_OPTIONS.forEach { (days, label) ->
                                DropdownMenuItem(
                                    text = { Text(label, color = EchoTheme.colors.primaryText) },
                                    onClick = {
                                        viewModel.setRetentionDays(days)
                                        retentionExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }

                item {
                    NavigationSettingRow(
                        title = "Export data (JSON)",
                        body = "Secure, unencrypted on-device export",
                        onClick = { confirmExport = true },
                        icon = Icons.Outlined.Download,
                    )
                }

                item {
                    Spacer(Modifier.height(EchoTheme.spacing.sm))
                    SectionHeader(
                        title = "Per-app capture controls",
                        subtitle = "${state.apps.count { it.captureEnabled }} of ${state.apps.size} apps enabled",
                    )
                }

                items(state.apps, key = { it.packageName }) { app ->
                    AppPrivacyRow(
                        app = app,
                        onToggle = { enabled ->
                            if (enabled && app.sensitive) pendingSensitiveApp = app
                            else viewModel.setCaptureEnabled(app.packageName, enabled)
                        },
                        onDelete = { pendingDeleteApp = app },
                    )
                }

                item {
                    Spacer(Modifier.height(EchoTheme.spacing.sm))
                    SectionHeader(title = "Danger zone")
                }

                item {
                    Box(Modifier.padding(horizontal = EchoTheme.spacing.lg, vertical = 4.dp)) {
                        DangerAction(
                            title = "Delete all captured data",
                            subtitle = "Permanently erase every snapshot and message from this device",
                            onConfirm = viewModel::clearCapturedData,
                        )
                    }
                }
            }

            if (state.busy) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = EchoTheme.colors.surface),
                        border = BorderStroke(1.dp, EchoTheme.colors.border),
                    ) {
                        CircularProgressIndicator(
                            Modifier.padding(28.dp),
                            color = EchoTheme.colors.signalBlue,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppPrivacyRow(
    app: AppSourceEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = EchoTheme.spacing.lg, vertical = 4.dp)
            .clip(RoundedCornerShape(EchoTheme.radius.md))
            .clickable { onToggle(!app.captureEnabled) },
        shape = RoundedCornerShape(EchoTheme.radius.md),
        colors = CardDefaults.cardColors(containerColor = EchoTheme.colors.surface),
        border = BorderStroke(1.dp, EchoTheme.colors.border),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(EchoTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(app.packageName, app.displayName, size = 38.dp)
            Spacer(Modifier.width(EchoTheme.spacing.md))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = EchoTheme.colors.primaryText,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (app.sensitive) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = EchoTheme.colors.dangerContainer,
                        ) {
                            Text(
                                text = "Sensitive",
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                color = EchoTheme.colors.danger,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                Text(
                    text = "${app.category.lowercase().replaceFirstChar { it.uppercase() }} · ${app.eventCount} events",
                    style = MaterialTheme.typography.bodySmall,
                    color = EchoTheme.colors.secondaryText,
                )
            }
            if (app.eventCount > 0) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteForever,
                        contentDescription = "Delete stored app data",
                        tint = EchoTheme.colors.secondaryText,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(4.dp))
            }
            Switch(
                checked = app.captureEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = EchoTheme.colors.mint,
                    uncheckedThumbColor = EchoTheme.colors.secondaryText,
                    uncheckedTrackColor = EchoTheme.colors.elevated,
                    uncheckedBorderColor = EchoTheme.colors.border,
                ),
            )
        }
    }
}

private val RETENTION_OPTIONS = listOf(
    7 to "7 days",
    30 to "30 days",
    90 to "90 days",
    365 to "1 year",
    -1 to "Keep until I delete",
)

private fun retentionLabel(days: Int): String = RETENTION_OPTIONS.firstOrNull { it.first == days }?.second ?: "$days days"
