package com.cmc.launcher.game.transfer

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withTimeoutOrNull

internal object TransferHotspots {
    private val ssidPattern = Regex("CMCLauncher-[A-Fa-f0-9]{8}")

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    suspend fun scan(context: Context): List<String> {
        val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
        val finished = CompletableDeferred<Unit>()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                finished.complete(Unit)
            }
        }
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        try {
            if (wifi.startScan()) withTimeoutOrNull(8000) { finished.await() }
            return wifi.scanResults.asSequence()
                .filter { ssidPattern.matches(it.SSID) }
                .sortedByDescending { it.level }
                .map { it.SSID }
                .distinct()
                .toList()
        } finally {
            context.unregisterReceiver(receiver)
        }
    }

    @RequiresApi(29)
    @SuppressLint("MissingPermission")
    fun join(context: Context, ssid: String, password: String) = callbackFlow<Network> {
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val specifier = WifiNetworkSpecifier.Builder().setSsid(ssid).setWpa2Passphrase(password).build()
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(specifier)
            .build()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(network)
            }

            override fun onLost(network: Network) {
                close()
            }

            override fun onUnavailable() {
                close()
            }
        }
        connectivity.requestNetwork(request, callback, 30_000)
        awaitClose { runCatching { connectivity.unregisterNetworkCallback(callback) } }
    }
}
