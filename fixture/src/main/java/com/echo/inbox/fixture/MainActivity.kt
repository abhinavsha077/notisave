package com.echo.inbox.fixture

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scenarios = NotificationScenarioManager(this)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FixtureApp(scenarios)
                }
            }
        }
    }
}

private data class Scenario(
    val title: String,
    val description: String,
    val run: suspend () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FixtureApp(manager: NotificationScenarioManager) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf<String?>(null) }
    var lastResult by remember { mutableStateOf("Ready") }
    var notificationsGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationsGranted = granted
        lastResult = if (granted) "Notification permission granted" else "Permission denied"
    }

    val scenarios = remember(manager) {
        listOf(
            Scenario("Simple + BigText", "Posts a standard notification with expanded text") {
                manager.postSimpleText()
            },
            Scenario("Eight cumulative messages", "Updates one MessagingStyle notification eight times") {
                manager.postCumulativeEightMessages()
            },
            Scenario("Two identical messages", "Posts OK twice with the same sender and notification ID") {
                manager.postRepeatedIdenticalMessages()
            },
            Scenario("Historic message rollover", "Moves First from current to historic while adding Second") {
                manager.postHistoricRollover()
            },
            Scenario("Sliding history window", "Replaces an older five-message window with messages 3–8") {
                manager.postSlidingWindowMessages()
            },
            Scenario("InboxStyle lines", "Posts four sender-prefixed lines") {
                manager.postInboxStyle()
            },
            Scenario("Long Unicode text", "Exercises BigText, emoji, Hindi, and multiline rendering") {
                manager.postBigText()
            },
            Scenario("Summary only", "Posts only “8 new messages,” with no message bodies") {
                manager.postSummaryOnly()
            },
            Scenario("OTP redaction", "Posts a six-digit verification code") {
                manager.postOtp()
            },
            Scenario("Two redacted OTP updates", "Changes one notification between two codes that render identically after redaction") {
                manager.postTwoRedactedOtpUpdates()
            },
            Scenario("Reused notification key", "Cancels and reposts identical content with the same notification ID") {
                manager.postReusedNotificationKey()
            },
            Scenario("Grouped children + summary", "Posts two child notifications and a group summary") {
                manager.postGroupedNotifications()
            },
            Scenario("Rapid burst ×100", "Exercises queue depth and write throughput") {
                manager.postRapidBurst(100)
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Echo Fixture")
                        Text(
                            "Deterministic notification scenarios",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Install Echo Inbox, enable Notification Access, then run these scenarios and inspect the reconstructed timeline.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(lastResult, style = MaterialTheme.typography.labelLarge)
                    if (running != null) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (!notificationsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Button(
                            onClick = {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Outlined.NotificationsActive, contentDescription = null)
                            Text(" Grant notification permission", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }

            items(scenarios, key = { it.title }) { scenario ->
                FilledTonalButton(
                    enabled = running == null && notificationsGranted,
                    onClick = {
                        running = scenario.title
                        scope.launch {
                            try {
                                scenario.run()
                                lastResult = "Completed: ${scenario.title}"
                            } catch (error: CancellationException) {
                                throw error
                            } catch (error: Exception) {
                                lastResult = "Failed: ${error.message ?: error::class.java.simpleName}"
                            } finally {
                                running = null
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(scenario.title, style = MaterialTheme.typography.titleMedium)
                        Text(scenario.description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item {
                Button(
                    enabled = running == null,
                    onClick = {
                        manager.cancelScenarioNotifications()
                        lastResult = "Fixture notifications cancelled; captured history remains in Echo Inbox"
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.DeleteSweep, contentDescription = null)
                    Text(" Cancel fixture notifications", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}
