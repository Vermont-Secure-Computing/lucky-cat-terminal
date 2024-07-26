package com.example.possin

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.ScaleAnimation
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.possin.network.RetrofitClient
import com.example.possin.model.ConversionResponse
import com.example.possin.network.ConversionRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.Properties
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

class CryptoOptionActivity : AppCompatActivity() {

    private var bitcoinManager: BitcoinManager? = null
    private var litecoinManager: LitecoinManager? = null
    private var ethereumManager: EthereumManager? = null
    private var dogecoinManager: DogecoinManager? = null
    private var woodcoinManager: WoodcoinManager? = null
    //    private var dashManager: DashManager? = null
    private var tronManager: TronManager? = null
    private lateinit var selectedCurrencyCode: String
    private lateinit var message: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.crypto_option)

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
//        xPubs["Dash"]?.let { dashManager = DashManager(this, it) }
        xPubs["Tether"]?.let { tronManager = TronManager(this, it) }

        // Assuming selectedCurrencyCode is obtained from POSView (e.g., from a Spinner or other input)
        selectedCurrencyCode = intent.getStringExtra("CURRENCY_CODE") ?: "BTC"

        val buttonContainer: LinearLayout = findViewById(R.id.buttonContainer)

        addImageButton(buttonContainer, R.drawable.bitcoin_logo, xPubs.containsKey("Bitcoin")) { handleBTCClick(price) }
        addImageButton(buttonContainer, R.drawable.litecoin_new_logo, xPubs.containsKey("Litecoin")) { handleLTCClick(price) }
        addImageButton(buttonContainer, R.drawable.ethereum_logo, xPubs.containsKey("Ethereum")) { handleETHClick(price) }
        addImageButton(buttonContainer, R.drawable.dogecoin_logo, xPubs.containsKey("Dogecoin")) { handleDOGEClick(price) }
//        addImageButton(buttonContainer, R.drawable.dashcoin_logo, xPubs.containsKey("Dash")) { handleDashClick(price) }
        addImageButton(buttonContainer, R.drawable.tether_logo, xPubs.containsKey("Tether")) { handleUSDTClick(price) }

        val backText: TextView = findViewById(R.id.backText)
        backText.setOnClickListener {
            finish() // Navigate back to the previous activity
        }
    }

    private fun addImageButton(container: LinearLayout, imageResId: Int, isVisible: Boolean, onClick: () -> Unit) {
        val originalBitmap = BitmapFactory.decodeResource(resources, imageResId)
        val targetSize = 200 // Target size in pixels for the image before scaling down
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

        val button = ImageButton(this).apply {
            this.setImageBitmap(scaledBitmap)
            this.visibility = if (isVisible) View.VISIBLE else View.GONE
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                width = LinearLayout.LayoutParams.WRAP_CONTENT
                height = LinearLayout.LayoutParams.WRAP_CONTENT
                setMargins(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            }
            setBackgroundResource(R.drawable.button_selector)

            // Set press animation
            setOnTouchListener { v, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        val scaleAnimation = ScaleAnimation(
                            1f, 0.9f, 1f, 0.9f,
                            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
                            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
                        ).apply {
                            duration = 150
                            fillAfter = true
                        }
                        this.startAnimation(scaleAnimation)
                    }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        val scaleAnimation = ScaleAnimation(
                            0.9f, 1f, 0.9f, 1f,
                            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
                            android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f
                        ).apply {
                            duration = 150
                            fillAfter = true
                        }
                        this.startAnimation(scaleAnimation)
                    }
                }
                false
            }
        }
        container.addView(button)
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

    private fun handleBTCClick(price: String) {
        bitcoinManager?.let { manager ->
            val (address, index) = if (BitcoinManager.isValidAddress(manager.getXpub())) Pair(manager.getXpub(), -1) else manager.getAddress()

            val numericPrice = price.filter { it.isDigit() || it == '.' }

            postConversionApi(numericPrice, selectedCurrencyCode, address, "BTC", R.drawable.bitcoin_logo) { feeStatus, status, formattedRate ->
                startGenerateQRActivity(address, formattedRate, R.drawable.bitcoin_logo, "BTC", index, feeStatus, status, "Bitcoin")
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
                startGenerateQRActivity(address, formattedRate, R.drawable.litecoin_new_logo, "LTC", index, feeStatus, status, "Litecoin")
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
                startGenerateQRActivity(address, formattedRate, R.drawable.ethereum_logo, "ETH", index, feeStatus, status, "Ethereum")
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
                startGenerateQRActivity(address, formattedRate, R.drawable.dogecoin_logo, "DOGE", index, feeStatus, status, "Dogecoin")
            }
        } ?: run {
            Toast.makeText(this, "Dogecoin Manager not initialized", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleUSDTClick(price: String) {
        tronManager?.let { manager ->
            val (address, index) = if (TronManager.isValidAddress(manager.getXpub())) Pair(manager.getXpub(), -1) else manager.getAddress()

            val numericPrice = price.filter { it.isDigit() || it == '.' }

            postConversionApi(numericPrice, selectedCurrencyCode, address, "TRON", R.drawable.tether_logo) { feeStatus, status, formattedRate ->
                startGenerateQRActivity(address, formattedRate, R.drawable.tether_logo, "TRON", index, feeStatus, status, "Tron")
            }
        } ?: run {
            Toast.makeText(this, "USDT Tron Manager not initialized", Toast.LENGTH_SHORT).show()
        }
    }

//    private fun handleDashClick(price: String) {
//        dashManager?.let { manager ->
//            val (address, index) = if (DashManager.isValidAddress(manager.getXpub())) Pair(manager.getXpub(), -1) else manager.getAddress()
//
//            val numericPrice = price.filter { it.isDigit() || it == '.' }
//
//            Log.d("ADDRESS", address)
//
////            postConversionApi(numericPrice, selectedCurrencyCode, address, "DASH", R.drawable.dashcoin_logo) { feeStatus, status, formattedRate ->
////                startGenerateQRActivity(address, formattedRate, R.drawable.dashcoin_logo, "Dash", index, feeStatus, status, "Dash")
////            }
//        } ?: run {
//            Toast.makeText(this, "Dash Manager not initialized", Toast.LENGTH_SHORT).show()
//        }
//    }

    private fun handleLOGClick(price: String) {
        woodcoinManager?.let {
            val address = it.getAddress()

            val numericPrice = price.filter { it.isDigit() || it == '.' }

            Log.d("ADDRESS", address)

            // Call the API to get the conversion rate
            // postConversionApi(numericPrice, selectedCurrencyCode, address, "LOG", R.drawable.woodcoin_logo)
        } ?: run {
            Toast.makeText(this, "Woodcoin Manager not initialized", Toast.LENGTH_SHORT).show()
        }
    }

    private fun postConversionApi(price: String, currency: String, address: String, chain: String, logoResId: Int, onResult: (String, String, String) -> Unit) {
        val apiService = RetrofitClient.apiService
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
                    Toast.makeText(this@CryptoOptionActivity, "Response not successful", Toast.LENGTH_SHORT).show()
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

    private fun startGenerateQRActivity(address: String, price: String, logoResId: Int, currency: String, index: Int, feeStatus: String, status: String, managerType: String) {
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
        }
        startActivity(intent)
    }
}
