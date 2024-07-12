package com.example.possin

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.possin.database.AppDatabase
import com.example.possin.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class ViewAllDetailActivity : AppCompatActivity() {

    private lateinit var chainTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var balanceTextView: TextView
    private lateinit var txidTextView: TextView
    private lateinit var feesTextView: TextView
    private lateinit var confirmationsTextView: TextView
    private lateinit var timeTextView: TextView
    private lateinit var messageTextView: TextView

    private lateinit var transaction: Transaction
    private lateinit var client: OkHttpClient
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_all_detail)

        chainTextView = findViewById(R.id.chainTextView)
        dateTextView = findViewById(R.id.dateTextView)
        balanceTextView = findViewById(R.id.balanceTextView)
        txidTextView = findViewById(R.id.txidTextView)
        feesTextView = findViewById(R.id.feesTextView)
        confirmationsTextView = findViewById(R.id.confirmationsTextView)
        timeTextView = findViewById(R.id.timeTextView)
        messageTextView = findViewById(R.id.messageTextView)

        transaction = intent.getParcelableExtra("transaction")!!

        transaction?.let {
            updateUI(it)
        }

        db = AppDatabase.getDatabase(this)
        client = OkHttpClient()

        // Call getConfirmations only once
        getConfirmations(transaction.chain, transaction.txid)
    }

    private fun updateUI(transaction: Transaction) {
        chainTextView.text = transaction.chain
        dateTextView.text = transaction.date
        balanceTextView.text = "Amount: ${String.format("%.8f", transaction.balance.toDouble() / 100000000)}"
        txidTextView.text = "TxID: ${transaction.txid}"
        feesTextView.text = "Fees: ${String.format("%.8f", transaction.fees.toDouble() / 100000000)}"
        confirmationsTextView.text = "Confirmations: ${transaction.confirmations}"
        timeTextView.text = "Time: ${transaction.time}"
        messageTextView.text = transaction.message ?: "No message"
    }

    private fun getConfirmations(chain: String, txid: String) {
        val url = "http://198.7.125.183/terminal/tx_confirmations/$chain/$txid"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    responseData?.let {
                        val jsonObject = JSONObject(it)
                        val confirmations = jsonObject.getInt("confirmations")

                        runOnUiThread {
                            confirmationsTextView.text = "Confirmations: $confirmations"

                            // Update the transaction in the database
                            updateTransactionConfirmations(confirmations)
                        }
                    }
                }
            }
        })
    }

    private fun updateTransactionConfirmations(confirmations: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            val updatedTransaction = transaction.copy(confirmations = confirmations)
            db.transactionDao().update(updatedTransaction)
        }
    }
}