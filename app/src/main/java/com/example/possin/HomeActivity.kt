package com.example.possin

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.possin.adapter.TransactionAdapter
import com.example.possin.viewmodel.TransactionViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.InputStreamReader
import java.util.Properties



class HomeActivity : AppCompatActivity() {

    private lateinit var merchantPropertiesFile: File
    private lateinit var configPropertiesFile: File
    private val transactionViewModel: TransactionViewModel by viewModels()
    private lateinit var transactionsCardView: CardView
    private lateinit var cryptocurrencyNames: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home)

        merchantPropertiesFile = File(filesDir, "merchant.properties")
        configPropertiesFile = File(filesDir, "config.properties")

        // Load cryptocurrency names from JSON file
        cryptocurrencyNames = loadCryptocurrencyNames()

        // Check if the properties files exist and have required inputs
        if (!propertiesFilesExist() || !inputsExist()) {
            showFillUpProfileDialog()
        }

        transactionsCardView = findViewById(R.id.transactionsCardView)
        val seeAllTextView = findViewById<TextView>(R.id.seeAllTextView)

        // Set up buttons
        setupButtons()

        // Set up RecyclerView
        val transactionsRecyclerView = findViewById<RecyclerView>(R.id.transactionsRecyclerView)
        transactionsRecyclerView.layoutManager = LinearLayoutManager(this)

        // Add custom divider item decoration
        val dividerItemDecoration = DividerItemDecoration(
            transactionsRecyclerView.context,
            (transactionsRecyclerView.layoutManager as LinearLayoutManager).orientation
        )
        ContextCompat.getDrawable(this, R.drawable.divider)?.let {
            dividerItemDecoration.setDrawable(it)
        }
        transactionsRecyclerView.addItemDecoration(dividerItemDecoration)

        transactionViewModel.allTransactions.observe(this, Observer { transactions ->
            transactions?.let {
                if (it.isEmpty()) {
                    transactionsCardView.visibility = View.GONE
                } else {
                    transactionsCardView.visibility = View.VISIBLE
                    val adapter = TransactionAdapter(this, it)
                    transactionsRecyclerView.adapter = adapter
                }
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
        // Set up ImageButton 1 to navigate to POSActivity
        val button1 = findViewById<ImageButton>(R.id.button1)
        button1.setOnClickListener {
            val intent = Intent(this, POSActivity::class.java)
            startActivity(intent)
        }

        val button3 = findViewById<ImageButton>(R.id.button3)
        button3.setOnClickListener {
            // Handle button 3 click
        }

        val button4 = findViewById<ImageButton>(R.id.button4)
        button4.setOnClickListener {
            val intent = Intent(this, MerchantActivity::class.java)
            startActivity(intent)
        }

        val button5 = findViewById<ImageButton>(R.id.button5)
        button5.setOnClickListener {
            val intent = Intent(this, XpubAddress::class.java)
            startActivity(intent)
        }

        val button6 = findViewById<ImageButton>(R.id.button6)
        button6.setOnClickListener {
            // Handle button 6 click
        }
    }

    private fun propertiesFilesExist(): Boolean {
        return merchantPropertiesFile.exists() && configPropertiesFile.exists()
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
            val address = configProperties.getProperty("${cryptoName}_type", "")
            val xpub = configProperties.getProperty("${cryptoName}_type", "")
            if (address.isNotEmpty() || xpub.isNotEmpty()) {
                addressOrXpubExists = true
                break
            }
        }

        return merchantName.isNotEmpty() && addressOrXpubExists
    }

    private fun showFillUpProfileDialog() {
        AlertDialog.Builder(this)
            .setTitle("Incomplete Profile")
            .setMessage("Please fill up your merchant profile, xpub, and address.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                val intent = Intent(this, MerchantActivity::class.java)
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
