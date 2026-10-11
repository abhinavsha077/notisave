package com.echo.inbox.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.echo.inbox.ui.components.NavigationSettingRow
import com.echo.inbox.ui.components.SectionHeader
import com.echo.inbox.ui.components.StatusMetricRow
import com.echo.inbox.ui.components.formatTime
import com.echo.inbox.ui.components.fullDateTime
import com.echo.inbox.ui.theme.EchoTheme
import com.echo.inbox.ui.viewmodel.HealthViewModel

@Composable
fun HealthScreen(
    viewModel: HealthViewModel,
    openNotificationAccessSettings: () -> Unit,
    openAppSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.postTest() else viewModel.notificationPermissionDenied()
    }

    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearActionMessage()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isOperational = state.notificationAccessGranted && state.health.listenerConnected
    val hasWarnings = state.health.parserFailureCount > 0 || state.health.pendingQueueCount > 10 || state.health.lastErrorCode != null
    val healthStatus = when {
        !state.notificationAccessGranted || !state.health.listenerConnected -> "DISCONNECTED"
        hasWarnings -> "DEGRADED"
        else -> "HEALTHY"
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
                    text = "Health",
                    style = MaterialTheme.typography.titleLarge,
                    color = EchoTheme.colors.primaryText,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Real-time capture health",
                    style = MaterialTheme.typography.bodySmall,
                    color = EchoTheme.colors.secondaryText,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(EchoTheme.spacing.md),
        ) {
            // Large Hero Operational Status Card
            item {
                val (containerColor, borderColor, icon, statusTitle, statusSubtitle) = when (healthStatus) {
                    "HEALTHY" -> {
                        val lastTime = state.health.lastCallbackAt?.let { "Since ${formatTime(it)} • Monitoring" } ?: "Monitoring notifications"
                        Tuple5(
                            EchoTheme.colors.mintContainer,
                            EchoTheme.colors.mint.copy(alpha = 0.5f),
                            Icons.Outlined.CheckCircle,
                            "Healthy",
                            "All systems operational\n$lastTime",
                        )
                    }
                    "DEGRADED" -> Tuple5(
                        EchoTheme.colors.warningContainer,
                        EchoTheme.colors.warning.copy(alpha = 0.5f),
                        Icons.Outlined.WarningAmber,
                        "Degraded",
                        "Some components limited\nReview diagnostic log below",
                    )
                    else -> Tuple5(
                        EchoTheme.colors.dangerContainer,
                        EchoTheme.colors.danger.copy(alpha = 0.5f),
                        Icons.Outlined.ErrorOutline,
                        "Disconnected",
                        "Capture is not connected.\nNotification access is required.",
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = EchoTheme.spacing.lg),
                    shape = RoundedCornerShape(EchoTheme.radius.lg),
                    colors = CardDefaults.cardColors(containerColor = containerColor),
                    border = BorderStroke(1.dp, borderColor),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(EchoTheme.spacing.xl),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(
                                    when (healthStatus) {
                                        "HEALTHY" -> EchoTheme.colors.mint
                                        "DEGRADED" -> EchoTheme.colors.warning
                                        else -> EchoTheme.colors.danger
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(30.dp),
                            )
                        }
                        Spacer(Modifier.width(EchoTheme.spacing.lg))
                        Column {
                            Text(
                                text = statusTitle,
                                style = MaterialTheme.typography.headlineMedium,
                                color = EchoTheme.colors.primaryText,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = statusSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = EchoTheme.colors.secondaryText,
                            )
                        }
                    }
                }
            }

            // Metrics Diagnostic Table
            item {
                SectionHeader(title = "Metrics", subtitle = "Diagnostics and memory states")
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = EchoTheme.spacing.lg),
                    shape = RoundedCornerShape(EchoTheme.radius.md),
                    colors = CardDefaults.cardColors(containerColor = EchoTheme.colors.surface),
                    border = BorderStroke(1.dp, EchoTheme.colors.border),
                ) {
                    Column(Modifier.fillMaxWidth().padding(vertical = EchoTheme.spacing.xs)) {
                        StatusMetricRow(
                            label = "Notification access",
                            value = if (state.notificationAccessGranted) "Granted" else "Limited",
                            icon = Icons.Outlined.Notifications,
                            isHealthy = state.notificationAccessGranted,
                            isDanger = !state.notificationAccessGranted,
                        )
                        HorizontalDivider(color = EchoTheme.colors.border.copy(alpha = 0.5f))
                        StatusMetricRow(
                            label = "Connection state",
                            value = if (state.health.listenerConnected) "Connected" else "Disconnected",
                            icon = Icons.Outlined.Wifi,
                            isHealthy = state.health.listenerConnected,
                            isDanger = !state.health.listenerConnected,
                        )
                        HorizontalDivider(color = EchoTheme.colors.border.copy(alpha = 0.5f))
                        StatusMetricRow(
                            label = "Last callback",
                            value = state.health.lastCallbackAt?.let(::fullDateTime) ?: "Never",
                            icon = Icons.Outlined.Schedule,
                        )
                        HorizontalDivider(color = EchoTheme.colors.border.copy(alpha = 0.5f))
                        StatusMetricRow(
                            label = "Last database write",
                            value = state.health.lastSuccessfulWriteAt?.let(::fullDateTime) ?: "Never",
                            icon = Icons.Outlined.Storage,
                        )
                        HorizontalDivider(color = EchoTheme.colors.border.copy(alpha = 0.5f))
                        StatusMetricRow(
                            label = "Queue backlog",
                            value = state.health.pendingQueueCount.toString(),
                            icon = Icons.Outlined.FormatListNumbered,
                            isWarning = state.health.pendingQueueCount > 10,
                        )
                        HorizontalDivider(color = EchoTheme.colors.border.copy(alpha = 0.5f))
                        StatusMetricRow(
                            label = "Parser failures (24h)",
                            value = state.health.parserFailureCount.toString(),
                            icon = Icons.Outlined.Code,
                            isWarning = state.health.parserFailureCount > 0,
                        )
                        HorizontalDivider(color = EchoTheme.colors.border.copy(alpha = 0.5f))
                        StatusMetricRow(
                            label = "DB failures (24h)",
                            value = state.health.databaseFailureCount.toString(),
                            icon = Icons.Outlined.Storage,
                            isDanger = state.health.databaseFailureCount > 0,
                        )
                        HorizontalDivider(color = EchoTheme.colors.border.copy(alpha = 0.5f))
                        StatusMetricRow(
                            label = "Dropped events (24h)",
                            value = state.health.droppedEventCount.toString(),
                            icon = Icons.Outlined.CheckCircle,
                            isWarning = state.health.droppedEventCount > 0,
                        )
                        HorizontalDivider(color = EchoTheme.colors.border.copy(alpha = 0.5f))
                        StatusMetricRow(
                            label = "Last error",
                            value = state.health.lastErrorCode ?: "None",
                            icon = Icons.Outlined.WarningAmber,
                            isWarning = state.health.lastErrorCode != null,
                        )
                    }
                }
            }

            // Actions Section
            item {
                SectionHeader(title = "Actions")
            }

            item {
                NavigationSettingRow(
                    title = "Open notification access",
                    body = "Review system permission settings",
                    onClick = openNotificationAccessSettings,
                    icon = Icons.Outlined.Security,
                )
            }

            item {
                NavigationSettingRow(
                    title = "Request listener rebind",
                    body = "Rebind the notification listener service",
                    onClick = viewModel::requestRebind,
                    icon = Icons.Outlined.Refresh,
                )
            }

            item {
                NavigationSettingRow(
                    title = "Run local capture test",
                    body = "Send test notification to verify pipeline",
                    onClick = {
                        val needsPermission = Build.VERSION.SDK_INT >= 33 &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        if (needsPermission) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else viewModel.postTest()
                    },
                    icon = Icons.Outlined.BugReport,
                )
            }

            // Device OEM Guidance Card
            item {
                SectionHeader(title = "Device guidance", subtitle = state.manufacturer)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = EchoTheme.spacing.lg),
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
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(EchoTheme.radius.sm))
                                .background(EchoTheme.colors.warningContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.BatteryAlert,
                                contentDescription = null,
                                tint = EchoTheme.colors.warning,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.width(EchoTheme.spacing.md))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Battery Optimization",
                                style = MaterialTheme.typography.titleMedium,
                                color = EchoTheme.colors.primaryText,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = state.guidance,
                                style = MaterialTheme.typography.bodySmall,
                                color = EchoTheme.colors.secondaryText,
                            )
                        }
                    }
                }
            }

            item {
                NavigationSettingRow(
                    title = "App Info & System settings",
                    body = "Manage background battery usage",
                    onClick = openAppSettings,
                    icon = Icons.Outlined.Settings,
                )
            }
        }
    }
}

private data class Tuple5<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
