package com.example.possin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.possin.R
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
    //     private var dashManager: DashManager? = null
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
        // xPubs["Dash"]?.let { dashManager = DashManager(this, it) }

        // Assuming selectedCurrencyCode is obtained from POSView (e.g., from a Spinner or other input)
        selectedCurrencyCode = intent.getStringExtra("CURRENCY_CODE") ?: "BTC"

        val btnBTC = findViewById<Button>(R.id.btnBTC)
        val btnLTC = findViewById<Button>(R.id.btnLTC)
        val btnDOGE = findViewById<Button>(R.id.btnDOGE)
        val btnETH = findViewById<Button>(R.id.btnETH)
        val btnXMR = findViewById<Button>(R.id.btnXMR)
        val btnUSDT = findViewById<Button>(R.id.btnUSDT)
        val btnLOG = findViewById<Button>(R.id.btnLOG)
        val btnDASH = findViewById<Button>(R.id.btnDASH)

        btnBTC.setOnClickListener { handleBTCClick(price) }
        btnLTC.setOnClickListener { handleLTCClick(price) }
        btnETH.setOnClickListener { handleETHClick(price) }
        btnDOGE.setOnClickListener { handleDOGEClick(price) }
        btnDASH.setOnClickListener { /* Handle DASH */ }
        btnXMR.setOnClickListener { /* Handle XMR */ }
        btnUSDT.setOnClickListener { /* Handle USDT */ }
        btnLOG.setOnClickListener { handleLOGClick(price) }

        // Set visibility based on XPUB availability
        btnBTC.visibility = if (xPubs.containsKey("Bitcoin")) View.VISIBLE else View.GONE
        btnLTC.visibility = if (xPubs.containsKey("Litecoin")) View.VISIBLE else View.GONE
        btnETH.visibility = if (xPubs.containsKey("Ethereum")) View.VISIBLE else View.GONE
        btnDOGE.visibility = if (xPubs.containsKey("Dogecoin")) View.VISIBLE else View.GONE
        btnDASH.visibility = if (xPubs.containsKey("Dash")) View.VISIBLE else View.GONE
        btnXMR.visibility = View.GONE // Assuming no XPUB required for XMR
        btnUSDT.visibility = View.GONE // Assuming no XPUB required for USDT
        btnLOG.visibility = if (xPubs.containsKey("Woodcoin")) View.VISIBLE else View.GONE
    }

    private fun loadXPubsFromSettings(): Map<String, String> {
        val properties = Properties()
        val propertiesFile = File(filesDir, "config.properties")
        if (propertiesFile.exists()) {
            properties.load(propertiesFile.inputStream())
        }

        val xPubs = mutableMapOf<String, String>()
        properties.getProperty("Bitcoin_xpub")?.let { xPubs["Bitcoin"] = it }
        properties.getProperty("Litecoin_xpub")?.let { xPubs["Litecoin"] = it }
        properties.getProperty("Dogecoin_xpub")?.let { xPubs["Dogecoin"] = it }
        properties.getProperty("Ethereum_xpub")?.let { xPubs["Ethereum"] = it }
        properties.getProperty("Woodcoin_xpub")?.let { xPubs["Woodcoin"] = it }
        // properties.getProperty("Dash_xpub")?.let { xPubs["Dash"] = it }

        return xPubs
    }

    private fun handleBTCClick(price: String) {
        bitcoinManager?.let { manager ->
            val (address, index) = manager.getAddress()

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
            val (address, index) = manager.getAddress()

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
            val (address, index) = manager.getAddress()

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
            val (address, index) = manager.getAddress()

            val numericPrice = price.filter { it.isDigit() || it == '.' }

            postConversionApi(numericPrice, selectedCurrencyCode, address, "DOGE", R.drawable.dogecoin_logo) { feeStatus, status, formattedRate ->
                startGenerateQRActivity(address, formattedRate, R.drawable.dogecoin_logo, "DOGE", index, feeStatus, status, "Dogecoin")
            }
        } ?: run {
            Toast.makeText(this, "Dogecoin Manager not initialized", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleLOGClick(price: String) {
        woodcoinManager?.let {
            val address = it.getAddress()

            val numericPrice = price.filter { it.isDigit() || it == '.' }

            Log.d("ADDRESS", address)

            // Call the API to get the conversion rate
            // postConversionApi(numericPrice, selectedCurrencyCode, address, "DOGE", R.drawable.dogecoin_logo)
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