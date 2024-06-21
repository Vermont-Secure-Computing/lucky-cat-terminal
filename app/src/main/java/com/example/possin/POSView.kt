package com.example.possin

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import org.json.JSONArray
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

    init {
        LayoutInflater.from(context).inflate(R.layout.pos_view, this, true)
        setupView()
        loadCurrencies()
    }

    private fun setupView() {
        itemPriceInput = findViewById(R.id.itemPriceInput)
        currencySpinner = findViewById(R.id.currencySpinner)

        val buttons = listOf(
            R.id.btn0, R.id.btn00, R.id.btnDot, R.id.btn1, R.id.btn2, R.id.btn3,
            R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9,
            R.id.btnEnter, R.id.btnDel, R.id.btnClear
        )

        buttons.forEach { id ->
            findViewById<Button>(id).setOnClickListener { onButtonClick(id) }
        }
    }

    private fun loadCurrencies() {
        val currencies = mutableListOf<String>()
        try {
            val inputStream: InputStream = resources.openRawResource(R.raw.currencies)
            val json = inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(json)

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val currencySymbol = jsonObject.getString("symbol")
                currencies.add(currencySymbol)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val adapter = ArrayAdapter(context, R.layout.spinner_item_selected, currencies)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        currencySpinner.adapter = adapter

        currencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedCurrency = currencies[position]
                currentCurrencySymbol = selectedCurrency
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
        if (currentInput.contains(".")) {
            val parts = currentInput.split(".")
            if (parts.size > 1 && parts[1].length >= 2) {
                // Already has 2 decimal places, do nothing
                return
            }
        }
        currentInput += value
    }

    private fun updatePriceDisplay() {
        itemPriceInput.text = if (currentInput.isEmpty()) "0.00" else currentInput
    }

    private fun confirmPrice() {
        if (currentInput.isNotEmpty()) {
            if (!currentInput.contains(".")) {
                currentInput += ".00"
            } else {
                val parts = currentInput.split(".")
                if (parts.size == 1 || parts[1].isEmpty()) {
                    currentInput += "00"
                } else if (parts[1].length == 1) {
                    currentInput += "0"
                }
            }

            val intent = Intent(context, PriceConfirmActivity::class.java)
            intent.putExtra("PRICE", "$currentCurrencySymbol$currentInput")
            context.startActivity(intent)
        }
    }

    private fun clearInput() {
        currentInput = ""
    }
}