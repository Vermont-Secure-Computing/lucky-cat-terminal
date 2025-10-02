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
import android.widget.Toast
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

class POSView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    // UI refs
    private lateinit var itemPriceInput: TextView
    private lateinit var currencySpinner: Spinner
    private lateinit var backArrow: ImageView
    private lateinit var addNoteCheckbox: CheckBox
    private lateinit var buttons: List<Button>
    private lateinit var btnEnter: Button

    // Error UI that we dynamically add/remove
    private var errorTextView: TextView? = null
    private var refreshButton: Button? = null

    // State
    private var currentInput = ""
    private var currentCurrencySymbol = "$"
    private var currentCurrencyCode = ""
    private var lastNetStatus: NetworkStatus = NetworkStatus.OFFLINE

    // Network/API
    private lateinit var apiService: ApiService

    // Coroutines
    private val rootJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + rootJob)

    init {
        LayoutInflater.from(context).inflate(R.layout.pos_view, this, true)
        setupView()
        loadCurrencies()
        initializeApiService()
        // Don't check server here; wait for CONNECTED via setNetworkStatus().
        setEnterEnabled(false)                            // <-- start disabled until CONNECTED arrives
    }

    /** Called by POSActivity whenever network status changes */
    fun setNetworkStatus(status: NetworkStatus) {
        if (status == lastNetStatus) return
        lastNetStatus = status

        when (status) {
            NetworkStatus.CONNECTED -> {
                setEnterEnabled(true)                     // <-- enable on good net
                checkServerStatus()
            }
            NetworkStatus.LIMITED, NetworkStatus.OFFLINE -> {
                setEnterEnabled(false)                    // <-- disable on weak/offline
                // No server checks while net is bad.
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        rootJob.cancel()
    }

    // ---- Setup ----

    private fun initializeApiService() {
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

        // Grab Enter specifically, and also build the full list for your existing handlers
        btnEnter = findViewById(R.id.btnEnter)           // <-- NEW: find Enter button
        buttons = listOf(
            findViewById(R.id.btn0), findViewById(R.id.btn00), findViewById(R.id.btnDot),
            findViewById(R.id.btn1), findViewById(R.id.btn2), findViewById(R.id.btn3),
            findViewById(R.id.btn4), findViewById(R.id.btn5), findViewById(R.id.btn6),
            findViewById(R.id.btn7), findViewById(R.id.btn8), findViewById(R.id.btn9),
            btnEnter, findViewById(R.id.btnDel), findViewById(R.id.btnClear)
        )
        buttons.forEach { btn -> btn.setOnClickListener { onButtonClick(btn.id) } }
    }

    // Toggle only the Enter button
    private fun setEnterEnabled(enabled: Boolean) {
        if (!::btnEnter.isInitialized) return
        btnEnter.isEnabled = enabled
        btnEnter.isClickable = enabled
        btnEnter.alpha = if (enabled) 1f else 0.5f        // dim when disabled (optional)
    }

    // ---- Server status / error handling ----

    private fun checkServerStatus() {
        if (lastNetStatus != NetworkStatus.CONNECTED) return

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getMetrics().execute()
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    withContext(Dispatchers.Main) { restoreViews() }
                } else {
                    withContext(Dispatchers.Main) { showServerError() }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { showServerError() }
            }
        }
    }

    private fun showServerError() {
        val parentLayout = findViewById<ConstraintLayout>(R.id.parent_layout)
        val headerLayout = findViewById<View>(R.id.header)
        val priceInputContainer = findViewById<View>(R.id.priceInputContainer)
        val gridLayout = findViewById<View>(R.id.gridLayout)

        headerLayout.visibility = View.VISIBLE
        priceInputContainer.visibility = View.GONE
        gridLayout.visibility = View.GONE
        buttons.forEach { it.isEnabled = false }
        setEnterEnabled(false)                             // <-- keep Enter disabled in error state

        if (errorTextView == null) {
            val localErrorTextView = TextView(context).apply {
                text = context.getString(R.string.accepted_coins_are_not_yet_setup)
                setTextColor(ContextCompat.getColor(context, R.color.tapeRed))
                textSize = 16f
                gravity = Gravity.CENTER
                id = View.generateViewId()
                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 8.dpToPx(), 0, 8.dpToPx()) }
            }
            errorTextView = localErrorTextView
            parentLayout.addView(localErrorTextView)

            refreshButton = Button(context).apply {
                text = context.getString(R.string.refresh)
                id = View.generateViewId()
                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 16.dpToPx(), 0, 8.dpToPx()) }
                setBackgroundResource(R.drawable.rounded_button_refresh)
                setTextColor(ContextCompat.getColor(context, android.R.color.white))
                setOnClickListener { checkServerStatus() }
            }
            parentLayout.addView(refreshButton)

            ConstraintSet().apply {
                clone(parentLayout)
                connect(localErrorTextView.id, ConstraintSet.TOP, headerLayout.id, ConstraintSet.BOTTOM)
                connect(localErrorTextView.id, ConstraintSet.START, parentLayout.id, ConstraintSet.START)
                connect(localErrorTextView.id, ConstraintSet.END, parentLayout.id, ConstraintSet.END)
                connect(refreshButton!!.id, ConstraintSet.TOP, localErrorTextView.id, ConstraintSet.BOTTOM)
                connect(refreshButton!!.id, ConstraintSet.START, parentLayout.id, ConstraintSet.START)
                connect(refreshButton!!.id, ConstraintSet.END, parentLayout.id, ConstraintSet.END)
                applyTo(parentLayout)
            }
        } else {
            errorTextView?.visibility = View.VISIBLE
            refreshButton?.visibility = View.VISIBLE
        }
    }

    private fun restoreViews() {
        findViewById<View>(R.id.priceInputContainer)?.visibility = View.VISIBLE
        findViewById<View>(R.id.gridLayout)?.visibility = View.VISIBLE

        errorTextView?.visibility = View.GONE
        refreshButton?.visibility = View.GONE

        buttons.forEach { it.isEnabled = true }
        setEnterEnabled(lastNetStatus == NetworkStatus.CONNECTED) // <-- respect current net state
    }

    // ---- Currency / keypad ----

    private fun loadCurrencies() {
        val symbols = mutableListOf<String>()
        val codes = mutableListOf<String>()
        try {
            val inputStream = resources.openRawResource(R.raw.currencies)
            val json = inputStream.bufferedReader().use { it.readText() }
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                symbols.add(obj.getString("symbol"))
                codes.add(obj.getString("code"))
            }
        } catch (_: Exception) {}

        val adapter = ArrayAdapter(context, R.layout.spinner_item_selected, symbols)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        currencySpinner.adapter = adapter

        val prefs = context.getSharedPreferences("pos_prefs", Context.MODE_PRIVATE)
        val savedCode = prefs.getString("last_currency_code", null)
        if (savedCode != null) {
            val index = codes.indexOf(savedCode)
            if (index >= 0) {
                currencySpinner.setSelection(index)
                currentCurrencySymbol = symbols[index]
                currentCurrencyCode = codes[index]
            }
        }

        currencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                currentCurrencySymbol = symbols.getOrNull(position) ?: "$"
                currentCurrencyCode = codes.getOrNull(position) ?: ""

                val prefs = context.getSharedPreferences("pos_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("last_currency_code", currentCurrencyCode).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun onButtonClick(id: Int) {
        // Guard: ignore Enter taps if disabled (extra safety)
        if (id == R.id.btnEnter && !btnEnter.isEnabled) return

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
        val decimalPlaces = if (currentCurrencyCode in listOf("BTC","LTC","DASH","DOGE","ETH","USDT","XMR","LOG")) 8 else 2

        // prevent multiple dots
        if (value == "." && currentInput.contains(".")) return

        // temporary string with new value
        val newInput = currentInput + value

        // try parse as BigDecimal for accuracy
        try {
            val parsed = newInput.toBigDecimalOrNull()
            if (parsed != null && parsed > java.math.BigDecimal("999999999")) {
                // reject if exceeds 100,000,000
                return
            }
        } catch (_: Exception) {
            return
        }

        // prevent exceeding decimal places
        if (newInput.contains(".")) {
            val parts = newInput.split(".")
            if (parts.size > 1 && parts[1].length > decimalPlaces) return
        }

        currentInput = newInput
    }


    private fun updatePriceDisplay() {
        itemPriceInput.text = if (currentInput.isEmpty()) "0.00" else currentInput
    }

    private fun confirmPrice() {
        if (currentInput.isEmpty()) return

        val parsed = currentInput.toBigDecimalOrNull()
        if (parsed == null || parsed > java.math.BigDecimal("999999999")) {
            Toast.makeText(context, "Maximum allowed amount is 999,999,999", Toast.LENGTH_SHORT).show()
            return
        }

        // Use the same decimalPlaces rule as appendToInput
        val decimalPlaces = if (currentCurrencyCode in listOf("BTC","LTC","DASH","DOGE","ETH","USDT","XMR","LOG")) 8 else 2

        if (!currentInput.contains(".")) {
            currentInput += "." + "0".repeat(decimalPlaces)
        } else {
            val parts = currentInput.split(".")
            currentInput += when {
                parts.size == 1 || parts[1].isEmpty() -> "0".repeat(decimalPlaces)
                parts[1].length < decimalPlaces -> "0".repeat(decimalPlaces - parts[1].length)
                else -> ""
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


    private fun clearInput() {
        currentInput = ""
    }

    // dp -> px
    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}
