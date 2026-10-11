package com.echo.inbox.data.repository

import androidx.room.withTransaction
import com.echo.inbox.core.model.InstalledAppInfo
import com.echo.inbox.data.local.AppSourceDao
import com.echo.inbox.data.local.AppSourceEntity
import com.echo.inbox.data.local.EchoDatabase
import kotlinx.coroutines.flow.Flow

class AppSourceRepository(
    private val database: EchoDatabase,
    private val dao: AppSourceDao,
) {
    val allApps: Flow<List<AppSourceEntity>> = dao.observeAll()
    val appsWithEvents: Flow<List<AppSourceEntity>> = dao.observeWithEvents()

    suspend fun initializeInstalledApps(apps: List<InstalledAppInfo>) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            apps.forEach { app ->
                val existing = dao.get(app.packageName)
                dao.upsert(
                    AppSourceEntity(
                        packageName = app.packageName,
                        displayName = app.displayName,
                        category = app.category.name,
                        captureEnabled = existing?.captureEnabled ?: false,
                        sensitive = app.sensitive,
                        firstSeenAt = existing?.firstSeenAt ?: now,
                        lastSeenAt = existing?.lastSeenAt ?: now,
                        eventCount = existing?.eventCount ?: 0,
                    ),
                )
            }
        }
    }

    suspend fun applyOnboardingSelection(apps: List<InstalledAppInfo>, selectedPackages: Set<String>) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            apps.forEach { app ->
                val existing = dao.get(app.packageName)
                dao.upsert(
                    AppSourceEntity(
                        packageName = app.packageName,
                        displayName = app.displayName,
                        category = app.category.name,
                        captureEnabled = app.packageName in selectedPackages,
                        sensitive = app.sensitive,
                        firstSeenAt = existing?.firstSeenAt ?: now,
                        lastSeenAt = existing?.lastSeenAt ?: now,
                        eventCount = existing?.eventCount ?: 0,
                    ),
                )
            }
        }
    }

    suspend fun currentApps(): List<AppSourceEntity> = dao.getAll()

    suspend fun setCaptureEnabled(packageName: String, enabled: Boolean) {
        dao.setCaptureEnabled(packageName, enabled)
    }
}
