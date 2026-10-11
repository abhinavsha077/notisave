package com.echo.inbox.platform

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.echo.inbox.core.model.AppCategory
import com.echo.inbox.core.util.AppCategoryClassifier

class AppMetadataResolver(private val context: Context) {
    data class Metadata(
        val displayName: String,
        val category: AppCategory,
        val sensitive: Boolean,
    )

    @Suppress("DEPRECATION")
    fun resolve(packageName: String): Metadata {
        val displayName = runCatching {
            val info: ApplicationInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(0),
                )
            } else {
                context.packageManager.getApplicationInfo(packageName, 0)
            }
            context.packageManager.getApplicationLabel(info).toString()
        }.getOrDefault(
            packageName.substringAfterLast('.').replaceFirstChar(Char::uppercase),
        )
        val classification = AppCategoryClassifier.classify(packageName, displayName)
        return Metadata(displayName, classification.category, classification.sensitive)
    }
}
