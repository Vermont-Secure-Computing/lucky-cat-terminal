package com.example.possin

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
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.example.possin.model.ConversionResponse
import com.example.possin.network.ConversionRequestBody
import com.example.possin.network.RetrofitClient
import com.google.gson.Gson
import com.google.gson.JsonObject
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

class CryptoOptionActivity : AppCompatActivity() {

    private var bitcoinManager: BitcoinManager? = null
    private var litecoinManager: LitecoinManager? = null
    private var ethereumManager: EthereumManager? = null
    private var dogecoinManager: DogecoinManager? = null
    private var woodcoinManager: WoodcoinManager? = null
    private var dashManager: DashManager? = null
    private var tronManager: TronManager? = null
    private var bitcoincashManager: BitcoinCashManager? = null
    private lateinit var selectedCurrencyCode: String
    private lateinit var message: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.crypto_option)

        window.statusBarColor = ContextCompat.getColor(this, R.color.tapeRed)

        val price = intent.getStringExtra("PRICE") ?: "0.00"
        val priceTextView: TextView = findViewById(R.id.priceTextView)
        priceTextView.text = price
        message = intent.getStringExtra("MESSAGE") ?: ""

        val xPubs = loadXPubsFromSettings()
        xPubs["Bitcoin"]?.let { bitcoinManager = BitcoinManager(this, it) }
        xPubs["Litecoin"]?.let { litecoinManager = LitecoinManager(this, it) }
        xPubs["Ethereum"]?.let { ethereumManager = EthereumManager(this, it) }
        xPubs["Dogecoin"]?.let { dogecoinManager = DogecoinManager(this, it) }
        xPubs["Woodcoin"]?.let { woodcoinManager = WoodcoinManager(this, it) }
        xPubs["Dash"]?.let { dashManager = DashManager(this, it) }
        xPubs["Tether"]?.let { tronManager = TronManager(this, it) }
        xPubs["Bitcoincash"]?.let { bitcoincashManager = BitcoinCashManager(this, it) }

        selectedCurrencyCode = intent.getStringExtra("CURRENCY_CODE") ?: "BTC"

        val buttonContainer: LinearLayout = findViewById(R.id.buttonContainer)
        val cryptoCurrencies = loadCryptocurrenciesFromJson()

        cryptoCurrencies.forEach { crypto ->
            val logoResId = resources.getIdentifier(crypto.logo, "drawable", packageName)
            addCardView(buttonContainer, logoResId, xPubs.containsKey(crypto.name), crypto) {
                handleCryptoClick(crypto, price)
            }
        }

        val backArrow: ImageView = findViewById(R.id.back_arrow)
        backArrow.setOnClickListener {
            finish() // Navigate back to the previous activity
        }
    }

    private fun loadCryptocurrenciesFromJson(): List<CryptoCurrency> {
        val inputStream = assets.open("cryptocurrencies.json")
        val reader = InputStreamReader(inputStream)
        val jsonObject = Gson().fromJson(reader, JsonObject::class.java)
        val cryptoList = Gson().fromJson(jsonObject.getAsJsonArray("cryptocurrencies"), Array<CryptoCurrency>::class.java).toList()
        reader.close()
        return cryptoList
    }

    private fun addCardView(container: LinearLayout, imageResId: Int, isVisible: Boolean, crypto: CryptoCurrency, onClick: () -> Unit) {
        val cardView = CardView(this).apply {
            radius = 16f
            cardElevation = 8f
            visibility = if (isVisible) View.VISIBLE else View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8)) // Increased left and right margins
            }
        }

        val cardContent = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8)) // Adjust padding as needed
            gravity = android.view.Gravity.CENTER
            setBackgroundResource(R.drawable.button_selector) // Apply button background for feedback
            setOnClickListener { onClick() } // Set click listener on the entire container
        }

        val imageButton = ImageButton(this).apply {
            val originalBitmap = BitmapFactory.decodeResource(resources, imageResId)
            val targetSize = 300 // Increased target size for larger logo
            val resizedBitmap = Bitmap.createScaledBitmap(
                originalBitmap,
                targetSize,
                targetSize,
                true
            )
            val scaledBitmap = Bitmap.createScaledBitmap(
                resizedBitmap,
                targetSize / 2,
                targetSize / 2,
                true
            )
            this.setImageBitmap(scaledBitmap)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                width = LinearLayout.LayoutParams.WRAP_CONTENT
                height = LinearLayout.LayoutParams.WRAP_CONTENT
                setMargins(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            }
            background = null // Remove the button background since the entire container is clickable
        }

        val textLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dpToPx(16), 0, 0, 0) // Add left margin to separate text from image
            }
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

    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun loadXPubsFromSettings(): Map<String, String> {
        val properties = Properties()
        val propertiesFile = File(filesDir, "config.properties")
        if (propertiesFile.exists()) {
            properties.load(propertiesFile.inputStream())
        }

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
        when (crypto.shortname) {
            "BTC" -> handleBTCClick(price)
            "LTC" -> handleLTCClick(price)
            "ETH" -> handleETHClick(price)
            "DOGE" -> handleDOGEClick(price)
            "USDT" -> handleUSDTClick(price)
            "DASH" -> handleDASHlick(price)
            "BCH" -> handleBCHlick(price)
            // Add other cryptocurrencies as needed
        }
    }

    private fun handleBTCClick(price: String) {
        bitcoinManager?.let { manager ->
            val (address, index) = if (BitcoinManager.isValidAddress(manager.getXpub())) Pair(manager.getXpub(), -1) else manager.getAddress()

            val numericPrice = price.filter { it.isDigit() || it == '.' }

            postConversionApi(numericPrice, selectedCurrencyCode, address, "BTC", R.drawable.bitcoin_logo) { feeStatus, status, formattedRate ->
                if (formattedRate.isNotEmpty()) {
                    startGenerateQRActivity(
                        address,
                        formattedRate,
                        R.drawable.bitcoin_logo,
                        "BTC",
                        index,
                        feeStatus,
                        status,
                        "Bitcoin",
                        numericPrice,
                        selectedCurrencyCode
                    )
                }
            }
        } ?: run {
            Toast.makeText(this, "Bitcoin Manager not initialized", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleLTCClick(price: String) {
        litecoinManager?.let { manager ->
            val (address, index) = if (LitecoinManager.isValidAddress(manager.getXpub())) Pair(manager.getXpub(), -1) else manager.getAddress()

            val numericPrice = price.filter { it.isDigit() || it == '.' }

            postConversionApi(numericPrice, selectedCurrencyCode, address, "LTC", R.drawable.litecoin_new_logo) { feeStatus, status, formattedRate ->
                if (formattedRate.isNotEmpty()) {
                    startGenerateQRActivity(
                        address,
                        formattedRate,
                        R.drawable.litecoin_new_logo,
                        "LTC",
                        index,
                        feeStatus,
                        status,
                        "Litecoin",
                        numericPrice,
                        selectedCurrencyCode
                    )
                }
            }
        } ?: run {
            Toast.makeText(this, "Litecoin Manager not initialized", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleETHClick(price: String) {
        ethereumManager?.let { manager ->
            val (address, index) = if (EthereumManager.isValidAddress(manager.getXpub())) Pair(manager.getXpub(), -1) else manager.getAddress()

            val numericPrice = price.filter { it.isDigit() || it == '.' }

            postConversionApi(numericPrice, selectedCurrencyCode, address, "ETH", R.drawable.ethereum_logo) { feeStatus, status, formattedRate ->
                if (formattedRate.isNotEmpty()) {
                    startGenerateQRActivity(
                        address,
                        formattedRate,
                        R.drawable.ethereum_logo,
                        "ETH",
                        index,
                        feeStatus,
                        status,
                        "Ethereum",
                        numericPrice,
                        selectedCurrencyCode
                    )
                }
            }
        } ?: run {
            Toast.makeText(this, "Ethereum Manager not initialized", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleDOGEClick(price: String) {
        dogecoinManager?.let { manager ->
            val (address, index) = if (DogecoinManager.isValidAddress(manager.getXpub())) Pair(manager.getXpub(), -1) else manager.getAddress()

            val numericPrice = price.filter { it.isDigit() || it == '.' }

            postConversionApi(numericPrice, selectedCurrencyCode, address, "DOGE", R.drawable.dogecoin_logo) { feeStatus, status, formattedRate ->
                if (formattedRate.isNotEmpty()) {
                    startGenerateQRActivity(
                        address,
                        formattedRate,
                        R.drawable.dogecoin_logo,
                        "DOGE",
                        index,
                        feeStatus,
                        status,
                        "Dogecoin",
                        numericPrice,
                        selectedCurrencyCode
                    )
                }
            }
        } ?: run {
            Toast.makeText(this, "Dogecoin Manager not initialized", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleUSDTClick(price: String) {
        tronManager?.let { manager ->
            val (address, index) = if (TronManager.isValidAddress(manager.getXpub())) Pair(manager.getXpub(), -1) else manager.getAddress()

            val numericPrice = price.filter { it.isDigit() || it == '.' }

            postConversionApi(numericPrice, selectedCurrencyCode, address, "TRON-NETWORK", R.drawable.tether_logo) { feeStatus, status, formattedRate ->
                if (formattedRate.isNotEmpty()) {
                    startGenerateQRActivity(
                        address,
                        formattedRate,
                        R.drawable.tether_logo,
                        "TRON-NETWORK",
                        index,
                        feeStatus,
                        status,
                        "Tron-network",
                        numericPrice,
                        selectedCurrencyCode
                    )
                }
            }
        } ?: run {
            Toast.makeText(this, "USDT Tron Manager not initialized", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleDASHlick(price: String) {
        dashManager?.let { manager ->
            val addressIndexPair: Pair<String, Int>? = if (DashManager.isValidAddress(manager.getXpub())) {
                Pair(manager.getXpub(), -1)
            } else {
                manager.getAddress()
            }

            if (addressIndexPair == null) {
                Toast.makeText(this, "Failed to derive Dash address", Toast.LENGTH_SHORT).show()
                return
            }

            val (address, index) = addressIndexPair

            val numericPrice = price.filter { it.isDigit() || it == '.' }

            postConversionApi(numericPrice, selectedCurrencyCode, address, "DASH", R.drawable.dashcoin_logo) { feeStatus, status, formattedRate ->
                if (formattedRate.isNotEmpty()) {
                    startGenerateQRActivity(
                        address,
                        formattedRate,
                        R.drawable.dashcoin_logo,
                        "DASH",
                        index,
                        feeStatus,
                        status,
                        "Dash",
                        numericPrice,
                        selectedCurrencyCode
                    )
                }
            }
        } ?: run {
            Toast.makeText(this, "Dash Manager not initialized", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleBCHlick(price: String) {
        bitcoincashManager?.let { manager ->
            val (address, index) = if (BitcoinCashManager.isValidAddress(manager.getXpub())) Pair(manager.getXpub(), -1) else manager.getAddress()

            val numericPrice = price.filter { it.isDigit() || it == '.' }

            postConversionApi(numericPrice, selectedCurrencyCode, address, "BCH", R.drawable.bitcoin_cash) { feeStatus, status, formattedRate ->
                if (formattedRate.isNotEmpty()) {
                    startGenerateQRActivity(
                        address,
                        formattedRate,
                        R.drawable.bitcoin_cash,
                        "BCH",
                        index,
                        feeStatus,
                        status,
                        "Bitcoincash",
                        numericPrice,
                        selectedCurrencyCode
                    )
                }
            }
        } ?: run {
            Toast.makeText(this, "Bitcoincash Manager not initialized", Toast.LENGTH_SHORT).show()
        }
    }

    private fun postConversionApi(price: String, currency: String, address: String, chain: String, logoResId: Int, onResult: (String, String, String) -> Unit) {
        val apiService = RetrofitClient.getApiService(this)
        println("Price $price")
        println("Currency $currency")
        println("Chain $chain")
        val requestBody = ConversionRequestBody(price, currency, chain)
        val call = apiService.postConversion(requestBody)

        call.enqueue(object : Callback<ConversionResponse> {
            override fun onResponse(call: Call<ConversionResponse>, response: Response<ConversionResponse>) {
                Log.d("API", "Response conversion $response")
                if (response.isSuccessful) {
                    val conversionResponse = response.body()
                    Log.d("Conversion", "Response body $conversionResponse")
                    conversionResponse?.let {
                        val feeStatus = it.feeStatus ?: ""
                        val status = it.status ?: ""
                        if (it.success) {
                            val formattedRate = formatConversionRate(it.conversionRate)
                            Log.d("API", "Conversion rate: $formattedRate")
                            Toast.makeText(this@CryptoOptionActivity, "Conversion rate: $formattedRate", Toast.LENGTH_SHORT).show()
                            onResult(feeStatus, status, formattedRate)  // Pass the values to the callback
                        } else {
                            Log.e("API", "Conversion failed")
                            Toast.makeText(this@CryptoOptionActivity, "Conversion failed", Toast.LENGTH_SHORT).show()
                            onResult(feeStatus, status, "")
                        }
                    } ?: run {
                        Log.e("API", "Response body is null")
                        onResult("", "", "")
                    }
                } else {
                    Log.e("API", "Response not successful")
                    Toast.makeText(this@CryptoOptionActivity, "Response not successful. Please check your setting or API key", Toast.LENGTH_SHORT).show()
                    onResult("", "", "")
                }
            }

            override fun onFailure(call: Call<ConversionResponse>, t: Throwable) {
                Log.e("API", "API call failed", t)
                Toast.makeText(this@CryptoOptionActivity, "API call failed", Toast.LENGTH_SHORT).show()
                onResult("", "", "")
            }
        })
    }

    fun formatConversionRate(rate: Double, decimalPlaces: Int = 8): String {
        val bigDecimal = BigDecimal(rate).setScale(decimalPlaces, RoundingMode.HALF_UP)
        val decimalFormat = DecimalFormat()
        decimalFormat.maximumFractionDigits = decimalPlaces
        return decimalFormat.format(bigDecimal)
    }

    private fun startGenerateQRActivity(address: String, price: String, logoResId: Int, currency: String, index: Int, feeStatus: String, status: String, managerType: String, numericPrice: String, selectedCurrencyCode: String) {
        Log.d("MESSAGE", message)
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
        }
        startActivity(intent)
    }
}
