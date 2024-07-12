package com.example.possin

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.possin.adapter.TransactionAdapter
import com.example.possin.viewmodel.TransactionViewModel
import java.io.File
import java.util.*

class HomeActivity : AppCompatActivity() {

    private lateinit var merchantPropertiesFile: File
    private val transactionViewModel: TransactionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home)

        merchantPropertiesFile = File(filesDir, "merchant.properties")

        val headerNameTextView = findViewById<TextView>(R.id.header_name)

        if (merchantPropertiesFile.exists()) {
            val properties = Properties().apply {
                load(merchantPropertiesFile.inputStream())
            }

            val merchantName = properties.getProperty("merchant_name", "")
            if (merchantName.isNotEmpty()) {
                headerNameTextView.text = "Good morning, $merchantName!"
            } else {
                headerNameTextView.text = "Good morning!"
            }
        } else {
            headerNameTextView.text = "Good morning!"
        }

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
                val adapter = TransactionAdapter(this, it)
                transactionsRecyclerView.adapter = adapter
            }
        })
    }

    private fun setupButtons() {
        // Set up ImageButton 1 to navigate to POSActivity
        val button1 = findViewById<ImageButton>(R.id.button1)
        button1.setOnClickListener {
            val intent = Intent(this, POSActivity::class.java)
            startActivity(intent)
        }

        // Set up other ImageButtons
        val button2 = findViewById<ImageButton>(R.id.button2)
        button2.setOnClickListener {
            // Handle button 2 click
        }

        val button3 = findViewById<ImageButton>(R.id.button3)
        button3.setOnClickListener {
            // Handle button 3 click
        }

        val button4 = findViewById<ImageButton>(R.id.button4)
        button4.setOnClickListener {
            // Handle button 4 click
        }

        val button5 = findViewById<ImageButton>(R.id.button5)
        button5.setOnClickListener {
            // Handle button 5 click
        }

        val button6 = findViewById<ImageButton>(R.id.button6)
        button6.setOnClickListener {
            // Handle button 6 click
        }
    }
}