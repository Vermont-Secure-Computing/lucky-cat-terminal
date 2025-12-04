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

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat

class POSActivity : BaseNetworkActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pos)
        setupNetworkMonitoring(R.id.networkBanner)
        window.statusBarColor = ContextCompat.getColor(this, R.color.darkerRed)
    }

    override fun onNetworkStatusChanged(status: NetworkStatus) {
        val overlay = findViewById<View>(R.id.offlineOverlay)
        val overlayText = findViewById<android.widget.TextView>(R.id.offlineOverlayText)
        findViewById<POSView>(R.id.posView).setNetworkStatus(status)

        when (status) {
            NetworkStatus.CONNECTED -> {
                overlay.visibility = View.GONE
            }
            NetworkStatus.LIMITED -> {
                overlayText.text = getString(R.string.internet_is_unstable_try_again)
                overlay.visibility = View.VISIBLE
            }
            NetworkStatus.OFFLINE -> {
                overlayText.text = getString(R.string.no_internet_connection)
                overlay.visibility = View.VISIBLE
            }
        }
    }
}

