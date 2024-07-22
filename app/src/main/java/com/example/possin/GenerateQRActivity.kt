package com.example.possin

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import android.os.CountDownTimer
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.media.MediaPlayer
import android.print.PrintManager
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.LinearLayout
import androidx.lifecycle.lifecycleScope
import androidx.appcompat.app.AppCompatActivity
import com.example.possin.websocket.CustomWebSocketListener
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.example.possin.database.AppDatabase
import com.example.possin.model.Transaction
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class GenerateQRActivity : AppCompatActivity(), CustomWebSocketListener.PaymentStatusCallback {

    private lateinit var timerTextView: TextView
    private lateinit var countDownTimer: CountDownTimer
    private lateinit var qrCodeImageView: ImageView
    private lateinit var client: OkHttpClient
    private lateinit var webSocket: WebSocket

    private lateinit var balanceTextView: TextView
    private lateinit var txidTextView: TextView
    private lateinit var feesTextView: TextView
    private lateinit var confirmationsTextView: TextView
    private lateinit var confirmationsLayout: LinearLayout
    private lateinit var confirmationBlocks: List<View>
    private lateinit var printButton: Button
    private lateinit var handler: Handler
    private lateinit var chain: String
    private lateinit var txid: String
    private lateinit var message: String

    private lateinit var db: AppDatabase

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.generate_qr)

        db = AppDatabase.getDatabase(this)

        val address = intent.getStringExtra("ADDRESS") ?: "No address provided"
        val price = intent.getStringExtra("PRICE") ?: "No price provided"
        val logoResId = intent.getIntExtra("LOGO_RES_ID", R.drawable.bitcoin_logo)
        val currency = intent.getStringExtra("CURRENCY") ?: "BTC"
        val addressIndex = intent.getIntExtra("ADDRESS_INDEX", -1)
        val feeStatus = intent.getStringExtra("FEE_STATUS") ?: ""
        val status = intent.getStringExtra("STATUS") ?: ""
        val managerType = intent.getStringExtra("MANAGER_TYPE") ?: "Bitcoin"
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        message = intent.getStringExtra("MESSAGE") ?: ""

        val formattedPrice = if (currency == "USDT-TRON") {
            BigDecimal(price).setScale(6, RoundingMode.HALF_UP).toPlainString()
        } else {
            price
        }

        val uri = when (currency) {
            "LTC" -> "litecoin:$address?amount=$formattedPrice"
            "DOGE" -> "dogecoin:$address?amount=$formattedPrice"
            "ETH" -> "ethereum:$address?amount=$formattedPrice"
            "USDT-TRON" -> "tron:$address?amount=$formattedPrice"
            else -> "bitcoin:$address?amount=$formattedPrice"
        }

        val addressTextView: TextView = findViewById(R.id.addressTextView)
        addressTextView.text = "Address: $address\nAmount: $formattedPrice $currency"

        qrCodeImageView = findViewById(R.id.qrCodeImageView)
        val qrCodeBitmap = generateQRCodeWithLogo(uri, logoResId)
        qrCodeImageView.setImageBitmap(qrCodeBitmap)

        // Initialize the timer TextView
        timerTextView = findViewById(R.id.timerTextView)

        // Initialize the new TextViews
        balanceTextView = findViewById(R.id.balanceTextView)
        txidTextView = findViewById(R.id.txidTextView)
        feesTextView = findViewById(R.id.feesTextView)
        confirmationsTextView = findViewById(R.id.confirmationsTextView)
        confirmationsLayout = findViewById(R.id.confirmationsLayout)
        printButton = findViewById(R.id.printButton)
        printButton.setOnClickListener {
            showReceiptDialog(deviceId)
        }

        confirmationBlocks = listOf(
            findViewById(R.id.block1),
            findViewById(R.id.block2),
            findViewById(R.id.block3),
            findViewById(R.id.block4),
            findViewById(R.id.block5),
            findViewById(R.id.block6)
        )

        handler = Handler(Looper.getMainLooper())

        requestBluetoothPermissions()

        // Start the 30-minute countdown timer
        startTimer(10 * 60 * 1000) // 10 minutes in milliseconds

        // Initialize WebSocket connection
        initializeWebSocket(address, formattedPrice, currency, addressIndex, managerType)

        if (status == "paid") {
            saveLastIndex(addressIndex, managerType)
        }

        lifecycleScope.launch {
            db.transactionDao().getAllTransactions().collect { transactions ->
                // Handle collected transactions
                transactions.forEach {
                    Log.d("GenerateQRActivity", "Transaction: $it")
                }
            }
        }

        // Handle cancel click
        val cancelText: TextView = findViewById(R.id.cancelText)
        cancelText.setOnClickListener {
            showCancelDialog()
        }
    }

    private fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT),
                    PERMISSION_REQUEST_CODE
                )
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission granted, proceed with your operation
            } else {
                Toast.makeText(this, "Bluetooth permissions are required for this app", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startTimer(milliseconds: Long) {
        countDownTimer = object : CountDownTimer(milliseconds, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 1000 / 60
                val seconds = millisUntilFinished / 1000 % 60
                timerTextView.text = String.format("%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                // Handle the timer finish event
                timerTextView.text = "00:00"
                // Perform any action when the timer finishes
                // For example, show a dialog or navigate to another screen
            }
        }.start()
    }

    private fun generateQRCodeWithLogo(text: String, logoResId: Int): Bitmap {
        val size = 500
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        val qrCodeBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)

        for (x in 0 until size) {
            for (y in 0 until size) {
                qrCodeBitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }

        // Load the logo
        val logo = BitmapFactory.decodeResource(resources, logoResId)
        val overlaySize = size / 5

        // Combine the QR code and logo
        return overlayBitmap(qrCodeBitmap, logo, overlaySize)
    }

    private fun overlayBitmap(qrCodeBitmap: Bitmap, logo: Bitmap, overlaySize: Int): Bitmap {
        val combined = Bitmap.createBitmap(qrCodeBitmap.width, qrCodeBitmap.height, qrCodeBitmap.config)
        val canvas = Canvas(combined)
        canvas.drawBitmap(qrCodeBitmap, 0f, 0f, null)

        val left = (qrCodeBitmap.width - overlaySize) / 2
        val top = (qrCodeBitmap.height - overlaySize) / 2
        val rect = Rect(left, top, left + overlaySize, top + overlaySize)
        canvas.drawBitmap(logo, null, rect, null)

        return combined
    }


    private fun initializeWebSocket(address: String, amount: String, chain: String, addressIndex: Int, managerType: String) {
        client = OkHttpClient()

        val request = Request.Builder()
            .url("ws://198.7.125.183/ws")
            .build()

        val listener = CustomWebSocketListener("ltc1qp4zm5e0a2s36cw5h5gfr64lzverrxvzn0ef9kg", "0.19469647", chain, "checkBalance", this, addressIndex, managerType)

        webSocket = client.newWebSocket(request, listener)

        client.dispatcher.executorService.shutdown()
    }

    private fun closeWebSocket() {
        webSocket.close(1000, "Payment received")
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel the timer if the activity is destroyed
        countDownTimer.cancel()
    }

    override fun onPaymentStatusPaid(status: String, balance: Long, txid: String, fees: Long, confirmations: Int, feeStatus: String, chain: String, addressIndex: Int, managerType: String) {
        runOnUiThread {
            if (::qrCodeImageView.isInitialized) {
                qrCodeImageView.setImageBitmap(null)
                closeWebSocket()

                // Update the TextViews with the received data
                val formattedBalance = if (chain == "USDT-TRON") {
                    String.format("%.6f", balance.toDouble() / 1000000)
                } else {
                    String.format("%.8f", balance.toDouble() / 100000000)
                }
                balanceTextView.text = "Amount: $formattedBalance"
                balanceTextView.visibility = TextView.VISIBLE
                txidTextView.text = "Transaction ID: $txid"
                txidTextView.visibility = TextView.VISIBLE
                feesTextView.text = "Fees: ${String.format("%.8f", fees.toDouble() / 100000000)}"
                feesTextView.visibility = TextView.VISIBLE
                confirmationsTextView.text = "$confirmations"
                confirmationsTextView.visibility = TextView.VISIBLE
                confirmationsLayout.visibility = View.VISIBLE

                // Save the last index if the status is "paid"
                if (status == "paid") {
                    val mediaPlayer = MediaPlayer.create(this@GenerateQRActivity, R.raw.coins_received)
                    mediaPlayer.start()
                    saveLastIndex(addressIndex, managerType)
                }

                // Store txid and chain for repeated API calls
                this.txid = txid
                this.chain = chain

                // Start repeated API calls
                startRepeatedApiCalls()

                if (feeStatus == "Fee is okay" || confirmations > 0) {
                    saveTransaction(balance, txid, fees, confirmations, chain, message)
                }

                // Show the print button if there is at least one confirmation
                if (confirmations > 0) {
                    printButton.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun saveLastIndex(addressIndex: Int, managerType: String) {
        val sharedPreferences = getSharedPreferences(getPrefsName(managerType), Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putInt(getLastIndexKey(managerType), addressIndex)
            apply()
        }
    }

    private fun getPrefsName(managerType: String): String {
        return when (managerType) {
            "Bitcoin" -> BitcoinManager.PREFS_NAME
            "Litecoin" -> LitecoinManager.PREFS_NAME
            "Ethereum" -> EthereumManager.PREFS_NAME
            "Dogecoin" -> DogecoinManager.PREFS_NAME
            "USDT-Tron" -> TronManager.PREFS_NAME
            else -> BitcoinManager.PREFS_NAME
        }
    }

    private fun getLastIndexKey(managerType: String): String {
        return when (managerType) {
            "Bitcoin" -> BitcoinManager.LAST_INDEX_KEY
            "Litecoin" -> LitecoinManager.LAST_INDEX_KEY
            "Ethereum" -> EthereumManager.LAST_INDEX_KEY
            "Dogecoin" -> DogecoinManager.LAST_INDEX_KEY
            "USDT-Tron" -> TronManager.LAST_INDEX_KEY
            else -> BitcoinManager.LAST_INDEX_KEY
        }
    }

    private fun startRepeatedApiCalls() {
        handler.post(object : Runnable {
            override fun run() {
                getConfirmations(chain, txid)
                handler.postDelayed(this, 60 * 1000) // 1 minute delay
            }
        })
    }

    private fun getConfirmations(chain: String, txid: String) {
        val url = "http://198.7.125.183/terminal/tx_confirmations/$chain/$txid"
        val request = Request.Builder().url(url).build()

        client = OkHttpClient()
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
                            for (i in 0 until confirmations.coerceAtMost(6)) {
                                confirmationBlocks[i].setBackgroundColor(Color.GREEN)
                            }
                        }
                    }
                }
            }
        })
    }

    private fun showReceiptDialog(deviceId: String) {
        val receiptDialog = ReceiptDialogFragment()

        // Pass data to the dialog fragment
        val args = Bundle()
        args.putString("receiptTitle", "RECEIPT")
        args.putString("receiptDetails", "Transaction Details")
        args.putString("receiptBalance", "Balance: ${balanceTextView.text}")
        args.putString("receiptTxID", "TxID: ${txidTextView.text}")
        args.putString("receiptFees", "Fees: ${feesTextView.text}")
        args.putString("receiptConfirmations", "Confirmations: ${confirmationsTextView.text}")
        args.putString("receiptChain", "Chain: ${chain}")
        args.putString("receiptDeviceID", "Device ID: $deviceId")
        receiptDialog.arguments = args

        receiptDialog.show(supportFragmentManager, "ReceiptDialog")
    }

    private fun saveTransaction(balance: Long? = null, txid: String? = null, fees: Long? = null, confirmations: Int = 0, chain: String, message: String? = null) {
        val currentBalance = balance ?: balanceTextView.text.toString().replace("Balance: ", "").toLong()
        val currentTxid = txid ?: txidTextView.text.toString().replace("Transaction ID: ", "")
        val currentFees = fees ?: feesTextView.text.toString().replace("Fees: ", "").toLong()
        val currentConfirmations = confirmationsTextView.text.toString().replace("Confirmations: ", "").toInt()
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        val transaction = Transaction(
            balance = currentBalance,
            txid = currentTxid,
            fees = currentFees,
            confirmations = currentConfirmations,
            date = currentDate,
            time = currentTime,
            chain = chain,
            message = message  // Include the message here
        )

        lifecycleScope.launch(Dispatchers.IO) {
            db.transactionDao().insert(transaction)
        }
    }

    private fun showCancelDialog() {
        val dialogView = layoutInflater.inflate(R.layout.cancel_dialog, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnOkay).setOnClickListener {
            dialog.dismiss()
            navigateToHome()
        }

        dialog.show()
    }

    private fun navigateToHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}
