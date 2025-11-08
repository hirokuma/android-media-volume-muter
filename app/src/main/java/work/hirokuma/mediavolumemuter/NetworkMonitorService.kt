package work.hirokuma.mediavolumemuter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat


class NetworkMonitorService : Service() {
    private lateinit var monitor: NetworkMonitor

    companion object {
        const val ACTION_START_MONITORING = "work.hirokuma.mediavolume.action.START_MONITORING"
        const val ACTION_STOP_MONITORING = "work.hirokuma.mediavolume.action.STOP_MONITORING"
        private const val FOREGROUND_NOTIFICATION_ID = 101
        private const val CHANNEL_ID_FOREGROUND = "network_monitor_foreground_channel"
        private const val TAG = "NetworkMonitorService"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()

        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        monitor = NetworkMonitor(connectivityManager)
        monitor.registerNetworkCallback(
            onLost = {
                setSilentMode(true)
                changeVolumeNotification("lost")
                LogRepository.addLog("Network Lost")
            },
            onConnect = { wifiInfo ->
                setSilentMode(false)
                changeVolumeNotification("connect")
                LogRepository.addLog("SSID: ${wifiInfo.ssid}")
            }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand received: ${intent?.action}")
        when (intent?.action) {
            ACTION_START_MONITORING -> {
                startForegroundServiceNotification()
            }

            ACTION_STOP_MONITORING -> {
                Log.i(TAG, "Network monitoring stopping")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                LogRepository.addLog("Network monitoring stopped")
            }
        }
        return START_STICKY // システムによってサービスが強制終了された場合、再起動を試みる
    }

    override fun onDestroy() {
        super.onDestroy()
        monitor.unregisterNetworkCallback()
        Log.i(TAG, "Service Destroyed, Network monitoring stopped")
        LogRepository.addLog("Service Destroyed")
    }

    private fun setSilentMode(silent: Boolean) {
        Volume.setSilent(applicationContext, silent)
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
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
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