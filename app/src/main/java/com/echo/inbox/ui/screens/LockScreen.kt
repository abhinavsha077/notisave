package com.echo.inbox.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.echo.inbox.ui.components.EchoPrimaryButton
import com.echo.inbox.ui.theme.EchoTheme

@Composable
fun LockScreen(
    error: String?,
    onUnlock: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EchoTheme.colors.background)
            .padding(EchoTheme.spacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(EchoTheme.colors.blueContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Fingerprint,
                contentDescription = null,
                tint = EchoTheme.colors.signalBlue,
                modifier = Modifier.size(40.dp),
            )
        }
        Spacer(Modifier.height(EchoTheme.spacing.xl))
        Text(
            text = "Echo Inbox is locked",
            style = MaterialTheme.typography.headlineMedium,
            color = EchoTheme.colors.primaryText,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(EchoTheme.spacing.sm))
        Text(
            text = "Authenticate with biometrics or your device PIN to view captured notification content.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = EchoTheme.colors.secondaryText,
        )
        error?.let {
            Spacer(Modifier.height(EchoTheme.spacing.md))
            Text(
                text = it,
                color = EchoTheme.colors.danger,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.height(EchoTheme.spacing.xxl))
        EchoPrimaryButton(
            text = "Unlock with Biometrics",
            onClick = onUnlock,
            leadingIcon = Icons.Outlined.Fingerprint,
        )
    }
}
