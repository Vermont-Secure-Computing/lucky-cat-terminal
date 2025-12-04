/*
 * Copyright 2024–2025 Vermont Secure Computing and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
http://www.apache.org/licenses/LICENSE-2.0

 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vermont.possin

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

class NetworkMonitor(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var callback: ConnectivityManager.NetworkCallback? = null
    private var probeJob: Job? = null

    private val _status = MutableStateFlow(NetworkStatus.CONNECTED)
    val status: StateFlow<NetworkStatus> = _status

    fun start() {
        if (callback != null) return
        callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

                if (!hasInternet) {
                    _status.value = NetworkStatus.OFFLINE
                    return
                }
                if (validated) {
                    _status.value = NetworkStatus.CONNECTED
                } else {
                    // Fallback probe when VALIDATED is unreliable
                    probeJob?.cancel()
                    probeJob = scope.launch(Dispatchers.IO) {
                        _status.value = if (isInternetReachable()) NetworkStatus.CONNECTED
                        else NetworkStatus.LIMITED
                    }
                }
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                _status.value = NetworkStatus.LIMITED
            }
            override fun onLost(network: Network) {
                _status.value = NetworkStatus.OFFLINE
            }
            override fun onUnavailable() {
                _status.value = NetworkStatus.OFFLINE
            }
        }
        cm.registerDefaultNetworkCallback(callback!!)
    }

    fun stop() {
        probeJob?.cancel()
        callback?.let { cm.unregisterNetworkCallback(it) }
        callback = null
    }

    private fun isInternetReachable(): Boolean {
        return try {
            val url = URL("https://clients3.google.com/generate_204")
            (url.openConnection() as HttpURLConnection).run {
                connectTimeout = 1500
                readTimeout = 1500
                instanceFollowRedirects = false
                requestMethod = "GET"
                connect()
                val ok = responseCode == 204
                disconnect()
                ok
            }
        } catch (_: Exception) {
            false
        }
    }
}
