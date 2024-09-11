package com.example.possin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen


class MainActivity : AppCompatActivity() {


//    private lateinit var recyclerView: RecyclerView
//    private lateinit var adapter: CryptocurrencyAdapter
//    private lateinit var cryptocurrencies: List<Cryptocurrency>
//    private lateinit var filteredCryptocurrencies: MutableList<Cryptocurrency>
//    private lateinit var selectedCryptocurrencies: MutableList<Cryptocurrency>
//    private lateinit var selectedCryptocurrenciesContainer: LinearLayout
//    private lateinit var propertiesFile: File
//    private lateinit var merchantPropertiesFile: File


    override fun onCreate(savedInstanceState: Bundle?) {
        // Install the splash screen
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Directly start HomeActivity without delay to see what's happening
        startActivity(Intent(this, HomeActivity::class.java))
        finish() // Close MainActivity



//        // Check if .properties file is empty or doesn't exist
//        propertiesFile = File(filesDir, "config.properties")
//        merchantPropertiesFile = File(filesDir, "merchant.properties")
//
//        // Check if cryptocurrencies were passed in the intent
//        val addedCryptocurrencies: ArrayList<Cryptocurrency>? = intent.getParcelableArrayListExtra("added_cryptocurrencies")
//        if (addedCryptocurrencies != null) {
//            // Continue with onboarding if cryptocurrencies were passed
//            setContentView(R.layout.activity_onboarding)
//            selectedCryptocurrencies = addedCryptocurrencies.toMutableList()
//            setupOnboardingView()
//        } else {
//            // Otherwise, check the properties file
//            val existingCryptocurrencies = getAddedCryptocurrencies()
//            if (existingCryptocurrencies.isEmpty()) {
//                setContentView(R.layout.activity_onboarding)
//                setupOnboardingView()
//            } else {
//                startActivity(Intent(this, HomeActivity::class.java))
//                finish()
//            }
//        }
    }

//    private fun getAddedCryptocurrencies(): List<Cryptocurrency> {
//        val addedCryptocurrencies = mutableListOf<Cryptocurrency>()
//
//        // Create the properties file if it doesn't exist
//        if (!propertiesFile.exists()) {
//            propertiesFile.createNewFile()
//            return addedCryptocurrencies
//        }
//
//        val properties = Properties().apply {
//            propertiesFile.inputStream().use { load(it) }
//        }
//
//        val cryptocurrencyNames = properties.getProperty("cryptocurrencies", "").split(",").filter { it.isNotBlank() }
//        val allCryptocurrencies = loadCryptocurrencies()
//
//        for (name in cryptocurrencyNames) {
//            val cryptocurrency = allCryptocurrencies.find { it.name == name }
//            if (cryptocurrency != null) {
//                addedCryptocurrencies.add(cryptocurrency)
//            }
//        }
//
//        Log.d("CryptoNames", cryptocurrencyNames.toString())
//        Log.d("CryptoAdded", addedCryptocurrencies.toString())
//
//        return addedCryptocurrencies
//    }
//
//
//    private fun setupOnboardingView() {
//        cryptocurrencies = loadCryptocurrencies()
//        filteredCryptocurrencies = cryptocurrencies.toMutableList()
//        selectedCryptocurrencies = getAddedCryptocurrencies().toMutableList()
//
//        recyclerView = findViewById(R.id.recycler_view)
//        recyclerView.layoutManager = GridLayoutManager(this, 2)
//        adapter = CryptocurrencyAdapter(this, filteredCryptocurrencies) { cryptocurrency: Cryptocurrency ->
//            if (selectedCryptocurrencies.contains(cryptocurrency)) {
//                selectedCryptocurrencies.remove(cryptocurrency)
//            } else {
//                selectedCryptocurrencies.add(cryptocurrency)
//            }
//            updateSelectedCryptocurrenciesView()
//        }
//        recyclerView.adapter = adapter
//
//        selectedCryptocurrenciesContainer = findViewById(R.id.selected_cryptocurrencies_container)
//        updateSelectedCryptocurrenciesView()
//
//        val searchBar = findViewById<AutoCompleteTextView>(R.id.search_bar)
//        val cryptocurrencyNames = cryptocurrencies.map { it.name }
//        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, cryptocurrencyNames)
//        searchBar.setAdapter(adapter)
//
//        searchBar.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                filterCryptocurrencies(s.toString())
//            }
//            override fun afterTextChanged(s: Editable?) {}
//        })
//
//        val submitText = findViewById<TextView>(R.id.submit_text)
//        submitText.setOnClickListener {
//            if (selectedCryptocurrencies.isEmpty()) {
//                Toast.makeText(this, "Please select at least one cryptocurrency.", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            val properties = Properties().apply {
//                propertiesFile.inputStream().use { load(it) }
//            }
//
//            val existingCryptocurrencies = selectedCryptocurrencies.map { it.name }.toMutableSet()
//
//            properties.setProperty("cryptocurrencies", existingCryptocurrencies.joinToString(","))
//            propertiesFile.outputStream().use {
//                properties.store(it, null)
//            }
//
//            setContentView(R.layout.activity_xpub_input)
//            setupXpubInputView()
//        }
//    }
//
//
//    private fun updateSelectedCryptocurrenciesView() {
//        selectedCryptocurrenciesContainer.removeAllViews()
//        selectedCryptocurrencies.forEach { cryptocurrency ->
//            val logoImageView = ImageView(this).apply {
//                val resourceId = resources.getIdentifier(cryptocurrency.logo, "drawable", packageName)
//                setImageResource(resourceId)
//                layoutParams = LinearLayout.LayoutParams(100, 100).apply {
//                    setMargins(8, 8, 8, 8)
//                }
//            }
//            selectedCryptocurrenciesContainer.addView(logoImageView)
//        }
//    }
//
//    private fun filterCryptocurrencies(query: String) {
//        filteredCryptocurrencies.clear()
//        if (query.isEmpty()) {
//            filteredCryptocurrencies.addAll(cryptocurrencies)
//        } else {
//            cryptocurrencies.forEach {
//                if (it.name.contains(query, true) || it.shortname.contains(query, true) || it.chain.contains(query, true)) {
//                    filteredCryptocurrencies.add(it)
//                }
//            }
//        }
//        adapter.notifyDataSetChanged()
//    }
//
//
//
//    private fun setupXpubInputView() {
//        val scrollView = findViewById<ScrollView>(R.id.scroll_view)
//        val xpubInputContainer = findViewById<LinearLayout>(R.id.xpub_input_container)
//        val submitText = findViewById<TextView>(R.id.submit_text)
//        val backArrow = findViewById<ImageView>(R.id.back_arrow)
//
//        submitText.isEnabled = false
//        submitText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
//
//        backArrow.setOnClickListener {
//            val intent = Intent(this, MainActivity::class.java)
//            intent.putParcelableArrayListExtra("added_cryptocurrencies", ArrayList(selectedCryptocurrencies))
//            startActivity(intent)
//            finish()
//        }
//
//        val rootView = findViewById<View>(android.R.id.content)
//        rootView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
//            override fun onGlobalLayout() {
//                val rect = Rect()
//                rootView.getWindowVisibleDisplayFrame(rect)
//                val screenHeight = rootView.height
//                val keypadHeight = screenHeight - rect.bottom
//
//                if (keypadHeight > screenHeight * 0.15) { // Keyboard is probably visible
//                    Log.d("KEYBOARD", "IS VISIBLE")
//                    val focusedView = currentFocus
//                    if (focusedView != null) {
//                        Log.d("INSIDEFOCUS", focusedView.toString())
//                        scrollView.postDelayed({
//                            val location = IntArray(2)
//                            focusedView.getLocationInWindow(location)
//                            val scrollViewLocation = IntArray(2)
//                            scrollView.getLocationInWindow(scrollViewLocation)
//
//                            val scrollToPosition = location[1] - scrollViewLocation[1] - keypadHeight + focusedView.height
//                            Log.d("SCROLL_TO", "Scrolling to position: $scrollToPosition (focusedView.bottom: ${focusedView.bottom}, location[1]: ${location[1]}, scrollViewLocation[1]: ${scrollViewLocation[1]}, keypadHeight: $keypadHeight)")
//                            scrollView.smoothScrollTo(0, scrollToPosition)
//                        }, 200) // Adjust the delay as needed
//                    }
//                }
//            }
//        })
//
//
//        selectedCryptocurrencies.forEach { currency ->
//            val layout = LinearLayout(this).apply {
//                orientation = LinearLayout.VERTICAL
//            }
//
//            val spinner = Spinner(this).apply {
//                adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, listOf("XPUB", "Address"))
//            }
//            layout.addView(spinner)
//
//            val editText = EditText(this).apply {
//                hint = "Enter XPUB or Address for ${currency.name}"
//                background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bordered_edit_text)
//                maxLines = 1
//                isSingleLine = true
//                setHorizontallyScrolling(true)
//                layoutParams = LinearLayout.LayoutParams(
//                    LinearLayout.LayoutParams.MATCH_PARENT,
//                    LinearLayout.LayoutParams.WRAP_CONTENT
//                ).apply {
//                    setMargins(0, 8, 0, 8)
//                }
//
////                setOnFocusChangeListener { _, hasFocus ->
////                    if (hasFocus) {
////                        Log.d("HASFOCUS", hasFocus.toString())
////                        scrollView.postDelayed({
////                            val location = IntArray(2)
////                            this.getLocationInWindow(location)
////                            val scrollViewLocation = IntArray(2)
////                            scrollView.getLocationInWindow(scrollViewLocation)
////
////                            // Calculate the keyboard height again
////                            val rect = Rect()
////                            rootView.getWindowVisibleDisplayFrame(rect)
////                            val screenHeight = rootView.height
////                            val keypadHeight = screenHeight - rect.bottom
////
////                            val scrollToPosition = location[1] - scrollViewLocation[1] - keypadHeight + this.height
////                            Log.d("SCROLL_TO", "Scrolling to position: $scrollToPosition (this.bottom: ${this.bottom}, location[1]: ${location[1]}, scrollViewLocation[1]: ${scrollViewLocation[1]}, keypadHeight: $keypadHeight)")
////                            scrollView.smoothScrollTo(0, scrollToPosition)
////                        }, 200) // Adjust the delay as needed
////                    }
////                }
//            }
//            layout.addView(editText)
//
//            val errorTextView = TextView(this).apply {
//                setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_dark))
//            }
//            layout.addView(errorTextView)
//
//            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
//                    val selectedItem = parent.getItemAtPosition(position).toString()
//                    editText.hint = "Enter $selectedItem for ${currency.name}"
//                    validateInput(editText, selectedItem, currency.name, errorTextView, submitText)
//                }
//
//                override fun onNothingSelected(parent: AdapterView<*>) {
//                    // Do nothing
//                }
//            }
//
//            editText.addTextChangedListener(object : TextWatcher {
//                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
//                    // Do nothing
//                }
//
//                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                    val selectedItem = spinner.selectedItem.toString()
//                    validateInput(editText, selectedItem, currency.name, errorTextView, submitText)
//                }
//
//                override fun afterTextChanged(s: Editable?) {
//                    // Do nothing
//                }
//            })
//
//            xpubInputContainer.addView(layout)
//        }
//
//        submitText.setOnClickListener {
//            if (!submitText.isEnabled) return@setOnClickListener
//
//            val properties = Properties().apply {
//                propertiesFile.inputStream().use { load(it) }
//            }
//            val existingCryptocurrencies = properties.getProperty("cryptocurrencies", "").split(",").filter { it.isNotBlank() }.toMutableSet()
//
//            var allXpubsOrAddressesEntered = true
//
//            selectedCryptocurrencies.forEachIndexed { index, currency ->
//                val layout = xpubInputContainer.getChildAt(index) as LinearLayout
//                val spinner = layout.getChildAt(0) as Spinner
//                val editText = layout.getChildAt(1) as EditText
//
//                val inputType = spinner.selectedItem.toString()
//                val value = editText.text.toString()
//
//                if (value.isBlank()) {
//                    allXpubsOrAddressesEntered = false
//                }
//
//                properties.setProperty("${currency.name}_type", inputType)
//                properties.setProperty("${currency.name}_value", value)
//                existingCryptocurrencies.add(currency.name)
//            }
//
//            if (!allXpubsOrAddressesEntered) {
//                Toast.makeText(this, "Please enter XPUB or Address for all selected cryptocurrencies.", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            properties.setProperty("cryptocurrencies", existingCryptocurrencies.joinToString(","))
//            propertiesFile.outputStream().use {
//                properties.store(it, null)
//            }
//
//            setContentView(R.layout.activity_merchant_details)
//            setupMerchantDetailsView()
//        }
//
//        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
//            override fun handleOnBackPressed() {
//                val intent = Intent(this@MainActivity, MainActivity::class.java)
//                intent.putParcelableArrayListExtra("added_cryptocurrencies", ArrayList(selectedCryptocurrencies))
//                startActivity(intent)
//                finish()
//            }
//        })
//    }
//
//    private fun validateInput(editText: EditText, inputType: String, currency: String, errorTextView: TextView, submitText: TextView) {
//        val value = editText.text.toString()
//
//        if (value.isBlank()) {
//            errorTextView.text = ""
//            submitText.isEnabled = false
//            submitText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
//            return
//        }
//
//        val isValid = when (currency) {
//            "Bitcoin" -> {
//                if (inputType == "XPUB") BitcoinManager.isValidXpub(value) else BitcoinManager.isValidAddress(value)
//            }
//            "Dogecoin" -> {
//                if (inputType == "XPUB") DogecoinManager.isValidXpub(value) else DogecoinManager.isValidAddress(value)
//            }
//            "Litecoin" -> {
//                if (inputType == "XPUB") LitecoinManager.isValidXpub(value) else LitecoinManager.isValidAddress(value)
//            }
//            "Ethereum" -> {
//                if (inputType == "XPUB") EthereumManager.isValidXpub(value) else EthereumManager.isValidAddress(value)
//            }
//            "Tether" -> {
//                if (inputType == "XPUB") TronManager.isValidXpub(value) else TronManager.isValidAddress(value)
//            }
//            else -> false
//        }
//
//        if (!isValid) {
//            errorTextView.text = "Invalid $inputType for $currency"
//            submitText.isEnabled = false
//            submitText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
//        } else {
//            errorTextView.text = ""
//            submitText.isEnabled = allInputsValid()
//            if (submitText.isEnabled) {
//                submitText.setTextColor(ContextCompat.getColor(this, android.R.color.white))
//            } else {
//                submitText.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
//            }
//        }
//    }
//
//    private fun allInputsValid(): Boolean {
//        val xpubInputContainer = findViewById<LinearLayout>(R.id.xpub_input_container)
//        var allValid = true
//        for (i in 0 until xpubInputContainer.childCount) {
//            val layout = xpubInputContainer.getChildAt(i) as LinearLayout
//            val errorTextView = layout.getChildAt(2) as TextView
//            if (errorTextView.text.isNotEmpty()) {
//                allValid = false
//                break
//            }
//        }
//        return allValid
//    }
//
//    private fun loadCryptocurrencies(): List<Cryptocurrency> {
//        val json: String?
//        val cryptocurrencies = mutableListOf<Cryptocurrency>()
//        try {
//            val inputStream: InputStream = assets.open("cryptocurrencies.json")
//            json = inputStream.bufferedReader().use { it.readText() }
//            val jsonObj = JSONObject(json)
//            val jsonArray = jsonObj.getJSONArray("cryptocurrencies")
//
//            for (i in 0 until jsonArray.length()) {
//                val item = jsonArray.getJSONObject(i)
//                val name = item.getString("name")
//                val shortname = item.getString("shortname")
//                val chain = item.getString("chain")
//                val logo = item.getString("logo")
//                cryptocurrencies.add(Cryptocurrency(name, shortname, chain, logo))
//            }
//        } catch (ex: Exception) {
//            ex.printStackTrace()
//        }
//        return cryptocurrencies
//    }
//
//    private fun setupMerchantDetailsView() {
//        val editTextBusinessName = findViewById<EditText>(R.id.edittext_business_name)
//        val editTextAddress = findViewById<EditText>(R.id.edittext_address)
//        val editTextCity = findViewById<EditText>(R.id.edittext_city)
//        val editTextState = findViewById<EditText>(R.id.edittext_state)
//        val editTextZipCode = findViewById<EditText>(R.id.edittext_zip_code)
//        val editTextCountry = findViewById<EditText>(R.id.edittext_country)
//        val editTextPhone = findViewById<EditText>(R.id.edittext_merchant_phone)
//        val editTextEmail = findViewById<EditText>(R.id.edittext_merchant_email)
//        val submitText = findViewById<TextView>(R.id.submit_text)
//
//        submitText.setOnClickListener {
//            val businessName = editTextBusinessName.text.toString().trim()
//            val address = editTextAddress.text.toString().trim()
//            val city = editTextCity.text.toString().trim()
//            val state = editTextState.text.toString().trim()
//            val zipCode = editTextZipCode.text.toString().trim()
//            val country = editTextCountry.text.toString().trim()
//            val phone = editTextPhone.text.toString().trim()
//            val email = editTextEmail.text.toString().trim()
//
//            // Check for required fields
//            if (businessName.isEmpty()) {
//                Toast.makeText(this, "Please enter your business name.", Toast.LENGTH_SHORT).show()
//                return@setOnClickListener
//            }
//
//            // Save merchant details to a separate properties file
//            val merchantProperties = Properties().apply {
//                setProperty("business_name", businessName)
//                if (address.isNotEmpty()) setProperty("address", address)
//                if (city.isNotEmpty()) setProperty("city", city)
//                if (state.isNotEmpty()) setProperty("state", state)
//                if (zipCode.isNotEmpty()) setProperty("zip_code", zipCode)
//                if (country.isNotEmpty()) setProperty("country", country)
//                if (phone.isNotEmpty()) setProperty("phone", phone)
//                if (email.isNotEmpty()) setProperty("email", email)
//            }
//            merchantPropertiesFile.outputStream().use {
//                merchantProperties.store(it, null)
//            }
//
//            // Proceed to home view
//            startActivity(Intent(this, HomeActivity::class.java))
//            finish()
//        }
//    }
//
//
//
//    private fun setupMainView() {
//        val rootView = findViewById<View>(R.id.gridLayout)
//        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            view.updatePadding(bottom = systemBars.bottom)
//            insets
//        }
//    }
}
