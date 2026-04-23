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
import android.app.Activity
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.zxing.integration.android.IntentIntegrator
import com.google.zxing.integration.android.IntentResult
import com.vermont.possin.model.ApiResponse
import com.vermont.possin.network.MoneroDeleteWalletRequestBody
import com.vermont.possin.network.MoneroWalletRequestBody
import com.vermont.possin.network.RetrofitClient
import com.vermont.possin.gif.GifHandler
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
    private lateinit var exportSettingsBtn: Button
    private lateinit var importSettingsBtn: Button
    private var currentInputField: EditText? = null
    private var loadingDialog: AlertDialog? = null

    companion object {
        const val PICK_BACKUP_FILE = 5001
    }

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

        exportSettingsBtn = findViewById(R.id.exportSettingsBtn)
        importSettingsBtn = findViewById(R.id.importSettingsBtn)

        exportSettingsBtn.setOnClickListener {
            exportCoinSettings()
        }

        importSettingsBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.restore_coins))
                .setMessage("Replace current coin settings?")
                .setPositiveButton("Yes") { _, _ ->
                    val intent = Intent(Intent.ACTION_GET_CONTENT)
                    intent.type = "text/*"
                    startActivityForResult(intent, PICK_BACKUP_FILE)
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }

        // Load cryptocurrencies from JSON
        cryptocurrencies = loadCryptocurrencies(this)
        filteredCryptocurrencies = cryptocurrencies

        // Set up the AutoCompleteTextView adapter
        val cryptoNames = cryptocurrencies.map { it.name }
        cryptocurrencyAdapter = ArrayAdapter(this, R.layout.dropdown_item, cryptoNames)
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
            // clear warnings before saving
            for (i in 0 until cryptocurrencyContainer.childCount) {
                val itemView = cryptocurrencyContainer.getChildAt(i)
                val errorTextView = itemView.findViewById<TextView>(R.id.error_text)
                if (errorTextView.text.toString().contains(getString(R.string.no_checksum_warning))) {
                    errorTextView.text = ""
                }
            }
            saveCryptocurrencyValues()
        }


        // Set up back arrow listener
        backArrow.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }


        // Populate the container with cryptocurrency items
        populateCryptocurrencyContainer()
    }

    private fun exportCoinSettings() {

        val input = EditText(this)
        input.hint = "Enter file name"

        AlertDialog.Builder(this)
            .setTitle("Backup Coins")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->

                var fileName = input.text.toString().trim()

                if (fileName.isEmpty()) {
                    fileName = "LCTerm_Coin_Backup"
                }

                if (!fileName.endsWith(".txt")) {
                    fileName += ".txt"
                }

                saveBackupFile(fileName)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveBackupFile(fileName: String) {

        val configFile = File(filesDir, "config.properties")

        if (!configFile.exists()) {
            Toast.makeText(this, "No config file found", Toast.LENGTH_SHORT).show()
            return
        }

        val content = configFile.readText()

        try {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = contentResolver.insert(
                    MediaStore.Files.getContentUri("external"),
                    values
                )

                uri?.let {
                    contentResolver.openOutputStream(it)?.use { stream ->
                        stream.write(content.toByteArray())
                    }
                }

            } else {

                val downloads = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )

                if (!downloads.exists()) downloads.mkdirs()

                File(downloads, fileName).writeText(content)
            }

            Toast.makeText(this, "Backup saved to Downloads", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Backup failed", Toast.LENGTH_LONG).show()
        }
    }

    private fun importCoinSettings(uri: Uri) {

        try {

            val text = contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?.trim()
                ?: ""

            if (text.isEmpty()) {
                Toast.makeText(this, "Invalid backup file", Toast.LENGTH_LONG).show()
                return
            }

            if (!text.contains("_value=") &&
                !text.contains("_type=") &&
                !text.contains("Monero_value=")
            ) {
                Toast.makeText(this, "Invalid backup file", Toast.LENGTH_LONG).show()
                return
            }

            val configFile = File(filesDir, "config.properties")
            configFile.writeText(text)

            properties = ConfigProperties.loadProperties(this)

            populateCryptocurrencyContainer()

            Toast.makeText(
                this,
                "Coin settings restored!",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Invalid backup file",
                Toast.LENGTH_LONG
            ).show()
        }
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

            chainTextView.text = crypto.chain

            if (crypto.name == "Lightning") {

                chainTextView.visibility = View.VISIBLE
                chainTextView.maxLines = 3
                chainTextView.isSingleLine = false

                chainTextView.text =
                    "${crypto.chain}\nMaximum is 100k satoshi"

                chainTextView.textSize = 11f
            }

            val logoId = resources.getIdentifier(crypto.logo, "drawable", packageName)
            logoImageView.setImageResource(logoId)
            nameTextView.text = crypto.name

            if (crypto.name == "Lightning") {

                // remove BTC LIGHTNING text
                shortnameTextView.text = ""

                // replace chain text only
                chainTextView.visibility = View.VISIBLE
                chainTextView.maxLines = 2
                chainTextView.isSingleLine = false
                chainTextView.text = "Maximum is 100k satoshi"
                chainTextView.textSize = 11f

            } else {

                shortnameTextView.text = crypto.shortname
                chainTextView.text = crypto.chain
            }

            // Set up type spinner
            val typeAdapter: ArrayAdapter<CharSequence> = if (
                crypto.name == "Monero" ||
                crypto.name == "Ethereum" ||
                crypto.name == "Tether" ||
                crypto.name == "Tether (Ethereum)" ||
                crypto.name == "USD Coin (Ethereum)" ||
                crypto.name == "DAI (Ethereum)" ||
                crypto.name == "Solana" ||
                crypto.name == "Nano" ||
                crypto.name == "Lightning"
            ) {
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
            val savedType =
                properties.getProperty("${crypto.shortname}_type")
                    ?: properties.getProperty("${crypto.name}_type")

            val savedSegwitLegacy =
                properties.getProperty("${crypto.shortname}_segwit_legacy")
                    ?: properties.getProperty("${crypto.name}_segwit_legacy")

            val savedValue =
                properties.getProperty("${crypto.shortname}_value")
                    ?: properties.getProperty("${crypto.name}_value")

            if (savedValue != null && properties.getProperty("${crypto.shortname}_value") == null) {
                properties.setProperty("${crypto.shortname}_value", savedValue)
            }

            if (savedType != null && properties.getProperty("${crypto.shortname}_type") == null) {
                properties.setProperty("${crypto.shortname}_type", savedType)
            }


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

        if (requestCode == PICK_BACKUP_FILE &&
            resultCode == Activity.RESULT_OK) {

            val uri = data?.data
            if (uri != null) {
                importCoinSettings(uri)
            }
            return
        }

        val result: IntentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)

        if (result.contents != null) {
            var scannedContent = result.contents

            // Check if the scanned result is in the Monero wallet format
            if (scannedContent.startsWith("monero_wallet:")) {
                // Handle Monero wallet-specific logic
                val uriWithoutPrefix = scannedContent.removePrefix("monero_wallet:")
                val parts = uriWithoutPrefix.split("?")
                if (parts.size == 2) {
                    val walletAddress = parts[0]
                    val queryParams = parts[1]

                    val queryMap = queryParams.split("&").associate {
                        val (key, value) = it.split("=")
                        key to value
                    }

                    val viewKey = queryMap["view_key"]

                    currentInputField?.setText(walletAddress)
                    currentInputField = null

                    if (viewKey != null) {
                        val itemView = cryptocurrencyContainer.getChildAt(
                            filteredCryptocurrencies.indexOfFirst { it.name == "Monero" }
                        )
                        val viewKeyField = itemView.findViewById<EditText>(R.id.view_key_field)
                        viewKeyField?.setText(viewKey)
                    }
                }
            } else {
                // Handle other scanned results (non-Monero)
                var scannedAddress = scannedContent

                if (scannedAddress.startsWith("bitcoincash:", ignoreCase = true)) {
                    // Check if it's a valid xpub or address for Bitcoin Cash
                    val addressWithoutPrefix = scannedAddress.removePrefix("bitcoincash:").trim()

                    if (BitcoinCashManager.isValidXpub(addressWithoutPrefix)) {
                        // If valid xpub, remove the prefix
                        scannedAddress = addressWithoutPrefix
                    } else if (BitcoinCashManager.isValidAddress(addressWithoutPrefix)) {
                        // If valid address, retain the prefix
                        scannedAddress = scannedAddress.trim()
                    }
                } else if (scannedAddress.contains(":")) {
                    // For other cryptocurrencies, remove the prefix
                    val parts = scannedAddress.split(":")
                    if (parts.size > 1) {
                        scannedAddress = parts[1].trim()
                    }
                }

                // Set the result in the current input field
                currentInputField?.setText(scannedAddress)
                currentInputField = null
            }
        }
    }


    private fun saveCryptocurrencyValues() {
        properties.remove("default_key")

        var moneroWalletData: MoneroWalletRequestBody? = null
        var moneroDeleted = false

        for (i in 0 until cryptocurrencyContainer.childCount) {
            val itemView = cryptocurrencyContainer.getChildAt(i)

            val nameTextView = itemView.findViewById<TextView>(R.id.crypto_name)
            val shortnameTextView = itemView.findViewById<TextView>(R.id.crypto_shortname)
            val chainTextView = itemView.findViewById<TextView>(R.id.crypto_chain)
            val typeSpinner = itemView.findViewById<Spinner>(R.id.type_spinner)
            val segwitLegacySpinner = itemView.findViewById<Spinner>(R.id.segwit_legacy_spinner)
            val inputField = itemView.findViewById<EditText>(R.id.input_field)

            val cryptoName = nameTextView.text.toString()
            val shortname = shortnameTextView.text.toString() // ✅ KEY FIX
            val chain = chainTextView.text.toString()
            val inputType = typeSpinner.selectedItem.toString()
            val segwitLegacy = segwitLegacySpinner.selectedItem.toString()
            val value = inputField.text.toString().trim()

            Log.d("SAVE_DEBUG", "Processing $shortname | chain=$chain | value=$value")

            // =====================
            // MONERO
            // =====================
            if (cryptoName == "Monero") {
                val viewKeyField = itemView.findViewById<EditText>(R.id.view_key_field)
                val viewKey = viewKeyField.text.toString().trim()

                val savedAddress = properties.getProperty("Monero_value")
                val savedViewKey = properties.getProperty("Monero_view_key")

                if (value.isEmpty() && savedAddress != null) {
                    properties.remove("Monero_value")
                    properties.remove("Monero_view_key")
                    properties.remove("Monero_type")
                    moneroDeleted = true
                } else if (value.isNotEmpty() && (savedAddress != value || savedViewKey != viewKey)) {
                    moneroWalletData = MoneroWalletRequestBody(
                        currentAddress = savedAddress,
                        newAddress = value,
                        privateViewKey = viewKey
                    )
                }

                // =====================
                // ETH + ERC20 (FIXED)
                // =====================
            } else if (chain == "Ethereum") {

                if (value.isNotEmpty()) {
                    val (isValidEth, normalized, _) = EthereumManager.validateAddress(value)

                    if (!isValidEth) {
                        Log.e("SAVE_DEBUG", "Invalid ETH address: $value")
                        continue
                    }

                    val finalValue = normalized ?: value

                    properties.setProperty("${shortname}_type", inputType)
                    properties.setProperty("${shortname}_value", finalValue)

                    Log.d("SAVE_DEBUG", "Saved ETH/ERC20: $shortname = $finalValue")

                } else {
                    properties.remove("${shortname}_type")
                    properties.remove("${shortname}_value")
                }

                // =====================
                // NANO
                // =====================
            } else if (shortname == "XNO") {

                if (value.isNotEmpty()) {
                    if (!NanoManager.isValidAddress(value)) {
                        Log.e("Nano", "Invalid Nano address: $value")
                        continue
                    }

                    properties.setProperty("${shortname}_type", "address")
                    properties.setProperty("${shortname}_value", value)

                } else {
                    properties.remove("${shortname}_type")
                    properties.remove("${shortname}_value")
                }

                // =====================
                // LIGHTNING
                // =====================
            } else if (shortname == "LIGHTNING") {

                if (value.isNotEmpty()) {
                    if (!LightningManager.isValidLightningAddress(value)) {
                        Log.e("Lightning", "Invalid Lightning address: $value")
                        continue
                    }

                    properties.setProperty("${shortname}_type", "address")
                    properties.setProperty("${shortname}_value", value)

                } else {
                    properties.remove("${shortname}_type")
                    properties.remove("${shortname}_value")
                }

                // =====================
                // OTHER COINS
                // =====================
            } else {

                val savedValue = properties.getProperty("${shortname}_value")

                if (value.isEmpty() && savedValue != null) {
                    properties.remove("${shortname}_type")
                    properties.remove("${shortname}_segwit_legacy")
                    properties.remove("${shortname}_value")

                } else if (value.isNotEmpty()) {
                    properties.setProperty("${shortname}_type", inputType)

                    if (inputType == "xpub" && (shortname == "BTC" || shortname == "LTC")) {
                        properties.setProperty("${shortname}_segwit_legacy", segwitLegacy)
                    } else if (inputType == "address" && (shortname == "BTC" || shortname == "LTC")) {
                        properties.remove("${shortname}_segwit_legacy")
                    }

                    properties.setProperty("${shortname}_value", value)
                }
            }
        }

        // =====================
        // MONERO API HANDLING
        // =====================
        if (moneroDeleted) {
            showLoadingDialog()
            val apiService = RetrofitClient.getApiService(this)
            val loadProperties = ConfigProperties.loadProperties(this)

            val deleteRequestBody = MoneroDeleteWalletRequestBody(
                primaryAddress = loadProperties.getProperty("Monero_value") ?: ""
            )

            val call = apiService.deleteMoneroWallet(deleteRequestBody)
            call.enqueue(object : retrofit2.Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: retrofit2.Response<ApiResponse>) {
                    dismissLoadingDialog()
                    if (response.isSuccessful) {
                        properties.remove("Monero_value")
                        properties.remove("Monero_view_key")
                        properties.remove("Monero_type")
                        savePropertiesToFile()
                        showSuccessModal()
                    } else {
                        showErrorModal("Failed to delete Monero data.")
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    dismissLoadingDialog()
                    showErrorModal("Failed to delete Monero.")
                }
            })

        } else if (moneroWalletData != null) {

            showLoadingDialog()
            val apiService = RetrofitClient.getApiService(this)

            val call = apiService.createMoneroWallet(moneroWalletData)
            call.enqueue(object : retrofit2.Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: retrofit2.Response<ApiResponse>) {
                    dismissLoadingDialog()

                    if (response.isSuccessful) {
                        properties.setProperty("Monero_view_key", moneroWalletData.privateViewKey)
                        properties.setProperty("Monero_value", moneroWalletData.newAddress)
                        properties.setProperty("Monero_type", "address")
                        savePropertiesToFile()
                        showSuccessModal()
                    } else {
                        showErrorModal("Failed to save Monero.")
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    dismissLoadingDialog()
                    showErrorModal("Failed to save Monero.")
                }
            })

        } else {
            savePropertiesToFile()
            showSuccessModal()
        }
    }


    private fun showLoadingDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_loading, null)
        val gifImageView: ImageView = dialogView.findViewById(R.id.loadingGifImageView)
        GifHandler.loadGif(gifImageView, R.raw.rotating_arc_gradient_thick)

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
                writer.write("$key=$value\n")
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

        if (value.isBlank()) {
            errorTextView.text = ""
            submitText.isEnabled = allInputsValid()
            submitText.setTextColor(
                ContextCompat.getColor(this, if (submitText.isEnabled) R.color.white else android.R.color.darker_gray)
            )
            return
        }

        var isValid = false
        var warningShown = false

        when (currency) {
            "Bitcoin" -> {
                isValid = if (inputType == "xpub") BitcoinManager.isValidXpub(value) else BitcoinManager.isValidAddress(value)
            }
            "Lightning" -> {
                isValid = LightningManager.isValidLightningAddress(value)
            }
            "Dogecoin" -> {
                isValid = if (inputType == "xpub") DogecoinManager.isValidXpub(value, this) else DogecoinManager.isValidAddress(value)
            }
            "Litecoin" -> {
                isValid = if (inputType == "xpub") LitecoinManager.isValidXpub(value, this) else LitecoinManager.isValidAddress(value)
            }
            "Ethereum", "Tether (Ethereum)", "USD Coin (Ethereum)", "DAI (Ethereum)" -> {
                if (inputType == "xpub") {
                    isValid = EthereumManager.isValidXpub(value)
                } else {
                    val (isValidEth, normalized, warning) = EthereumManager.validateAddress(value)
                    isValid = isValidEth

                    if (!isValidEth) {
                        errorTextView.text = getString(R.string.invalid_for, inputType, currency)
                        submitText.isEnabled = false
                        submitText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
                        return
                    }

                    if (warning != null) {
                        // Show checksum warning
                        errorTextView.text = getString(R.string.no_checksum_warning)
                        warningShown = true
                        submitText.isEnabled = true
                        submitText.setTextColor(ContextCompat.getColor(this, R.color.white))
                    } else {
                        errorTextView.text = ""
                    }
                }
            }

            "Tether" -> {
                isValid = if (inputType == "xpub") TronManager.isValidXpub(value) else TronManager.isValidAddress(value)
            }
            "Dash" -> {
                isValid = if (inputType == "xpub") DashManager.isValidXpub(value) else DashManager.isValidAddress(value)
            }
            "Bitcoincash" -> {
                isValid = if (inputType == "xpub") BitcoinCashManager.isValidXpub(value) else BitcoinCashManager.isValidAddress(value)
            }
            "Zcash" -> {
                isValid = if (inputType == "xpub") ZcashManager.isValidXpub(value)
                else ZcashManager.isValidAddress(value)
            }
            "Monero" -> {
                val itemView = cryptocurrencyContainer.getChildAt(
                    filteredCryptocurrencies.indexOfFirst { it.name == "Monero" }
                )
                val viewKeyField = itemView.findViewById<EditText>(R.id.view_key_field)
                val viewKey = viewKeyField.text.toString().trim()

                val isAddressValid = MoneroManager.isValidAddress(value)
                val isViewKeyValid = if (viewKey.isNotEmpty()) MoneroManager.isValidPrivateViewKey(viewKey) else true

                if (!isAddressValid) {
                    errorTextView.text = getString(R.string.invalid_monero_address)
                } else if (!isViewKeyValid) {
                    errorTextView.text = getString(R.string.invalid_Monero_private_view_key)
                }
                isValid = isAddressValid && isViewKeyValid
            }
            "Solana" -> {
                isValid = if (inputType == "xpub") SolanaManager.isValidXpub(value) else SolanaManager.isValidAddress(value)
            }
            "Nano" -> {
                isValid = NanoManager.isValidAddress(value)
            }
            else -> isValid = false
        }

        // Final UI update
        if (!isValid && !warningShown) {
            errorTextView.text = getString(R.string.invalid_for, inputType, currency)
            submitText.isEnabled = false
            submitText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        } else {
            if (!warningShown) errorTextView.text = "" // clear only if no warning
            submitText.isEnabled = allInputsValid()
            submitText.setTextColor(
                ContextCompat.getColor(this, if (submitText.isEnabled) R.color.white else android.R.color.darker_gray)
            )
        }
    }

    private fun allInputsValid(): Boolean {
        for (i in 0 until cryptocurrencyContainer.childCount) {
            val itemView = cryptocurrencyContainer.getChildAt(i)
            val errorTextView = itemView.findViewById<TextView>(R.id.error_text)
            if (errorTextView.text.isNotEmpty() &&
                errorTextView.text.toString() != getString(R.string.no_checksum_warning)) {
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