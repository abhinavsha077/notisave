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
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echo.inbox.data.local.ConversationSummaryRow
import com.echo.inbox.ui.theme.EchoTheme

@Composable
fun ConversationCard(
    conversation: ConversationSummaryRow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isUnread = conversation.unreadCount > 0
    val isOtpRedacted = conversation.latestBody?.contains("•••") == true || conversation.latestBody?.contains("••••••") == true

    Card(
        modifier = modifier
            .fillMaxWidth()
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
            // Unread Indicator Dot
            if (isUnread) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(EchoTheme.colors.signalBlue),
                )
                Spacer(Modifier.width(EchoTheme.spacing.sm))
            }

            // Avatar with App Badge
            Box(contentAlignment = Alignment.BottomEnd) {
                ContactAvatar(
                    displayName = conversation.displayName,
                    size = 46.dp,
                    appPackage = conversation.appPackage,
                )
                Surface(
                    shape = CircleShape,
                    color = EchoTheme.colors.surface,
                    modifier = Modifier.size(20.dp),
                    border = BorderStroke(1.5.dp, EchoTheme.colors.surface),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AppIcon(conversation.appPackage, conversation.appName, size = 16.dp)
                    }
                }
            }

            Spacer(Modifier.width(EchoTheme.spacing.md))

            // Main Text Column
            Column(Modifier.weight(1f)) {
                // Row 1: Title + Relative Timestamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = conversation.displayName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        color = EchoTheme.colors.primaryText,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = relativeTime(conversation.lastEventAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = EchoTheme.colors.secondaryText,
                    )
                }

                // Row 2: Message preview + Lock icon if OTP + Pin icon
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isOtpRedacted) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Redacted verification code",
                            modifier = Modifier
                                .size(13.dp)
                                .padding(end = 3.dp),
                            tint = EchoTheme.colors.warning,
                        )
                    } else if (conversation.latestEventType == "SUMMARY") {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = "Summary only",
                            modifier = Modifier
                                .size(13.dp)
                                .padding(end = 3.dp),
                            tint = EchoTheme.colors.secondaryText,
                        )
                    }

                    val previewText = when {
                        !conversation.latestSender.isNullOrBlank() && conversation.latestSender != conversation.displayName ->
                            "${conversation.latestSender}: ${conversation.latestBody.orEmpty()}"
                        else ->
                            conversation.latestBody ?: "Captured notification"
                    }

                    Text(
                        text = previewText,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUnread) EchoTheme.colors.primaryText else EchoTheme.colors.secondaryText,
                        fontWeight = if (isUnread) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (conversation.isPinned) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Pinned",
                            tint = EchoTheme.colors.signalBlue,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }

                // Row 3: App Name tag + Unread count pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = conversation.appName,
                        style = MaterialTheme.typography.labelSmall,
                        color = EchoTheme.colors.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (conversation.unreadCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = if (isOtpRedacted) EchoTheme.colors.mint else EchoTheme.colors.signalBlue,
                        ) {
                            Text(
                                text = conversation.unreadCount.coerceAtMost(99).toString(),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }
}
