package com.vermont.possin

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.vermont.possin.model.ConversionResponse
import com.vermont.possin.network.ConversionRequestBody
import com.vermont.possin.network.RetrofitClient
import org.json.JSONObject
import pl.droidsonroids.gif.GifDrawable
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.InputStreamReader
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.Properties

data class CryptoCurrency(val name: String, val shortname: String, val chain: String, val logo: String)

class CryptoOptionActivity : BaseNetworkActivity() {   // <— use your network helpers

    private var bitcoinManager: BitcoinManager? = null
    private var litecoinManager: LitecoinManager? = null
    private var ethereumManager: EthereumManager? = null
    private var dogecoinManager: DogecoinManager? = null
    private var woodcoinManager: WoodcoinManager? = null
    private var dashManager: DashManager? = null
    private var tronManager: TronManager? = null
    private var bitcoincashManager: BitcoinCashManager? = null
    private var moneroManager: MoneroManager? = null
    private var solanaManager: SolanaManager? = null
    private lateinit var selectedCurrencyCode: String
    private lateinit var message: String
    private var loadingDialog: AlertDialog? = null

    // keep references to enable/disable on net changes
    private val clickableViews = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.crypto_option)

        // Optional banner; safe even if crypto_option has no networkBanner TextView
        setupNetworkMonitoring(R.id.networkBanner)

        window.statusBarColor = ContextCompat.getColor(this, R.color.darkerRed)

        val price = intent.getStringExtra("PRICE") ?: "0.00"
        val priceTextView: TextView = findViewById(R.id.priceTextView)
        val cleanedPrice = price.replace(Regex("[^\\d.]"), "").trim()
        val priceValue = cleanedPrice.toDoubleOrNull() ?: 0.00
        if (priceValue >= 10000) priceTextView.textSize = 30f
        priceTextView.text = price

        message = intent.getStringExtra("MESSAGE") ?: ""

        val xPubs = loadXPubsFromSettings()
        xPubs["Bitcoin"]?.let { bitcoinManager = BitcoinManager(this, it) }

        xPubs["Litecoin"]?.let { litecoinValue ->
            val litecoinType = getCryptoTypeFromConfig("Litecoin_type")
            if (litecoinType == "xpub") {
                val convertedXpub = if (litecoinValue.startsWith("xpub")) {
                    try { LitecoinManager.convertBitcoinXpubToLitecoin(litecoinValue) }
                    catch (e: Exception) { Log.e("LitecoinManager","Error converting xPub: ${e.message}"); return@let }
                } else litecoinValue
                if (LitecoinManager.isValidXpub(convertedXpub, this)) {
                    litecoinManager = LitecoinManager(this, convertedXpub)
                } else Log.e("LitecoinManager","xPub is invalid: $convertedXpub")
            } else {
                litecoinManager = LitecoinManager(this, litecoinValue)
            }
        }

        xPubs["Ethereum"]?.let { ethereumManager = EthereumManager(this, it) }
        xPubs["Dogecoin"]?.let { dogecoinValue ->
            val dogecoinType = getCryptoTypeFromConfig("Dogecoin_type")
            if (dogecoinType == "xpub") {
                val convertedXpub = if (dogecoinValue.startsWith("xpub")) {
                    try { DogecoinManager.convertBitcoinXpubToDogecoin(dogecoinValue) }
                    catch (e: Exception) { Log.e("DogecoinManager","Error converting xPub: ${e.message}"); return@let }
                } else dogecoinValue
                if (DogecoinManager.isValidXpub(convertedXpub, this)) {
                    dogecoinManager = DogecoinManager(this, convertedXpub)
                } else Log.e("DogecoinManager","xPub is invalid: $convertedXpub")
            } else {
                dogecoinManager = DogecoinManager(this, dogecoinValue)
            }
        }

        xPubs["Woodcoin"]?.let { woodcoinManager = WoodcoinManager(this, it) }
        xPubs["Dash"]?.let { dashManager = DashManager(this, it) }
        xPubs["Tether"]?.let { tronManager = TronManager(this, it) }
        xPubs["Bitcoincash"]?.let { bitcoincashManager = BitcoinCashManager(this, it) }
        xPubs["Solana"]?.let { solanaManager = SolanaManager(this, it) }
        xPubs["USDC"]?.let { solanaManager = SolanaManager(this, it) }

        val properties = loadPropertiesFromConfigFile()
        val privateViewKey = properties.getProperty("Monero_view_key")
        val moneroAddress = properties.getProperty("Monero_value")
        if (privateViewKey != null && moneroAddress != null) {
            moneroManager = MoneroManager(this, privateViewKey, moneroAddress)
        } else {
            Log.e("Monero","Monero keys or address not found in config file.")
        }

        selectedCurrencyCode = intent.getStringExtra("CURRENCY_CODE") ?: "BTC"

        val buttonContainer: LinearLayout = findViewById(R.id.buttonContainer)
        val cryptoCurrencies = loadCryptocurrenciesFromJson()
        cryptoCurrencies.forEach { crypto ->
            val logoResId = resources.getIdentifier(crypto.logo, "drawable", packageName)
            addCardView(buttonContainer, logoResId, xPubs.containsKey(crypto.name), crypto) {
                // HARD GUARD: only handle clicks when truly CONNECTED
                if (lastObservedNetworkStatus != NetworkStatus.CONNECTED) {
                    Toast.makeText(this, getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show()
                    return@addCardView
                }
                handleCryptoClick(crypto, price)
            }
        }

        findViewById<ImageView>(R.id.back_arrow)?.setOnClickListener { finish() }
        findViewById<ImageView>(R.id.nekuGifImageView)?.apply {
            val gifDrawable = GifDrawable(resources, R.raw.neku)
            setImageDrawable(gifDrawable)
        }

        // Initial state (will be corrected again by onNetworkStatusChanged after debounce)
        applyNetworkStateToUi(lastObservedNetworkStatus)
    }

    override fun onResume() {
        super.onResume()
        // Re-apply quickly when returning from settings etc.
        applyNetworkStateToUi(lastObservedNetworkStatus)
    }

    /** Called by BaseNetworkActivity whenever status (debounced) changes */
    override fun onNetworkStatusChanged(status: NetworkStatus) {
        super.onNetworkStatusChanged(status) // optional
        applyNetworkStateToUi(status) // you already have this

        // Toggle the overlay (optional visual block)
        val overlay = findViewById<View>(R.id.offlineOverlay)
        val label   = findViewById<TextView>(R.id.offlineOverlayText)
        when (status) {
            NetworkStatus.CONNECTED -> overlay?.visibility = View.GONE
            NetworkStatus.LIMITED -> {
                label?.text = getString(R.string.internet_is_unstable_try_again)
                overlay?.visibility = View.VISIBLE
            }
            NetworkStatus.OFFLINE -> {
                label?.text = getString(R.string.no_internet_connection)
                overlay?.visibility = View.VISIBLE
            }
        }
    }

    private fun applyNetworkStateToUi(status: NetworkStatus) {
        val enable = status == NetworkStatus.CONNECTED
        clickableViews.forEach { v ->
            v.isEnabled = enable
            v.isClickable = enable
            v.alpha = if (enable) 1f else 0.5f
        }
    }

    private fun getCryptoTypeFromConfig(cryptoTypeKey: String): String {
        val properties = Properties()
        val propertiesFile = File(filesDir, "config.properties")
        if (propertiesFile.exists()) properties.load(propertiesFile.inputStream())
        return properties.getProperty(cryptoTypeKey, "xpub")
    }

    private fun loadCryptocurrenciesFromJson(): List<CryptoCurrency> {
        val inputStream = assets.open("cryptocurrencies.json")
        val reader = InputStreamReader(inputStream)
        val jsonObject = Gson().fromJson(reader, JsonObject::class.java)
        val cryptoList = Gson().fromJson(jsonObject.getAsJsonArray("cryptocurrencies"), Array<CryptoCurrency>::class.java).toList()
        reader.close()
        return cryptoList
    }

    // Loading dialog with GIF
    private fun showLoadingDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_loading, null)
        val gifImageView: ImageView = dialogView.findViewById(R.id.loadingGifImageView)
        val gifDrawable = GifDrawable(resources, R.raw.rotating_arc_gradient_thick)
        gifImageView.setImageDrawable(gifDrawable)

        loadingDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        loadingDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        loadingDialog?.window?.setDimAmount(0.5f)
        loadingDialog?.show()
    }

    private fun dismissLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    private fun addCardView(
        container: LinearLayout,
        imageResId: Int,
        isVisible: Boolean,
        crypto: CryptoCurrency,
        onClick: () -> Unit
    ) {
        var isClicked = false

        val cardView = CardView(this).apply {
            radius = 16f
            cardElevation = 8f
            visibility = if (isVisible) View.VISIBLE else View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8)) }

            setOnClickListener {
                if (!isClicked) {
                    isClicked = true
                    onClick()
                }
            }
        }

        val cardContent = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
            gravity = android.view.Gravity.CENTER
            setBackgroundResource(R.drawable.button_selector)
        }

        val imageButton = ImageButton(this).apply {
            val originalBitmap = BitmapFactory.decodeResource(resources, imageResId)
            val targetSize = 300
            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, targetSize, targetSize, true)
            val scaledBitmap = Bitmap.createScaledBitmap(resizedBitmap, targetSize / 2, targetSize / 2, true)
            setImageBitmap(scaledBitmap)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8)) }
            background = null
            setOnClickListener {
                if (!isClicked) {
                    isClicked = true
                    onClick()
                }
            }
        }

        val textLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(dpToPx(16), 0, 0, 0) }
        }

        val nameTextView = TextView(this).apply {
            text = crypto.name
            textSize = 18f
            setTextColor(resources.getColor(android.R.color.black))
        }

        val shortnameTextView = TextView(this).apply {
            text = crypto.shortname
            textSize = 14f
            setTextColor(resources.getColor(android.R.color.darker_gray))
        }

        val chainTextView = TextView(this).apply {
            text = crypto.chain
            textSize = 14f
            setTextColor(resources.getColor(android.R.color.darker_gray))
        }

        textLayout.addView(nameTextView)
        textLayout.addView(shortnameTextView)
        textLayout.addView(chainTextView)

        cardContent.addView(imageButton)
        cardContent.addView(textLayout)

        cardView.addView(cardContent)
        container.addView(cardView)

        // Track clickables so we can enable/disable them on net changes
        if (isVisible) {
            clickableViews.add(cardView)
            clickableViews.add(imageButton)
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    private fun loadXPubsFromSettings(): Map<String, String> {
        val properties = Properties()
        val propertiesFile = File(filesDir, "config.properties")
        if (propertiesFile.exists()) properties.load(propertiesFile.inputStream())

        val xPubs = mutableMapOf<String, String>()
        properties.stringPropertyNames().forEach { key ->
            if (key.endsWith("_value")) {
                val cryptoKey = key.substringBefore("_value")
                properties.getProperty(key)?.let { xPubs[cryptoKey] = it }
            }
        }
        return xPubs
    }

    private fun handleCryptoClick(crypto: CryptoCurrency, price: String) {
        showLoadingDialog()
        when (crypto.shortname) {
            "BTC" -> handleBTCClick(price)
            "LTC" -> handleLTCClick(price)
            "ETH" -> handleETHClick(price)
            "DOGE" -> handleDOGEClick(price)
            "USDT" -> handleUSDTClick(price)
            "DASH" -> handleDASHlick(price)
            "BCH" -> handleBCHlick(price)
            "XMR" -> handleMoneroClick(price)
            "SOL" -> handleSolanaClick(price)
            "USDC" -> handleUSDCClick(price)
        }
    }

    private fun handleBTCClick(price: String) {
        bitcoinManager?.let { manager ->
            val (address, index) = if (BitcoinManager.isValidAddress(manager.getXpub())) Pair(manager.getXpub(), -1) else manager.getAddress()
            val numericPrice = price.filter { it.isDigit() || it == '.' }

            postConversionApi(numericPrice, selectedCurrencyCode, address, "BTC", R.drawable.bitcoin_logo) { feeStatus, status, formattedRate ->
                dismissLoadingDialog()
                if (formattedRate.isNotEmpty()) {
                    startGenerateQRActivity(address, formattedRate, R.drawable.bitcoin_logo, "BTC", index, feeStatus, status, "Bitcoin", numericPrice, selectedCurrencyCode, "BTC")
                }
            }
        } ?: run {
            dismissLoadingDialog()
            Toast.makeText(this, R.string.bitcoin_manager_not_initialized, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleLTCClick(price: String) {
        litecoinManager?.let { manager ->
            try {
                val (address, index) = if (LitecoinManager.isValidAddress(manager.getXpub())) Pair(manager.getXpub(), -1) else manager.getAddress()
                val numericPrice = price.filter { it.isDigit() || it == '.' }

                postConversionApi(numericPrice, selectedCurrencyCode, address, "LTC", R.drawable.litecoin_new_logo) { feeStatus, status, formattedRate ->
                    dismissLoadingDialog()
                    if (formattedRate.isNotEmpty()) {
                        startGenerateQRActivity(address, formattedRate, R.drawable.litecoin_new_logo, "LTC", index, feeStatus, status, "Litecoin", numericPrice, selectedCurrencyCode, "LTC")
                    }
                }
            } catch (e: Exception) {
                dismissLoadingDialog()
                Log.e("CryptoOptionActivity","Error handling Litecoin click: ${e.message}")
                Toast.makeText(this, R.string.failed_to_derive_address, Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            dismissLoadingDialog()
            Toast.makeText(this, R.string.litecoin_manager_not_initialized, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleETHClick(price: String) {
        ethereumManager?.let { manager ->
            val xpubOrAddress = manager.getXpub()
            val (isValidEth, normalized, _) = EthereumManager.validateAddress(xpubOrAddress)

            val (address, index) = if (isValidEth && normalized != null) {
                Pair(normalized, -1)
            } else {
                manager.getAddress()
            }

            val numericPrice = price.filter { it.isDigit() || it == '.' }

            postConversionApi(numericPrice, selectedCurrencyCode, address, "ETH", R.drawable.ethereum_logo) { feeStatus, status, formattedRate ->
                dismissLoadingDialog()
                if (formattedRate.isNotEmpty()) {
                    startGenerateQRActivity(address, formattedRate, R.drawable.ethereum_logo, "ETH", index, feeStatus, status, "Ethereum", numericPrice, selectedCurrencyCode, "ETH")
                }
            }
        } ?: run {
            dismissLoadingDialog()
            Toast.makeText(this, R.string.ethereum_manager_not_initialized, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleDOGEClick(price: String) {
        dogecoinManager?.let { manager ->
            val (address, index) = if (DogecoinManager.isValidAddress(manager.getXpub())) Pair(manager.getXpub(), -1) else manager.getAddress()
            val numericPrice = price.filter { it.isDigit() || it == '.' }

            postConversionApi(numericPrice, selectedCurrencyCode, address, "DOGE", R.drawable.dogecoin_logo) { feeStatus, status, formattedRate ->
                dismissLoadingDialog()
                if (formattedRate.isNotEmpty()) {
                    startGenerateQRActivity(address, formattedRate, R.drawable.dogecoin_logo, "DOGE", index, feeStatus, status, "Dogecoin", numericPrice, selectedCurrencyCode, "DOGE")
                }
            }
        } ?: run {
            dismissLoadingDialog()
            Toast.makeText(this, R.string.dogecoin_manager_not_initialized, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleUSDTClick(price: String) {
        tronManager?.let { manager ->
            val (address, index) = if (TronManager.isValidAddress(manager.getXpub())) Pair(manager.getXpub(), -1) else manager.getAddress()
            val numericPrice = price.filter { it.isDigit() || it == '.' }

            postConversionApi(numericPrice, selectedCurrencyCode, address, "TRON-NETWORK", R.drawable.tether_logo) { feeStatus, status, formattedRate ->
                dismissLoadingDialog()
                if (formattedRate.isNotEmpty()) {
                    startGenerateQRActivity(address, formattedRate, R.drawable.tether_logo, "TRON-NETWORK", index, feeStatus, status, "Tron-network", numericPrice, selectedCurrencyCode, "USDT")
                }
            }
        } ?: run {
            dismissLoadingDialog()
            Toast.makeText(this, R.string.usdt_manager_not_initialized, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleDASHlick(price: String) {
        dashManager?.let { manager ->
            val addressIndexPair: Pair<String, Int>? = if (DashManager.isValidAddress(manager.getXpub())) {
                Pair(manager.getXpub(), -1)
            } else manager.getAddress()

            if (addressIndexPair == null) {
                Toast.makeText(this, R.string.failed_to_derive_dash_address, Toast.LENGTH_SHORT).show()
                return
            }

            val (address, index) = addressIndexPair
            val numericPrice = price.filter { it.isDigit() || it == '.' }

            postConversionApi(numericPrice, selectedCurrencyCode, address, "DASH", R.drawable.dashcoin_logo) { feeStatus, status, formattedRate ->
                dismissLoadingDialog()
                if (formattedRate.isNotEmpty()) {
                    startGenerateQRActivity(address, formattedRate, R.drawable.dashcoin_logo, "DASH", index, feeStatus, status, "Dash", numericPrice, selectedCurrencyCode, "DASH")
                }
            }
        } ?: run {
            dismissLoadingDialog()
            Toast.makeText(this, R.string.dash_manager_not_initialized, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleBCHlick(price: String) {
        bitcoincashManager?.let { manager ->
            val (address, index) = if (BitcoinCashManager.isValidAddress(manager.getXpub())) Pair(manager.getXpub(), -1) else manager.getAddress()
            val numericPrice = price.filter { it.isDigit() || it == '.' }

            postConversionApi(numericPrice, selectedCurrencyCode, address, "BCH", R.drawable.bitcoin_cash) { feeStatus, status, formattedRate ->
                dismissLoadingDialog()
                if (formattedRate.isNotEmpty()) {
                    startGenerateQRActivity(address, formattedRate, R.drawable.bitcoin_cash, "BCH", index, feeStatus, status, "Bitcoincash", numericPrice, selectedCurrencyCode, "BCH")
                }
            }
        } ?: run {
            dismissLoadingDialog()
            Toast.makeText(this, R.string.bitcoincash_manager_not_initialized, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleMoneroClick(price: String) {
        moneroManager?.let { manager ->
            val address = manager.getAddress()
            if (address != null && MoneroManager.isValidAddress(address)) {
                val numericPrice = price.filter { it.isDigit() || it == '.' }
                postConversionApi(numericPrice, selectedCurrencyCode, address, "XMR", R.drawable.monero_logo) { feeStatus, status, formattedRate ->
                    dismissLoadingDialog()
                    if (formattedRate.isNotEmpty()) {
                        startGenerateQRActivity(address, formattedRate, R.drawable.monero_logo, "XMR", 0, feeStatus, status, "Monero", numericPrice, selectedCurrencyCode, "XMR")
                    } else {
                        Toast.makeText(this, R.string.conversion_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                dismissLoadingDialog()
                Toast.makeText(this, R.string.invalid_monero_address, Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            dismissLoadingDialog()
            Toast.makeText(this, R.string.monero_manager_not_initialized, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleSolanaClick(price: String) {
        solanaManager?.let { manager ->
            val (address, index) = if (SolanaManager.isValidAddress(manager.getXpub())) Pair(manager.getXpub(), -1) else manager.getAddress()
            val numericPrice = price.filter { it.isDigit() || it == '.' }
            postConversionApi(numericPrice, selectedCurrencyCode, address, "SOL", R.drawable.solana) { feeStatus, status, formattedRate ->
                dismissLoadingDialog()
                if (formattedRate.isNotEmpty()) {
                    startGenerateQRActivity(address, formattedRate, R.drawable.solana, "SOL", index, feeStatus, status, "Solana", numericPrice, selectedCurrencyCode, "SOL")
                }
            }
        } ?: run {
            dismissLoadingDialog()
            Toast.makeText(this, R.string.solana_manager_not_initialized, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleUSDCClick(price: String) {
        solanaManager?.let { manager ->
            val (address, index) = if (SolanaManager.isValidAddress(manager.getXpub())) Pair(manager.getXpub(), -1) else manager.getAddress()
            val numericPrice = price.filter { it.isDigit() || it == '.' }
            postConversionApi(numericPrice, selectedCurrencyCode, address, "USDC", R.drawable.usdc) { feeStatus, status, formattedRate ->
                dismissLoadingDialog()
                if (formattedRate.isNotEmpty()) {
                    startGenerateQRActivity(address, formattedRate, R.drawable.solana, "USDC", index, feeStatus, status, "USDC", numericPrice, selectedCurrencyCode, "USDC")
                }
            }
        } ?: run {
            dismissLoadingDialog()
            Toast.makeText(this, R.string.solana_manager_not_initialized, Toast.LENGTH_SHORT).show()
        }
    }

    private fun postConversionApi(
        price: String,
        currency: String,
        address: String,
        chain: String,
        logoResId: Int,
        onResult: (String, String, String) -> Unit
    ) {
        val apiService = RetrofitClient.getApiService(this)
        val call = apiService.postConversion(ConversionRequestBody(price, currency, chain))

        call.enqueue(object : Callback<ConversionResponse> {
            override fun onResponse(call: Call<ConversionResponse>, response: Response<ConversionResponse>) {
                if (response.isSuccessful) {
                    val conversionResponse = response.body()
                    conversionResponse?.let {
                        val feeStatus = it.feeStatus ?: ""
                        val status = it.status ?: ""
                        if (it.success) {
                            val formattedRate = formatConversionRate(it.conversionRate)
                            Toast.makeText(this@CryptoOptionActivity, getString(R.string.conversion_rate, formattedRate), Toast.LENGTH_SHORT).show()
                            onResult(feeStatus, status, formattedRate)
                        } else {
                            Toast.makeText(this@CryptoOptionActivity, R.string.conversion_failed, Toast.LENGTH_SHORT).show()
                            onResult(feeStatus, status, "")
                        }
                    } ?: run { onResult("", "", "") }
                    return
                }

                if (response.code() == 403) {
                    val errMsg = try {
                        val raw = response.errorBody()?.string().orEmpty()
                        if (raw.isNotEmpty()) JSONObject(raw).optString("message", "") else ""
                    } catch (_: Exception) { "" }

                    if (errMsg.equals("API key has expired", ignoreCase = true)) {
                        dismissLoadingDialog()
                        showExpiredDialog()
                        onResult("", "", "")
                        return
                    }
                }

                Toast.makeText(
                    this@CryptoOptionActivity,
                    R.string.response_not_successful_Please_check_your_setting_or_API_key,
                    Toast.LENGTH_SHORT
                ).show()
                onResult("", "", "")
            }

            override fun onFailure(call: Call<ConversionResponse>, t: Throwable) {
                Toast.makeText(this@CryptoOptionActivity, R.string.API_call_failed, Toast.LENGTH_SHORT).show()
                onResult("", "", "")
            }
        })
    }

    private fun loadPropertiesFromConfigFile(): Properties {
        val properties = Properties()
        val propertiesFile = File(filesDir, "config.properties")
        if (propertiesFile.exists()) properties.load(propertiesFile.inputStream())
        return properties
    }

    fun formatConversionRate(rate: Double, decimalPlaces: Int = 8): String {
        val bigDecimal = BigDecimal(rate).setScale(decimalPlaces, RoundingMode.HALF_UP)
        val decimalFormat = DecimalFormat()
        decimalFormat.maximumFractionDigits = decimalPlaces
        return decimalFormat.format(bigDecimal)
    }

    private fun startGenerateQRActivity(
        address: String,
        price: String,
        logoResId: Int,
        currency: String,
        index: Int,
        feeStatus: String,
        status: String,
        managerType: String,
        numericPrice: String,
        selectedCurrencyCode: String,
        shortname: String
    ) {
        val intent = Intent(this, GenerateQRActivity::class.java).apply {
            putExtra("ADDRESS", address)
            putExtra("PRICE", price)
            putExtra("LOGO_RES_ID", logoResId)
            putExtra("CURRENCY", currency)
            putExtra("ADDRESS_INDEX", index)
            putExtra("FEE_STATUS", feeStatus)
            putExtra("STATUS", status)
            putExtra("MANAGER_TYPE", managerType)
            putExtra("MESSAGE", message)
            putExtra("NUMERIC_PRICE", numericPrice)
            putExtra("SELECTED_CURRENCY_CODE", selectedCurrencyCode)
            putExtra("SHORTNAME", shortname)
        }
        startActivity(intent)
    }

    private fun showExpiredDialog() {
        AlertDialog.Builder(this)
            .setTitle("API Key Expired")
            .setMessage("Your API key has expired. Please renew it on the server to continue.")
            .setCancelable(false)
            .setPositiveButton("OK") { d, _ -> d.dismiss() }
            .show()
    }
}
