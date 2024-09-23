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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vermont.possin.adapter.TransactionDividerAdapter
import com.vermont.possin.model.TransactionViewModel
import org.json.JSONObject
import pl.droidsonroids.gif.GifDrawable
import java.io.File
import java.io.InputStreamReader
import java.util.Locale
import java.util.Properties


class HomeActivity : AppCompatActivity() {

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


    // Request permission result launcher
    private val requestBluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Bluetooth permission granted, request enabling Bluetooth
            enableBluetooth()
        } else {
            // Permission denied
            showPermissionDeniedDialog()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home)

        window.statusBarColor = ContextCompat.getColor(this, R.color.darkerRed)

        merchantPropertiesFile = File(filesDir, "merchant.properties")
        configPropertiesFile = File(filesDir, "config.properties")
        apiPropertiesFile = File(filesDir, "api.properties")

        // Load cryptocurrency names from JSON file
        cryptocurrencyNames = loadCryptocurrencyNames()

        // Check if a pin is saved and prompt for it if found
        val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val userPin = sharedPreferences.getString("USER_PIN", null)


        val seeAllTextView = findViewById<TextView>(R.id.seeAllTextView)


        // Set up buttons
        setupButtons()

        // Set up RecyclerView
        val transactionsRecyclerView = findViewById<RecyclerView>(R.id.transactionsRecyclerView)
        val noTransactionsTextView = findViewById<TextView>(R.id.noTransactionsTextView)
        transactionsRecyclerView.layoutManager = LinearLayoutManager(this)

        transactionViewModel.allTransactions.observe(this, Observer { transactions ->
            if (transactions.isNullOrEmpty()) {
                // Show the "No transactions yet" message
                noTransactionsTextView.visibility = View.VISIBLE
                transactionsRecyclerView.visibility = View.GONE

                // Disable the "See all" TextView
                seeAllTextView.isEnabled = false
                seeAllTextView.setTextColor(ContextCompat.getColor(this, R.color.grey)) // Set a color indicating it's disabled

            } else {
                // Hide the "No transactions yet" message and show the RecyclerView
                noTransactionsTextView.visibility = View.GONE
                transactionsRecyclerView.visibility = View.VISIBLE

                // Enable the "See all" TextView
                seeAllTextView.isEnabled = true
                seeAllTextView.setTextColor(ContextCompat.getColor(this, R.color.tapeRed)) // Restore the original color

                val limitedTransactions = transactions.take(3)
                val adapter = TransactionDividerAdapter(this, limitedTransactions)
                transactionsRecyclerView.adapter = adapter
            }
        })

        setPinActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // The pin has been set, update the userPin value
                val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                val userPin = sharedPreferences.getString("USER_PIN", null)
                // Now you can use userPin where needed
                setupButtons()
            }
        }

        seeAllTextView.setOnClickListener {
            // Handle the "See all" click event here
            // Navigate to ViewAllActivity
            val intent = Intent(this, ViewAllActivity::class.java)
            startActivity(intent)
        }

        val nekuGifView = findViewById<ImageView>(R.id.nekuGifView)
        val gifDrawable = GifDrawable(resources, R.raw.neku)
        nekuGifView.setImageDrawable(gifDrawable)

        populateCryptoContainer()

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // Initialize the BroadcastReceiver for Wi-Fi and Bluetooth state changes
        wifiBluetoothReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.action
                if (action == WifiManager.WIFI_STATE_CHANGED_ACTION) {
                    val wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
                    if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
                        // Wi-Fi is enabled, dismiss the dialog
                        dialog?.dismiss()
                    }
                } else if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    if (bluetoothState == BluetoothAdapter.STATE_ON) {
                        // Bluetooth is enabled, dismiss the dialog
                        dialog?.dismiss()
                    }
                }
            }
        }

        // Register receiver for Wi-Fi and Bluetooth state changes
        val wifiFilter = IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION)
        val bluetoothFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)

        registerReceiver(wifiBluetoothReceiver, wifiFilter)
        registerReceiver(wifiBluetoothReceiver, bluetoothFilter)

        // Initial check
        checkWifiAndBluetooth()

        // Initialize the locale spinner
        val localeSpinner = findViewById<Spinner>(R.id.localeSpinner)

        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.locale_array,
            R.layout.spinner_locale
        )

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        localeSpinner.adapter = adapter

        // Set the selected item based on the current locale
        val currentLocale = resources.configuration.locale.language
        localeSpinner.setSelection(if (currentLocale == "zh") 1 else 0)

        // Set a listener for locale selection
        localeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedLocale = when (position) {
                    0 -> "en" // English (US)
                    1 -> "zh" // Chinese
                    else -> Locale.getDefault().language
                }

                // Only set the locale if the selected one is different from the current one
                if (resources.configuration.locale.language != selectedLocale) {
                    setLocale(selectedLocale)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // No action needed
            }
        }

    }

    private fun setupButtons() {
        val button1 = findViewById<ImageButton>(R.id.button1)
        val button2 = findViewById<ImageButton>(R.id.button2)
//        val button3 = findViewById<ImageButton>(R.id.button3)
        val button4 = findViewById<ImageButton>(R.id.button4)
        val button5 = findViewById<ImageButton>(R.id.button5)
        val button6 = findViewById<ImageButton>(R.id.button6)

        val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val userPin = sharedPreferences.getString("USER_PIN", "")

        button1?.setOnClickListener {
            if (!propertiesFilesExist()) {
                if (!merchantPropertiesFile.exists()) {
                    showFillUpProfileDialog(MerchantActivity::class.java)
                } else if (!configPropertiesFile.exists() || configPropertiesContainsDefaultKey()) {
                    showFillUpProfileDialog(XpubAddress::class.java)
                }
            } else if (!inputsExist() || !apiKeyExists()) {
                showFillUpProfileDialog(APIActivity::class.java)
            } else {
                val intent = Intent(this, POSActivity::class.java)
                startActivity(intent)
            }
        }

        button2?.setOnClickListener {
            if (userPin.isNullOrEmpty()) {
                // No pin set, navigate to APIActivity directly
                val intent = Intent(this, APIActivity::class.java)
                startActivity(intent)
            } else {
                // Pin is set, ask the user to enter it
                promptForPin(userPin) {
                    val intent = Intent(this, APIActivity::class.java)
                    startActivity(intent)
                }
            }
        }

//        button3?.setOnClickListener {
//            val intent = Intent(this, BaseCurrency::class.java)
//            startActivity(intent)
//        }

        button4?.setOnClickListener {
            if (userPin.isNullOrEmpty()) {
                // No pin set, navigate to APIActivity directly
                val intent = Intent(this, MerchantActivity::class.java)
                startActivity(intent)
            } else {
                // Pin is set, ask the user to enter it
                promptForPin(userPin) {
                    val intent = Intent(this, MerchantActivity::class.java)
                    startActivity(intent)
                }
            }
        }

        button5?.setOnClickListener {
            if (userPin.isNullOrEmpty()) {
                // No pin set, navigate to APIActivity directly
                val intent = Intent(this, XpubAddress::class.java)
                startActivity(intent)
            } else {
                // Pin is set, ask the user to enter it
                promptForPin(userPin) {
                    val intent = Intent(this, XpubAddress::class.java)
                    startActivity(intent)
                }
            }
        }

        button6.setOnClickListener {
            val intent = Intent(this, ExportDataActivity::class.java)
            startActivity(intent)
        }
    }


    private fun propertiesFilesExist(): Boolean {
        return merchantPropertiesFile.exists() && configPropertiesFile.exists()
    }

    private fun configPropertiesContainsDefaultKey(): Boolean {
        val configProperties = Properties()
        if (configPropertiesFile.exists()) {
            configProperties.load(configPropertiesFile.inputStream())
        }
        return configProperties.getProperty("default_key") != null
    }

    private fun inputsExist(): Boolean {
        val merchantProperties = Properties()
        if (merchantPropertiesFile.exists()) {
            merchantProperties.load(merchantPropertiesFile.inputStream())
        }
        val merchantName = merchantProperties.getProperty("merchant_name", "")

        val configProperties = Properties()
        if (configPropertiesFile.exists()) {
            configProperties.load(configPropertiesFile.inputStream())
        }

        var addressOrXpubExists = false
        for (cryptoName in cryptocurrencyNames) {
            val addressXpub = configProperties.getProperty("${cryptoName}_type", "")
            if (addressXpub.isNotEmpty()) {
                addressOrXpubExists = true
                break
            }
        }

        return merchantName.isNotEmpty() && addressOrXpubExists
    }

    private fun apiKeyExists(): Boolean {
        val apiProperties = Properties()
        if (apiPropertiesFile.exists()) {
            apiProperties.load(apiPropertiesFile.inputStream())
        }
        return apiProperties.getProperty("api_key", "").isNotEmpty()
    }

    private fun showFillUpProfileDialog(activityClass: Class<*>) {
        AlertDialog.Builder(this)
            .setTitle(R.string.incomplete_profile)
            .setMessage(R.string.please_fill_up_your_merchant_profile_xpub_and_address_)
            .setPositiveButton(R.string.ok) { dialog, _ ->
                dialog.dismiss()
                val intent = Intent(this, activityClass)
                startActivity(intent)
            }
            .setCancelable(false)
            .show()
    }

    private fun loadCryptocurrencyNames(): List<String> {
        val inputStream = assets.open("cryptocurrencies.json")
        val reader = InputStreamReader(inputStream)
        val type = object : TypeToken<CryptocurrenciesWrapper>() {}.type
        val cryptocurrenciesWrapper: CryptocurrenciesWrapper = Gson().fromJson(reader, type)
        reader.close()
        inputStream.close()
        return cryptocurrenciesWrapper.cryptocurrencies.map { it.name }
    }

//    private fun checkPinAndProceed(userPin: String?, onSuccess: () -> Unit) {
//        if (userPin.isNullOrEmpty()) {
//            Toast.makeText(this, "No Pin Set. Please Set a Pin First.", Toast.LENGTH_SHORT).show()
//            startActivity(Intent(this, SetPinActivity::class.java))
//        } else {
//            // Show a dialog or another activity to input the pin
//            val pinInput = EditText(this)
//            pinInput.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
//
//            AlertDialog.Builder(this)
//                .setTitle("Enter Pin")
//                .setView(pinInput)
//                .setPositiveButton("OK") { _, _ ->
//                    if (pinInput.text.toString() == userPin) {
//                        onSuccess()
//                    } else {
//                        Toast.makeText(this, "Incorrect Pin", Toast.LENGTH_SHORT).show()
//                    }
//                }
//                .setNegativeButton("Cancel", null)
//                .show()
//        }
//    }

    private fun promptForPin(userPin: String, onSuccess: () -> Unit) {
        // Create a LinearLayout to hold the logo, "Enter Pin" text, pin circles, and keyboard
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Create and configure the ImageView for the logo
        val logoImageView = ImageView(this).apply {
            setImageResource(R.drawable.logo)
            layoutParams = LinearLayout.LayoutParams(
                48.dpToPx(), // Adjust size as needed
                48.dpToPx()
            ).apply {
                setMargins(0, 8.dpToPx(), 0, 8.dpToPx()) // Add bottom margin
            }
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }

        // Add the logo to the main layout
        mainLayout.addView(logoImageView)

        // Create and configure the "Enter Pin" TextView
        val enterPinTextView = TextView(this).apply {
            text = R.string.enter_pin.toString()
            textSize = 18f
            setTextColor(ContextCompat.getColor(this@HomeActivity, R.color.black))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16.dpToPx()) // Add bottom margin
            }
        }

        // Add the "Enter Pin" TextView to the main layout
        mainLayout.addView(enterPinTextView)

        // Create a LinearLayout to hold the pin circles
        val pinLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Create views for each PIN circle
        val pinCircles = arrayOfNulls<View>(4)
        for (i in pinCircles.indices) {
            pinCircles[i] = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(24.dpToPx(), 24.dpToPx()).apply {
                    setMargins(8.dpToPx(), 0, 8.dpToPx(), 0)
                }
                setBackgroundResource(R.drawable.circle_background)
            }
            pinLayout.addView(pinCircles[i])
        }

        // Add pinLayout to mainLayout
        mainLayout.addView(pinLayout)

        // Track user input
        val enteredPin = StringBuilder()

        // Create a GridLayout for the custom keyboard
        val keyboardLayout = GridLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 24.dpToPx(), 0, 0)
            }
            rowCount = 4
            columnCount = 3
        }

        // Add buttons 1-9, 0, and backspace to the keyboard
        val buttonLabels = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "", "0", "⌫")
        for (label in buttonLabels) {
            val button = Button(this).apply {
                text = label
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 60.dpToPx()
                    height = 60.dpToPx()
                    setMargins(4.dpToPx(), 4.dpToPx(), 4.dpToPx(), 4.dpToPx())
                }
                textSize = 18f
                setBackgroundResource(R.drawable.button_background) // Rounded button background
                setTextColor(ContextCompat.getColor(this@HomeActivity, R.color.white))
                isClickable = true
                isFocusable = true
            }

            // Handle button clicks
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

            // Add button to the keyboard layout
            keyboardLayout.addView(button)
        }

        // Add keyboardLayout to mainLayout
        mainLayout.addView(keyboardLayout)

        // Show the custom dialog
        val dialog = AlertDialog.Builder(this)
            .setTitle(null) // No title to keep "Enter Pin" as the only header text
            .setView(mainLayout)
            .setPositiveButton(R.string.ok) { _, _ ->
                if (enteredPin.toString() == userPin) {
                    onSuccess()
                } else {
                    Toast.makeText(this, R.string.incorrect_pin, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            // Change the color of the positive and negative buttons
            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.tapeRed))
            negativeButton.setTextColor(ContextCompat.getColor(this, R.color.tapeRed))
        }

        dialog.show()
    }

    // Function to update the circles based on the number of digits entered
    private fun updatePinCircles(pinLength: Int, pinCircles: Array<View?>) {
        for (i in pinCircles.indices) {
            if (i < pinLength) {
                pinCircles[i]?.setBackgroundResource(R.drawable.filled_circle_background)
            } else {
                pinCircles[i]?.setBackgroundResource(R.drawable.circle_background)
            }
        }
    }

    private fun populateCryptoContainer() {
        val cryptoContainer = findViewById<LinearLayout>(R.id.crypto_container)

        // Load the config.properties file
        val configPropertiesFile = File(filesDir, "config.properties")
        val configProperties = Properties()

        // Remove existing views from the container
        cryptoContainer.removeAllViews()

        // Check if config.properties file exists or if it contains only "default_key"
        val isDefaultKeyOrFileMissing = if (configPropertiesFile.exists()) {
            configProperties.load(configPropertiesFile.inputStream())
            configProperties.size == 1 && configProperties.containsKey("default_key")
        } else {
            true
        }

        // If the config file is missing or only contains "default_key", show a message
        if (isDefaultKeyOrFileMissing) {
            val messageView = TextView(this).apply {
                text = getString(R.string.accepted_coins_are_not_yet_setup)
                textSize = 16f
                setTextColor(ContextCompat.getColor(this@HomeActivity, R.color.dark_gray))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16.dpToPx(), 16.dpToPx(), 16.dpToPx(), 16.dpToPx())
                }
            }

            // Add the message to the crypto_container inside the CardView
            cryptoContainer.addView(messageView)

        } else {
            // Load the JSON file from assets
            val inputStream = assets.open("cryptocurrencies.json")
            val json = inputStream.bufferedReader().use { it.readText() }

            // Parse the JSON as a JSONObject
            val jsonObject = JSONObject(json)

            // Get the JSONArray from the JSONObject
            val jsonArray = jsonObject.getJSONArray("cryptocurrencies")

            // Add logos only for accepted coins
            for (i in 0 until jsonArray.length()) {
                val cryptoObject = jsonArray.getJSONObject(i)
                val cryptoName = cryptoObject.getString("name")
                val cryptoLogo = cryptoObject.getString("logo")

                // Check if this cryptocurrency is accepted in config.properties
                val cryptoValueKey = "${cryptoName}_value"
                val cryptoTypeKey = "${cryptoName}_type"

                if (configProperties.containsKey(cryptoValueKey) && configProperties.containsKey(cryptoTypeKey)) {
                    // If the cryptocurrency is accepted, add its logo

                    // Create an ImageView for the logo, with smaller dimensions
                    val logoView = ImageView(this).apply {
                        val logoResId = resources.getIdentifier(cryptoLogo, "drawable", packageName)
                        setImageResource(logoResId)
                        layoutParams = LinearLayout.LayoutParams(40.dpToPx(), 40.dpToPx()).apply {
                            setMargins(16.dpToPx(), 0, 16.dpToPx(), 0)
                        }
                        scaleType = ImageView.ScaleType.CENTER_INSIDE
                    }

                    // Add the ImageView to the cryptoContainer
                    cryptoContainer.addView(logoView)
                }
            }
        }
    }



    // Extension function to convert dp to pixels
    fun Int.dpToPx(): Int {
        return (this * Resources.getSystem().displayMetrics.density).toInt()
    }

    override fun onResume() {
        super.onResume()
        // Re-check Wi-Fi and Bluetooth status when the user returns to the app
        checkWifiAndBluetooth()
    }

    private fun checkWifiAndBluetooth() {
        var shouldShowDialog = false
        var serviceName = ""

        // Check Wi-Fi status
        if (!wifiManager.isWifiEnabled) {
            shouldShowDialog = true
            serviceName = "Wi-Fi"
        }

        // Check Bluetooth status
        val isBluetoothEnabled = bluetoothAdapter?.isEnabled ?: false
        if (!isBluetoothEnabled) {
            shouldShowDialog = true
            serviceName = if (serviceName.isEmpty()) "Bluetooth" else serviceName // Give priority to Wi-Fi if both are disabled
        }

        if (shouldShowDialog) {
            // Show the dialog if either Wi-Fi or Bluetooth is off
            showWifiBluetoothDialog(serviceName)
        } else {
            // Dismiss the dialog only if both services are enabled
            dialog?.dismiss()
        }
    }

    private fun showWifiBluetoothDialog(serviceName: String) {
        if (dialog == null || !(dialog?.isShowing ?: false)) {
            dialog = AlertDialog.Builder(this)
                .setTitle("$serviceName ${R.string.is_turned_off}")
                .setMessage("${R.string.please_enable} $serviceName ${R.string.to_continue_using_the_apps_features}")
                .setPositiveButton("${R.string.enable} $serviceName") { _, _ ->
                    if (serviceName == "Wi-Fi") {
                        // Open Wi-Fi settings
                        startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                    } else if (serviceName == "Bluetooth") {
                        // Check Bluetooth permission before enabling
                        checkBluetoothPermission()
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    // Check if Bluetooth permissions are granted
    private fun checkBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For Android 12+ (API level 31)
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Permission granted, enable Bluetooth
                enableBluetooth()
            } else {
                // Request Bluetooth connect permission
                requestBluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            // For Android versions lower than 12, directly enable Bluetooth
            enableBluetooth()
        }
    }

    // Function to enable Bluetooth with proper error handling
    private fun enableBluetooth() {
        try {
            // Launch Bluetooth settings and wait for the result
            startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } catch (e: SecurityException) {
            // Handle the case where the Bluetooth permission is not granted
            showPermissionDeniedDialog()
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
        wifiBluetoothReceiver?.let {
            unregisterReceiver(it)
        }
    }

    private fun setLocale(localeCode: String) {
        val locale = Locale(localeCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)

        // Restart the activity to apply the locale change
        val refreshIntent = Intent(this, HomeActivity::class.java)
        startActivity(refreshIntent)
        finish() // Close the current activity so that the new one takes its place
    }
}



data class CryptocurrenciesWrapper(
    val cryptocurrencies: List<CryptoCurrencyInfo>
)
