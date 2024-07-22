package com.example.possin

import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.example.possin.database.AppDatabase
import com.example.possin.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

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

    private lateinit var chain: String
    private lateinit var deviceId: String

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

        // Get the device ID
        deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        // Set up back button
        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        // Set up print button
        val printButton = findViewById<Button>(R.id.printButton)
        printButton.setOnClickListener {
            showReceiptDialog()
        }
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

    private fun showReceiptDialog() {
        val receiptDialog = ReceiptDialogFragment()

        // Pass data to the dialog fragment
        val args = Bundle()
        args.putString("receiptTitle", "RECEIPT")
        args.putString("receiptDetails", "Transaction Details")
        args.putString("receiptBalance", "Balance: ${balanceTextView.text}")
        args.putString("receiptTxID", "TxID: ${txidTextView.text}")
        args.putString("receiptFees", "Fees: ${feesTextView.text}")
        args.putString("receiptConfirmations", "Confirmations: ${confirmationsTextView.text}")
        args.putString("receiptChain", "Chain: ${chainTextView.text}")
        args.putString("receiptDeviceID", "Device ID: $deviceId")
        receiptDialog.arguments = args

        receiptDialog.show(supportFragmentManager, "ReceiptDialog")
    }

    fun performPrint() {
        val bluetoothConnection = BluetoothPrintersConnections.selectFirstPaired()
        if (bluetoothConnection == null) {
            Toast.makeText(this, "No paired Bluetooth printer found", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val printer = EscPosPrinter(bluetoothConnection, 203, 48f, 32)
            for (i in 1..2) {
                val copyType = if (i == 1) "Customer Copy" else "Owner's Copy"
                printer.printFormattedText(
                    "[C]<u><font size='big'>RECEIPT</font></u>\n" +
                            "[L]\n" +
                            "[C]-------------------------------\n" +
                            "[L]\n" +
                            "[L]<b>Transaction Details</b>\n" +
                            "[L]\n" +
                            "[L]${balanceTextView.text}\n" +
                            "[L]${txidTextView.text}\n" +
                            "[L]${feesTextView.text}\n" +
                            "[L]${confirmationsTextView.text}\n" +
                            "[L]Chain: ${chainTextView.text}\n" +
                            "[L]Device ID: $deviceId\n" +
                            "[L]\n" +
                            "[C]-------------------------------\n" +
                            "[C]$copyType\n" +
                            "[L]\n" +
                            "[C]Thank you for your payment!\n"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle printing errors
        }
    }
}
