package work.hirokuma.mediavolumemuter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class NetworkMonitorService : Service() {

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    companion object {
        const val ACTION_START_MONITORING = "work.hirokuma.mediavolume.action.START_MONITORING"
        const val ACTION_STOP_MONITORING = "work.hirokuma.mediavolume.action.STOP_MONITORING"
        private const val FOREGROUND_NOTIFICATION_ID = 101
        private const val NETWORK_STATUS_NOTIFICATION_ID = 102
        private const val CHANNEL_ID_FOREGROUND = "network_monitor_foreground_channel"
        private const val CHANNEL_ID_STATUS = "network_status_update_channel"
        private const val TAG = "NetworkMonitorService"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service Created")
        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        createNotificationChannels()
        initializeNetworkCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand received: ${intent?.action}")
        when (intent?.action) {
            ACTION_START_MONITORING -> {
                startForegroundServiceNotification()
                registerNetworkCallback()
                Log.i(TAG, "Network monitoring started")
            }

            ACTION_STOP_MONITORING -> {
                Log.i(TAG, "Network monitoring stopping")
                stopSelf() // サービスを停止
            }
        }
        return START_STICKY // システムによってサービスが強制終了された場合、再起動を試みる
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterNetworkCallback()
        Log.i(TAG, "Service Destroyed, Network monitoring stopped")
    }

    private fun initializeNetworkCallback() {
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.i(TAG, "Network Available")
                showNetworkStatusNotification("ネットワークに接続しました", true)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.i(TAG, "Network Lost")
                showNetworkStatusNotification("ネットワーク接続が失われました", false)
            }
        }
    }

    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
            Log.i(TAG, "Network callback registered")
            // 初期状態の確認と通知 (任意)
            checkInitialNetworkStateAndNotify()
        } catch (e: SecurityException) {
            Log.e(
                TAG, "Failed to register network callback due to SecurityException. " +
                        "Ensure ACCESS_NETWORK_STATE permission is granted.", e
            )
            // ここでユーザーに権限がないことを通知するか、サービスを安全に停止する処理を追加
            stopSelf()
        }
    }

    private fun unregisterNetworkCallback() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            Log.i(TAG, "Network callback unregistered")
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Network callback was not registered or already unregistered.", e)
        }
    }

    private fun checkInitialNetworkStateAndNotify() {
        val activeNetwork = connectivityManager.activeNetwork
        if (activeNetwork != null) {
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            if (capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                showNetworkStatusNotification(
                    "ネットワークに接続中です (初期状態)",
                    true,
                    isInitialCheck = true
                )
            } else {
                showNetworkStatusNotification(
                    "ネットワークに接続されていません (初期状態)",
                    false,
                    isInitialCheck = true
                )
            }
        } else {
            showNetworkStatusNotification(
                "ネットワークに接続されていません (初期状態)",
                false,
                isInitialCheck = true
            )
        }
    }


    private fun startForegroundServiceNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java) // 通知タップ時の遷移先Activity
        val pendingIntentFlags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, pendingIntentFlags
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID_FOREGROUND)
            .setContentTitle("ネットワーク監視中")
            .setContentText("ネットワークの状態を監視しています...")
            .setSmallIcon(R.drawable.ic_notification) // フォアグラウンドサービス用のアイコン
            .setContentIntent(pendingIntent)
            .setOngoing(true) // ユーザーがスワイプで消せないようにする
            .build()

        startForeground(FOREGROUND_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        Log.d(TAG, "Service started in foreground")
    }

    private fun showNetworkStatusNotification(
        message: String,
        isConnected: Boolean,
        isInitialCheck: Boolean = false
    ) {
        Log.d(TAG, "isConnected=$isConnected")

        // 初期チェック時の通知は、すでに同じ状態なら表示しないなどの工夫も可能
        if (isInitialCheck) {
            Log.d(TAG, "Initial check notification: $message")
            // ここでは初期チェックでも通知を出すようにしていますが、
            // ユーザー体験を考慮して、アプリ起動直後の冗長な通知は避けることも検討できます。
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntentFlags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, pendingIntentFlags
        )

        val icon = R.drawable.ic_notification

        val notification = NotificationCompat.Builder(this, CHANNEL_ID_STATUS)
            .setContentTitle("ネットワーク状態変化")
            .setContentText(message)
            .setSmallIcon(icon)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // タップで通知を消す
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NETWORK_STATUS_NOTIFICATION_ID, notification)
        Log.d(TAG, "Network status notification shown: $message")
    }

    private fun createNotificationChannels() {
        // Foreground Service Channel
        val foregroundChannelName = "ネットワーク監視サービス"
        val foregroundChannelDescription =
            "ネットワーク状態を監視するためのフォアグラウンドサービス"
        val foregroundImportance = NotificationManager.IMPORTANCE_LOW // Ongoingなので低めでOK
        val foregroundChannel = NotificationChannel(
            CHANNEL_ID_FOREGROUND,
            foregroundChannelName,
            foregroundImportance
        ).apply {
            description = foregroundChannelDescription
        }

        // Network Status Update Channel
        val statusChannelName = "ネットワーク状態更新"
        val statusChannelDescription = "ネットワークの接続状態が変化した際の通知"
        val statusImportance = NotificationManager.IMPORTANCE_DEFAULT
        val statusChannel =
            NotificationChannel(CHANNEL_ID_STATUS, statusChannelName, statusImportance).apply {
                description = statusChannelDescription
            }

        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(foregroundChannel)
        notificationManager.createNotificationChannel(statusChannel)
        Log.d(TAG, "Notification channels created")
    }

    override fun onBind(intent: Intent): IBinder? {
        // バインドされたサービスではないので null を返す
        return null
    }
}