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
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class PriceConfirmActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.price_confirm)

        window.statusBarColor = ContextCompat.getColor(this, R.color.darkerRed)

        val price = intent.getStringExtra("PRICE") ?: "0.00"
        val currencyCode = intent.getStringExtra("CURRENCY_CODE") ?: "USD"
        val priceTextView: TextView = findViewById(R.id.priceTextView)
        priceTextView.text = price

        val messageEditText: EditText = findViewById(R.id.messageEditText)

        val backArrow: ImageView = findViewById(R.id.back_arrow)
        backArrow.setOnClickListener {
            finish() // Navigate back to the previous activity
        }

        val submitButton: Button = findViewById(R.id.submit_button)
        submitButton.setOnClickListener {
            val intent = Intent(this, CryptoOptionActivity::class.java)
            intent.putExtra("PRICE", price)
            intent.putExtra("CURRENCY_CODE", currencyCode)
            intent.putExtra("MESSAGE", messageEditText.text.toString())
            startActivity(intent)
        }
    }
}
