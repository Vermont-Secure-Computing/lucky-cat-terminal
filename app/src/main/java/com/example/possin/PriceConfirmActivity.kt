package com.example.possin

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PriceConfirmActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.price_confirm)

        val price = intent.getStringExtra("PRICE") ?: "0.00"
        val priceTextView: TextView = findViewById(R.id.priceTextView)
        priceTextView.text = price
    }
}