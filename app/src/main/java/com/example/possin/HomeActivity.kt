package com.example.possin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.possin.adapter.TransactionAdapter
import com.example.possin.adapter.TransactionDividerAdapter
import com.example.possin.model.TransactionViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.InputStreamReader
import java.util.Properties

class HomeActivity : AppCompatActivity() {

    private lateinit var merchantPropertiesFile: File
    private lateinit var configPropertiesFile: File
    private lateinit var apiPropertiesFile: File
    private val transactionViewModel: TransactionViewModel by viewModels()
    private lateinit var cryptocurrencyNames: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home)

        window.statusBarColor = ContextCompat.getColor(this, R.color.tapeRed)

        merchantPropertiesFile = File(filesDir, "merchant.properties")
        configPropertiesFile = File(filesDir, "config.properties")
        apiPropertiesFile = File(filesDir, "api.properties")

        // Load cryptocurrency names from JSON file
        cryptocurrencyNames = loadCryptocurrencyNames()

        val seeAllTextView = findViewById<TextView>(R.id.seeAllTextView)

        // Set up buttons
        setupButtons()

        // Set up RecyclerView
        val transactionsRecyclerView = findViewById<RecyclerView>(R.id.transactionsRecyclerView)
        val noTransactionsTextView = findViewById<TextView>(R.id.noTransactionsTextView)
        transactionsRecyclerView.layoutManager = LinearLayoutManager(this)

        transactionViewModel.allTransactions.observe(this, Observer { transactions ->
            if (transactions.isNullOrEmpty()) {
                // Show the "No transactions yet" message
                noTransactionsTextView.visibility = View.VISIBLE
                transactionsRecyclerView.visibility = View.GONE

                // Disable the "See all" TextView
                seeAllTextView.isEnabled = false
                seeAllTextView.setTextColor(ContextCompat.getColor(this, R.color.grey)) // Set a color indicating it's disabled

            } else {
                // Hide the "No transactions yet" message and show the RecyclerView
                noTransactionsTextView.visibility = View.GONE
                transactionsRecyclerView.visibility = View.VISIBLE

                // Enable the "See all" TextView
                seeAllTextView.isEnabled = true
                seeAllTextView.setTextColor(ContextCompat.getColor(this, R.color.tapeRed)) // Restore the original color

                val limitedTransactions = transactions.take(3)
                val adapter = TransactionDividerAdapter(this, limitedTransactions)
                transactionsRecyclerView.adapter = adapter
            }
        })

        seeAllTextView.setOnClickListener {
            // Handle the "See all" click event here
            // Navigate to ViewAllActivity
            val intent = Intent(this, ViewAllActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupButtons() {
        val button1 = findViewById<ImageButton>(R.id.button1)
        val button2 = findViewById<ImageButton>(R.id.button2)
        val button3 = findViewById<ImageButton>(R.id.button3)
        val button4 = findViewById<ImageButton>(R.id.button4)
        val button5 = findViewById<ImageButton>(R.id.button5)
        val button6 = findViewById<ImageButton>(R.id.button6)

        // Log to check if buttons are null
        Log.d("HomeActivity", "button1: $button1, button2: $button2, button3: $button3, button4: $button4, button5: $button5, button6: $button6")

        button1?.setOnClickListener {
            if (!propertiesFilesExist()) {
                if (!merchantPropertiesFile.exists()) {
                    showFillUpProfileDialog(MerchantActivity::class.java)
                } else if (!configPropertiesFile.exists() || configPropertiesContainsDefaultKey()) {
                    showFillUpProfileDialog(XpubAddress::class.java)
                }
            } else if (!inputsExist() || !apiKeyExists()) {
                showFillUpProfileDialog(APIActivity::class.java)
            } else {
                val intent = Intent(this, POSActivity::class.java)
                startActivity(intent)
            }
        }

        button2?.setOnClickListener {
            val intent = Intent(this, APIActivity::class.java)
            startActivity(intent)
        }

        button3?.setOnClickListener {
            val intent = Intent(this, BaseCurrency::class.java)
            startActivity(intent)
        }

        button4?.setOnClickListener {
            val intent = Intent(this, MerchantActivity::class.java)
            startActivity(intent)
        }

        button5?.setOnClickListener {
            val intent = Intent(this, XpubAddress::class.java)
            startActivity(intent)
        }

        button6.setOnClickListener {
            val intent = Intent(this, ExportDataActivity::class.java)
            startActivity(intent)
        }
    }

    private fun propertiesFilesExist(): Boolean {
        return merchantPropertiesFile.exists() && configPropertiesFile.exists()
    }

    private fun configPropertiesContainsDefaultKey(): Boolean {
        val configProperties = Properties()
        if (configPropertiesFile.exists()) {
            configProperties.load(configPropertiesFile.inputStream())
        }
        return configProperties.getProperty("default_key") != null
    }

    private fun inputsExist(): Boolean {
        val merchantProperties = Properties()
        if (merchantPropertiesFile.exists()) {
            merchantProperties.load(merchantPropertiesFile.inputStream())
        }
        val merchantName = merchantProperties.getProperty("merchant_name", "")

        val configProperties = Properties()
        if (configPropertiesFile.exists()) {
            configProperties.load(configPropertiesFile.inputStream())
        }

        var addressOrXpubExists = false
        for (cryptoName in cryptocurrencyNames) {
            val addressXpub = configProperties.getProperty("${cryptoName}_type", "")
            if (addressXpub.isNotEmpty()) {
                addressOrXpubExists = true
                break
            }
        }

        return merchantName.isNotEmpty() && addressOrXpubExists
    }

    private fun apiKeyExists(): Boolean {
        val apiProperties = Properties()
        if (apiPropertiesFile.exists()) {
            apiProperties.load(apiPropertiesFile.inputStream())
        }
        return apiProperties.getProperty("api_key", "").isNotEmpty()
    }

    private fun showFillUpProfileDialog(activityClass: Class<*>) {
        AlertDialog.Builder(this)
            .setTitle("Incomplete Profile")
            .setMessage("Please fill up your merchant profile, xpub, and address.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                val intent = Intent(this, activityClass)
                startActivity(intent)
            }
            .setCancelable(false)
            .show()
    }

    private fun loadCryptocurrencyNames(): List<String> {
        val inputStream = assets.open("cryptocurrencies.json")
        val reader = InputStreamReader(inputStream)
        val type = object : TypeToken<CryptocurrenciesWrapper>() {}.type
        val cryptocurrenciesWrapper: CryptocurrenciesWrapper = Gson().fromJson(reader, type)
        reader.close()
        inputStream.close()
        return cryptocurrenciesWrapper.cryptocurrencies.map { it.name }
    }
}


data class CryptocurrenciesWrapper(
    val cryptocurrencies: List<CryptoCurrencyInfo>
)
