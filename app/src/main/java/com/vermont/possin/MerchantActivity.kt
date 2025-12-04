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

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

class MerchantActivity : AppCompatActivity() {

    private lateinit var editTextBusinessName: EditText
    private lateinit var editTextAddress: EditText
    private lateinit var editTextCity: EditText
    private lateinit var editTextState: EditText
    private lateinit var editTextZipCode: EditText
    private lateinit var editTextCountry: EditText
    private lateinit var editTextMerchantPhone: EditText
    private lateinit var editTextMerchantEmail: EditText
    private lateinit var currencySpinner: Spinner
    private var currentCurrencyCode = ""
    private var currentCurrencySymbol = "$"
    private val currencyNames = mapOf(
        "USD" to "US Dollar",
        "EUR" to "Euro",
        "JPY" to "Japanese Yen",
        "GBP" to "British Pound",
        "AUD" to "Australian Dollar",
        "CAD" to "Canadian Dollar",
        "CHF" to "Swiss Franc",
        "CNY" to "Chinese Yuan",
        "SEK" to "Swedish Krona",
        "NZD" to "New Zealand Dollar",
        "BTC" to "Bitcoin",
        "LTC" to "Litecoin",
        "DASH" to "Dash",
        "DOGE" to "Dogecoin",
        "ETH" to "Ethereum",
        "USDT" to "Tether",
        "XMR" to "Monero",
        "LOG" to "Woodcoin"
    )



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_merchant)

        window.statusBarColor = ContextCompat.getColor(this, R.color.tapeRed)

        // Initialize the EditTexts
        editTextBusinessName = findViewById(R.id.edittext_business_name)
        editTextAddress = findViewById(R.id.edittext_address)
        editTextCity = findViewById(R.id.edittext_city)
        editTextState = findViewById(R.id.edittext_state)
        editTextZipCode = findViewById(R.id.edittext_zip_code)
        editTextCountry = findViewById(R.id.edittext_country)
        editTextMerchantPhone = findViewById(R.id.edittext_merchant_phone)
        editTextMerchantEmail = findViewById(R.id.edittext_merchant_email)

        val setPinTextView = findViewById<TextView>(R.id.setPinTextView)
        // Check if a PIN is already set
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val userPin = sharedPreferences.getString("USER_PIN", null)

        // If a PIN is already set, change the text to "Update Pin"
        if (!userPin.isNullOrEmpty()) {
            setPinTextView.text = getString(R.string.update_pin)
        }

        setPinTextView.setOnClickListener {
            val intent = Intent(this, com.vermont.possin.SetPinActivity::class.java)
            startActivity(intent)
        }

        currencySpinner = findViewById(R.id.currencySpinnerMerchant)
        setupCurrencySpinner()


        // Load the properties if they exist
        loadMerchantProperties()

        // Set up the back arrow click listener
        val backArrow = findViewById<ImageView>(R.id.back_arrow)
        backArrow.setOnClickListener {
            finish()
        }

        // Set up the submit button click listener
        val submitText = findViewById<TextView>(R.id.submit_text)
        submitText.setOnClickListener {
            if (editTextBusinessName.text.toString().isEmpty()) {
                Toast.makeText(this, R.string.business_name_is_required, Toast.LENGTH_SHORT).show()
            } else {
                saveMerchantProperties()
                showSuccessDialog()
            }
        }
    }

    private fun loadMerchantProperties() {
        val file = File(filesDir, "merchant.properties")
        if (file.exists()) {
            val properties = Properties()
            val inputStream = FileInputStream(file)
            properties.load(inputStream)
            inputStream.close()

            editTextBusinessName.setText(properties.getProperty("merchant_name", ""))
            editTextAddress.setText(properties.getProperty("address", ""))
            editTextCity.setText(properties.getProperty("city", ""))
            editTextState.setText(properties.getProperty("state", ""))
            editTextZipCode.setText(properties.getProperty("zip_code", ""))
            editTextCountry.setText(properties.getProperty("country", ""))
            editTextMerchantPhone.setText(properties.getProperty("phone", ""))
            editTextMerchantEmail.setText(properties.getProperty("email", ""))
        }
    }

    private fun saveMerchantProperties() {
        val properties = Properties()
        properties.setProperty("merchant_name", editTextBusinessName.text.toString())
        properties.setProperty("address", editTextAddress.text.toString())
        properties.setProperty("city", editTextCity.text.toString())
        properties.setProperty("state", editTextState.text.toString())
        properties.setProperty("zip_code", editTextZipCode.text.toString())
        properties.setProperty("country", editTextCountry.text.toString())
        properties.setProperty("phone", editTextMerchantPhone.text.toString())
        properties.setProperty("email", editTextMerchantEmail.text.toString())

        val file = File(filesDir, "merchant.properties")
        val fileOutputStream = FileOutputStream(file)
        properties.store(fileOutputStream, "Merchant Properties")
        fileOutputStream.close()
    }

    private fun showSuccessDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_success, null)
        val dialog = Dialog(this, R.style.CustomDialog)
        dialog.setContentView(dialogView)
        dialog.setCancelable(false)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogView.findViewById<Button>(R.id.btn_ok).setOnClickListener {
            dialog.dismiss()
            navigateToHome()
        }
        dialog.show()
    }

    private fun setupCurrencySpinner() {
        val displayList = mutableListOf<String>()
        val codes = mutableListOf<String>()
        val symbols = mutableListOf<String>()

        try {
            val inputStream = resources.openRawResource(R.raw.currencies)
            val json = inputStream.bufferedReader().use { it.readText() }
            val arr = org.json.JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val symbol = obj.getString("symbol")
                val code = obj.getString("code")
                val name = currencyNames[code] ?: code

                displayList.add("$symbol $name")   // 👈 show both
                codes.add(code)
                symbols.add(symbol)
            }
        } catch (_: Exception) {}

        val adapter = android.widget.ArrayAdapter(
            this,
            R.layout.spinner_item,
            displayList
        )
        adapter.setDropDownViewResource(R.layout.spinner_item)
        currencySpinner.adapter = adapter

        val prefs = getSharedPreferences("pos_prefs", Context.MODE_PRIVATE)
        val savedCode = prefs.getString("last_currency_code", null)
        if (savedCode != null) {
            val index = codes.indexOf(savedCode)
            if (index >= 0) {
                currencySpinner.setSelection(index)
                currentCurrencySymbol = symbols[index]
                currentCurrencyCode = codes[index]
            }
        }

        currencySpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                currentCurrencySymbol = symbols.getOrNull(position) ?: "$"
                currentCurrencyCode = codes.getOrNull(position) ?: ""

                prefs.edit().putString("last_currency_code", currentCurrencyCode).apply()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }
    }



    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}
