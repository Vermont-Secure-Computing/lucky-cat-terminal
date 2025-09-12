package com.vermont.possin

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vermont.possin.adapter.TransactionDividerAdapter
import com.vermont.possin.model.TransactionViewModel
import pl.droidsonroids.gif.GifDrawable
import java.io.File
import java.io.InputStreamReader
import java.util.Locale
import java.util.Properties

class HomeActivity : BaseNetworkActivity() {

    private lateinit var merchantPropertiesFile: File
    private lateinit var configPropertiesFile: File
    private lateinit var apiPropertiesFile: File
    private val transactionViewModel: TransactionViewModel by viewModels()
    private lateinit var cryptocurrencyNames: List<String>
    private lateinit var setPinActivityLauncher: ActivityResultLauncher<Intent>
    private var wifiBluetoothReceiver: BroadcastReceiver? = null
    private lateinit var wifiManager: WifiManager
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var dialog: AlertDialog? = null
    private lateinit var posButton: ImageButton

    // Request permission result launcher
    private val requestBluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) enableBluetooth() else showPermissionDeniedDialog()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home)
        setupNetworkMonitoring(R.id.networkBanner) // <<< set up the shared banner/observer

        window.statusBarColor = ContextCompat.getColor(this, R.color.darkerRed)

        merchantPropertiesFile = File(filesDir, "merchant.properties")
        configPropertiesFile = File(filesDir, "config.properties")
        apiPropertiesFile = File(filesDir, "api.properties")

        cryptocurrencyNames = loadCryptocurrencyNames()

        val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val userPin = sharedPreferences.getString("USER_PIN", null)

        val seeAllTextView = findViewById<TextView>(R.id.seeAllTextView)

        setupButtons()
        applyNetworkStateToButtons(currentNetStatusNow())

        val transactionsRecyclerView = findViewById<RecyclerView>(R.id.transactionsRecyclerView)
        val noTransactionsTextView = findViewById<TextView>(R.id.noTransactionsTextView)
        transactionsRecyclerView.layoutManager = LinearLayoutManager(this)

        transactionViewModel.allTransactions.observe(this, Observer { transactions ->
            if (transactions.isNullOrEmpty()) {
                noTransactionsTextView.visibility = View.VISIBLE
                transactionsRecyclerView.visibility = View.GONE
                seeAllTextView.isEnabled = false
                seeAllTextView.setTextColor(ContextCompat.getColor(this, R.color.grey))
            } else {
                noTransactionsTextView.visibility = View.GONE
                transactionsRecyclerView.visibility = View.VISIBLE
                seeAllTextView.isEnabled = true
                seeAllTextView.setTextColor(ContextCompat.getColor(this, R.color.tapeRed))
                val limitedTransactions = transactions.take(3)
                val adapter = TransactionDividerAdapter(this, limitedTransactions)
                transactionsRecyclerView.adapter = adapter
            }
        })

        setPinActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) setupButtons()
        }

        seeAllTextView.setOnClickListener {
            startActivity(Intent(this, ViewAllActivity::class.java))
        }

        val nekuGifView = findViewById<ImageView>(R.id.nekuGifView)
        val gifDrawable = GifDrawable(resources, R.raw.neku)
        nekuGifView.setImageDrawable(gifDrawable)

//        populateCryptoContainer()

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // Wi-Fi & Bluetooth state receiver
        wifiBluetoothReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                        // Re-evaluate immediately on any Wi-Fi state change
                        applyNetworkStateToButtons(currentNetStatusNow())
                        val wifiState = intent.getIntExtra(
                            WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN
                        )
                        if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
                            dialog?.dismiss()
                        }
                    }
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                        if (state == BluetoothAdapter.STATE_ON) dialog?.dismiss()
                    }
                }
            }
        }

        registerReceiver(wifiBluetoothReceiver, IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION))
        registerReceiver(wifiBluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))

        // Initial check
        checkWifiAndBluetooth()

        // Locale spinner
        val localeSpinner = findViewById<Spinner>(R.id.localeSpinner)
        val adapter = ArrayAdapter.createFromResource(this, R.array.locale_array, R.layout.spinner_locale)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        localeSpinner.adapter = adapter

        val currentLocale = resources.configuration.locale.language
        localeSpinner.setSelection(when (currentLocale) { "zh" -> 1; "ru" -> 2; else -> 0 })

        localeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selected = when (position) { 0 -> "en"; 1 -> "zh"; 2 -> "ru"; else -> Locale.getDefault().language }
                if (resources.configuration.locale.language != selected) setLocale(selected)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    override fun onResume() {
        super.onResume()
        checkWifiAndBluetooth()
        val now = currentNetStatusNow()
        applyNetworkStateToButtons(now)
        populateCryptoContainer(now)
    }

    /** React to network changes from BaseNetworkActivity */
    override fun onNetworkStatusChanged(status: NetworkStatus) {
        applyNetworkStateToButtons(status)
        populateCryptoContainer(status)
    }

    private fun setupButtons() {
        posButton = findViewById<ImageButton>(R.id.button1)
        val button2 = findViewById<ImageButton>(R.id.button2)
        val button4 = findViewById<ImageButton>(R.id.button4)
        val button5 = findViewById<ImageButton>(R.id.button5)
        val button6 = findViewById<ImageButton>(R.id.button6)

        val sp = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val userPin = sp.getString("USER_PIN", "")

        posButton.setOnClickListener {
            // Block if not truly connected right now
            if (currentNetStatusNow() != NetworkStatus.CONNECTED) {
                Toast.makeText(this, getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val next = determineNextSetupStep()
            if (next != null) {
                showFillUpProfileDialog(next)
            } else {
                startActivity(Intent(this, POSActivity::class.java))
            }
        }

        button2?.setOnClickListener {
            if (userPin.isNullOrEmpty()) startActivity(Intent(this, APIActivity::class.java))
            else promptForPin(userPin) { startActivity(Intent(this, APIActivity::class.java)) }
        }

        button4?.setOnClickListener {
            if (userPin.isNullOrEmpty()) startActivity(Intent(this, MerchantActivity::class.java))
            else promptForPin(userPin) { startActivity(Intent(this, MerchantActivity::class.java)) }
        }

        button5?.setOnClickListener {
            if (userPin.isNullOrEmpty()) startActivity(Intent(this, XpubAddress::class.java))
            else promptForPin(userPin) { startActivity(Intent(this, XpubAddress::class.java)) }
        }

        button6.setOnClickListener { startActivity(Intent(this, ExportDataActivity::class.java)) }
    }

    private fun showFillUpProfileDialog(activityClass: Class<*>) {
        AlertDialog.Builder(this)
            .setTitle(R.string.incomplete_profile)
            .setMessage(R.string.please_fill_up_your_merchant_profile_xpub_and_address_)
            .setPositiveButton(R.string.ok) { dialog, _ ->
                dialog.dismiss()
                startActivity(Intent(this, activityClass))
            }
            .setCancelable(false)
            .show()
    }

    private fun loadCryptocurrencyNames(): List<String> {
        val inputStream = assets.open("cryptocurrencies.json")
        val reader = InputStreamReader(inputStream)
        val type = object : TypeToken<CryptocurrenciesWrapper>() {}.type
        val wrapper: CryptocurrenciesWrapper = Gson().fromJson(reader, type)
        reader.close(); inputStream.close()
        return wrapper.cryptocurrencies.map { it.name }
    }

    private fun promptForPin(userPin: String, onSuccess: () -> Unit) {
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val logoImageView = ImageView(this).apply {
            setImageResource(R.drawable.logo)
            layoutParams = LinearLayout.LayoutParams(48.dpToPx(), 48.dpToPx()).apply { setMargins(0, 8.dpToPx(), 0, 8.dpToPx()) }
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        mainLayout.addView(logoImageView)

        val enterPinTextView = TextView(this).apply {
            text = R.string.enter_pin.toString()
            textSize = 18f
            setTextColor(ContextCompat.getColor(this@HomeActivity, R.color.black))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, 16.dpToPx())
            }
        }
        mainLayout.addView(enterPinTextView)

        val pinLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val pinCircles = arrayOfNulls<View>(4)
        for (i in pinCircles.indices) {
            pinCircles[i] = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(24.dpToPx(), 24.dpToPx()).apply { setMargins(8.dpToPx(), 0, 8.dpToPx(), 0) }
                setBackgroundResource(R.drawable.circle_background)
            }
            pinLayout.addView(pinCircles[i])
        }
        mainLayout.addView(pinLayout)

        val enteredPin = StringBuilder()
        val keyboardLayout = GridLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 24.dpToPx(), 0, 0)
            }
            rowCount = 4; columnCount = 3
        }
        val buttonLabels = listOf("1","2","3","4","5","6","7","8","9","","0","⌫")
        for (label in buttonLabels) {
            val button = Button(this).apply {
                text = label
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 60.dpToPx(); height = 60.dpToPx(); setMargins(4.dpToPx(), 4.dpToPx(), 4.dpToPx(), 4.dpToPx())
                }
                textSize = 18f
                setBackgroundResource(R.drawable.button_background)
                setTextColor(ContextCompat.getColor(this@HomeActivity, R.color.white))
                isClickable = true; isFocusable = true
            }
            button.setOnClickListener {
                if (label == "⌫") {
                    if (enteredPin.isNotEmpty()) {
                        enteredPin.deleteCharAt(enteredPin.length - 1)
                        updatePinCircles(enteredPin.length, pinCircles)
                    }
                } else if (label.isNotEmpty() && enteredPin.length < 4) {
                    enteredPin.append(label)
                    updatePinCircles(enteredPin.length, pinCircles)
                }
            }
            keyboardLayout.addView(button)
        }
        mainLayout.addView(keyboardLayout)

        val dialog = AlertDialog.Builder(this)
            .setTitle(null)
            .setView(mainLayout)
            .setPositiveButton(R.string.ok) { _, _ ->
                if (enteredPin.toString() == userPin) onSuccess()
                else Toast.makeText(this, R.string.incorrect_pin, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel) { d, _ -> d.dismiss() }
            .setCancelable(false)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.tapeRed))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.tapeRed))
        }
        dialog.show()
    }

    private fun updatePinCircles(pinLength: Int, pinCircles: Array<View?>) {
        for (i in pinCircles.indices) {
            if (i < pinLength) pinCircles[i]?.setBackgroundResource(R.drawable.filled_circle_background)
            else pinCircles[i]?.setBackgroundResource(R.drawable.circle_background)
        }
    }

    private fun populateCryptoContainer(status: NetworkStatus = currentNetStatusNow()) {
        val cryptoContainer = findViewById<LinearLayout>(R.id.crypto_container)
        cryptoContainer.removeAllViews()

        when (status) {
            NetworkStatus.OFFLINE -> { addInfoMessage(cryptoContainer, getString(R.string.no_internet_connection)); return }
            NetworkStatus.LIMITED -> { addInfoMessage(cryptoContainer, getString(R.string.internet_is_unstable_try_again)); return }
            NetworkStatus.CONNECTED -> { /* proceed */ }
        }

        val cfg = Properties()
        val isDefaultKeyOrFileMissing =
            if (configPropertiesFile.exists()) {
                cfg.load(configPropertiesFile.inputStream())
                cfg.size == 1 && cfg.containsKey("default_key")
            } else true

        if (isDefaultKeyOrFileMissing) {
            addInfoMessage(cryptoContainer, getString(R.string.accepted_coins_are_not_yet_setup))
            return
        }

        val inputStream = assets.open("cryptocurrencies.json")
        val json = inputStream.bufferedReader().use { it.readText() }
        val jsonObject = org.json.JSONObject(json)
        val jsonArray = jsonObject.getJSONArray("cryptocurrencies")
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val name = obj.getString("name")
            val logo = obj.getString("logo")
            if (cfg.containsKey("${name}_value") && cfg.containsKey("${name}_type")) {
                val logoView = ImageView(this).apply {
                    val resId = resources.getIdentifier(logo, "drawable", packageName)
                    setImageResource(resId)
                    layoutParams = LinearLayout.LayoutParams(40.dpToPx(), 40.dpToPx()).apply {
                        setMargins(16.dpToPx(), 0, 16.dpToPx(), 0)
                    }
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                }
                cryptoContainer.addView(logoView)
            }
        }
    }

    private fun addInfoMessage(container: LinearLayout, text: String) {
        val tv = TextView(this).apply {
            this.text = text
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@HomeActivity, R.color.dark_gray))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(16.dpToPx(), 16.dpToPx(), 16.dpToPx(), 16.dpToPx()) }
        }
        container.addView(tv)
    }

    fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()

    private fun checkWifiAndBluetooth() {
        var shouldShowDialog = false
        var serviceName = ""

        if (!wifiManager.isWifiEnabled) {
            shouldShowDialog = true
            serviceName = "Wi-Fi"
        }

        val isBluetoothEnabled = bluetoothAdapter?.isEnabled ?: false
        if (!isBluetoothEnabled) {
            shouldShowDialog = true
            serviceName = if (serviceName.isEmpty()) "Bluetooth" else serviceName
        }

        if (shouldShowDialog) showWifiBluetoothDialog(serviceName) else dialog?.dismiss()
    }

    private fun showWifiBluetoothDialog(serviceName: String) {
        if (dialog == null || !(dialog?.isShowing ?: false)) {
            dialog = AlertDialog.Builder(this)
                .setTitle("$serviceName ${getString(R.string.is_turned_off)}")
                .setMessage("${getString(R.string.please_enable)} $serviceName ${getString(R.string.to_continue_using_the_apps_features)}")
                .setPositiveButton("${getString(R.string.enable)} $serviceName") { _, _ ->
                    if (serviceName == "Wi-Fi") startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                    else checkBluetoothPermission()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun checkBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                enableBluetooth()
            } else {
                requestBluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            enableBluetooth()
        }
    }

    private fun enableBluetooth() {
        try {
            startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } catch (e: SecurityException) {
            showPermissionDeniedDialog()
        }
    }

    private fun isMerchantProfileComplete(): Boolean {
        if (!merchantPropertiesFile.exists()) return false
        val merchantProps = Properties().apply { load(merchantPropertiesFile.inputStream()) }
        val merchantName = merchantProps.getProperty("merchant_name", "").trim()
        return merchantName.isNotEmpty()
    }

    private fun isCryptoConfigured(): Boolean {
        if (!configPropertiesFile.exists()) return false
        val cfg = Properties().apply { load(configPropertiesFile.inputStream()) }
        val onlyDefaultKey = cfg.size == 1 && cfg.containsKey("default_key")
        if (onlyDefaultKey) return false
        for (cryptoName in cryptocurrencyNames) {
            val type = cfg.getProperty("${cryptoName}_type", "").trim()
            val value = cfg.getProperty("${cryptoName}_value", "").trim()
            if (type.isNotEmpty() && value.isNotEmpty()) return true
        }
        return false
    }

    private fun isApiKeyPresent(): Boolean {
        val key = ApiKeyStore.get(this)?.trim().orEmpty()
        return key.isNotEmpty()
    }

    private fun determineNextSetupStep(): Class<*>? {
        val merchantComplete = isMerchantProfileComplete()
        val cryptoConfigured = isCryptoConfigured()
        val apiPresent = isApiKeyPresent()

        Log.d("SetupCheck", "merchant=$merchantComplete, crypto=$cryptoConfigured, api=$apiPresent")

        return when {
            !isMerchantProfileComplete() -> MerchantActivity::class.java
            !isCryptoConfigured() -> XpubAddress::class.java
            !isApiKeyPresent() -> APIActivity::class.java
            else -> null
        }
    }

    private fun showPermissionDeniedDialog() {
        dialog = AlertDialog.Builder(this)
            .setTitle(R.string.bluetooth_permission_denied)
            .setMessage(R.string.bluetooth_is_required_for_this_feature_please_enable_the_permission_from_settings)
            .setPositiveButton(R.string.go_to_settings) { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivity(intent)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        wifiBluetoothReceiver?.let { unregisterReceiver(it) }
    }

    private fun setLocale(localeCode: String) {
        val locale = Locale(localeCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun applyNetworkStateToButtons(status: NetworkStatus) {
        val enablePos = status == NetworkStatus.CONNECTED
        posButton.isEnabled = enablePos
        posButton.isClickable = enablePos
        posButton.alpha = if (enablePos) 1f else 0.5f  // dim when disabled (optional)
    }

    private fun currentNetStatusNow(): NetworkStatus {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = cm.activeNetwork ?: return NetworkStatus.OFFLINE
        val caps = cm.getNetworkCapabilities(network) ?: return NetworkStatus.OFFLINE
        val hasInternet = caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val validated  = caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        return when {
            hasInternet && validated -> NetworkStatus.CONNECTED
            hasInternet && !validated -> NetworkStatus.LIMITED
            else -> NetworkStatus.OFFLINE
        }
    }
}

data class CryptocurrenciesWrapper(
    val cryptocurrencies: List<CryptoCurrencyInfo>
)
