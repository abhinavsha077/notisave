package com.echo.inbox.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.echo.inbox.core.model.InstalledAppInfo
import com.echo.inbox.ui.components.AppIcon
import com.echo.inbox.ui.components.CaptureHealthPill
import com.echo.inbox.ui.components.EchoFilterChip
import com.echo.inbox.ui.components.EchoPrimaryButton
import com.echo.inbox.ui.components.EchoSearchField
import com.echo.inbox.ui.theme.EchoTheme
import com.echo.inbox.ui.viewmodel.OnboardingUiState
import com.echo.inbox.ui.viewmodel.OnboardingViewModel

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    openNotificationAccessSettings: () -> Unit,
    openAppSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshNotificationAccess()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = EchoTheme.colors.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EchoTheme.colors.background),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = EchoTheme.spacing.lg, vertical = EchoTheme.spacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Echo Inbox",
                        style = MaterialTheme.typography.titleLarge,
                        color = EchoTheme.colors.primaryText,
                        fontWeight = FontWeight.Bold,
                    )
                    Surface(
                        shape = CircleShape,
                        color = EchoTheme.colors.elevated,
                        border = BorderStroke(1.dp, EchoTheme.colors.border),
                    ) {
                        Text(
                            text = "${state.step + 1} of 4",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = EchoTheme.colors.secondaryText,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { (state.step + 1) / 4f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = EchoTheme.colors.signalBlue,
                    trackColor = EchoTheme.colors.elevated,
                )
            }
        },
        bottomBar = {
            Surface(
                color = EchoTheme.colors.surface,
                border = BorderStroke(1.dp, EchoTheme.colors.border),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(EchoTheme.spacing.lg),
                ) {
                    state.error?.let { error ->
                        Text(
                            text = error,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = EchoTheme.spacing.sm),
                            color = EchoTheme.colors.danger,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(EchoTheme.spacing.md),
                    ) {
                        if (state.step > 0) {
                            OutlinedButton(
                                onClick = viewModel::previous,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 52.dp),
                                shape = RoundedCornerShape(EchoTheme.radius.md),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = EchoTheme.colors.primaryText),
                                border = BorderStroke(1.dp, EchoTheme.colors.border),
                            ) {
                                Text("Back", fontWeight = FontWeight.SemiBold)
                            }
                        }
                        val ctaText = when (state.step) {
                            0 -> "Continue"
                            1 -> "Continue to app selection"
                            2 -> "Continue with selected apps"
                            else -> "Finish setup"
                        }
                        EchoPrimaryButton(
                            text = ctaText,
                            onClick = if (state.step == 3) viewModel::complete else viewModel::next,
                            modifier = Modifier.weight(1.5f),
                            isLoading = state.saving,
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(EchoTheme.colors.background),
        ) {
            when (state.step) {
                0 -> WelcomeStep()
                1 -> DisclosureStep()
                2 -> AppSelectionStep(state, viewModel)
                else -> PermissionStep(state, viewModel, openNotificationAccessSettings, openAppSettings)
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = EchoTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        item {
            Spacer(Modifier.height(EchoTheme.spacing.xl))
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(EchoTheme.colors.blueContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.NotificationsActive,
                    contentDescription = null,
                    tint = EchoTheme.colors.signalBlue,
                    modifier = Modifier.size(36.dp),
                )
            }
            Spacer(Modifier.height(EchoTheme.spacing.xl))
            Text(
                text = "Your private\nnotification memory",
                style = MaterialTheme.typography.displaySmall,
                color = EchoTheme.colors.primaryText,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(EchoTheme.spacing.md))
            Text(
                text = "Echo Inbox keeps your notifications on-device, reconstructs your context, and puts you back in control.",
                style = MaterialTheme.typography.bodyLarge,
                color = EchoTheme.colors.secondaryText,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(EchoTheme.spacing.xl))
        }

        item {
            BenefitCard(
                icon = Icons.Outlined.Shield,
                title = "100% on-device",
                description = "Everything stays on your device. No cloud. No syncing.",
            )
            Spacer(Modifier.height(EchoTheme.spacing.sm))
            BenefitCard(
                icon = Icons.Outlined.Lock,
                title = "Zero analytics, zero trackers",
                description = "We don't collect, share, or sell any of your data.",
            )
            Spacer(Modifier.height(EchoTheme.spacing.sm))
            BenefitCard(
                icon = Icons.Outlined.Schedule,
                title = "Smart timeline reconstruction",
                description = "Find what matters with AI that works entirely offline.",
            )
            Spacer(Modifier.height(EchoTheme.spacing.xl))
        }
    }
}

@Composable
private fun BenefitCard(
    icon: ImageVector,
    title: String,
    description: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(EchoTheme.radius.sm))
                    .background(EchoTheme.colors.blueContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = EchoTheme.colors.signalBlue,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(EchoTheme.spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = EchoTheme.colors.primaryText,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = EchoTheme.colors.secondaryText,
                )
            }
        }
    }
}

@Composable
private fun DisclosureStep() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(EchoTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(EchoTheme.spacing.md),
    ) {
        item {
            Text(
                text = "Your privacy\nis our foundation",
                style = MaterialTheme.typography.headlineMedium,
                color = EchoTheme.colors.primaryText,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(EchoTheme.spacing.xs))
            Text(
                text = "Here's how Echo Inbox protects you.",
                style = MaterialTheme.typography.bodyLarge,
                color = EchoTheme.colors.secondaryText,
            )
            Spacer(Modifier.height(EchoTheme.spacing.xs))
        }

        item {
            DetailedDisclosureCard(
                icon = Icons.Outlined.Folder,
                title = "What Echo Inbox stores",
                bullets = listOf(
                    "Your notifications",
                    "Reconstructed context",
                    "App selections & settings",
                ),
                footnote = "Stored securely on this device.",
            )
        }

        item {
            DetailedDisclosureCard(
                icon = Icons.Outlined.VisibilityOff,
                title = "What it cannot access",
                bullets = listOf(
                    "Your messages or content",
                    "Microphone or camera",
                    "Location or contacts",
                ),
                footnote = "We don't request or use these.",
            )
        }

        item {
            DetailedDisclosureCard(
                icon = Icons.Outlined.Person,
                title = "You stay in control",
                bullets = listOf(
                    "Choose which apps to include",
                    "Export or delete any time",
                    "Works offline by default",
                ),
                footnote = "You decide. You're in charge.",
            )
        }
    }
}

@Composable
private fun DetailedDisclosureCard(
    icon: ImageVector,
    title: String,
    bullets: List<String>,
    footnote: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(EchoTheme.radius.md),
        colors = CardDefaults.cardColors(containerColor = EchoTheme.colors.surface),
        border = BorderStroke(1.dp, EchoTheme.colors.border),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(EchoTheme.spacing.lg),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(EchoTheme.radius.sm))
                        .background(EchoTheme.colors.mintContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = EchoTheme.colors.onMintContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(EchoTheme.spacing.md))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = EchoTheme.colors.primaryText,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(EchoTheme.spacing.md))
            bullets.forEach { bullet ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(EchoTheme.colors.mint),
                    )
                    Spacer(Modifier.width(EchoTheme.spacing.sm))
                    Text(
                        text = bullet,
                        style = MaterialTheme.typography.bodyMedium,
                        color = EchoTheme.colors.primaryText,
                    )
                }
            }
            Spacer(Modifier.height(EchoTheme.spacing.sm))
            Text(
                text = footnote,
                style = MaterialTheme.typography.bodySmall,
                color = EchoTheme.colors.secondaryText,
            )
        }
    }
}

@Composable
private fun AppSelectionStep(state: OnboardingUiState, viewModel: OnboardingViewModel) {
    var pendingSensitiveApp by remember { mutableStateOf<InstalledAppInfo?>(null) }

    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(EchoTheme.spacing.lg),
        ) {
            Text(
                text = "Choose your apps",
                style = MaterialTheme.typography.headlineMedium,
                color = EchoTheme.colors.primaryText,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Select the apps you want Echo Inbox to watch.",
                style = MaterialTheme.typography.bodyMedium,
                color = EchoTheme.colors.secondaryText,
            )
            Spacer(Modifier.height(EchoTheme.spacing.md))
            EchoSearchField(
                query = state.query,
                onQueryChange = viewModel::setQuery,
                placeholder = "Search installed apps",
            )
            Spacer(Modifier.height(EchoTheme.spacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(EchoTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EchoFilterChip(
                    selected = false,
                    onClick = viewModel::selectRecommended,
                    label = "Messaging & Social",
                )
                EchoFilterChip(
                    selected = false,
                    onClick = viewModel::clearSelection,
                    label = "Clear",
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${state.selectedPackages.size} selected",
                    style = MaterialTheme.typography.labelMedium,
                    color = EchoTheme.colors.signalBlue,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        HorizontalDivider(color = EchoTheme.colors.border)

        if (state.loadingApps) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EchoTheme.colors.signalBlue)
            }
        } else {
            val filtered = state.apps.filter { app ->
                state.query.isBlank() || app.displayName.contains(state.query, true) || app.packageName.contains(state.query, true)
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
            ) {
                items(filtered, key = InstalledAppInfo::packageName) { app ->
                    val selected = app.packageName in state.selectedPackages
                    AppSelectionRow(
                        app = app,
                        selected = selected,
                        onToggle = {
                            if (app.sensitive && !selected) {
                                pendingSensitiveApp = app
                            } else {
                                viewModel.togglePackage(app.packageName)
                            }
                        },
                    )
                }
            }
        }
    }

    pendingSensitiveApp?.let { app ->
        AlertDialog(
            onDismissRequest = { pendingSensitiveApp = null },
            title = { Text("Capture notifications from ${app.displayName}?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This application is classified as sensitive. Its notifications may contain verification codes, financial alerts, or personal data. Echo Inbox will store exposed notification text in its encrypted local database.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.confirmSensitivePackage(app.packageName)
                        pendingSensitiveApp = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EchoTheme.colors.signalBlue),
                ) {
                    Text("I understand — enable")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingSensitiveApp = null }) { Text("Keep disabled") }
            },
            containerColor = EchoTheme.colors.surface,
            titleContentColor = EchoTheme.colors.primaryText,
            textContentColor = EchoTheme.colors.secondaryText,
            shape = RoundedCornerShape(EchoTheme.radius.lg),
        )
    }
}

@Composable
private fun AppSelectionRow(app: InstalledAppInfo, selected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = EchoTheme.spacing.lg, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app.packageName, app.displayName, size = 40.dp)
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
                text = "${app.category.name.lowercase().replaceFirstChar { it.uppercase() }} · ${app.packageName}",
                style = MaterialTheme.typography.bodySmall,
                color = EchoTheme.colors.secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Checkbox(
            checked = selected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = EchoTheme.colors.signalBlue,
                uncheckedColor = EchoTheme.colors.secondaryText,
            ),
        )
    }
}

@Composable
private fun PermissionStep(
    state: OnboardingUiState,
    viewModel: OnboardingViewModel,
    openNotificationAccessSettings: () -> Unit,
    openAppSettings: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(EchoTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(EchoTheme.spacing.md),
    ) {
        item {
            Text(
                text = "Grant notification access",
                style = MaterialTheme.typography.headlineMedium,
                color = EchoTheme.colors.primaryText,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(EchoTheme.spacing.xs))
            Text(
                text = "Echo Inbox needs permission to read and organize your notifications securely.",
                style = MaterialTheme.typography.bodyLarge,
                color = EchoTheme.colors.secondaryText,
            )
            Spacer(Modifier.height(EchoTheme.spacing.xs))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(EchoTheme.radius.md),
                colors = CardDefaults.cardColors(containerColor = EchoTheme.colors.surface),
                border = BorderStroke(1.dp, EchoTheme.colors.border),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(EchoTheme.spacing.lg),
                ) {
                    PermissionStepRow(number = 1, title = "Open Android settings", body = "Tap the button below to open Notification access settings.")
                    Spacer(Modifier.height(EchoTheme.spacing.md))
                    PermissionStepRow(number = 2, title = "Find Echo Inbox", body = "Locate Echo Inbox in the list.")
                    Spacer(Modifier.height(EchoTheme.spacing.md))
                    PermissionStepRow(number = 3, title = "Enable permission", body = "Turn on the toggle for Echo Inbox.")
                }
            }
        }

        item {
            val isGranted = state.notificationAccessGranted
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(EchoTheme.radius.md),
                colors = CardDefaults.cardColors(
                    containerColor = if (isGranted) EchoTheme.colors.mintContainer else EchoTheme.colors.warningContainer.copy(alpha = 0.5f),
                ),
                border = BorderStroke(
                    1.dp,
                    if (isGranted) EchoTheme.colors.mint.copy(alpha = 0.5f) else EchoTheme.colors.warning.copy(alpha = 0.5f),
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(EchoTheme.spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isGranted) EchoTheme.colors.mint else EchoTheme.colors.warning),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isGranted) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(EchoTheme.spacing.md))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (isGranted) "Access granted" else "Permission required",
                            style = MaterialTheme.typography.titleMedium,
                            color = EchoTheme.colors.primaryText,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (isGranted) "Echo Inbox can now organize your notifications." else "Enable Echo Inbox in Android settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = EchoTheme.colors.secondaryText,
                        )
                    }
                    CaptureHealthPill(status = if (isGranted) "HEALTHY" else "DISCONNECTED")
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(EchoTheme.spacing.md),
            ) {
                OutlinedButton(
                    onClick = openNotificationAccessSettings,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(EchoTheme.radius.md),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EchoTheme.colors.primaryText),
                    border = BorderStroke(1.dp, EchoTheme.colors.border),
                ) {
                    Text("Open settings", fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = viewModel::refreshNotificationAccess,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(EchoTheme.radius.md),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EchoTheme.colors.primaryText),
                    border = BorderStroke(1.dp, EchoTheme.colors.border),
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Re-check", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PermissionStepRow(number: Int, title: String, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            shape = CircleShape,
            color = EchoTheme.colors.signalBlue,
            modifier = Modifier.size(24.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number.toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(Modifier.width(EchoTheme.spacing.md))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = EchoTheme.colors.primaryText,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = EchoTheme.colors.secondaryText,
            )
        }
    }
}
