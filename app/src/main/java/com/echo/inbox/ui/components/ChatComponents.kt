package com.echo.inbox.ui.components

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DeliveryDining
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echo.inbox.data.local.MessageEventEntity
import com.echo.inbox.ui.theme.EchoTheme
import java.io.File
import kotlin.math.abs

private val AVATAR_COLORS = listOf(
    Color(0xFF00897B),
    Color(0xFF1E88E5),
    Color(0xFF5E35B1),
    Color(0xFFD81B60),
    Color(0xFFE53935),
    Color(0xFFFB8C00),
    Color(0xFF43A047),
    Color(0xFF3949AB),
    Color(0xFF00ACC1),
    Color(0xFF8E24AA),
)

fun avatarColorFor(name: String): Color {
    val hash = abs(name.hashCode())
    return AVATAR_COLORS[hash % AVATAR_COLORS.size]
}

fun extractInitials(name: String): String {
    val clean = name.trim()
    if (clean.isBlank()) return "?"
    val parts = clean.split(Regex("[\\s._-]+")).filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].first().uppercase()}${parts[1].first().uppercase()}"
        clean.length >= 2 && clean.first().isLetter() -> clean.take(2).uppercase()
        clean.isNotEmpty() -> clean.take(1).uppercase()
        else -> "?"
    }
}

@Composable
fun ContactAvatar(
    displayName: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    appPackage: String? = null,
) {
    val bgColor = avatarColorFor(displayName)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = extractInitials(displayName),
            color = Color.White,
            fontSize = (size.value * 0.38f).sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun DateSeparatorPill(
    dateText: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = EchoTheme.spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = EchoTheme.colors.elevated,
            shape = RoundedCornerShape(EchoTheme.radius.sm),
            border = BorderStroke(1.dp, EchoTheme.colors.border),
        ) {
            Text(
                text = dateText,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = EchoTheme.colors.secondaryText,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
fun AdaptiveMessageItem(
    message: MessageEventEntity,
    appCategory: String,
    modifier: Modifier = Modifier,
    isGroupChat: Boolean = false,
    onCopy: (String) -> Unit = {},
) {
    when (appCategory.uppercase()) {
        "EMAIL" -> EmailMessageCard(message = message, onCopy = onCopy, modifier = modifier)
        "SHOPPING" -> ShoppingOrderCard(message = message, onCopy = onCopy, modifier = modifier)
        "SENSITIVE" -> FinanceAlertCard(message = message, onCopy = onCopy, modifier = modifier)
        else -> ChatBubble(message = message, isGroupChat = isGroupChat, onCopy = onCopy, modifier = modifier)
    }
}

@Composable
fun ChatBubble(
    message: MessageEventEntity,
    modifier: Modifier = Modifier,
    isGroupChat: Boolean = false,
    onCopy: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val isSummary = message.eventType == "SUMMARY"
    val timestamp = message.sourceTimestamp ?: message.capturedAt
    var pendingUrl by remember { mutableStateOf<String?>(null) }

    if (pendingUrl != null) {
        AlertDialog(
            onDismissRequest = { pendingUrl = null },
            title = { Text("Open external link?", fontWeight = FontWeight.Bold) },
            text = { Text("Do you want to leave Echo Inbox and open this link in your browser?\n\n$pendingUrl") },
            confirmButton = {
                TextButton(onClick = {
                    val target = pendingUrl
                    pendingUrl = null
                    if (target != null) {
                        runCatching {
                            val fixedUrl = if (!target.startsWith("http://") && !target.startsWith("https://")) "https://$target" else target
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fixedUrl)))
                        }
                    }
                }) { Text("Open Link", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pendingUrl = null }) { Text("Cancel") }
            },
            containerColor = EchoTheme.colors.surface,
            titleContentColor = EchoTheme.colors.primaryText,
            textContentColor = EchoTheme.colors.secondaryText,
            shape = RoundedCornerShape(EchoTheme.radius.lg),
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 4.dp,
                topEnd = EchoTheme.radius.md,
                bottomEnd = EchoTheme.radius.md,
                bottomStart = EchoTheme.radius.md,
            ),
            color = if (isSummary) {
                EchoTheme.colors.warningContainer.copy(alpha = 0.5f)
            } else if (message.isDeletedBySender) {
                EchoTheme.colors.dangerContainer.copy(alpha = 0.35f)
            } else {
                EchoTheme.colors.surface
            },
            border = BorderStroke(1.dp, EchoTheme.colors.border),
            modifier = Modifier
                .widthIn(min = 120.dp, max = 340.dp)
                .clickable { onCopy(message.body) },
        ) {
            Column(
                modifier = Modifier.padding(horizontal = EchoTheme.spacing.md, vertical = 10.dp),
            ) {
                if (message.isDeletedBySender) {
                    DeletedBySenderPill()
                    Spacer(Modifier.height(4.dp))
                }

                if (isGroupChat && !message.sender.isNullOrBlank()) {
                    Text(
                        text = message.sender,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = EchoTheme.colors.signalBlue,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                } else if (isSummary) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 2.dp),
                    ) {
                        Icon(
                            Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = EchoTheme.colors.warning,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Notification Summary",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = EchoTheme.colors.warning,
                        )
                    }
                }

                if (!message.mediaPath.isNullOrBlank()) {
                    MediaImageThumbnail(message.mediaPath)
                    Spacer(Modifier.height(6.dp))
                }

                if (message.attachmentType != "NONE" && message.mediaPath.isNullOrBlank()) {
                    AttachmentBadge(message.attachmentType)
                    Spacer(Modifier.height(4.dp))
                }

                if (message.body.isNotBlank()) {
                    Text(
                        text = message.body,
                        style = MaterialTheme.typography.bodyLarge,
                        color = EchoTheme.colors.primaryText,
                    )
                }

                val detectedUrls = remember(message.body) { extractUrls(message.body) }
                detectedUrls.forEach { url ->
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EchoTheme.colors.blueContainer,
                        modifier = Modifier.clickable { pendingUrl = url },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = EchoTheme.colors.signalBlue,
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = url,
                                style = MaterialTheme.typography.labelSmall,
                                color = EchoTheme.colors.signalBlue,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (message.isHistoricMessage) {
                        Icon(
                            Icons.Outlined.History,
                            contentDescription = "Historic message",
                            modifier = Modifier.size(12.dp),
                            tint = EchoTheme.colors.secondaryText,
                        )
                    }
                    if (message.isPossibleEdit) {
                        Text(
                            text = "edited",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = EchoTheme.colors.signalBlue,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Text(
                        text = formatTime(timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = EchoTheme.colors.secondaryText,
                    )
                }
            }
        }
    }
}

@Composable
fun EmailMessageCard(
    message: MessageEventEntity,
    modifier: Modifier = Modifier,
    onCopy: (String) -> Unit = {},
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onCopy(message.body) },
        shape = RoundedCornerShape(EchoTheme.radius.md),
        colors = CardDefaults.cardColors(containerColor = EchoTheme.colors.surface),
        border = BorderStroke(1.dp, EchoTheme.colors.border),
    ) {
        Column(Modifier.fillMaxWidth().padding(EchoTheme.spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Email,
                    contentDescription = null,
                    tint = EchoTheme.colors.signalBlue,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = message.sender ?: "Email",
                    style = MaterialTheme.typography.titleMedium,
                    color = EchoTheme.colors.primaryText,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatTime(message.sourceTimestamp ?: message.capturedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = EchoTheme.colors.secondaryText,
                )
            }
            Spacer(Modifier.height(8.dp))
            if (!message.mediaPath.isNullOrBlank()) {
                MediaImageThumbnail(message.mediaPath)
                Spacer(Modifier.height(6.dp))
            }
            Text(
                text = message.body,
                style = MaterialTheme.typography.bodyMedium,
                color = EchoTheme.colors.primaryText,
            )
        }
    }
}

@Composable
fun ShoppingOrderCard(
    message: MessageEventEntity,
    modifier: Modifier = Modifier,
    onCopy: (String) -> Unit = {},
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onCopy(message.body) },
        shape = RoundedCornerShape(EchoTheme.radius.md),
        colors = CardDefaults.cardColors(containerColor = EchoTheme.colors.surface),
        border = BorderStroke(1.dp, EchoTheme.colors.border),
    ) {
        Column(Modifier.fillMaxWidth().padding(EchoTheme.spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.DeliveryDining,
                    contentDescription = null,
                    tint = EchoTheme.colors.mint,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = message.sender ?: "Order Update",
                    style = MaterialTheme.typography.titleMedium,
                    color = EchoTheme.colors.primaryText,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatTime(message.sourceTimestamp ?: message.capturedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = EchoTheme.colors.secondaryText,
                )
            }
            Spacer(Modifier.height(8.dp))
            if (!message.mediaPath.isNullOrBlank()) {
                MediaImageThumbnail(message.mediaPath)
                Spacer(Modifier.height(6.dp))
            }
            Text(
                text = message.body,
                style = MaterialTheme.typography.bodyLarge,
                color = EchoTheme.colors.primaryText,
            )
            val otpMatch = remember(message.body) { extractOtpCode(message.body) }
            if (otpMatch != null) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = EchoTheme.colors.mintContainer,
                ) {
                    Text(
                        text = "OTP: $otpMatch",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EchoTheme.colors.onMintContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun FinanceAlertCard(
    message: MessageEventEntity,
    modifier: Modifier = Modifier,
    onCopy: (String) -> Unit = {},
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onCopy(message.body) },
        shape = RoundedCornerShape(EchoTheme.radius.md),
        colors = CardDefaults.cardColors(containerColor = EchoTheme.colors.surface),
        border = BorderStroke(1.dp, EchoTheme.colors.border),
    ) {
        Column(Modifier.fillMaxWidth().padding(EchoTheme.spacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.AccountBalance,
                    contentDescription = null,
                    tint = EchoTheme.colors.warning,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = message.sender ?: "Transaction Alert",
                    style = MaterialTheme.typography.titleMedium,
                    color = EchoTheme.colors.primaryText,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatTime(message.sourceTimestamp ?: message.capturedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = EchoTheme.colors.secondaryText,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = message.body,
                style = MaterialTheme.typography.bodyLarge,
                color = EchoTheme.colors.primaryText,
            )
        }
    }
}

@Composable
private fun MediaImageThumbnail(mediaPath: String) {
    val bitmap = remember(mediaPath) {
        runCatching {
            val file = File(mediaPath)
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        }.getOrNull()
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Captured Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp)
                .clip(RoundedCornerShape(EchoTheme.radius.sm)),
        )
    }
}

@Composable
private fun DeletedBySenderPill() {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = EchoTheme.colors.dangerContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.DeleteOutline,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = EchoTheme.colors.danger,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Deleted by sender",
                style = MaterialTheme.typography.labelSmall,
                color = EchoTheme.colors.danger,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun AttachmentBadge(attachmentType: String) {
    val (icon, label) = when (attachmentType) {
        "IMAGE" -> Icons.Outlined.Image to "Photo"
        "VIDEO" -> Icons.Outlined.Videocam to "Video"
        "AUDIO" -> Icons.Outlined.Audiotrack to "Audio"
        "FILE" -> Icons.Outlined.Description to "Document"
        else -> Icons.Outlined.AttachFile to "Attachment"
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = EchoTheme.colors.blueContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = EchoTheme.colors.signalBlue,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = EchoTheme.colors.signalBlue,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

private val URL_REGEX = Regex("(?i)\\b(?:https?://|www\\.)[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")
private val OTP_REGEX = Regex("(?i)(?:otp|code|pin|verification[\\s-]?code)(?:\\s+is|:)?\\s*([0-9]{4,8})")

fun extractUrls(text: String): List<String> {
    return URL_REGEX.findAll(text).map { it.value }.take(3).toList()
}

fun extractOtpCode(text: String): String? {
    return OTP_REGEX.find(text)?.groupValues?.getOrNull(1)
}
