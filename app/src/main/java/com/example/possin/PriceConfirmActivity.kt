package com.example.possin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PriceConfirmActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.price_confirm)

        val price = intent.getStringExtra("PRICE") ?: "0.00"
        val currencyCode = intent.getStringExtra("CURRENCY_CODE") ?: "USD"
        val priceTextView: TextView = findViewById(R.id.priceTextView)
        priceTextView.text = price

        val btnConfirm: Button = findViewById(R.id.btnConfirm)
        btnConfirm.setOnClickListener {
            val intent = Intent(this, CryptoOptionActivity::class.java)
            intent.putExtra("PRICE", price)
            intent.putExtra("CURRENCY_CODE", currencyCode)
            startActivity(intent)
        }
    }
}