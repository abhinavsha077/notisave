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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echo.inbox.ui.theme.EchoTheme

@Composable
fun SwitchSettingRow(
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = EchoTheme.spacing.lg, vertical = 4.dp)
            .clip(RoundedCornerShape(EchoTheme.radius.md))
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
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
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(EchoTheme.radius.sm))
                        .background(if (checked) EchoTheme.colors.mintContainer else EchoTheme.colors.elevated),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (checked) EchoTheme.colors.onMintContainer else EchoTheme.colors.secondaryText,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(EchoTheme.spacing.md))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = EchoTheme.colors.primaryText,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = EchoTheme.colors.secondaryText,
                )
            }
            Spacer(Modifier.width(EchoTheme.spacing.sm))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
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

@Composable
fun NavigationSettingRow(
    title: String,
    body: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailingText: String? = null,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = EchoTheme.spacing.lg, vertical = 4.dp)
            .clip(RoundedCornerShape(EchoTheme.radius.md))
            .clickable(onClick = onClick),
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
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(EchoTheme.radius.sm))
                        .background(EchoTheme.colors.elevated),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = EchoTheme.colors.secondaryText,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(EchoTheme.spacing.md))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = EchoTheme.colors.primaryText,
                    fontWeight = FontWeight.SemiBold,
                )
                if (body != null) {
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodySmall,
                        color = EchoTheme.colors.secondaryText,
                    )
                }
            }
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = EchoTheme.colors.secondaryText,
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = EchoTheme.colors.secondaryText,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
