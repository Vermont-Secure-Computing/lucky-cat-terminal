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

class CryptoOptionActivity : AppCompatActivity() {

    private var bitcoinManager: BitcoinManager? = null
    private var litecoinManager: LitecoinManager? = null
    private var ethereumManager: EthereumManager? = null
    // private var dogecoinManager: DogecoinManager? = null
//     private var dashManager: DashManager? = null
    private lateinit var selectedCurrencyCode: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.crypto_option)

        val price = intent.getStringExtra("PRICE") ?: "0.00"
        val priceTextView: TextView = findViewById(R.id.priceTextView)
        priceTextView.text = price

        val xPubs = loadXPubsFromSettings()
        xPubs["Bitcoin"]?.let { bitcoinManager = BitcoinManager(this, it) }
        xPubs["Litecoin"]?.let { litecoinManager = LitecoinManager(this, it) }
        xPubs["Ethereum"]?.let { ethereumManager = EthereumManager(this, it) }
        // xPubs["Dogecoin"]?.let { dogecoinManager = DogecoinManager(this, it) }
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
        // btnDOGE.setOnClickListener { handleDOGEClick(price) }
        btnDASH.setOnClickListener { /* Handle DASH */ }
        btnXMR.setOnClickListener { /* Handle XMR */ }
        btnUSDT.setOnClickListener { /* Handle USDT */ }
        btnLOG.setOnClickListener { /* Handle LOG */ }

        // Set visibility based on XPUB availability
        btnBTC.visibility = if (xPubs.containsKey("Bitcoin")) View.VISIBLE else View.GONE
        btnLTC.visibility = if (xPubs.containsKey("Litecoin")) View.VISIBLE else View.GONE
        btnETH.visibility = if (xPubs.containsKey("Ethereum")) View.VISIBLE else View.GONE
        btnDOGE.visibility = if (xPubs.containsKey("Dogecoin")) View.VISIBLE else View.GONE
        btnDASH.visibility = if (xPubs.containsKey("Dash")) View.VISIBLE else View.GONE
        btnXMR.visibility = View.GONE // Assuming no XPUB required for XMR
        btnUSDT.visibility = View.GONE // Assuming no XPUB required for USDT
        btnLOG.visibility = View.GONE // Assuming no XPUB required for LOG
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
        // properties.getProperty("Dash_xpub")?.let { xPubs["Dash"] = it }

        return xPubs
    }

    private fun handleBTCClick(price: String) {
        bitcoinManager?.let {
            val address = it.getAddress()

            // Remove any non-numeric characters (except for decimal point) from the price
            val numericPrice = price.filter { it.isDigit() || it == '.' }

            // Call the API to get the conversion rate
            postConversionApi(numericPrice, selectedCurrencyCode, address, "BTC", R.drawable.bitcoin_logo)
        } ?: run {
            Toast.makeText(this, "Bitcoin Manager not initialized", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleLTCClick(price: String) {
        litecoinManager?.let {
            val address = it.getAddress()

            // Remove any non-numeric characters (except for decimal point) from the price
            val numericPrice = price.filter { it.isDigit() || it == '.' }

            // Call the API to get the conversion rate
            postConversionApi(numericPrice, selectedCurrencyCode, address, "LTC", R.drawable.litecoin_new_logo)
        } ?: run {
            Toast.makeText(this, "Litecoin Manager not initialized", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleETHClick(price: String) {
        ethereumManager?.let {
            val address = it.getAddress()

            // Remove any non-numeric characters (except for decimal point) from the price
            val numericPrice = price.filter { it.isDigit() || it == '.' }

            // Call the API to get the conversion rate
            postConversionApi(numericPrice, selectedCurrencyCode, address, "ETH", R.drawable.ethereum_logo)
        } ?: run {
            Toast.makeText(this, "Ethereum Manager not initialized", Toast.LENGTH_SHORT).show()
        }
    }

    private fun postConversionApi(price: String, currency: String, address: String, chain: String, logoResId: Int) {
        val apiService = RetrofitClient.apiService
        val requestBody = ConversionRequestBody(price, currency, chain)
        val call = apiService.postConversion(requestBody)

        call.enqueue(object : Callback<ConversionResponse> {
            override fun onResponse(call: Call<ConversionResponse>, response: Response<ConversionResponse>) {
                Log.d("API", "Response conversion $response")
                if (response.isSuccessful) {
                    Log.d("API", "response body ${response.body()}")
                    val conversionResponse = response.body()
                    Log.d("API", "Conversion response body $conversionResponse")
                    conversionResponse?.let {
                        if (it.success) {
                            Log.d("API", "Conversion rate: ${it.conversionRate}")
                            Toast.makeText(this@CryptoOptionActivity, "Conversion rate: ${it.conversionRate}", Toast.LENGTH_SHORT).show()
                            // Start GenerateQRActivity with the address and price from the API
                            startGenerateQRActivity(address, it.conversionRate.toString(), logoResId, chain)
                        } else {
                            Log.e("API", "Conversion failed")
                            Toast.makeText(this@CryptoOptionActivity, "Conversion failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.e("API", "Response not successful")
                    Toast.makeText(this@CryptoOptionActivity, "Response not successful", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ConversionResponse>, t: Throwable) {
                Log.e("API", "API call failed", t)
                Toast.makeText(this@CryptoOptionActivity, "API call failed", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun startGenerateQRActivity(address: String, price: String, logoResId: Int, currency: String) {
        val intent = Intent(this, GenerateQRActivity::class.java)
        intent.putExtra("ADDRESS", address)
        intent.putExtra("PRICE", price)
        intent.putExtra("LOGO_RES_ID", logoResId)
        intent.putExtra("CURRENCY", currency)
        startActivity(intent)
    }
}