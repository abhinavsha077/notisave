package com.echo.inbox.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.echo.inbox.BuildConfig
import com.echo.inbox.core.model.ThemeMode
import com.echo.inbox.ui.components.NavigationSettingRow
import com.echo.inbox.ui.components.SectionHeader
import com.echo.inbox.ui.components.SwitchSettingRow
import com.echo.inbox.ui.theme.EchoTheme
import com.echo.inbox.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var confirmReset by remember { mutableStateOf(false) }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Run onboarding again?", fontWeight = FontWeight.Bold) },
            text = { Text("Your captured data and selected app settings will remain. The initial setup and disclosure screens will reopen.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetOnboarding()
                        confirmReset = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = EchoTheme.colors.signalBlue),
                ) { Text("Restart setup", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
            containerColor = EchoTheme.colors.surface,
            titleContentColor = EchoTheme.colors.primaryText,
            textContentColor = EchoTheme.colors.secondaryText,
            shape = RoundedCornerShape(EchoTheme.radius.lg),
        )
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
                        text = "Settings",
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
            contentPadding = PaddingValues(bottom = 60.dp),
            verticalArrangement = Arrangement.spacedBy(EchoTheme.spacing.xs),
        ) {
            item {
                SectionHeader(title = "Appearance", subtitle = "Choose interface theme")
            }

            // Theme Mode Segmented Selector
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = EchoTheme.spacing.lg, vertical = 4.dp),
                    shape = RoundedCornerShape(EchoTheme.radius.md),
                    colors = CardDefaults.cardColors(containerColor = EchoTheme.colors.surface),
                    border = BorderStroke(1.dp, EchoTheme.colors.border),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(EchoTheme.spacing.md),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(EchoTheme.spacing.sm),
                        ) {
                            ThemeSegment(
                                label = "System",
                                icon = Icons.Outlined.SettingsBrightness,
                                selected = settings.themeMode == ThemeMode.SYSTEM,
                                onClick = { viewModel.setTheme(ThemeMode.SYSTEM) },
                                modifier = Modifier.weight(1f),
                            )
                            ThemeSegment(
                                label = "Light",
                                icon = Icons.Outlined.LightMode,
                                selected = settings.themeMode == ThemeMode.LIGHT,
                                onClick = { viewModel.setTheme(ThemeMode.LIGHT) },
                                modifier = Modifier.weight(1f),
                            )
                            ThemeSegment(
                                label = "Dark",
                                icon = Icons.Outlined.DarkMode,
                                selected = settings.themeMode == ThemeMode.DARK,
                                onClick = { viewModel.setTheme(ThemeMode.DARK) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Spacer(Modifier.height(EchoTheme.spacing.sm))
                        val helperText = when (settings.themeMode) {
                            ThemeMode.SYSTEM -> "Follow system dark mode preference"
                            ThemeMode.LIGHT -> "Always use Light theme"
                            ThemeMode.DARK -> "Always use Dark theme"
                        }
                        Text(
                            text = helperText,
                            style = MaterialTheme.typography.bodySmall,
                            color = EchoTheme.colors.secondaryText,
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(EchoTheme.spacing.sm))
                SectionHeader(title = "Capture defaults")
            }

            item {
                SwitchSettingRow(
                    title = "Auto-enable newly installed apps",
                    body = "Automatically start capturing notifications from new non-sensitive apps.",
                    checked = settings.captureUnknownApps,
                    onCheckedChange = viewModel::setCaptureUnknown,
                    icon = Icons.Outlined.AutoAwesome,
                )
            }

            item {
                Spacer(Modifier.height(EchoTheme.spacing.sm))
                SectionHeader(title = "Onboarding")
            }

            item {
                NavigationSettingRow(
                    title = "Restart onboarding",
                    body = "Run the setup flow again",
                    onClick = { confirmReset = true },
                    icon = Icons.Outlined.RestartAlt,
                )
            }

            item {
                Spacer(Modifier.height(EchoTheme.spacing.sm))
                SectionHeader(title = "About")
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = EchoTheme.spacing.lg, vertical = 4.dp),
                    shape = RoundedCornerShape(EchoTheme.radius.md),
                    colors = CardDefaults.cardColors(containerColor = EchoTheme.colors.surface),
                    border = BorderStroke(1.dp, EchoTheme.colors.border),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(EchoTheme.spacing.lg),
                    ) {
                        Text(
                            text = "Echo Inbox",
                            style = MaterialTheme.typography.titleLarge,
                            color = EchoTheme.colors.primaryText,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.BUILD_TYPE})",
                            style = MaterialTheme.typography.bodySmall,
                            color = EchoTheme.colors.secondaryText,
                        )
                        Spacer(Modifier.height(EchoTheme.spacing.md))
                        HorizontalDivider(color = EchoTheme.colors.border.copy(alpha = 0.5f))
                        Spacer(Modifier.height(EchoTheme.spacing.md))

                        Text(
                            text = "Architecture",
                            style = MaterialTheme.typography.labelLarge,
                            color = EchoTheme.colors.primaryText,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(EchoTheme.spacing.xs))
                        AboutRow(label = "Capture service", value = "Foreground listener")
                        AboutRow(label = "Database", value = "SQLCipher")
                        AboutRow(label = "Network permission", value = "None (100% on-device)")
                    }
                }
            }

            // Privacy Declaration Banner Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = EchoTheme.spacing.lg, vertical = EchoTheme.spacing.sm),
                    shape = RoundedCornerShape(EchoTheme.radius.md),
                    colors = CardDefaults.cardColors(containerColor = EchoTheme.colors.mintContainer),
                    border = BorderStroke(1.dp, EchoTheme.colors.mint.copy(alpha = 0.4f)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(EchoTheme.spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = EchoTheme.colors.onMintContainer,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(EchoTheme.spacing.md))
                        Text(
                            text = "Echo Inbox is private by design.\nEverything stays on-device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = EchoTheme.colors.onMintContainer,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSegment(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(EchoTheme.radius.sm))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(EchoTheme.radius.sm),
        color = if (selected) EchoTheme.colors.blueContainer else EchoTheme.colors.elevated,
        border = BorderStroke(
            1.dp,
            if (selected) EchoTheme.colors.signalBlue else EchoTheme.colors.border,
        ),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) EchoTheme.colors.signalBlue else EchoTheme.colors.secondaryText,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) EchoTheme.colors.signalBlue else EchoTheme.colors.secondaryText,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = EchoTheme.colors.secondaryText,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = EchoTheme.colors.primaryText,
            fontWeight = FontWeight.Medium,
        )
    }
}
