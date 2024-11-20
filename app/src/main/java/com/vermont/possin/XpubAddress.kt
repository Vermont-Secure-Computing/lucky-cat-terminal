package com.vermont.possin

import MoneroWalletRequestBody
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import com.vermont.possin.model.ApiResponse
import com.vermont.possin.network.MoneroDeleteWalletRequestBody
import com.vermont.possin.network.RetrofitClient
import pl.droidsonroids.gif.GifDrawable
import retrofit2.Call
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
    private var loadingDialog: AlertDialog? = null

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

                // Load saved Monero-specific values (view key and restore height)
                val savedViewKey = properties.getProperty("Monero_view_key")

                // Set the saved values to the respective fields if they exist
                if (savedViewKey != null) {
                    viewKeyField.setText(savedViewKey)
                }

            } else {
                viewKeyField.visibility = View.GONE
            }

            val logoId = resources.getIdentifier(crypto.logo, "drawable", packageName)
            logoImageView.setImageResource(logoId)
            nameTextView.text = crypto.name
            shortnameTextView.text = crypto.shortname
            chainTextView.text = crypto.chain

            // Set up type spinner
            val typeAdapter: ArrayAdapter<CharSequence> = if (crypto.name == "Monero" || crypto.name == "Ethereum" || crypto.name == "Tether") {
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
                integrator.setPrompt(getString(R.string.scan_a_barcode_or_QR_code))
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

                    // Extract the view key and restore height from the query parameters
                    val queryMap = queryParams.split("&").associate {
                        val (key, value) = it.split("=")
                        key to value
                    }

                    val viewKey = queryMap["view_key"]

                    // Set the values to the respective fields
                    currentInputField?.setText(walletAddress)
                    currentInputField = null // Clear the reference after use

                    if (viewKey != null) {
                        val itemView = cryptocurrencyContainer.getChildAt(filteredCryptocurrencies.indexOfFirst { it.name == "Monero" })

                        // Find the view key and restore height fields
                        val viewKeyField = itemView.findViewById<EditText>(R.id.view_key_field)
                        viewKeyField?.setText(viewKey)
                    }
                }
            } else {
                // Handle other scanned results (non-Monero)
                var scannedAddress = result.contents

                // Remove prefix for all cryptocurrencies except Bitcoin Cash
                if (!scannedAddress.startsWith("bitcoincash:", true) && scannedAddress.contains(":")) {
                    val parts = scannedAddress.split(":")
                    if (parts.size > 1) {
                        scannedAddress = parts[1] // Take only the address part
                    }
                }

                currentInputField?.setText(scannedAddress)
                currentInputField = null // Clear the reference after use
            }
        }
    }



    private fun saveCryptocurrencyValues() {
        properties.remove("default_key")

        var moneroWalletData: MoneroWalletRequestBody? = null // To store Monero data for API call
        var moneroDeleted = false // To track if Monero needs to be deleted via API

        for (i in 0 until cryptocurrencyContainer.childCount) {
            val itemView = cryptocurrencyContainer.getChildAt(i)
            val nameTextView = itemView.findViewById<TextView>(R.id.crypto_name)
            val typeSpinner = itemView.findViewById<Spinner>(R.id.type_spinner)
            val segwitLegacySpinner = itemView.findViewById<Spinner>(R.id.segwit_legacy_spinner)
            val inputField = itemView.findViewById<EditText>(R.id.input_field)

            val cryptoName = nameTextView.text.toString()
            val inputType = typeSpinner.selectedItem.toString()
            val segwitLegacy = segwitLegacySpinner.selectedItem.toString()
            val value = inputField.text.toString().trim()

            // Handle Monero-specific logic
            if (cryptoName == "Monero") {
                val viewKeyField = itemView.findViewById<EditText>(R.id.view_key_field)
                val viewKey = viewKeyField.text.toString().trim()

                val savedAddress = properties.getProperty("Monero_value")
                val savedViewKey = properties.getProperty("Monero_view_key")

                if (value.isEmpty() && savedAddress != null) {
                    // Monero address was removed
                    properties.remove("Monero_value")
                    properties.remove("Monero_view_key")
                    properties.remove("Monero_type")
                    moneroDeleted = true
                } else if (value.isNotEmpty() && (savedAddress != value || savedViewKey != viewKey)) {
                    // Monero address was modified
                    moneroWalletData = MoneroWalletRequestBody(
                        currentAddress = savedAddress,
                        newAddress = value,
                        privateViewKey = viewKey
                    )
                }
            } else {
                // Handle other cryptocurrencies
                val savedValue = properties.getProperty("${cryptoName}_value")

                if (value.isEmpty() && savedValue != null) {
                    // Address or xpub was removed
                    properties.remove("${cryptoName}_type")
                    properties.remove("${cryptoName}_segwit_legacy")
                    properties.remove("${cryptoName}_value")
                } else if (value.isNotEmpty()) {
                    // Address or xpub was updated
                    properties.setProperty("${cryptoName}_type", inputType)
                    if (inputType == "xpub" && (cryptoName == "Bitcoin" || cryptoName == "Litecoin")) {
                        properties.setProperty("${cryptoName}_segwit_legacy", segwitLegacy)
                    } else if (inputType == "address" && (cryptoName == "Bitcoin" || cryptoName == "Litecoin")) {
                        // Remove segwit_legacy property if the type changes from xpub to address
                        properties.remove("${cryptoName}_segwit_legacy")
                    }
                    properties.setProperty("${cryptoName}_value", value)
                }
            }
        }

        // Handle Monero API calls
        if (moneroDeleted) {
            showLoadingDialog() // Show loading dialog for deletion
            val apiService = RetrofitClient.getApiService(this)
            val deleteRequestBody = MoneroDeleteWalletRequestBody(
                primaryAddress = properties.getProperty("Monero_value") ?: ""
            )
            val call = apiService.deleteMoneroWallet(deleteRequestBody)
            call.enqueue(object : retrofit2.Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: retrofit2.Response<ApiResponse>) {
                    dismissLoadingDialog()
                    if (response.isSuccessful) {
                        // Remove Monero properties from the file after successful deletion
                        properties.remove("Monero_value")
                        properties.remove("Monero_view_key")
                        properties.remove("Monero_type")
                        savePropertiesToFile()
                        showSuccessModal()
                    } else {
                        showErrorModal("Failed to delete Monero data. Please try again.")
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    dismissLoadingDialog()
                    showErrorModal("Failed to delete Monero. Please check your network connection.")
                }
            })
        } else if (moneroWalletData != null) {
            // Make the API call for Monero save
            showLoadingDialog()
            val apiService = RetrofitClient.getApiService(this)
            val call = apiService.createMoneroWallet(moneroWalletData!!)
            call.enqueue(object : retrofit2.Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: retrofit2.Response<ApiResponse>) {
                    dismissLoadingDialog()
                    if (response.isSuccessful) {
                        properties.setProperty("Monero_view_key", moneroWalletData!!.privateViewKey)
                        properties.setProperty("Monero_value", moneroWalletData!!.newAddress)
                        properties.setProperty("Monero_type", "address")
                        savePropertiesToFile()
                        showSuccessModal()
                    } else {
                        showErrorModal("Failed to save Monero data. Please try again.")
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    dismissLoadingDialog()
                    showErrorModal("Failed to save Monero. Please check your network connection.")
                }
            })
        } else {
            // Save other cryptocurrencies
            savePropertiesToFile()
            showSuccessModal()
        }
    }


    private fun showLoadingDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_loading, null)
        val gifImageView: ImageView = dialogView.findViewById(R.id.loadingGifImageView)
        val gifDrawable = GifDrawable(resources, R.raw.rotating_arc_gradient_thick)
        gifImageView.setImageDrawable(gifDrawable)

        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        builder.setCancelable(false)

        loadingDialog = builder.create()
        loadingDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        loadingDialog?.window?.setDimAmount(0.5f)
        loadingDialog?.show()
    }

    // Function to dismiss the loading dialog
    private fun dismissLoadingDialog() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }
    private fun savePropertiesToFile() {
        val propertiesFile = File(filesDir, "config.properties")
        propertiesFile.bufferedWriter().use { writer ->
            properties.forEach { key, value ->
                writer.write("$key=${value.toString().replace("\\", "")}\n")
            }
        }
    }






    private fun validateInput(
        editText: EditText,
        inputType: String,
        segwitLegacy: String,
        currency: String,
        errorTextView: TextView,
        submitText: TextView
    ) {
        val value = editText.text.toString()

        // Check for blank input but allow submit
        if (value.isBlank()) {
            errorTextView.text = ""
            // Enable submit button if there are no validation errors in other fields
            submitText.isEnabled = allInputsValid()
            submitText.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (submitText.isEnabled) R.color.white else android.R.color.darker_gray
                )
            )
            return
        }

        val isValid = when (currency) {
            "Bitcoin" -> {
                if (inputType == "xpub") BitcoinManager.isValidXpub(value) else BitcoinManager.isValidAddress(value)
            }
            "Dogecoin" -> {
                if (inputType == "xpub") DogecoinManager.isValidXpub(value, this) else DogecoinManager.isValidAddress(value)
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
                val itemView = cryptocurrencyContainer.getChildAt(
                    filteredCryptocurrencies.indexOfFirst { it.name == "Monero" }
                )
                val viewKeyField = itemView.findViewById<EditText>(R.id.view_key_field)
                val viewKey = viewKeyField.text.toString().trim()

                // Validate Monero address and private view key separately
                val isAddressValid = MoneroManager.isValidAddress(value)
                val isViewKeyValid =
                    if (viewKey.isNotEmpty()) MoneroManager.isValidPrivateViewKey(viewKey) else true

                if (!isAddressValid) {
                    errorTextView.text = getString(R.string.invalid_monero_address)
                } else if (!isViewKeyValid) {
                    errorTextView.text = getString(R.string.invalid_Monero_private_view_key)
                }

                // Validation passes if the address is valid and the view key is valid (or not provided)
                isAddressValid && isViewKeyValid
            }
            else -> false
        }

        if (!isValid) {
            errorTextView.text = getString(R.string.invalid_for, inputType, currency)
            submitText.isEnabled = false
            submitText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        } else {
            errorTextView.text = ""
            // Enable submit button if all inputs are valid or blank
            submitText.isEnabled = allInputsValid()
            submitText.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (submitText.isEnabled) R.color.white else android.R.color.darker_gray
                )
            )
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

    private fun showErrorModal(message: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_error, null)
        val dialog = Dialog(this, R.style.CustomDialog)
        dialog.setContentView(dialogView)
        dialog.setCancelable(false)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val errorMessage = dialogView.findViewById<TextView>(R.id.error_message)
        errorMessage.text = message
        dialogView.findViewById<Button>(R.id.btn_ok).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
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