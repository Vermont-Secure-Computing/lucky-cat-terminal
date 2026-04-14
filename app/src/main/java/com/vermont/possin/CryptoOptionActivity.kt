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
import com.vermont.possin.gif.GifHandler
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
    private var zcashManager: ZcashManager? = null
    private var tronManager: TronManager? = null
    private var bitcoincashManager: BitcoinCashManager? = null
    private var moneroManager: MoneroManager? = null
    private var solanaManager: SolanaManager? = null
    private var nanoManager: NanoManager? = null
    private lateinit var selectedCurrencyCode: String
    private lateinit var message: String
    private var loadingDialog: AlertDialog? = null

    // keep references to enable/disable on net changes
    private val clickableViews = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.crypto_option)

        setupNetworkMonitoring(R.id.networkBanner)

        window.statusBarColor =
            ContextCompat.getColor(this, R.color.darkerRed)

        val price = intent.getStringExtra("PRICE") ?: "0.00"

        val priceTextView: TextView =
            findViewById(R.id.priceTextView)

        val cleanedPrice =
            price.replace(Regex("[^\\d.]"), "").trim()

        val priceValue =
            cleanedPrice.toDoubleOrNull() ?: 0.00

        if (priceValue >= 10000) {
            priceTextView.textSize = 30f
        }

        priceTextView.text = price

        message = intent.getStringExtra("MESSAGE") ?: ""

        val xPubs = loadXPubsFromSettings()
        val props = loadPropertiesFromConfigFile()

        /*
         ==========================================
         BTC
         ==========================================
        */
        (xPubs["BTC"])?.let {
            bitcoinManager = BitcoinManager(this, it)
        }

        /*
         ==========================================
         LTC
         ==========================================
        */
        (xPubs["LTC"])?.let { litecoinValue ->

            val litecoinType =
                getCryptoTypeFromConfig("Litecoin_type")

            if (litecoinType.equals("xpub", true)) {

                val convertedXpub =
                    if (litecoinValue.startsWith("xpub")) {
                        try {
                            LitecoinManager
                                .convertBitcoinXpubToLitecoin(
                                    litecoinValue
                                )
                        } catch (e: Exception) {
                            Log.e(
                                "LitecoinManager",
                                e.message ?: ""
                            )
                            null
                        }
                    } else {
                        litecoinValue
                    }

                convertedXpub?.let {
                    litecoinManager =
                        LitecoinManager(this, it)
                }

            } else {
                litecoinManager =
                    LitecoinManager(this, litecoinValue)
            }
        }

        /*
         ==========================================
         ETH
         ==========================================
        */
        (xPubs["ETH"])?.let {
            ethereumManager = EthereumManager(this, it)
        }

        /*
         ==========================================
         DOGE
         ==========================================
        */
        (xPubs["DOGE"])?.let { dogeValue ->

            val dogeType =
                getCryptoTypeFromConfig("Dogecoin_type")

            if (dogeType.equals("xpub", true)) {

                val converted =
                    if (dogeValue.startsWith("xpub")) {
                        try {
                            DogecoinManager
                                .convertBitcoinXpubToDogecoin(
                                    dogeValue
                                )
                        } catch (e: Exception) {
                            Log.e(
                                "DogecoinManager",
                                e.message ?: ""
                            )
                            null
                        }
                    } else {
                        dogeValue
                    }

                converted?.let {
                    dogecoinManager =
                        DogecoinManager(this, it)
                }

            } else {
                dogecoinManager =
                    DogecoinManager(this, dogeValue)
            }
        }

        /*
         ==========================================
         DASH
         ==========================================
        */
        (xPubs["DASH"])?.let {
            try {
                dashManager = DashManager(this, it)
            } catch (e: Exception) {
                Log.e("DashManager", e.message ?: "")
            }
        }

        /*
         ==========================================
         BCH
         ==========================================
        */
        (xPubs["BCH"])?.let {
            bitcoincashManager =
                BitcoinCashManager(this, it)
        }

        /*
         ==========================================
         ZEC
         ==========================================
        */
        (xPubs["ZEC"])?.let {
            zcashManager = ZcashManager(this, it)
        }

        /*
         ==========================================
         TRON USDT
         ==========================================
        */
        (xPubs["USDT"])?.let {
            tronManager = TronManager(this, it)
        }

        /*
         ==========================================
         SOL
         ==========================================
        */
        (xPubs["SOL"])?.let {
            solanaManager = SolanaManager(this, it)
        }

        /*
         ==========================================
         MONERO
         supports old + new keys
         ==========================================
        */
        val moneroAddress =
            props.getProperty("XMR_value")
                ?: props.getProperty("Monero_value")

        val moneroViewKey =
            props.getProperty("Monero_view_key")

        if (!moneroAddress.isNullOrBlank()
            && !moneroViewKey.isNullOrBlank()
        ) {
            moneroManager =
                MoneroManager(
                    this,
                    moneroViewKey,
                    moneroAddress
                )
        }

        /*
         ==========================================
         NANO
         ==========================================
        */
        val nanoRaw =
            props.getProperty("XNO_value")
                ?: props.getProperty("Nano_value")

        if (!nanoRaw.isNullOrBlank()) {

            val nanoAddresses =
                nanoRaw.split(",")
                    .map { it.trim() }
                    .filter {
                        NanoManager.isValidAddress(it)
                    }

            if (nanoAddresses.isNotEmpty()) {
                nanoManager =
                    NanoManager(this, nanoAddresses)
            }
        }

        /*
         ==========================================
         SELECTED CURRENCY
         ==========================================
        */
        selectedCurrencyCode =
            intent.getStringExtra("CURRENCY_CODE")
                ?: "BTC"

        /*
         ==========================================
         BUILD BUTTONS
         ==========================================
        */
        val buttonContainer: LinearLayout =
            findViewById(R.id.buttonContainer)

        val cryptoCurrencies =
            loadCryptocurrenciesFromJson()

        cryptoCurrencies.forEach { crypto ->

            val logoResId =
                resources.getIdentifier(
                    crypto.logo,
                    "drawable",
                    packageName
                )

            val code =
                crypto.shortname.trim().uppercase()

            val isConfigured = when (code) {

                "USDT" ->
                    xPubs.containsKey("USDT")

                "USDT-ETH" ->
                    xPubs.containsKey("USDT-ETH")

                "USDC-ETH" ->
                    xPubs.containsKey("USDC-ETH")

                "DAI-ETH" ->
                    xPubs.containsKey("DAI-ETH")

                "LIGHTNING" ->
                    !props.getProperty("LIGHTNING_value")
                        .isNullOrBlank()

                else ->
                    xPubs.containsKey(code)
            }

            addCardView(
                buttonContainer,
                logoResId,
                isConfigured,
                crypto
            ) {

                if (lastObservedNetworkStatus
                    != NetworkStatus.CONNECTED
                ) {
                    Toast.makeText(
                        this,
                        getString(
                            R.string.no_internet_connection
                        ),
                        Toast.LENGTH_SHORT
                    ).show()

                    return@addCardView
                }

                handleCryptoClick(crypto, price)
            }
        }

        /*
         ==========================================
         BACK BUTTON
         ==========================================
        */
        findViewById<ImageView>(R.id.back_arrow)
            ?.setOnClickListener {
                finish()
            }

        /*
         ==========================================
         GIF
         ==========================================
        */
        findViewById<ImageView>(R.id.nekuGifImageView)
            ?.let {
                GifHandler.loadGif(it, R.raw.neku)
            }

        applyNetworkStateToUi(
            lastObservedNetworkStatus
        )
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
        GifHandler.loadGif(gifImageView, R.raw.rotating_arc_gradient_thick)

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
            setTextColor(ContextCompat.getColor(context, R.color.textPrimary))
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

        val props = Properties()
        val file = File(filesDir, "config.properties")

        if (file.exists()) {
            props.load(file.inputStream())
        }

        val map = mutableMapOf<String, String>()

        props.stringPropertyNames().forEach { key ->

            if (!key.endsWith("_value")) return@forEach

            val rawKey = key.removeSuffix("_value").trim()

            val normalized = when (rawKey.uppercase()) {

                "BITCOIN", "BTC" -> "BTC"
                "LITECOIN", "LTC" -> "LTC"
                "ETHEREUM", "ETH" -> "ETH"
                "DOGECOIN", "DOGE" -> "DOGE"

                "DASH" -> "DASH"
                "BITCOINCASH", "BCH" -> "BCH"
                "ZCASH", "ZEC" -> "ZEC"

                "MONERO", "XMR" -> "XMR"

                "SOLANA", "SOL" -> "SOL"

                "NANO", "XNO" -> "XNO"

                "LIGHTNING" -> "LIGHTNING"

                "TETHER", "USDT" -> "USDT"

                "USDT-ETH", "TETHER (ETHEREUM)" -> "USDT-ETH"
                "USDC-ETH", "USD COIN (ETHEREUM)" -> "USDC-ETH"
                "DAI-ETH", "DAI (ETHEREUM)" -> "DAI-ETH"

                else -> rawKey.uppercase()
            }

            map[normalized] = props.getProperty(key)
        }

        return map
    }

    private fun handleCryptoClick(crypto: CryptoCurrency, price: String) {
        showLoadingDialog()

        when (crypto.shortname.trim().uppercase()) {

            "BTC" -> handleBTCClick(price)

            "LIGHTNING" -> handleLightningClick(price)

            "ETH" -> handleETHClick(price)

            "XMR" -> handleMoneroClick(price)

            "LTC" -> handleLTCClick(price)

            "DOGE" -> handleDOGEClick(price)

            "USDT" -> handleUSDTClick(price)

            "USDT-ETH" -> handleERC20Click(
                price,
                "USDT-ETH",
                R.drawable.tether_eth
            )

            "USDC-ETH" -> handleERC20Click(
                price,
                "USDC-ETH",
                R.drawable.usdc_eth
            )

            "DAI-ETH" -> handleERC20Click(
                price,
                "DAI-ETH",
                R.drawable.dai_eth
            )

            "DASH" -> handleDASHlick(price)

            "BCH" -> handleBCHlick(price)

            "SOL" -> handleSolanaClick(price)

            "ZEC" -> handleZcashClick(price)

            "XNO" -> handleNanoClick(price)

            else -> {
                dismissLoadingDialog()

                Toast.makeText(
                    this,
                    "Unsupported coin: ${crypto.shortname}",
                    Toast.LENGTH_LONG
                ).show()

                Log.e(
                    "COIN_DEBUG",
                    "No handler for shortname=${crypto.shortname}"
                )
            }
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

    private fun handleZcashClick(price: String) {
        zcashManager?.let { manager ->
            val addressIndexPair: Pair<String, Int>? = if (ZcashManager.isValidAddress(manager.getXpub())) {
                Pair(manager.getXpub(), -1)
            } else manager.getAddress()

            if (addressIndexPair == null) {
                Toast.makeText(this, R.string.failed_to_derive_zcash_address, Toast.LENGTH_SHORT).show()
                return
            }

            val (address, index) = addressIndexPair
            val numericPrice = price.filter { it.isDigit() || it == '.' }

            postConversionApi(numericPrice, selectedCurrencyCode, address, "ZEC", R.drawable.zcash_logo) { feeStatus, status, formattedRate ->
                dismissLoadingDialog()
                if (formattedRate.isNotEmpty()) {
                    startGenerateQRActivity(
                        address,
                        formattedRate,
                        R.drawable.zcash_logo,
                        "ZEC",
                        index,
                        feeStatus,
                        status,
                        "Zcash",
                        numericPrice,
                        selectedCurrencyCode,
                        "ZEC"
                    )
                }
            }
        } ?: run {
            dismissLoadingDialog()
            Toast.makeText(this, R.string.zcash_manager_not_initialized, Toast.LENGTH_SHORT).show()
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

    private fun handleNanoClick(price: String) {
        nanoManager?.let { manager ->
            val (address, index) = manager.getAddress()
            val numericPrice = price.filter { it.isDigit() || it == '.' }

            postConversionApi(
                numericPrice,
                selectedCurrencyCode,
                address,
                "NANO",
                R.drawable.nano
            ) { feeStatus, status, formattedRate ->
                dismissLoadingDialog()
                if (formattedRate.isNotEmpty()) {
                    startGenerateQRActivity(
                        address,
                        formattedRate,
                        R.drawable.nano,
                        "XNO",
                        index,
                        feeStatus,
                        status,
                        "Nano",
                        numericPrice,
                        selectedCurrencyCode,
                        "XNO"
                    )
                }
            }
        } ?: run {
            dismissLoadingDialog()
            Toast.makeText(this, "Nano not configured", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleLightningClick(price: String) {

        val properties = loadPropertiesFromConfigFile()

        val address =
            properties.getProperty("LIGHTNING_value")
                ?: properties.getProperty("Lightning_value")

        if (address.isNullOrBlank()) {
            dismissLoadingDialog()

            Toast.makeText(
                this,
                "Lightning address not set",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val numericPrice =
            price.filter { it.isDigit() || it == '.' }

        postConversionApi(
            numericPrice,
            selectedCurrencyCode,
            address.trim(),
            "LIGHTNING",
            R.drawable.bitcoin_lightning_logo
        ) { feeStatus, status, response ->

            dismissLoadingDialog()

            if (response.isNotEmpty()) {

                startGenerateQRActivity(
                    response,
                    numericPrice,
                    R.drawable.bitcoin_lightning_logo,
                    "LIGHTNING",
                    -1,
                    feeStatus,
                    status,
                    "Lightning",
                    numericPrice,
                    selectedCurrencyCode,
                    "LIGHTNING"
                )
            }
        }
    }

    private fun handleERC20Click(price: String, token: String, logo: Int) {

        val properties = loadPropertiesFromConfigFile()

        // Determine correct key (supports new + old)
        val tokenKey = when (token) {
            "USDT-ETH" -> "USDT-ETH"
            "USDC-ETH" -> "USDC-ETH"
            "DAI-ETH"  -> "DAI-ETH"
            else -> "ETH"
        }

        val address =
            properties.getProperty("${tokenKey}_value")
                ?: properties.getProperty("${token.replace("-ETH", " (Ethereum)")}_value") // fallback old format

        if (address.isNullOrBlank()) {
            dismissLoadingDialog()
            Toast.makeText(this, "$token not configured", Toast.LENGTH_SHORT).show()
            return
        }

        val numericPrice = price.filter { it.isDigit() || it == '.' }

        // IMPORTANT: chain MUST identify token
        val chain = tokenKey

        postConversionApi(
            numericPrice,
            selectedCurrencyCode,
            address,
            chain,
            logo
        ) { feeStatus, status, formattedRate ->

            dismissLoadingDialog()

            if (formattedRate.isNotEmpty()) {
                startGenerateQRActivity(
                    address,
                    formattedRate,
                    logo,
                    token,
                    -1,
                    feeStatus,
                    status,
                    token,
                    numericPrice,
                    selectedCurrencyCode,
                    token
                )
            }
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
        val call = apiService.postConversion(ConversionRequestBody(price, currency, chain, address))

        call.enqueue(object : Callback<ConversionResponse> {
            override fun onResponse(call: Call<ConversionResponse>, response: Response<ConversionResponse>) {
                if (response.isSuccessful) {
                    val conversionResponse = response.body()
                    conversionResponse?.let {
                        val feeStatus = it.feeStatus ?: ""
                        val status = it.status ?: ""
                        if (it.success) {

                            val formattedRate = if (chain == "LIGHTNING") {
                                it.relayInvoice ?: ""
                            } else {
                                it.conversionRate?.let { rate ->
                                    formatConversionRate(rate)
                                } ?: ""
                            }

                            Toast.makeText(
                                this@CryptoOptionActivity,
                                getString(R.string.conversion_rate, formattedRate),
                                Toast.LENGTH_SHORT
                            ).show()

                            onResult(feeStatus, status, formattedRate)

                        } else {

                            dismissLoadingDialog()

                            val errorMessage = it.error ?: ""

                            // ✅ HANDLE LIGHTNING LIMIT ERROR
                            if (errorMessage.contains("over the limit", ignoreCase = true)) {

                                AlertDialog.Builder(this@CryptoOptionActivity)
                                    .setTitle("Limit Reached")
                                    .setMessage("Lightning limit is 100k sats. Please use BTC, DOGE, or other supported currencies.")
                                    .setCancelable(false)
                                    .setPositiveButton("OK") { _, _ -> }
                                    .show()

                            } else {

                                Toast.makeText(
                                    this@CryptoOptionActivity,
                                    errorMessage.ifEmpty { getString(R.string.conversion_failed) },
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

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
//            putExtra("XPUB", isXpubCurrency(managerType))
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

    private fun isXpubCurrency(currency: String): Boolean {
        // Only these coins actually support xpub
        val xpubCapable = setOf("Bitcoin", "Litecoin", "Dogecoin", "Dash", "Bitcoincash")

        if (!xpubCapable.contains(currency)) return false

        val props = Properties()
        val file = File(filesDir, "config.properties")
        if (file.exists()) props.load(file.inputStream())

        val type = props.getProperty("${currency}_type")
        return type.equals("xpub", ignoreCase = true)
    }
}
