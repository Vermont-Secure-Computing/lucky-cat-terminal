package com.example.possin

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_xpub_address)

        window.statusBarColor = ContextCompat.getColor(this, R.color.tapeRed)

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
            val inputField = itemView.findViewById<EditText>(R.id.input_field)
            val errorTextView = itemView.findViewById<TextView>(R.id.error_text)

            val logoId = resources.getIdentifier(crypto.logo, "drawable", packageName)
            logoImageView.setImageResource(logoId)
            nameTextView.text = crypto.name
            shortnameTextView.text = crypto.shortname
            chainTextView.text = crypto.chain

            // Set up type spinner
            val adapter = ArrayAdapter.createFromResource(this, R.array.type_array, android.R.layout.simple_spinner_item)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            typeSpinner.adapter = adapter

            // Load saved values if they exist
            val savedType = properties.getProperty("${crypto.name}_type")
            val savedValue = properties.getProperty("${crypto.name}_value")

            if (savedType != null) {
                val spinnerPosition = adapter.getPosition(savedType)
                typeSpinner.setSelection(spinnerPosition)
            }

            if (savedValue != null) {
                inputField.setText(savedValue)
            }

            inputField.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    validateInput(inputField, typeSpinner.selectedItem.toString(), crypto.name, errorTextView, submitText)
                }
            })

            typeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    val selectedType = parent.getItemAtPosition(position).toString()
                    inputField.hint = "Enter $selectedType ${crypto.name} name"
                    validateInput(inputField, selectedType, crypto.name, errorTextView, submitText)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

            cryptocurrencyContainer.addView(itemView)
        }
    }

    private fun saveCryptocurrencyValues() {
        properties.remove("default_key")
        for (i in 0 until cryptocurrencyContainer.childCount) {
            val itemView = cryptocurrencyContainer.getChildAt(i)
            val nameTextView = itemView.findViewById<TextView>(R.id.crypto_name)
            val typeSpinner = itemView.findViewById<Spinner>(R.id.type_spinner)
            val inputField = itemView.findViewById<EditText>(R.id.input_field)

            val cryptoName = nameTextView.text.toString()
            val inputType = typeSpinner.selectedItem.toString()
            val value = inputField.text.toString()

            // Only save if value is not empty
            if (value.isNotEmpty()) {
                properties.setProperty("${cryptoName}_type", inputType)
                properties.setProperty("${cryptoName}_value", value)
            }
        }

        ConfigProperties.saveProperties(this, properties)
        showSuccessModal()

    }

    private fun validateInput(editText: EditText, inputType: String, currency: String, errorTextView: TextView, submitText: TextView) {
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
                if (inputType == "xpub") LitecoinManager.isValidXpub(value) else LitecoinManager.isValidAddress(value)
            }
            "Ethereum" -> {
                if (inputType == "xpub") EthereumManager.isValidXpub(value) else EthereumManager.isValidAddress(value)
            }
            "Tether" -> {
                if (inputType == "xpub") TronManager.isValidXpub(value) else TronManager.isValidAddress(value)
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
                submitText.setTextColor(ContextCompat.getColor(this, android.R.color.white))
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
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_success)
        dialog.setCancelable(false)

        val okButton: Button = dialog.findViewById(R.id.btn_ok)
        okButton.setOnClickListener {
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
