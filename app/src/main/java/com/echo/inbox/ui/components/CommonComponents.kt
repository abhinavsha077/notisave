package com.echo.inbox.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echo.inbox.ui.theme.EchoTheme

@Composable
fun EchoTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingActions: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = EchoTheme.spacing.lg, vertical = EchoTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false),
        ) {
            if (onBack != null) {
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
            } else if (leadingIcon != null) {
                leadingIcon()
                Spacer(Modifier.width(EchoTheme.spacing.sm))
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = EchoTheme.colors.primaryText,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = EchoTheme.colors.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (trailingActions != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(EchoTheme.spacing.xs),
            ) {
                trailingActions()
            }
        }
    }
}

@Composable
fun CaptureHealthPill(
    status: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val (bgColor, textColor, icon) = when (status.uppercase()) {
        "HEALTHY" -> Triple(EchoTheme.colors.mintContainer, EchoTheme.colors.onMintContainer, Icons.Outlined.CheckCircle)
        "DEGRADED" -> Triple(EchoTheme.colors.warningContainer, EchoTheme.colors.onWarningContainer, Icons.Outlined.WarningAmber)
        "ON_DEVICE", "ON-DEVICE ONLY" -> Triple(EchoTheme.colors.mintContainer, EchoTheme.colors.onMintContainer, Icons.Outlined.Lock)
        else -> Triple(EchoTheme.colors.dangerContainer, EchoTheme.colors.onDangerContainer, Icons.Outlined.ErrorOutline)
    }

    val label = when (status.uppercase()) {
        "HEALTHY" -> "Healthy"
        "DEGRADED" -> "Degraded"
        "ON_DEVICE", "ON-DEVICE ONLY" -> "On-device only"
        else -> "Disconnected"
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(EchoTheme.radius.xl))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = bgColor,
        shape = RoundedCornerShape(EchoTheme.radius.xl),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun EchoPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(EchoTheme.radius.md),
        colors = ButtonDefaults.buttonColors(
            containerColor = EchoTheme.colors.signalBlue,
            contentColor = Color.White,
            disabledContainerColor = EchoTheme.colors.elevated,
            disabledContentColor = EchoTheme.colors.secondaryText,
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = Color.White,
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (leadingIcon != null) {
                    Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(EchoTheme.spacing.sm))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
fun EchoSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search captured notifications",
    onSearch: (() -> Unit)? = null,
    onFilterClick: (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = EchoTheme.colors.secondaryText,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search",
                tint = EchoTheme.colors.secondaryText,
                modifier = Modifier.size(20.dp),
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Outlined.Clear,
                        contentDescription = "Clear search",
                        tint = EchoTheme.colors.secondaryText,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(EchoTheme.radius.md),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = EchoTheme.colors.surface,
            unfocusedContainerColor = EchoTheme.colors.surface,
            disabledContainerColor = EchoTheme.colors.elevated,
            focusedBorderColor = EchoTheme.colors.signalBlue,
            unfocusedBorderColor = EchoTheme.colors.border,
            focusedTextColor = EchoTheme.colors.primaryText,
            unfocusedTextColor = EchoTheme.colors.primaryText,
        ),
    )
}

@Composable
fun EchoFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
) {
    val containerColor = if (selected) EchoTheme.colors.mintContainer else EchoTheme.colors.surface
    val contentColor = if (selected) EchoTheme.colors.onMintContainer else EchoTheme.colors.secondaryText
    val borderColor = if (selected) EchoTheme.colors.mint else EchoTheme.colors.border

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(EchoTheme.radius.xl))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(EchoTheme.radius.xl),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            )
            if (count != null && count > 0) {
                Surface(
                    shape = CircleShape,
                    color = if (selected) EchoTheme.colors.mint else EchoTheme.colors.elevated,
                ) {
                    Text(
                        text = count.toString(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = if (selected) Color.White else EchoTheme.colors.secondaryText,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
fun AppSourceChip(
    packageName: String,
    displayName: String,
    eventCount: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .width(92.dp)
            .clip(RoundedCornerShape(EchoTheme.radius.md))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(EchoTheme.radius.md),
        colors = CardDefaults.cardColors(containerColor = EchoTheme.colors.surface),
        border = BorderStroke(1.dp, EchoTheme.colors.border),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(EchoTheme.spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AppIcon(packageName = packageName, displayName = displayName, size = 42.dp)
            Spacer(Modifier.height(EchoTheme.spacing.xs))
            Text(
                text = displayName,
                style = MaterialTheme.typography.labelSmall,
                color = EchoTheme.colors.primaryText,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "$eventCount",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = EchoTheme.colors.secondaryText,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(EchoTheme.radius.md),
        colors = CardDefaults.cardColors(containerColor = EchoTheme.colors.surface),
        border = BorderStroke(1.dp, EchoTheme.colors.border),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(EchoTheme.spacing.md),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = EchoTheme.colors.primaryText,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = EchoTheme.colors.secondaryText,
            )
        }
    }
}

@Composable
fun StatusMetricRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isHealthy: Boolean = true,
    isWarning: Boolean = false,
    isDanger: Boolean = false,
    icon: ImageVector? = null,
) {
    val valueColor = when {
        isDanger -> EchoTheme.colors.danger
        isWarning -> EchoTheme.colors.warning
        isHealthy && (value == "Granted" || value == "Connected" || value == "Healthy") -> EchoTheme.colors.mint
        else -> EchoTheme.colors.secondaryText
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = EchoTheme.spacing.md, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = EchoTheme.colors.secondaryText,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = EchoTheme.colors.primaryText,
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = if (valueColor != EchoTheme.colors.secondaryText) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
fun DangerAction(
    title: String,
    subtitle: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmTitle: String = "Delete all captured data?",
    confirmBody: String = "This permanently removes all captured snapshots, conversations, and search entries. This action cannot be undone.",
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(confirmTitle, fontWeight = FontWeight.Bold) },
            text = { Text(confirmBody) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        onConfirm()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = EchoTheme.colors.danger),
                ) {
                    Text("Delete everything", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = EchoTheme.colors.surface,
            titleContentColor = EchoTheme.colors.primaryText,
            textContentColor = EchoTheme.colors.secondaryText,
            shape = RoundedCornerShape(EchoTheme.radius.lg),
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(EchoTheme.radius.md))
            .clickable { showDialog = true },
        shape = RoundedCornerShape(EchoTheme.radius.md),
        colors = CardDefaults.cardColors(containerColor = EchoTheme.colors.dangerContainer.copy(alpha = 0.35f)),
        border = BorderStroke(1.dp, EchoTheme.colors.danger.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(EchoTheme.spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(EchoTheme.colors.dangerContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteForever,
                    contentDescription = null,
                    tint = EchoTheme.colors.danger,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(EchoTheme.spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = EchoTheme.colors.danger,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = EchoTheme.colors.secondaryText,
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Inbox,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(EchoTheme.spacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(EchoTheme.colors.elevated),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = EchoTheme.colors.secondaryText,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.height(EchoTheme.spacing.lg))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = EchoTheme.colors.primaryText,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(EchoTheme.spacing.xs))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = EchoTheme.colors.secondaryText,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = EchoTheme.spacing.lg, vertical = EchoTheme.spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = EchoTheme.colors.primaryText,
                fontWeight = FontWeight.Bold,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = EchoTheme.colors.secondaryText,
                )
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    healthy: Boolean,
    modifier: Modifier = Modifier,
) {
    CaptureHealthPill(
        status = if (healthy) "HEALTHY" else "DISCONNECTED",
        modifier = modifier,
    )
}
