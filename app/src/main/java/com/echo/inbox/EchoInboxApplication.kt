package com.echo.inbox

import android.app.Application
import com.echo.inbox.di.AppContainer
import com.echo.inbox.di.DefaultAppContainer
import com.echo.inbox.worker.RetentionScheduler

class EchoInboxApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        RetentionScheduler.schedule(this)
    }
}
