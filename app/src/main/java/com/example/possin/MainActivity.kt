package com.example.possin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import java.io.File
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var selectedCryptocurrencies: List<String>
    private lateinit var propertiesFile: File
    private lateinit var merchantPropertiesFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check if .properties file is empty or doesn't exist
        propertiesFile = File(filesDir, "config.properties")
        merchantPropertiesFile = File(filesDir, "merchant.properties")
        if (!propertiesFile.exists() || propertiesFile.readText().isBlank()) {
            setContentView(R.layout.activity_onboarding)
            setupOnboardingView()
        } else {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }

    private fun setupOnboardingView() {
        val checkboxBitcoin = findViewById<CheckBox>(R.id.checkbox_bitcoin)
        val checkboxEthereum = findViewById<CheckBox>(R.id.checkbox_ethereum)
        val checkboxLitecoin = findViewById<CheckBox>(R.id.checkbox_litecoin)
        val checkboxDogecoin = findViewById<CheckBox>(R.id.checkbox_dogecoin)
        val checkboxWoodcoin = findViewById<CheckBox>(R.id.checkbox_woodcoin)
        val buttonSubmit = findViewById<Button>(R.id.button_submit)

        buttonSubmit.setOnClickListener {
            selectedCryptocurrencies = mutableListOf<String>().apply {
                if (checkboxBitcoin.isChecked) add("Bitcoin")
                if (checkboxEthereum.isChecked) add("Ethereum")
                if (checkboxLitecoin.isChecked) add("Litecoin")
                if (checkboxDogecoin.isChecked) add("Dogecoin")
                if (checkboxWoodcoin.isChecked) add("Woodcoin")
            }

            if (selectedCryptocurrencies.isEmpty()) {
                Toast.makeText(this, "Please select at least one cryptocurrency.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save selected cryptocurrencies to properties file
            val properties = Properties()
            properties.setProperty("cryptocurrencies", selectedCryptocurrencies.joinToString(","))
            propertiesFile.outputStream().use {
                properties.store(it, null)
            }

            // Proceed to XPUB input view
            setContentView(R.layout.activity_xpub_input)
            setupXpubInputView()
        }
    }

    private fun setupXpubInputView() {
        val xpubInputContainer = findViewById<LinearLayout>(R.id.xpub_input_container)
        selectedCryptocurrencies.forEach { currency ->
            val editText = EditText(this).apply {
                hint = "Enter XPUB for $currency"
            }
            xpubInputContainer.addView(editText)
        }

        val buttonSubmitXpub = findViewById<Button>(R.id.button_submit_xpub)
        buttonSubmitXpub.setOnClickListener {
            val properties = Properties()
            var allXpubsEntered = true

            selectedCryptocurrencies.forEachIndexed { index, currency ->
                val editText = xpubInputContainer.getChildAt(index) as EditText
                val xpub = editText.text.toString()
                if (xpub.isBlank()) {
                    allXpubsEntered = false
                }
                properties.setProperty("${currency}_xpub", xpub)
            }

            if (!allXpubsEntered) {
                Toast.makeText(this, "Please enter XPUB for all selected cryptocurrencies.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save XPUBs to properties file
            propertiesFile.outputStream().use {
                properties.store(it, null)
            }

            // Proceed to merchant details input view
            setContentView(R.layout.activity_merchant_details)
            setupMerchantDetailsView()
        }
    }

    private fun setupMerchantDetailsView() {
        val editTextName = findViewById<EditText>(R.id.edittext_merchant_name)
        val editTextPhone = findViewById<EditText>(R.id.edittext_merchant_phone)
        val editTextEmail = findViewById<EditText>(R.id.edittext_merchant_email)
        val buttonSubmitMerchantDetails = findViewById<Button>(R.id.button_submit_merchant_details)

        buttonSubmitMerchantDetails.setOnClickListener {
            val name = editTextName.text.toString().trim()
            val phone = editTextPhone.text.toString().trim()
            val email = editTextEmail.text.toString().trim()

            // Save merchant details to separate properties file (if provided)
            val merchantProperties = Properties()
            if (name.isNotEmpty()) merchantProperties.setProperty("merchant_name", name)
            if (phone.isNotEmpty()) merchantProperties.setProperty("merchant_phone", phone)
            if (email.isNotEmpty()) merchantProperties.setProperty("merchant_email", email)
            merchantPropertiesFile.outputStream().use {
                merchantProperties.store(it, null)
            }

            // Proceed to home view
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }

    private fun setupMainView() {
        val rootView = findViewById<View>(R.id.gridLayout)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = systemBars.bottom)
            insets
        }
    }
}