package com.vermont.possin

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import com.vermont.possin.network.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.InputStream

class POSView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var itemPriceInput: TextView
    private lateinit var currencySpinner: Spinner
    private var currentInput = ""
    private var currentCurrencySymbol = "$"
    private var currentCurrencyCode = ""
    private lateinit var apiService: ApiService
    private lateinit var backArrow: ImageView
    private lateinit var buttons: List<Button>
    private var errorTextView: TextView? = null
    private var refreshButton: Button? = null
    private lateinit var addNoteCheckbox: CheckBox

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job()) // Define a coroutine scope

    init {
        LayoutInflater.from(context).inflate(R.layout.pos_view, this, true)
        setupView()
        loadCurrencies()
        initializeApiService()
        checkServerStatus()
    }

    private fun initializeApiService() {
        // Initialize Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl("https://dogpay.mom/")
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)
    }

    private fun setupView() {
        itemPriceInput = findViewById(R.id.itemPriceInput)
        currencySpinner = findViewById(R.id.currencySpinner)
        addNoteCheckbox = findViewById(R.id.add_note_checkbox)
        backArrow = findViewById(R.id.back_arrow)
        backArrow.setOnClickListener {
            context.startActivity(Intent(context, HomeActivity::class.java))
        }

        buttons = listOf(
            findViewById(R.id.btn0), findViewById(R.id.btn00), findViewById(R.id.btnDot),
            findViewById(R.id.btn1), findViewById(R.id.btn2), findViewById(R.id.btn3),
            findViewById(R.id.btn4), findViewById(R.id.btn5), findViewById(R.id.btn6),
            findViewById(R.id.btn7), findViewById(R.id.btn8), findViewById(R.id.btn9),
            findViewById(R.id.btnEnter), findViewById(R.id.btnDel), findViewById(R.id.btnClear)
        )

        buttons.forEach { button ->
            button.setOnClickListener { onButtonClick(button.id) }
        }
    }

    private fun showServerError() {
        if (errorTextView == null) {
            // Create the TextView with the error message
            val localErrorTextView = TextView(context).apply {
                text = context.getString(R.string.accepted_coins_are_not_yet_setup)
                setTextColor(ContextCompat.getColor(context, R.color.tapeRed))
                textSize = 16f
                gravity = Gravity.CENTER
                id = View.generateViewId()  // Assign a unique ID to the errorTextView
                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 8.dpToPx(), 0, 8.dpToPx()) // Adding margins
                }
            }

            // Create the Refresh Button if it's not already created
            refreshButton = Button(context).apply {
                text = R.string.refresh.toString()
                id = View.generateViewId()  // Assign a unique ID to the refresh button
                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 16.dpToPx(), 0, 8.dpToPx())  // Add margins below the error text
                }
                setBackgroundResource(R.drawable.rounded_button_refresh)  // Use a custom drawable for rounded corners
                setTextColor(ContextCompat.getColor(context, android.R.color.white))
                setOnClickListener {
                    // Retry the API call when the button is clicked
                    checkServerStatus()
                }
            }

            // Assign localErrorTextView to the mutable property after creation
            errorTextView = localErrorTextView

            // Find the root ConstraintLayout
            val parentLayout = findViewById<ConstraintLayout>(R.id.parent_layout)

            // Find the header layout
            val headerLayout = findViewById<View>(R.id.header)

            // Ensure the header layout is visible
            headerLayout.visibility = View.VISIBLE

            // Hide all views below the header
            val priceInputContainer = findViewById<View>(R.id.priceInputContainer)
            val gridLayout = findViewById<View>(R.id.gridLayout)
            priceInputContainer.visibility = View.GONE
            gridLayout.visibility = View.GONE

            // Add the errorTextView to the ConstraintLayout
            parentLayout.addView(localErrorTextView)
            // Add the refresh button below the errorTextView
            parentLayout.addView(refreshButton)

            // Set constraints for the errorTextView (below the header)
            val constraintSet = ConstraintSet()
            constraintSet.clone(parentLayout)
            constraintSet.connect(localErrorTextView.id, ConstraintSet.TOP, headerLayout.id, ConstraintSet.BOTTOM)
            constraintSet.connect(localErrorTextView.id, ConstraintSet.START, parentLayout.id, ConstraintSet.START)
            constraintSet.connect(localErrorTextView.id, ConstraintSet.END, parentLayout.id, ConstraintSet.END)

            // Set constraints for the refreshButton (below the errorTextView)
            constraintSet.connect(refreshButton!!.id, ConstraintSet.TOP, localErrorTextView.id, ConstraintSet.BOTTOM)
            constraintSet.connect(refreshButton!!.id, ConstraintSet.START, parentLayout.id, ConstraintSet.START)
            constraintSet.connect(refreshButton!!.id, ConstraintSet.END, parentLayout.id, ConstraintSet.END)

            // Apply the constraints to the layout
            constraintSet.applyTo(parentLayout)

            // Optionally, disable all buttons except backArrow
            buttons.forEach { it.isEnabled = false }
        }
    }

    private fun checkServerStatus() {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getMetrics().execute()
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    // Server is available, refresh the view if necessary
                    withContext(Dispatchers.Main) {
                        // Restore the hidden views and remove the errorTextView and refresh button
                        restoreViews()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showServerError()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showServerError()
                }
            }
        }
    }

    private fun restoreViews() {
        // Restore the views below the header if the server is back online
        val priceInputContainer = findViewById<View>(R.id.priceInputContainer)
        val gridLayout = findViewById<View>(R.id.gridLayout)
        priceInputContainer.visibility = View.VISIBLE
        gridLayout.visibility = View.VISIBLE

        // Hide the errorTextView and refresh button
        errorTextView?.visibility = View.GONE
        refreshButton?.visibility = View.GONE

        // Enable the buttons again
        buttons.forEach { it.isEnabled = true }
    }





    private fun loadCurrencies() {
        val currencySymbols = mutableListOf<String>()
        val currencyCodes = mutableListOf<String>()
        try {
            val inputStream: InputStream = resources.openRawResource(R.raw.currencies)
            val json = inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(json)

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val currencySymbol = jsonObject.getString("symbol")
                val currencyCode = jsonObject.getString("code")
                currencySymbols.add(currencySymbol)
                currencyCodes.add(currencyCode)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val adapter = ArrayAdapter(context, R.layout.spinner_item_selected, currencySymbols)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        currencySpinner.adapter = adapter

        currencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                currentCurrencySymbol = currencySymbols[position]
                currentCurrencyCode = currencyCodes[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun onButtonClick(id: Int) {
        when (id) {
            R.id.btn0 -> appendToInput("0")
            R.id.btn00 -> appendToInput("00")
            R.id.btnDot -> if (!currentInput.contains(".")) appendToInput(".")
            R.id.btn1 -> appendToInput("1")
            R.id.btn2 -> appendToInput("2")
            R.id.btn3 -> appendToInput("3")
            R.id.btn4 -> appendToInput("4")
            R.id.btn5 -> appendToInput("5")
            R.id.btn6 -> appendToInput("6")
            R.id.btn7 -> appendToInput("7")
            R.id.btn8 -> appendToInput("8")
            R.id.btn9 -> appendToInput("9")
            R.id.btnEnter -> confirmPrice()
            R.id.btnDel -> if (currentInput.isNotEmpty()) currentInput = currentInput.dropLast(1)
            R.id.btnClear -> clearInput()
        }
        updatePriceDisplay()
    }

    private fun appendToInput(value: String) {
        val decimalPlaces = if (currentCurrencyCode in listOf("BTC", "LTC", "DASH", "DOGE", "ETH", "USDT", "XMR", "LOG")) 6 else 2
        if (currentInput.contains(".")) {
            val parts = currentInput.split(".")
            if (parts.size > 1 && parts[1].length >= decimalPlaces) {
                return // Already has the maximum decimal places, do nothing
            }
        }
        currentInput += value
    }

    private fun updatePriceDisplay() {
        itemPriceInput.text = if (currentInput.isEmpty()) "0.00" else currentInput
    }

    private fun confirmPrice() {
        val decimalPlaces = if (currentCurrencyCode in listOf("BTC", "LTC", "DASH", "DOGE", "ETH", "USDT", "XMR", "LOG")) 6 else 2
        if (currentInput.isNotEmpty()) {
            if (!currentInput.contains(".")) {
                currentInput += "." + "0".repeat(decimalPlaces)
            } else {
                val parts = currentInput.split(".")
                if (parts.size == 1 || parts[1].isEmpty()) {
                    currentInput += "0".repeat(decimalPlaces)
                } else if (parts[1].length < decimalPlaces) {
                    currentInput += "0".repeat(decimalPlaces - parts[1].length)
                }
            }

            val intent = if (addNoteCheckbox.isChecked) {
                Intent(context, PriceConfirmActivity::class.java).apply {
                    putExtra("PRICE", "$currentCurrencySymbol$currentInput")
                    putExtra("CURRENCY_CODE", currentCurrencyCode)
                }
            } else {
                Intent(context, CryptoOptionActivity::class.java).apply {
                    putExtra("PRICE", "$currentCurrencySymbol$currentInput")
                    putExtra("CURRENCY_CODE", currentCurrencyCode)
                }
            }

            context.startActivity(intent)
        }
    }


    private fun clearInput() {
        currentInput = ""
    }

    // Extension function to convert dp to pixels
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}
