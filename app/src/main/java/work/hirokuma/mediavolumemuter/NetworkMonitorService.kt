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
        monitor.registerNetworkCallback { wifiInfo ->
            run {
                LogRepository.addLog("SSID: ${wifiInfo.ssid}")
            }
        }
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

//    private fun initializeNetworkCallback() {
//        networkCallback = object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
//            override fun onAvailable(network: Network) {
//                super.onAvailable(network)
//                Log.i(TAG, "Network Available")
//                LogRepository.addLog("Network Available")
//            }
//
//            override fun onLost(network: Network) {
//                super.onLost(network)
//                Log.i(TAG, "Network Lost")
//                setSilentMode(true)
//                changeVolumeNotification(getString(R.string.silent))
//                LogRepository.addLog("Network Lost")
//            }
//
//            override fun onCapabilitiesChanged(
//                network: Network,
//                networkCapabilities: NetworkCapabilities
//            ) {
//                super.onCapabilitiesChanged(network, networkCapabilities)
//
////                CoroutineScope(Dispatchers.Main).launch() {
////                    while (true) {
////                        val network = connectivityManager.activeNetwork
////                        val latestCapabilities = connectivityManager.getNetworkCapabilities(network)
////                        if (latestCapabilities != null) {
////                            val isWifiConnected =
////                                latestCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
////                            val isValidated =
////                                latestCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
////
////                            if (!isWifiConnected) {
////                                // ログ: Wi-Fi接続自体が切断されたか、一時的にトランスポートを失った
////                                // この場合は諦めるか、onLostを待つべき
////                                LogRepository.addLog("onCap: !isWifiConnected")
////                            }
////                            if (!isValidated) {
////                                // ログ: Wi-Fiには接続しているが、インターネット検証が未完了
////                                // この場合、SSID取得に失敗しても不思議ではない
////                                LogRepository.addLog("onCap: !isValidate")
////                            }
////                            val info = (networkCapabilities.transportInfo as? WifiInfo)
////                            if (info != null && info.ssid != WifiManager.UNKNOWN_SSID) {
////                                LogRepository.addLog("onCap: ($isWifiConnected) ($isValidated) $info")
////                                return@launch
////                            }
////                            Log.d(TAG, "SSID: unknown")
////                            delay(1000L)
////                        }
////                    }
////                }
//
//                val info = (networkCapabilities.transportInfo as? WifiInfo)
//                if (info == null) {
//                    LogRepository.addLog("SSID: failed")
//                    return
//                }
//                if (info.ssid != WifiManager.UNKNOWN_SSID) {
//                    setSilentMode(false)
//                    changeVolumeNotification(getString(R.string.normal))
//                    LogRepository.addLog("SSID: ${info.ssid}")
//                } else {
//                    LogRepository.addLog("SSID: unknown")
//                }
//            }
//        }
//    }
//
//    private fun registerNetworkCallback() {
//        val networkRequest = NetworkRequest.Builder()
//            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
//            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
//            .build()
//        try {
//            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
//            Log.i(TAG, "Network callback registered")
//            LogRepository.addLog("Network callback registered")
//        } catch (e: SecurityException) {
//            Log.e(
//                TAG, "Failed to register network callback due to SecurityException. " +
//                        "Ensure ACCESS_NETWORK_STATE permission is granted.", e
//            )
//            LogRepository.addLog("Failed to register network")
//            stopSelf()
//        }
//    }
//
//    private fun unregisterNetworkCallback() {
//        try {
//            connectivityManager.unregisterNetworkCallback(networkCallback)
//            Log.i(TAG, "Network callback unregistered")
//            LogRepository.addLog("Network callback unregistered")
//        } catch (e: IllegalArgumentException) {
//            Log.w(TAG, "Network callback was not registered or already unregistered.", e)
//            LogRepository.addLog("Network callback was not registered")
//        }
//    }

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