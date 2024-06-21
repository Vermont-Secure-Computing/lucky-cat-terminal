package com.example.possin

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


class MainActivity : AppCompatActivity() {

    private lateinit var textView1: TextView
    private lateinit var textView2: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(POSView(this))

//        textView1 = findViewById(R.id.textView1)
//        textView2 = findViewById(R.id.textView2)
//        val button1: Button = findViewById(R.id.button1)
//        val button2: Button = findViewById(R.id.button2)
//        button1.setOnClickListener {
//            textView1.text = generateBitcoinAddress()
//        }
//        button2.setOnClickListener {
//            textView2.text = generateLitecoinAddress()
//        }

    }

    private fun generateBitcoinAddress(): String {
        try {
            // Replace with your xpub
            val xPub = "xpub661MyMwAqRbcGFHkiswngcM4zmSu78tnSd5Mp1Kvc5DoQwtkfxuM6gqpcYADkRUSMmmu6p3sU9UGMKNRHXepDf7mwFFzGWDgd95X35cGuNz"

            // Initialize Bitcoin Manage
            val bitcoinManager = BitcoinManager(xPub)


            // Derive the key at the specified index
            val index = 1 // Change the index as needed for different addresses
            val address = bitcoinManager.getAddress(index)

            // Print or use the derived address
            return address
        } catch (e: Exception) {
            return "Something went wrong"
        }
    }

    private fun generateLitecoinAddress(): String {
        try {
            // Replace with your xpub
            val xPub = "xpub661MyMwAqRbcGFHkiswngcM4zmSu78tnSd5Mp1Kvc5DoQwtkfxuM6gqpcYADkRUSMmmu6p3sU9UGMKNRHXepDf7mwFFzGWDgd95X35cGuNz"

            // Initialize Bitcoin Manage
            val litecoinManager = LitecoinManager(xPub)


            // Derive the key at the specified index
            val index = 1 // Change the index as needed for different addresses
            val address = litecoinManager.getAddress(index)

            // Print or use the derived address
            return address
        } catch (e: Exception) {
            return "Something went wrong"
        }
    }

}