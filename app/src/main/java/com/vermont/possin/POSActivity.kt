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

