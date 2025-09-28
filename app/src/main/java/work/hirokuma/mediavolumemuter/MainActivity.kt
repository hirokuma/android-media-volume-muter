package work.hirokuma.mediavolumemuter

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import work.hirokuma.mediavolumemuter.ui.theme.MediaVolumeMuterTheme
import androidx.core.content.edit

private const val NOTIFICATION_ID = 200
private const val CHANNEL_ID = "MediaVolume"
private const val TAG = "MainActivity"
private val PREFS_NAME = CHANNEL_ID
private val KEY_NAME = "volume"

class MainActivity() : ComponentActivity() {
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        createNotificationChannel()

        checkAndRequestNotificationPermission()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.i("NetworkMonitor", "Network Available")
                // showNetworkStatusNotification("ネットワークに接続しました")
                // ここでUIの更新など、接続時の処理を実装できます
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.i("NetworkMonitor", "Network Lost")
                // showNetworkStatusNotification("ネットワーク接続が失われました")
                // ここでUIの更新など、切断時の処理を実装できます
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val isWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                val isCellular = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                Log.i("NetworkMonitor", "Capabilities Changed: WiFi=$isWifi, Cellular=$isCellular")
                // ネットワークの種類に応じた処理を実装できます
            }
        }

        enableEdgeToEdge()
        setContent {
            MediaVolumeMuterTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ChangeVolume(
                        context = this,
                        modifier = Modifier.padding(100.dp)
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
                Toast.makeText(this, "通知権限が拒否されました。機能が制限されます。", Toast.LENGTH_LONG).show()
            }
        }
    private fun checkAndRequestNotificationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> {
                // 権限は既に許可されている
                startNetworkService()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                // ユーザーに権限が必要な理由を説明する (例: ダイアログ表示)
                // ここでは簡略化のため、再度リクエストのみ行います
                Toast.makeText(this, "ネットワーク状態の通知には通知権限が必要です。", Toast.LENGTH_LONG).show()
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            else -> {
                // 権限をリクエスト
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }


    private fun startNetworkService() {
        val serviceIntent = Intent(this, NetworkMonitorService::class.java).apply {
            action = NetworkMonitorService.ACTION_START_MONITORING
        }
        startForegroundService(serviceIntent)
        Toast.makeText(this, "ネットワーク監視サービスを開始しました", Toast.LENGTH_SHORT).show()
    }

    private fun stopNetworkService() {
        val serviceIntent = Intent(this, NetworkMonitorService::class.java).apply {
            action = NetworkMonitorService.ACTION_STOP_MONITORING
        }
        // stopService(serviceIntent) でも良いが、サービス自身が stopSelf() を呼ぶ設計なので
        // startService で停止アクションを送るだけでも機能する
        startService(serviceIntent)
        Toast.makeText(this, "ネットワーク監視サービスを停止しました", Toast.LENGTH_SHORT).show()
    }

    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) // インターネット接続の監視
            // .addTransportType(NetworkCapabilities.TRANSPORT_WIFI) // WiFiのみ監視する場合
            // .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR) // モバイルデータのみ監視する場合
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        Log.i("NetworkMonitor", "Network callback registered")

        // 初期状態の確認 (オプション)
        checkInitialNetworkState()
    }

    private fun unregisterNetworkCallback() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            Log.i("NetworkMonitor", "Network callback unregistered")
        } catch (e: IllegalArgumentException) {
            // コールバックが既に登録解除されている場合など
            Log.w("NetworkMonitor", "Failed to unregister network callback", e)
        }
    }

    private fun checkInitialNetworkState() {
        val activeNetwork = connectivityManager.activeNetwork
        if (activeNetwork != null) {
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            if (networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                Log.i("NetworkMonitor", "Initial State: Network Available")
            } else {
                Log.i("NetworkMonitor", "Initial State: Network Lost or No Internet")
            }
        } else {
            Log.i("NetworkMonitor", "Initial State: No Active Network")
        }
    }

    // --- 通知関連のメソッド (通知を実装する場合にコメントを解除) ---
    private fun createNotificationChannel() {
        val name = getString(R.string.channel_name) // strings.xml に定義
        val descriptionText = getString(R.string.channel_description) // strings.xml に定義
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun showNetworkStatusNotification(message: String) {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // 通知用のアイコンを適宜設定
            .setContentTitle("ネットワーク状態")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true) // タップで通知を消す

        val notificationManager = ContextCompat.getSystemService(
            this,
            NotificationManager::class.java
        ) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }
}

fun isWifiConnected(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
    return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
}

@Composable
fun ChangeVolume(context: Context, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Button(
            onClick = {
                val audioManager =
                    context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                val sharedPreferences: SharedPreferences =
                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val savedVolume = sharedPreferences.getInt(KEY_NAME, 5)
                sharedPreferences.edit {
                    putInt(KEY_NAME, volume)
                }
                Log.d(TAG, "save volume: $savedVolume --> $volume")

            },
        ) {
            Text("Save Volume")
        }

        Button(
            onClick = {
                val audioManager =
                    context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                if (volume == 0) {
                    val sharedPreferences: SharedPreferences =
                        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val savedVolume = sharedPreferences.getInt(KEY_NAME, 5)

                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, savedVolume, 0)
                    Log.d(TAG, "音量: $volume --> $savedVolume")
                } else {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                    Log.d(TAG, "音量: $volume --> ゼロ")
                }
            },
        ) {
            Text("Change Volume")
        }

        Button(
            onClick = {
                val state = isWifiConnected(context)
                val audioManager =
                    context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                val sharedPreferences: SharedPreferences =
                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val savedVolume = sharedPreferences.getInt(KEY_NAME, 5)
                if (!state) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                    Log.d(TAG, "音量: $volume --> ゼロ")
                } else {
                    Log.d(TAG, "何もしない: $volume")
                }
            },
        ) {
            Text("No WiFi")
        }
    }
}
