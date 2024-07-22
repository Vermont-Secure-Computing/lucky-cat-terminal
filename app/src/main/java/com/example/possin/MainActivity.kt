package com.example.possin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.text.Editable
import android.text.TextWatcher
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
        val checkboxDash = findViewById<CheckBox>(R.id.checkbox_dash)
        val checkboxUSDTTron = findViewById<CheckBox>(R.id.checkbox_usdt)
        val checkboxWoodcoin = findViewById<CheckBox>(R.id.checkbox_woodcoin)
        val buttonSubmit = findViewById<Button>(R.id.button_submit)

        buttonSubmit.setOnClickListener {
            selectedCryptocurrencies = mutableListOf<String>().apply {
                if (checkboxBitcoin.isChecked) add("Bitcoin")
                if (checkboxEthereum.isChecked) add("Ethereum")
                if (checkboxLitecoin.isChecked) add("Litecoin")
                if (checkboxDogecoin.isChecked) add("Dogecoin")
                if (checkboxDash.isChecked) add("Dash")
                if (checkboxUSDTTron.isChecked) add("USDT-Tron")
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
        val buttonSubmitXpub = findViewById<Button>(R.id.button_submit_xpub)

        selectedCryptocurrencies.forEach { currency ->
            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

            val spinner = Spinner(this).apply {
                adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, listOf("XPUB", "Address"))
            }
            layout.addView(spinner)

            val editText = EditText(this).apply {
                hint = "Enter XPUB or Address for $currency"
            }
            layout.addView(editText)

            val errorTextView = TextView(this).apply {
                setTextColor(resources.getColor(android.R.color.holo_red_dark))
            }
            layout.addView(errorTextView)

            // Add listener to spinner to update hint
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    val selectedItem = parent.getItemAtPosition(position).toString()
                    editText.hint = "Enter $selectedItem for $currency"
                    validateInput(editText, selectedItem, currency, errorTextView, buttonSubmitXpub)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // Do nothing
                }
            }

            // Add text change listener to validate input
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val selectedItem = spinner.selectedItem.toString()
                    validateInput(editText, selectedItem, currency, errorTextView, buttonSubmitXpub)
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            xpubInputContainer.addView(layout)
        }

        buttonSubmitXpub.setOnClickListener {
            val properties = Properties()
            var allXpubsOrAddressesEntered = true

            selectedCryptocurrencies.forEachIndexed { index, currency ->
                val layout = xpubInputContainer.getChildAt(index) as LinearLayout
                val spinner = layout.getChildAt(0) as Spinner
                val editText = layout.getChildAt(1) as EditText

                val inputType = spinner.selectedItem.toString()
                val value = editText.text.toString()

                if (value.isBlank()) {
                    allXpubsOrAddressesEntered = false
                }

                properties.setProperty("${currency}_type", inputType)
                properties.setProperty("${currency}_value", value)
            }

            if (!allXpubsOrAddressesEntered) {
                Toast.makeText(this, "Please enter XPUB or Address for all selected cryptocurrencies.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save XPUBs or addresses to properties file
            propertiesFile.outputStream().use {
                properties.store(it, null)
            }

            // Proceed to merchant details input view
            setContentView(R.layout.activity_merchant_details)
            setupMerchantDetailsView()
        }
    }

    private fun validateInput(editText: EditText, inputType: String, currency: String, errorTextView: TextView, submitButton: Button) {
        val value = editText.text.toString()

        if (value.isBlank()) {
            errorTextView.text = ""
            submitButton.isEnabled = false
            return
        }

        val isValid = when (currency) {
            "Bitcoin" -> {
                if (inputType == "XPUB") BitcoinManager.isValidXpub(value) else BitcoinManager.isValidAddress(value)
            }
            "Dogecoin" -> {
                if (inputType == "XPUB") DogecoinManager.isValidXpub(value) else DogecoinManager.isValidAddress(value)
            }
            "Litecoin" -> {
                if (inputType == "XPUB") LitecoinManager.isValidXpub(value) else LitecoinManager.isValidAddress(value)
            }
            "Ethereum" -> {
                if (inputType == "XPUB") EthereumManager.isValidXpub(value) else EthereumManager.isValidAddress(value)
            }
            "USDT-Tron" -> {
                if (inputType == "XPUB") TronManager.isValidXpub(value) else TronManager.isValidAddress(value)
            }
            else -> false
        }

        if (!isValid) {
            errorTextView.text = "Invalid $inputType for $currency"
            submitButton.isEnabled = false
        } else {
            errorTextView.text = ""
            submitButton.isEnabled = allInputsValid()
        }
    }

    private fun allInputsValid(): Boolean {
        val xpubInputContainer = findViewById<LinearLayout>(R.id.xpub_input_container)
        var allValid = true
        for (i in 0 until xpubInputContainer.childCount) {
            val layout = xpubInputContainer.getChildAt(i) as LinearLayout
            val errorTextView = layout.getChildAt(2) as TextView
            if (errorTextView.text.isNotEmpty()) {
                allValid = false
                break
            }
        }
        return allValid
    }

    private fun setupMerchantDetailsView() {
        val editTextBusinessName = findViewById<EditText>(R.id.edittext_business_name)
        val editTextAddress = findViewById<EditText>(R.id.edittext_address)
        val editTextCity = findViewById<EditText>(R.id.edittext_city)
        val editTextState = findViewById<EditText>(R.id.edittext_state)
        val editTextZipCode = findViewById<EditText>(R.id.edittext_zip_code)
        val editTextCountry = findViewById<EditText>(R.id.edittext_country)
        val editTextPhone = findViewById<EditText>(R.id.edittext_merchant_phone)
        val editTextEmail = findViewById<EditText>(R.id.edittext_merchant_email)
        val buttonSubmitMerchantDetails = findViewById<Button>(R.id.button_submit_merchant_details)

        buttonSubmitMerchantDetails.setOnClickListener {
            val businessName = editTextBusinessName.text.toString().trim()
            val address = editTextAddress.text.toString().trim()
            val city = editTextCity.text.toString().trim()
            val state = editTextState.text.toString().trim()
            val zipCode = editTextZipCode.text.toString().trim()
            val country = editTextCountry.text.toString().trim()
            val phone = editTextPhone.text.toString().trim()
            val email = editTextEmail.text.toString().trim()

            // Check for required fields
            if (businessName.isEmpty() || address.isEmpty() || city.isEmpty() || zipCode.isEmpty() || country.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save merchant details to separate properties file
            val merchantProperties = Properties()
            merchantProperties.setProperty("business_name", businessName)
            merchantProperties.setProperty("address", address)
            merchantProperties.setProperty("city", city)
            if (state.isNotEmpty()) merchantProperties.setProperty("state", state)
            merchantProperties.setProperty("zip_code", zipCode)
            merchantProperties.setProperty("country", country)
            if (phone.isNotEmpty()) merchantProperties.setProperty("phone", phone)
            if (email.isNotEmpty()) merchantProperties.setProperty("email", email)
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
