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
        private const val CHANNEL_ID_FOREGROUND = "network_monitor_foreground_channel"
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
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
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
                setSilentMode(false)
                changeVolumeNotification(getString(R.string.normal))
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.i(TAG, "Network Lost")
                setSilentMode(true)
                changeVolumeNotification(getString(R.string.silent))
            }
        }
    }

    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        try {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
            Log.i(TAG, "Network callback registered")
        } catch (e: SecurityException) {
            Log.e(
                TAG, "Failed to register network callback due to SecurityException. " +
                        "Ensure ACCESS_NETWORK_STATE permission is granted.", e
            )
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

    private fun setSilentMode(silent: Boolean) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.setInterruptionFilter(
            if (silent) NotificationManager.INTERRUPTION_FILTER_NONE
            else NotificationManager.INTERRUPTION_FILTER_ALL
        )
    }

    private fun startForegroundServiceNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java) // 通知タップ時の遷移先Activity
        val pendingIntentFlags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, pendingIntentFlags
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID_FOREGROUND)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.app_description))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // ユーザーがスワイプで消せないようにする
            .build()

        startForeground(
            FOREGROUND_NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
        Log.d(TAG, "Service started in foreground")
    }

    private fun changeVolumeNotification(
        message: String
    ) {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntentFlags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, pendingIntentFlags
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID_FOREGROUND)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(FOREGROUND_NOTIFICATION_ID, notification)
        Log.d(TAG, "Mute status notification shown: $message")
    }

    private fun createNotificationChannels() {
        val foregroundChannel = NotificationChannel(
            CHANNEL_ID_FOREGROUND,
            getString(R.string.app_name),
            NotificationManager.IMPORTANCE_LOW // Ongoingなので低めでOK
        ).apply {
            description = getString(R.string.app_description)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(foregroundChannel)
    }

    override fun onBind(intent: Intent): IBinder? {
        // バインドされたサービスではないので null を返す
        return null
    }
}