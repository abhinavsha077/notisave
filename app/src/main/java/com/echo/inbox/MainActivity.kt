package com.echo.inbox

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.echo.inbox.security.BiometricAuthenticator
import com.echo.inbox.ui.navigation.EchoNavigation
import com.echo.inbox.ui.screens.LockScreen
import com.echo.inbox.ui.screens.OnboardingScreen
import com.echo.inbox.ui.theme.EchoInboxTheme
import com.echo.inbox.ui.viewmodel.OnboardingViewModel
import com.echo.inbox.ui.viewmodel.viewModelFactory

class MainActivity : FragmentActivity() {
    private val container get() = (application as EchoInboxApplication).container
    private lateinit var biometricAuthenticator: BiometricAuthenticator
    private var unlocked by mutableStateOf(false)
    private var authenticationError by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        biometricAuthenticator = BiometricAuthenticator(this)

        setContent {
            val settings by container.settingsRepository.settings.collectAsStateWithLifecycle()
            SideEffect {
                if (settings.screenshotProtectionEnabled) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            EchoInboxTheme(settings.themeMode) {
                when {
                    !settings.onboardingComplete -> {
                        val vm: OnboardingViewModel = viewModel(
                            factory = viewModelFactory {
                                OnboardingViewModel(
                                    container.installedAppProvider,
                                    container.appSourceRepository,
                                    container.settingsRepository,
                                    container.notificationAccessManager,
                                )
                            },
                        )
                        OnboardingScreen(vm, ::openNotificationAccessSettings, ::openAppSettings)
                    }
                    settings.biometricLockEnabled && !unlocked -> {
                        val lockError = authenticationError ?: if (!biometricAuthenticator.canAuthenticate()) {
                            "Echo Inbox is locked. Configure a biometric or secure device credential in Android settings, then try again."
                        } else {
                            null
                        }
                        LockScreen(lockError, ::authenticate)
                    }
                    else -> {
                        EchoNavigation(
                            container = container,
                            openNotificationAccessSettings = ::openNotificationAccessSettings,
                            openAppSettings = ::openAppSettings,
                        )
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Always clear the in-memory unlock token when the task leaves the foreground. This avoids
        // relying on a possibly not-yet-loaded DataStore value during process startup.
        if (!isChangingConfigurations) {
            unlocked = false
            authenticationError = null
        }
    }

    private fun authenticate() {
        authenticationError = null
        biometricAuthenticator.authenticate(
            onSuccess = {
                unlocked = true
                authenticationError = null
            },
            onError = { error -> authenticationError = error },
        )
    }

    private fun openNotificationAccessSettings() {
        runCatching { startActivity(container.notificationAccessManager.settingsIntent()) }
            .onFailure {
                runCatching { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                    .onFailure { startActivity(Intent(Settings.ACTION_SETTINGS)) }
            }
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        )
        startActivity(intent)
    }
}
