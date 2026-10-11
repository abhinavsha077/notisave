package com.echo.inbox.di

import android.content.Context
import com.echo.inbox.capture.CaptureCoordinator
import com.echo.inbox.capture.CaptureProcessor
import com.echo.inbox.capture.NotificationNormalizer
import com.echo.inbox.data.local.DatabaseFactory
import com.echo.inbox.data.local.EchoDatabase
import com.echo.inbox.data.repository.AppSourceRepository
import com.echo.inbox.data.repository.CaptureHealthRepository
import com.echo.inbox.data.repository.NotificationRepository
import com.echo.inbox.data.settings.SettingsRepository
import com.echo.inbox.export.JsonExportWriter
import com.echo.inbox.platform.AppMetadataResolver
import com.echo.inbox.platform.InstalledAppProvider
import com.echo.inbox.platform.MediaStorageManager
import com.echo.inbox.platform.NotificationAccessManager
import com.echo.inbox.platform.NotificationTestManager
import com.echo.inbox.security.AndroidKeystoreIdentityHasher
import com.echo.inbox.security.DeviceAuthenticationCapability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

interface AppContainer {
    val appScope: CoroutineScope
    val database: EchoDatabase
    val settingsRepository: SettingsRepository
    val appSourceRepository: AppSourceRepository
    val notificationRepository: NotificationRepository
    val captureHealthRepository: CaptureHealthRepository
    val captureCoordinator: CaptureCoordinator
    val installedAppProvider: InstalledAppProvider
    val notificationAccessManager: NotificationAccessManager
    val notificationTestManager: NotificationTestManager
    val mediaStorageManager: MediaStorageManager
    val jsonExportWriter: JsonExportWriter
    val deviceAuthenticationCapability: DeviceAuthenticationCapability
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val appContext = context.applicationContext

    override val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    override val database: EchoDatabase by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        DatabaseFactory.create(appContext)
    }
    override val settingsRepository: SettingsRepository = SettingsRepository(appContext, appScope)
    override val appSourceRepository: AppSourceRepository by lazy {
        AppSourceRepository(database, database.appSourceDao())
    }
    override val notificationRepository: NotificationRepository by lazy {
        NotificationRepository(appContext, database)
    }
    override val captureHealthRepository: CaptureHealthRepository by lazy {
        CaptureHealthRepository(database.captureHealthDao())
    }
    override val installedAppProvider: InstalledAppProvider = InstalledAppProvider(appContext)
    override val notificationAccessManager: NotificationAccessManager = NotificationAccessManager(appContext)
    override val notificationTestManager: NotificationTestManager = NotificationTestManager(appContext)
    override val mediaStorageManager: MediaStorageManager = MediaStorageManager(appContext)
    override val deviceAuthenticationCapability: DeviceAuthenticationCapability = DeviceAuthenticationCapability(appContext)
    override val jsonExportWriter: JsonExportWriter by lazy {
        JsonExportWriter(appContext, notificationRepository)
    }
    override val captureCoordinator: CaptureCoordinator by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val processor = CaptureProcessor(
            context = appContext,
            database = database,
            normalizer = NotificationNormalizer(AndroidKeystoreIdentityHasher()),
            metadataResolver = AppMetadataResolver(appContext),
            settingsRepository = settingsRepository,
            healthRepository = captureHealthRepository,
        )
        CaptureCoordinator(appScope, processor, captureHealthRepository)
    }
}
