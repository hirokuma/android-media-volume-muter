package work.hirokuma.mediavolumemuter

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.util.Log

private const val TAG = "NetworkMonitor"

class NetworkMonitor(var connectivityManager: ConnectivityManager) {
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    fun registerNetworkCallback(onLost: () -> Unit, onConnect: (WifiInfo) -> Unit) {
        networkCallback = object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.i(TAG, "Network Available")
                onLost()
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.i(TAG, "Network Lost")
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val info = (networkCapabilities.transportInfo as? WifiInfo)
                if (info == null) {
                    Log.i(TAG, "no WifiInfo")
                    return
                }
                onConnect(info)
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    fun unregisterNetworkCallback() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}