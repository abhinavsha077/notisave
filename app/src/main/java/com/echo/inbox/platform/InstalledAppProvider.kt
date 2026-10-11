package com.echo.inbox.platform

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.echo.inbox.core.model.InstalledAppInfo
import com.echo.inbox.core.util.AppCategoryClassifier

class InstalledAppProvider(private val context: Context) {
    @Suppress("DEPRECATION")
    fun loadLaunchableApps(): List<InstalledAppInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()),
            )
        } else {
            context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        }

        return activities
            .asSequence()
            .map { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName
                val displayName = resolveInfo.loadLabel(context.packageManager)
                    .toString()
                    .ifBlank { packageName }
                val classification = AppCategoryClassifier.classify(packageName, displayName)
                InstalledAppInfo(
                    packageName = packageName,
                    displayName = displayName,
                    category = classification.category,
                    sensitive = classification.sensitive,
                )
            }
            .filterNot { it.packageName == context.packageName }
            .distinctBy { it.packageName }
            .sortedWith(
                compareBy<InstalledAppInfo> { it.category.ordinal }
                    .thenBy { it.displayName.lowercase() },
            )
            .toList()
    }
}
