package work.hirokuma.mediavolumemuter

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import work.hirokuma.mediavolumemuter.ui.theme.MediaVolumeMuterTheme

private const val TAG = "MainActivity"

class MainActivity() : ComponentActivity() {
    private val logViewModel: LogViewModel by viewModels()

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestNotificationPermission()
        checkAndRequestNotificationPolicyPermission()

        enableEdgeToEdge()
        setContent {
            val logItems by logViewModel.logItems.collectAsState()
            MediaVolumeMuterTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(getString(R.string.app_name)) },
                            modifier = Modifier.statusBarsPadding()
                        )
                    }
                ) { innerPadding ->
                    ChangeVolume(
                        context = this,
                        logItems = logItems,
                        onStopService = { stopNetworkService() },
                        onClearLog = logViewModel::clearLogs,
                        onSaveVolume = { onSaveVolume() },
                        modifier = Modifier
                            .padding(innerPadding)
                            .statusBarsPadding(),
                    )
                }
            }
        }
    }

    private fun onSaveVolume() {
        val savedVolume = Volume.saveVolume(this)
        Toast.makeText(this, getString(R.string.volume_saved, savedVolume), Toast.LENGTH_SHORT)
            .show()
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
fun LogItemCard(item: LogItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Text(
            text = item.timestamp,
            modifier = Modifier.padding(top = 4.dp, start = 16.dp, end = 16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = item.message,
            modifier = Modifier.padding(bottom = 4.dp, start = 16.dp, end = 16.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ChangeVolume(
    context: Context,
    onStopService: () -> Unit,
    onClearLog: () -> Unit,
    onSaveVolume: () -> Unit,
    logItems: List<LogItem>,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(key1 = logItems.size) {
        if (logItems.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(index = logItems.size - 1)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Button(
                onClick = {
                    onStopService()
                },
            ) {
                Text(context.getString(R.string.stop_service))
            }
            Button(
                onClick = {
                    onClearLog()
                },
            ) {
                Text(context.getString(R.string.clear_log))
            }
            Button(
                onClick = {
                    onSaveVolume()
                },
            ) {
                Text(context.getString(R.string.save_volume))
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = logItems, key = { it.id }) { item ->
                LogItemCard(item = item) // LogItemCardの実装は前回と同じ
            }
        }
    }
}

@PreviewScreenSizes()
@Composable
fun ChangeVolumePreview() {
    Surface {
        val item = LogItem(id = 10, timestamp = "1234/45/67", message = "test log")
        ChangeVolume(
            context = LocalContext.current,
            onStopService = {},
            onClearLog = {},
            onSaveVolume = {},
            logItems = listOf(item),
            modifier = Modifier.fillMaxSize()
        )
    }
}