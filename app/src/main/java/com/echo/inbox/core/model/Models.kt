package com.echo.inbox.core.model

enum class AppCategory {
    MESSAGING,
    SOCIAL,
    WORK,
    EMAIL,
    SHOPPING,
    SYSTEM,
    SENSITIVE,
    OTHER,
}

enum class ConversationConfidence {
    HIGH,
    MEDIUM,
    LOW,
}

enum class NotificationStyleType {
    MESSAGING,
    INBOX,
    BIG_TEXT,
    BASIC,
    UNKNOWN,
}

enum class MessageEventType {
    MESSAGE,
    SUMMARY,
    SYSTEM,
}

enum class AttachmentType {
    NONE,
    IMAGE,
    VIDEO,
    AUDIO,
    FILE,
    UNKNOWN,
}

data class RawMessage(
    val sender: String?,
    val body: String,
    val timestamp: Long?,
    val dataMimeType: String?,
    val dataUri: String?,
    val historic: Boolean,
    val mediaPath: String? = null,
    val isDeletedBySender: Boolean = false,
)

data class RawNotificationSnapshot(
    val systemKey: String,
    val packageName: String,
    val notificationId: Int,
    val notificationTag: String?,
    val groupKey: String?,
    val channelId: String?,
    val shortcutId: String?,
    val locusId: String?,
    val postedAt: Long,
    val sourceWhen: Long,
    val capturedAt: Long,
    val title: String?,
    val text: String?,
    val bigText: String?,
    val subText: String?,
    val summaryText: String?,
    val conversationTitle: String?,
    val textLines: List<String>,
    val messages: List<RawMessage>,
    val historicMessages: List<RawMessage>,
    val template: String?,
    val category: String?,
    val flags: Int,
    val isGroupSummary: Boolean,
    val isOngoing: Boolean,
    val userId: Int,
    val mediaPath: String? = null,
)

data class NormalizedMessage(
    val sender: String?,
    val body: String,
    val sourceTimestamp: Long?,
    val attachmentType: AttachmentType = AttachmentType.NONE,
    val historic: Boolean = false,
    val eventType: MessageEventType = MessageEventType.MESSAGE,
    val identityHash: String? = null,
    val mediaPath: String? = null,
    val isDeletedBySender: Boolean = false,
)

data class ConversationResolution(
    val stableKey: String,
    val displayName: String,
    val subtitle: String?,
    val confidence: ConversationConfidence,
)

data class NormalizedNotification(
    val raw: RawNotificationSnapshot,
    val styleType: NotificationStyleType,
    val resolution: ConversationResolution,
    val messages: List<NormalizedMessage>,
    val isSummaryOnly: Boolean,
    val contentHash: String,
    val parserVersion: Int,
)

data class InstalledAppInfo(
    val packageName: String,
    val displayName: String,
    val category: AppCategory,
    val sensitive: Boolean,
)

data class UserSettings(
    val onboardingComplete: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val retentionDays: Int = 90,
    val biometricLockEnabled: Boolean = false,
    val screenshotProtectionEnabled: Boolean = false,
    val redactOtpEnabled: Boolean = true,
    val captureUnknownApps: Boolean = true,
    val capturePaused: Boolean = false,
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}
