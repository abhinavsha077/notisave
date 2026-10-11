package com.echo.inbox.platform

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.echo.inbox.capture.NotificationCaptureService

class NotificationAccessManager(private val context: Context) {
    private val componentName = ComponentName(context, NotificationCaptureService::class.java)

    fun isGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= 27) {
            context.getSystemService(NotificationManager::class.java)
                .isNotificationListenerAccessGranted(componentName)
        } else {
            context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)
        }
    }

    /**
     * Checks whether the app is likely blocked by Android 13+ "restricted settings".
     * Sideloaded apps (installed via APK, not from a recognized app store) are blocked
     * from enabling sensitive permissions like NotificationListenerService until the
     * user explicitly allows restricted settings from App Info.
     */
    fun isRestrictedSettingsBlocked(): Boolean {
        if (Build.VERSION.SDK_INT < 33) return false
        if (isGranted()) return false
        return !isInstalledFromTrustedSource()
    }

    /**
     * Intent to open this app's system App Info page, where the user can allow
     * restricted settings via the three-dot overflow menu.
     */
    fun appInfoIntent(): Intent {
        return Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        )
    }

    fun settingsIntent(): Intent {
        if (Build.VERSION.SDK_INT >= 30) {
            val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, componentName.flattenToString())
            if (detail.resolveActivity(context.packageManager) != null) return detail
        }
        return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    }

    fun requestRebind() {
        android.service.notification.NotificationListenerService.requestRebind(componentName)
    }

    private fun isInstalledFromTrustedSource(): Boolean {
        val trustedInstallers = setOf(
            "com.android.vending",       // Google Play Store
            "com.amazon.venezia",         // Amazon Appstore
            "com.huawei.appmarket",       // Huawei AppGallery
            "com.samsung.android.vending", // Samsung Galaxy Store
            "com.xiaomi.market",          // Xiaomi GetApps / Mi Store
            "com.oppo.market",            // OPPO App Market
            "com.heytap.market",          // OPPO / OnePlus / Realme HeyTap Market
            "com.bbk.appstore",           // Vivo V-Appstore
        )
        return runCatching {
            val installer = if (Build.VERSION.SDK_INT >= 30) {
                context.packageManager
                    .getInstallSourceInfo(context.packageName)
                    .installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName)
            }
            installer in trustedInstallers
        }.getOrDefault(false)
    }
}
