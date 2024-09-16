package com.example.possin

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import java.io.File
import java.util.Properties


class XpubAddress : AppCompatActivity() {

    private lateinit var searchField: AutoCompleteTextView
    private lateinit var cryptocurrencyContainer: LinearLayout
    private lateinit var cryptocurrencyAdapter: ArrayAdapter<String>
    private lateinit var cryptocurrencies: List<CryptoCurrencyInfo>
    private lateinit var filteredCryptocurrencies: List<CryptoCurrencyInfo>
    private lateinit var properties: Properties
    private lateinit var submitText: TextView
    private lateinit var backArrow: ImageView
    private var currentInputField: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_xpub_address)

        window.statusBarColor = ContextCompat.getColor(this, R.color.darkerRed)

        properties = ConfigProperties.loadProperties(this)

        // Find views
        searchField = findViewById(R.id.search_field)
        cryptocurrencyContainer = findViewById(R.id.cryptocurrency_container)
        backArrow = findViewById(R.id.back_arrow)
        submitText = findViewById(R.id.submit_text)

        // Load cryptocurrencies from JSON
        cryptocurrencies = loadCryptocurrencies(this)
        filteredCryptocurrencies = cryptocurrencies

        // Set up the AutoCompleteTextView adapter
        val cryptoNames = cryptocurrencies.map { it.name }
        cryptocurrencyAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, cryptoNames)
        searchField.setAdapter(cryptocurrencyAdapter)

        // Set up search field listener
        searchField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                filterCryptocurrencies(query)
                populateCryptocurrencyContainer()
                // Update the state to show/hide clear button
                searchField.setCompoundDrawablesWithIntrinsicBounds(
                    null, null,
                    if (query.isNotEmpty()) ContextCompat.getDrawable(this@XpubAddress, R.drawable.ic_clear_search) else null,
                    null
                )
            }
        })

        submitText.setOnClickListener {
            saveCryptocurrencyValues()
        }

        // Set up back arrow listener
        backArrow.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }


        // Populate the container with cryptocurrency items
        populateCryptocurrencyContainer()
    }

    private fun filterCryptocurrencies(query: String) {
        filteredCryptocurrencies = if (query.isEmpty()) {
            cryptocurrencies
        } else {
            cryptocurrencies.filter { it.name.contains(query, ignoreCase = true) }
        }
    }

    private fun populateCryptocurrencyContainer() {
        cryptocurrencyContainer.removeAllViews()
        val inflater = layoutInflater
        for (crypto in filteredCryptocurrencies) {
            val itemView = inflater.inflate(R.layout.item_cryptocurrency, cryptocurrencyContainer, false)
            val logoImageView = itemView.findViewById<ImageView>(R.id.crypto_logo)
            val nameTextView = itemView.findViewById<TextView>(R.id.crypto_name)
            val shortnameTextView = itemView.findViewById<TextView>(R.id.crypto_shortname)
            val chainTextView = itemView.findViewById<TextView>(R.id.crypto_chain)
            val typeSpinner = itemView.findViewById<Spinner>(R.id.type_spinner)
            val segwitLegacySpinner = itemView.findViewById<Spinner>(R.id.segwit_legacy_spinner)
            val inputField = itemView.findViewById<EditText>(R.id.input_field)
            val errorTextView = itemView.findViewById<TextView>(R.id.error_text)
            val scannerButton = itemView.findViewById<ImageButton>(R.id.scanner_button)

            // Get the private view key field (initially hidden)
            val viewKeyField = itemView.findViewById<EditText>(R.id.view_key_field)

            // Show the view key field if the crypto is Monero
            if (crypto.name == "Monero") {
                viewKeyField.visibility = View.VISIBLE
            } else {
                viewKeyField.visibility = View.GONE
            }

            val logoId = resources.getIdentifier(crypto.logo, "drawable", packageName)
            logoImageView.setImageResource(logoId)
            nameTextView.text = crypto.name
            shortnameTextView.text = crypto.shortname
            chainTextView.text = crypto.chain

            // Set up type spinner
            val typeAdapter: ArrayAdapter<CharSequence> = if (crypto.name == "Monero") {
                // For Monero, only show "address" option
                ArrayAdapter.createFromResource(this, R.array.monero_type_array, android.R.layout.simple_spinner_item)
            } else {
                // For other cryptocurrencies, use the regular type array
                ArrayAdapter.createFromResource(this, R.array.type_array, android.R.layout.simple_spinner_item)
            }
            typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            typeSpinner.adapter = typeAdapter

            // Set up segwit/legacy spinner
            val segwitLegacyAdapter = ArrayAdapter.createFromResource(this, R.array.segwit_legacy_array, android.R.layout.simple_spinner_item)
            segwitLegacyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            segwitLegacySpinner.adapter = segwitLegacyAdapter

            // Load saved values if they exist
            val savedType = properties.getProperty("${crypto.name}_type")
            val savedSegwitLegacy = properties.getProperty("${crypto.name}_segwit_legacy")
            val savedValue = properties.getProperty("${crypto.name}_value")

            if (savedType != null) {
                val spinnerPosition = typeAdapter.getPosition(savedType)
                typeSpinner.setSelection(spinnerPosition)
            }

            if (savedSegwitLegacy != null) {
                val spinnerPosition = segwitLegacyAdapter.getPosition(savedSegwitLegacy)
                segwitLegacySpinner.setSelection(spinnerPosition)
            }

            if (savedValue != null) {
                inputField.setText(savedValue)
            }

            // Initially hide segwit/legacy spinner
            segwitLegacySpinner.visibility = View.GONE

            inputField.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    validateInput(inputField, typeSpinner.selectedItem.toString(), segwitLegacySpinner.selectedItem.toString(), crypto.name, errorTextView, submitText)
                }
            })

            typeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    val selectedType = parent.getItemAtPosition(position).toString()
                    inputField.hint = "Enter $selectedType ${crypto.name}"
                    if (selectedType == "xpub" && (crypto.name == "Bitcoin" || crypto.name == "Litecoin")) {
                        segwitLegacySpinner.visibility = View.VISIBLE
                    } else {
                        segwitLegacySpinner.visibility = View.GONE
                    }
                    validateInput(inputField, selectedType, segwitLegacySpinner.selectedItem.toString(), crypto.name, errorTextView, submitText)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

            segwitLegacySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    // No validation needed here, handle other logic if necessary
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

            scannerButton.setOnClickListener {
                currentInputField = inputField // Store the current input field
                val integrator = IntentIntegrator(this@XpubAddress)
                integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
                integrator.setPrompt("Scan a barcode or QR code")
                integrator.setCameraId(0)  // Use a specific camera of the device
                integrator.setBeepEnabled(true)
                integrator.setBarcodeImageEnabled(true)
                integrator.initiateScan()
            }

            cryptocurrencyContainer.addView(itemView)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val result: IntentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)

        if (result.contents != null) {
            val scannedContent = result.contents

            // Check if the scanned result is in the Monero wallet format
            if (scannedContent.startsWith("monero_wallet:")) {
                // Remove the "monero_wallet:" prefix
                val uriWithoutPrefix = scannedContent.removePrefix("monero_wallet:")

                // Split the string at the '?' to separate the wallet address and query parameters
                val parts = uriWithoutPrefix.split("?")
                if (parts.size == 2) {
                    val walletAddress = parts[0] // The address part (before the ?)
                    val queryParams = parts[1] // The query parameters (after the ?)

                    // Extract the view key from the query parameters
                    val queryMap = queryParams.split("&").associate {
                        val (key, value) = it.split("=")
                        key to value
                    }

                    val viewKey = queryMap["view_key"]

                    // Set the values to the respective fields
                    currentInputField?.setText(walletAddress)
                    currentInputField = null // Clear the reference after use

                    // Find the view key field in the current item and set the value
                    if (viewKey != null) {
                        val itemView = cryptocurrencyContainer.getChildAt(filteredCryptocurrencies.indexOfFirst { it.name == "Monero" })
                        val viewKeyField = itemView.findViewById<EditText>(R.id.view_key_field)
                        viewKeyField?.setText(viewKey)
                    }
                }
            } else {
                // Handle other scanned results (non-Monero)
                currentInputField?.setText(result.contents)
                currentInputField = null // Clear the reference after use
            }
        }
    }

    private fun saveCryptocurrencyValues() {
        properties.remove("default_key")

        for (i in 0 until cryptocurrencyContainer.childCount) {
            val itemView = cryptocurrencyContainer.getChildAt(i)
            val nameTextView = itemView.findViewById<TextView>(R.id.crypto_name)
            val typeSpinner = itemView.findViewById<Spinner>(R.id.type_spinner)
            val segwitLegacySpinner = itemView.findViewById<Spinner>(R.id.segwit_legacy_spinner)
            val inputField = itemView.findViewById<EditText>(R.id.input_field)

            val cryptoName = nameTextView.text.toString()
            val inputType = typeSpinner.selectedItem.toString()
            val segwitLegacy = segwitLegacySpinner.selectedItem.toString()
            var value = inputField.text.toString()

            // Check if inputType is "address" and cryptoName is "Bitcoincash"
            if (inputType == "address" && cryptoName == "Bitcoincash") {
                // Skip saving if no value is entered
                if (value.isEmpty()) {
                    continue // Skip this entry if there's no value
                }
                // Add "bitcoincash:" prefix if it's not present
                if (!value.startsWith("bitcoincash:", true)) {
                    value = "bitcoincash:$value"
                }
            }

            // For Monero, save the private view key
            if (cryptoName == "Monero") {
                val viewKeyField = itemView.findViewById<EditText>(R.id.view_key_field)
                val viewKey = viewKeyField.text.toString()
                if (viewKey.isNotEmpty()) {
                    properties.setProperty("view_key", viewKey)
                }
            }

            // Only save if value is not empty
            if (value.isNotEmpty()) {
                properties.setProperty("${cryptoName}_type", inputType)
                if (inputType == "xpub" && (cryptoName == "Bitcoin" || cryptoName == "Litecoin")) {
                    properties.setProperty("${cryptoName}_segwit_legacy", segwitLegacy)
                }
                properties.setProperty("${cryptoName}_value", value)
            }
        }

        // Manually write properties to avoid colon escaping
        val propertiesFile = File(filesDir, "config.properties")
        propertiesFile.bufferedWriter().use { writer ->
            properties.forEach { key, v ->
                val value = v.toString().replace("\\", "") // Ensure no backslashes
                writer.write("$key=$value\n")
            }
        }

        showSuccessModal()
    }




    private fun validateInput(editText: EditText, inputType: String, segwitLegacy: String, currency: String, errorTextView: TextView, submitText: TextView) {
        val value = editText.text.toString()
        println(value)

        if (value.isBlank()) {
            errorTextView.text = ""
            submitText.isEnabled = false
            submitText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
            return
        }

        val isValid = when (currency) {
            "Bitcoin" -> {
                println(inputType)
                if (inputType == "xpub") BitcoinManager.isValidXpub(value) else BitcoinManager.isValidAddress(value)
            }
            "Dogecoin" -> {
                if (inputType == "xpub") DogecoinManager.isValidXpub(value) else DogecoinManager.isValidAddress(value)
            }
            "Litecoin" -> {
                if (inputType == "xpub") LitecoinManager.isValidXpub(value, this) else LitecoinManager.isValidAddress(value)
            }
            "Ethereum" -> {
                if (inputType == "xpub") EthereumManager.isValidXpub(value) else EthereumManager.isValidAddress(value)
            }
            "Tether" -> {
                if (inputType == "xpub") TronManager.isValidXpub(value) else TronManager.isValidAddress(value)
            }
            "Dash" -> {
                if (inputType == "xpub") DashManager.isValidXpub(value) else DashManager.isValidAddress(value)
            }
            "Bitcoincash" -> {
                if (inputType == "xpub") BitcoinCashManager.isValidXpub(value) else BitcoinCashManager.isValidAddress(value)
            }
            "Monero" -> {
                // Check both address and view key for Monero
                val itemView = cryptocurrencyContainer.getChildAt(filteredCryptocurrencies.indexOfFirst { it.name == "Monero" })
                val viewKeyField = itemView.findViewById<EditText>(R.id.view_key_field)
                val viewKey = viewKeyField.text.toString()
                Log.d("MONERO ADDRESS", MoneroManager.isValidAddress(value).toString())
                Log.d("MONERO PRIVKEY", MoneroManager.isValidPrivateViewKey(viewKey).toString())

                MoneroManager.isValidAddress(value) && MoneroManager.isValidPrivateViewKey(viewKey)
            }
            else -> false
        }

        if (!isValid) {
            errorTextView.text = "Invalid $inputType for $currency"
            submitText.isEnabled = false
            submitText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        } else {
            errorTextView.text = ""
            submitText.isEnabled = allInputsValid()
            if (submitText.isEnabled) {
                submitText.setTextColor(ContextCompat.getColor(this, R.color.white))
            } else {
                submitText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
            }
        }
    }

    private fun allInputsValid(): Boolean {
        for (i in 0 until cryptocurrencyContainer.childCount) {
            val itemView = cryptocurrencyContainer.getChildAt(i)
            val errorTextView = itemView.findViewById<TextView>(R.id.error_text)
            if (errorTextView.text.isNotEmpty()) {
                return false
            }
        }
        return true
    }

    private fun showSuccessModal() {
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




    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    // Getter methods for testing
//    fun getCryptocurrencies(): List<CryptoCurrencyInfo> {
//        return cryptocurrencies
//    }
//
//    fun getFilteredCryptocurrencies(): List<CryptoCurrencyInfo> {
//        return filteredCryptocurrencies
//    }

}