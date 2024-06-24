package com.example.possin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.InputStream
import java.util.Properties

class CryptoOptionActivity : AppCompatActivity() {

    private lateinit var bitcoinManager: BitcoinManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.crypto_option)

        val price = intent.getStringExtra("PRICE") ?: "0.00"
        val priceTextView: TextView = findViewById(R.id.priceTextView)
        priceTextView.text = price

        val xPub = loadXPubFromSettings()
        bitcoinManager = BitcoinManager(xPub)

        findViewById<Button>(R.id.btnBTC).setOnClickListener { handleBTCClick(price) }
        findViewById<Button>(R.id.btnLTC).setOnClickListener { /* Handle LTC */ }
        findViewById<Button>(R.id.btnDASH).setOnClickListener { /* Handle DASH */ }
        findViewById<Button>(R.id.btnDOGE).setOnClickListener { /* Handle DOGE */ }
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
        val address = bitcoinManager.getAddress(0) // Example: Get the address at index 0
        val intent = Intent(this, GenerateQRActivity::class.java)
        intent.putExtra("ADDRESS", address)
        intent.putExtra("PRICE", price)
        startActivity(intent)
    }
}