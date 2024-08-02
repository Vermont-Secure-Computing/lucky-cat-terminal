package com.example.possin

import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.example.possin.database.AppDatabase
import com.example.possin.model.Transaction
import okhttp3.OkHttpClient
import java.io.File
import java.util.Properties

class ViewAllDetailActivity : AppCompatActivity() {

    private lateinit var chainTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var balanceTextView: TextView
    private lateinit var txidTextView: TextView
    private lateinit var feesTextView: TextView
    private lateinit var confirmationsTextView: TextView
    private lateinit var timeTextView: TextView
    private lateinit var messageTextView: TextView
    private lateinit var baseCurrencyTextView: TextView
    private lateinit var basePriceTextView: TextView
    private lateinit var merchantName: TextView
    private lateinit var merchantAddress: TextView

    private lateinit var transaction: Transaction
    private lateinit var client: OkHttpClient
    private lateinit var db: AppDatabase

    private lateinit var chain: String
    private lateinit var deviceId: String
    private lateinit var merchantPropertiesFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_all_detail)

        window.statusBarColor = ContextCompat.getColor(this, R.color.tapeRed)

        chainTextView = findViewById(R.id.chainTextView)
        dateTextView = findViewById(R.id.dateTextView)
        balanceTextView = findViewById(R.id.balanceTextView)
        baseCurrencyTextView = findViewById(R.id.baseCurrencyTextView)
        basePriceTextView = findViewById(R.id.basePriceTextView)
        txidTextView = findViewById(R.id.txidTextView)
        feesTextView = findViewById(R.id.feesTextView)
        confirmationsTextView = findViewById(R.id.confirmationsTextView)
        timeTextView = findViewById(R.id.timeTextView)
        messageTextView = findViewById(R.id.messageTextView)
        merchantAddress = findViewById(R.id.merchantAddress)
        merchantName = findViewById(R.id.merchantName)
        merchantPropertiesFile = File(filesDir, "merchant.properties")

        transaction = intent.getParcelableExtra("transaction")!!

        transaction?.let {
            updateUI(it)
        }

        db = AppDatabase.getDatabase(this)
        client = OkHttpClient()

        // Get the device ID
        deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        // Set up back arrow
        val backArrow = findViewById<ImageView>(R.id.back_arrow)
        backArrow.setOnClickListener {
            finish()
        }

        // Set up print button
        val printButton = findViewById<Button>(R.id.printButton)
        printButton.setOnClickListener {
            showReceiptDialog()
        }
    }

    private fun getProperty(key: String): String? {
        val properties = Properties()
        if (merchantPropertiesFile.exists()) {
            properties.load(merchantPropertiesFile.inputStream())
        }
        return properties.getProperty(key)
    }

    fun getMerchantName(): String? {
        return getProperty("merchant_name")
    }

    fun getMechantAddress(): String? {
        return getProperty("address")
    }

    private fun updateUI(transaction: Transaction) {
        Log.d("CONFIRMATION", transaction.confirmations.toString())
        merchantName.text = "Name: ${getMerchantName()}"
        merchantAddress.text = "Address: ${getMechantAddress()}"
        chainTextView.text = "Chain: ${transaction.chain}"
        balanceTextView.text = "Amount: ${transaction.balance}"
        baseCurrencyTextView.text = "Base Currency: ${transaction.selectedCurrencyCode}"
        basePriceTextView.text = "Base Price: ${transaction.numericPrice}"
        dateTextView.text = "Date: ${transaction.date}"
        txidTextView.text = "TxID: ${transaction.txid}"
        feesTextView.text = "Fees: ${transaction.fees}"
        confirmationsTextView.text = "Confirmations: ${transaction.confirmations}"
        timeTextView.text = "Time: ${transaction.time}"
        messageTextView.text = transaction.message ?: "No message"
    }

    private fun showReceiptDialog() {
        val receiptDialog = ReceiptDialogFragment()

        // Pass data to the dialog fragment
        val args = Bundle()
        args.putString("receiptTitle", "${merchantName.text}")
        args.putString("receiptAddress", "${merchantAddress.text}")
        args.putString("receiptDetails", "Transaction Details")
        args.putString("receiptBalance", "${balanceTextView.text}")
        args.putString("receiptTxID", "${txidTextView.text}")
        args.putString("receiptFees", "${feesTextView.text}")
        args.putString("receiptConfirmations", "${confirmationsTextView.text}")
        args.putString("receiptChain", "${chainTextView.text}")
        args.putString("receiptDeviceID", "$deviceId")
        args.putString("receiptNumericPrice", "B${basePriceTextView.text}")
        args.putString("receiptSelectedCurrencyCode", "${baseCurrencyTextView.text}")
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
