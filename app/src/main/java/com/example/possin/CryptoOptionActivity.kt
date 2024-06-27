package com.example.possin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.possin.R
import com.example.possin.network.RetrofitClient
import com.example.possin.model.ConversionResponse
import com.example.possin.network.ConversionRequestBody
import com.example.possin.BitcoinManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.InputStream
import java.util.Properties

class CryptoOptionActivity : AppCompatActivity() {

    private lateinit var bitcoinManager: BitcoinManager
    private lateinit var litecoinManager: LitecoinManager
    private lateinit var dogecoinManager: DogecoinManager
//    private lateinit var dashManager: DashManager
    private lateinit var selectedCurrencyCode: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.crypto_option)

        val price = intent.getStringExtra("PRICE") ?: "0.00"
        val priceTextView: TextView = findViewById(R.id.priceTextView)
        priceTextView.text = price

        val xPub = loadXPubFromSettings()
        bitcoinManager = BitcoinManager(this, xPub)
        litecoinManager = LitecoinManager(this, xPub)
        dogecoinManager = DogecoinManager(this, xPub)
//        dashManager = DashManager(this, xPub)


        // Assuming selectedCurrencyCode is obtained from POSView (e.g., from a Spinner or other input)
        selectedCurrencyCode = intent.getStringExtra("CURRENCY_CODE") ?: "BTC"

        findViewById<Button>(R.id.btnBTC).setOnClickListener { handleBTCClick(price) }
        findViewById<Button>(R.id.btnLTC).setOnClickListener { handleLTCClick(price) }
        findViewById<Button>(R.id.btnDASH).setOnClickListener {  /* Handle DASH */ }
        findViewById<Button>(R.id.btnDOGE).setOnClickListener { handleDOGEClick(price) }
        findViewById<Button>(R.id.btnETH).setOnClickListener { /* Handle ETH */ }
        findViewById<Button>(R.id.btnXMR).setOnClickListener { /* Handle XMR */ }
        findViewById<Button>(R.id.btnUSDT).setOnClickListener { /* Handle USDT */ }
        findViewById<Button>(R.id.btnLOG).setOnClickListener { /* Handle LOG */ }
    }

    private fun loadXPubFromSettings(): String {
        val properties = Properties()
        val inputStream: InputStream = assets.open("settings.properties")
        properties.load(inputStream)
        return properties.getProperty("xpub", "")
    }

    private fun handleBTCClick(price: String) {
        val address = bitcoinManager.getAddress() // Example: Get the address at index 0

        // Remove any non-numeric characters (except for decimal point) from the price
        val numericPrice = price.filter { it.isDigit() || it == '.' }

        // Call the API to get the conversion rate
//        Log.d("Currency ", "Currency code $selectedCurrencyCode")
        postConversionApi(numericPrice, selectedCurrencyCode, address, "BTC", R.drawable.bitcoin_logo)
//        startGenerateQRActivity(address, numericPrice, R.drawable.bitcoin_logo, "BTC")
    }

    private fun handleLTCClick(price: String) {
        val address = litecoinManager.getAddress() // Example: Get the address at index 0

        // Remove any non-numeric characters (except for decimal point) from the price
        val numericPrice = price.filter { it.isDigit() || it == '.' }

        // Call the API to get the conversion rate
//        Log.d("Currency ", "Currency code $selectedCurrencyCode")
        postConversionApi(numericPrice, selectedCurrencyCode, address, "LTC", R.drawable.litecoin_new_logo)
//        startGenerateQRActivity(address, numericPrice, R.drawable.litecoin_new_logo, "LTC")
    }

    private fun handleDOGEClick(price: String) {
        val address = dogecoinManager.getAddress() // Get the next address

        // Remove any non-numeric characters (including the decimal point) from the price
        val numericPrice = price.filter { it.isDigit() }

        // Call the API to get the conversion rate
        postConversionApi(numericPrice, selectedCurrencyCode, address, "DOGE", R.drawable.dogecoin_logo)
//        startGenerateQRActivity(address, numericPrice, R.drawable.dogecoin_logo, "DOGE")
    }

//    private fun handleDASHClick(price: String) {
//        val address = dashManager.getAddress() ?: run {
//            Toast.makeText(this, "Failed to derive DASH address", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        // Remove any non-numeric characters (including the decimal point) from the price
//        val numericPrice = price.filter { it.isDigit() }
//
//        // Call the API to get the conversion rate
//        postConversionApi(numericPrice, selectedCurrencyCode, address, "DASH", R.drawable.dashcoin_logo )
////        startGenerateQRActivity(address, numericPrice, R.drawable.dashcoin_logo, "DASH")
//    }

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