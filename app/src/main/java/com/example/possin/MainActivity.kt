package com.example.possin

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check if .properties file is empty or doesn't exist
        propertiesFile = File(filesDir, "config.properties")
        if (!propertiesFile.exists() || propertiesFile.readText().isBlank()) {
            setContentView(R.layout.activity_onboarding)
            setupOnboardingView()
        } else {
            setContentView(POSView(this))
            setupMainView()
        }
    }

    private fun setupOnboardingView() {
        val checkboxBitcoin = findViewById<CheckBox>(R.id.checkbox_bitcoin)
        val checkboxEthereum = findViewById<CheckBox>(R.id.checkbox_ethereum)
        val checkboxLitecoin = findViewById<CheckBox>(R.id.checkbox_litecoin)
        val checkboxDogecoin = findViewById<CheckBox>(R.id.checkbox_dogecoin)
        val buttonSubmit = findViewById<Button>(R.id.button_submit)

        buttonSubmit.setOnClickListener {
            selectedCryptocurrencies = mutableListOf<String>().apply {
                if (checkboxBitcoin.isChecked) add("Bitcoin")
                if (checkboxEthereum.isChecked) add("Ethereum")
                if (checkboxLitecoin.isChecked) add("Litecoin")
                if (checkboxDogecoin.isChecked) add("Dogecoin")
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
            selectedCryptocurrencies.forEachIndexed { index, currency ->
                val editText = xpubInputContainer.getChildAt(index) as EditText
                properties.setProperty("${currency}_xpub", editText.text.toString())
            }

            // Save XPUBs to properties file
            propertiesFile.outputStream().use {
                properties.store(it, null)
            }

            // Proceed to main view
            setContentView(POSView(this))
            setupMainView()
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