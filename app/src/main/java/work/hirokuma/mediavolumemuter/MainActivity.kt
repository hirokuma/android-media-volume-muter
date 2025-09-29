package work.hirokuma.mediavolumemuter

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import work.hirokuma.mediavolumemuter.ui.theme.MediaVolumeMuterTheme

private const val TAG = "MainActivity"

class MainActivity() : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestNotificationPermission()
        checkAndRequestNotificationPolicyPermission()

        enableEdgeToEdge()
        setContent {
            MediaVolumeMuterTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ChangeVolume(
                        context = this,
                        stopService = { stopNetworkService() }
                    )
                }
            }
        }
    }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startNetworkService()
            } else {
                Log.d(TAG, "Requested permission is denied.")
                Toast.makeText(this, getString(R.string.denied), Toast.LENGTH_LONG).show()
            }
        }
    private fun checkAndRequestNotificationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> {
                startNetworkService()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            else -> {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun checkAndRequestNotificationPolicyPermission() {
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            startActivity(intent)
        }
    }

    private fun startNetworkService() {
        val serviceIntent = Intent(this, NetworkMonitorService::class.java).apply {
            action = NetworkMonitorService.ACTION_START_MONITORING
        }
        startForegroundService(serviceIntent)
    }

    private fun stopNetworkService() {
        val serviceIntent = Intent(this, NetworkMonitorService::class.java).apply {
            action = NetworkMonitorService.ACTION_STOP_MONITORING
        }
        startService(serviceIntent)
        finishAndRemoveTask()
        Log.d(TAG, "stopNetworkService")
    }
}

@Composable
fun ChangeVolume(context: Context, stopService: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier) {
        Button(
            onClick = {
                stopService()
            },
        ) {
            Text(context.getString(R.string.stop_service))
        }
    }
}
