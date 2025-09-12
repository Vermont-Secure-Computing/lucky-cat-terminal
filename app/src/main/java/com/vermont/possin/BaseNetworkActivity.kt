package com.vermont.possin

import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

abstract class BaseNetworkActivity : AppCompatActivity() {

    private lateinit var monitor: NetworkMonitor
    private var collectJob: Job? = null

    private var bannerView: TextView? = null
    protected var lastObservedNetworkStatus: NetworkStatus = NetworkStatus.CONNECTED
        private set

    private var pendingStatus: NetworkStatus? = null
    private var debounceJob: Job? = null
    private val debounceMsConnected = 800L
    private val debounceMsLimitedOrOff = 1000L

    /** Call this in onCreate() AFTER setContentView() */
    protected fun setupNetworkMonitoring(bannerTextViewId: Int) {
        bannerView = findViewById(bannerTextViewId)
        monitor = NetworkMonitor(this, lifecycleScope)
    }

    override fun onStart() {
        super.onStart()
        monitor.start()
        collectJob = lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                monitor.status.collect { status ->
                    scheduleStatus(status)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        collectJob?.cancel()
        monitor.stop()
    }

    private fun scheduleStatus(status: NetworkStatus) {
        pendingStatus = status
        debounceJob?.cancel()
        val delayMs = if (status == NetworkStatus.CONNECTED) debounceMsConnected else debounceMsLimitedOrOff
        debounceJob = lifecycleScope.launch {
            delay(delayMs)
            if (pendingStatus == status) {
                lastObservedNetworkStatus = status
                renderBanner(status)
                onNetworkStatusChanged(status) // hook for subclasses if needed
            }
        }
    }

    private fun renderBanner(status: NetworkStatus) {
        val banner = bannerView ?: return
        when (status) {
            NetworkStatus.CONNECTED -> banner.visibility = View.GONE
            NetworkStatus.LIMITED -> {
                banner.text = getString(R.string.internet_is_unstable_try_again)
                banner.visibility = View.VISIBLE
            }
            NetworkStatus.OFFLINE -> {
                banner.text = getString(R.string.no_internet_connection)
                banner.visibility = View.VISIBLE
            }
        }
    }

    /** Optional override in child screens if you want to react to changes */
    protected open fun onNetworkStatusChanged(status: NetworkStatus) = Unit
}
