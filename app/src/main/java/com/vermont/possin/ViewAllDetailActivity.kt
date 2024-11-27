package com.vermont.possin

import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.vermont.possin.database.AppDatabase
import com.vermont.possin.model.Transaction
import com.vermont.possin.utils.PropertiesUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.IOException
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
    private lateinit var receivingAddress: TextView
    private lateinit var txType: TextView
    private lateinit var previousBalanceInTextView: TextView
    private lateinit var previousTxidInTextView: TextView

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
        receivingAddress = findViewById(R.id.receivingAddress)
        feesTextView = findViewById(R.id.feesTextView)
        confirmationsTextView = findViewById(R.id.confirmationsTextView)
        timeTextView = findViewById(R.id.timeTextView)
        messageTextView = findViewById(R.id.messageTextView)
        merchantAddress = findViewById(R.id.merchantAddress)
        merchantName = findViewById(R.id.merchantName)
        txType = findViewById(R.id.txType)
        previousBalanceInTextView = findViewById(R.id.previousBalanceInTextView)
        previousTxidInTextView = findViewById(R.id.previousTxidInTextView)
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

        getConfirmations(transaction.chain, transaction.txid)
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

    fun getMerchantAddress(): String? {
        return getProperty("address")
    }

    private fun getConfirmations(chain: String, txid: String) {
        val url = "https://dogpay.mom/terminal/tx_confirmations/$chain/$txid"
        val apiKey = PropertiesUtil.getProperty(this, "api_key")

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .addHeader("x-api-key", apiKey ?: "")
            .build()

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

                        // Update the UI on the main thread
                        runOnUiThread {
                            confirmationsTextView.text = getString(R.string.confirmations, confirmations)
                        }

                        // Update the transaction in the database
                        lifecycleScope.launch(Dispatchers.IO) {
                            val transaction = db.transactionDao().getTransactionByTxid(txid)
                            transaction?.let {
                                it.confirmations = confirmations
                                db.transactionDao().update(it)
                            }
                        }
                    }
                }
            }
        })
    }


    private fun updateUI(transaction: Transaction) {
        Log.d("CONFIRMATION", transaction.toString())
        val merchAddress = getMerchantAddress()
        if (merchAddress.isNullOrEmpty()) {
            merchantAddress.visibility = TextView.GONE
        } else {
            merchantAddress.text = getString(R.string.merchantAddress, merchAddress)
        }
        merchantName.text = getString(R.string.merchantName, getMerchantName())
        chainTextView.text = getString(R.string.chainText, transaction.chain)
        balanceTextView.text = getString(R.string.balance, transaction.balance)
        baseCurrencyTextView.text = getString(R.string.baseCurrency, transaction.selectedCurrencyCode)
        basePriceTextView.text = getString(R.string.basePrice, transaction.numericPrice)
        dateTextView.text = getString(R.string.dateText, transaction.date)
        txidTextView.text = getString(R.string.txidText, transaction.txid)
        receivingAddress.text = getString(R.string.receivingAddress, transaction.address)
        feesTextView.text = getString(R.string.feesText, transaction.fees)
        txType.text = getString(R.string.txType, transaction.txtype)
        confirmationsTextView.text = getString(R.string.confirmationsText, transaction.confirmations)
        timeTextView.text = getString(R.string.timeText, transaction.time)
        messageTextView.text = if (transaction.message.isNullOrEmpty()) {
            ""
        } else {
            transaction.message
        }

        if (transaction.balanceIn != 0.0) {
            previousBalanceInTextView.visibility = View.VISIBLE
            if (transaction.txtype == "insufficient") {
                previousBalanceInTextView.text = getString(R.string.difference, transaction.balanceIn)
            } else {
                previousBalanceInTextView.text = getString(R.string.previous_receive, transaction.balanceIn)
            }
        } else {
            previousBalanceInTextView.visibility = View.GONE
        }

        if (transaction.txidIn != "") {
            previousTxidInTextView.visibility = View.VISIBLE
            previousTxidInTextView.text = getString(R.string.previousTxidIn, transaction.txidIn)
        } else {
            previousTxidInTextView.visibility = View.GONE
        }
    }

    private fun showReceiptDialog() {
        val receiptDialog = ReceiptDialogFragment()

        // Pass data to the dialog fragment
        val args = Bundle()
        args.putString("receiptTitle", "${getMerchantName()}")
        args.putString("receiptAddress", "${getMerchantAddress()}")
        args.putString("receiptDetails", R.string.transaction_details.toString())
        args.putString("receiptBalance", "${balanceTextView.text}")
        args.putString("receiptTxID", "${txidTextView.text}")
        args.putString("receivingAddress", "${receivingAddress.text}")
        args.putString("receiptFees", "${feesTextView.text}")
        args.putString("receiptConfirmations", "${confirmationsTextView.text}")
        args.putString("receiptChain", "${chainTextView.text}")
        args.putString("receiptDeviceID", "$deviceId")
        args.putString("receiptNumericPrice", "${basePriceTextView.text}")
        args.putString("receiptSelectedCurrencyCode", "${baseCurrencyTextView.text}")
        args.putString("message", "${messageTextView.text}")
        receiptDialog.arguments = args

        receiptDialog.show(supportFragmentManager, "ReceiptDialog")
    }

    fun performPrint() {
        val bluetoothConnection = BluetoothPrintersConnections.selectFirstPaired()
        if (bluetoothConnection == null) {
            Toast.makeText(this, R.string.no_paired_Bluetooth_printer_found, Toast.LENGTH_SHORT).show()
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
                            "[L]<b>${R.string.transaction_details}</b>\n" +
                            "[L]\n" +
                            "[L]${balanceTextView.text}\n" +
                            "[L]${txidTextView.text}\n" +
                            "[L]${feesTextView.text}\n" +
                            "[L]${confirmationsTextView.text}\n" +
                            "[L]${chainTextView.text}\n" +
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
