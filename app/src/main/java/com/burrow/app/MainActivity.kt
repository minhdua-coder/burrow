package com.burrow.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.burrow.app.ui.BurrowRoot
import com.burrow.app.ui.theme.Burrow
import com.burrow.app.ui.theme.BurrowTheme
import com.burrow.app.update.EXTRA_PENDING_UPDATE_APK_URL
import com.burrow.app.update.EXTRA_PENDING_UPDATE_TAG
import com.burrow.app.update.UpdateChecker
import com.burrow.app.update.UpdateInstaller
import com.burrow.app.viewmodel.BurrowViewModel

class MainActivity : FragmentActivity() {
    private val pendingIntentState = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingIntentState.value = intent
        setContent {
            BurrowApp(pendingIntentState.value) { pendingIntentState.value = null }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingIntentState.value = intent
    }
}

@Composable
private fun BurrowApp(pendingIntent: Intent?, onPendingIntentHandled: () -> Unit) {
    BurrowTheme {
        Surface(color = Burrow.Neutral200, modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                val viewModel: BurrowViewModel = viewModel()
                val context = LocalContext.current

                val notificationPermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    UpdateChecker.checkForUpdate(context)
                }

                LaunchedEffect(pendingIntent) {
                    val apkUrl = pendingIntent?.getStringExtra(EXTRA_PENDING_UPDATE_APK_URL)
                    val tag = pendingIntent?.getStringExtra(EXTRA_PENDING_UPDATE_TAG)
                    if (apkUrl != null && tag != null) {
                        UpdateInstaller.downloadAndInstall(context, apkUrl, tag)
                        onPendingIntentHandled()
                    }
                }

                BurrowRoot(viewModel)
            }
        }
    }
}
