package com.example.possin

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.possin.database.AppDatabase
import com.example.possin.model.Transaction
import com.example.possin.utils.PropertiesUtil
import com.example.possin.websocket.CustomWebSocketListener
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import org.json.JSONObject
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GenerateQRActivity : AppCompatActivity(), CustomWebSocketListener.PaymentStatusCallback {

    private lateinit var timerTextView: TextView
    private lateinit var gatheringBlocksTextView: TextView
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
    private lateinit var cancelText: TextView

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

        Log.d("CURRENCY", currency)
        Log.d("PRICE", price)


        val formattedPrice = if (currency == "TRON") {
            BigDecimal(price).setScale(6, RoundingMode.HALF_UP).toPlainString()
        } else {
            price
        }

        Log.d("DogeAddress", address)
        Log.d("DogeChain", currency)
        Log.d("Price", formattedPrice)

        val uri = when (currency) {
            "LTC" -> "litecoin:$address?amount=$formattedPrice"
            "DOGE" -> "dogecoin:${urlEncode(address)}?amount=${urlEncode(formattedPrice)}"
            "ETH" -> "ethereum:$address?amount=$formattedPrice"
            "TRON" -> "tron:$address?amount=$formattedPrice"
            else -> "bitcoin:$address?amount=$formattedPrice"
        }

        val addressTextView: TextView = findViewById(R.id.addressTextView)
        addressTextView.text = "Address: $address\nAmount: $formattedPrice $currency"

        qrCodeImageView = findViewById(R.id.qrCodeImageView)
        val qrCodeBitmap = generateQRCodeWithLogo(uri, logoResId)
//        val qrCodeBitmap = generateQRCode(uri)
        qrCodeImageView.setImageBitmap(qrCodeBitmap)

        // Initialize the timer TextView
        timerTextView = findViewById(R.id.timerTextView)

        // Initialize the gathering blocks TextView
        gatheringBlocksTextView = findViewById(R.id.gatheringBlocksTextView)

        // Initialize the new TextViews
        balanceTextView = findViewById(R.id.balanceTextView)
        txidTextView = findViewById(R.id.txidTextView)
        feesTextView = findViewById(R.id.feesTextView)
        confirmationsTextView = findViewById(R.id.confirmationsTextView)
        confirmationsLayout = findViewById(R.id.confirmationsLayout)
        printButton = findViewById(R.id.printButton)
        printButton.setOnClickListener {
            handler.removeCallbacksAndMessages(null)
            gatheringBlocksTextView.visibility = View.GONE
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
        startTimer(30 * 60 * 1000) // 10 minutes in milliseconds

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
        cancelText = findViewById(R.id.cancelText)
        cancelText.setOnClickListener {
            showCancelDialog()
        }
    }

    private fun urlEncode(value: String): String {
        return URLEncoder.encode(value, "UTF-8")
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

    private fun generateQRCode(text: String): Bitmap {
        val size = 600
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        val qrCodeBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)

        for (x in 0 until size) {
            for (y in 0 until size) {
                qrCodeBitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        return qrCodeBitmap
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


    private lateinit var websocketParams: WebSocketParams

    data class WebSocketParams(
        val address: String,
        val amount: String,
        val chain: String,
        val addressIndex: Int,
        val managerType: String
    )

    private fun initializeWebSocket(address: String, amount: String, chain: String, addressIndex: Int, managerType: String) {
        websocketParams = WebSocketParams(address, amount, chain, addressIndex, managerType)
        connectWebSocket("checkBalance")
    }

    private fun connectWebSocket(type: String) {
        Log.d("WebSocket body", websocketParams.address)
        Log.d("WebSocket body", websocketParams.amount)
        Log.d("WebSocket body", websocketParams.chain)
        Log.d("WebSocket body", websocketParams.addressIndex.toString())
        Log.d("WebSocket body", websocketParams.managerType)

        val apiKey = PropertiesUtil.getProperty(this, "api_key")
        Log.d("API", apiKey.toString())

        client = OkHttpClient()

        val request = Request.Builder()
            .url("wss://dogpay.mom/ws?apiKey=$apiKey")
            .build()

        val listener = CustomWebSocketListener(
            websocketParams.address,
            websocketParams.amount,
            websocketParams.chain,
            type,
            this,
            websocketParams.addressIndex,
            websocketParams.managerType
        )

        webSocket = client.newWebSocket(request, listener)
    }

    private fun closeWebSocket() {
        webSocket.close(1000, "Payment received")
        cancelText.visibility = View.GONE
        timerTextView.visibility = View.GONE
        gatheringBlocksTextView.visibility = View.VISIBLE
        startGatheringBlocksAnimation()
        connectWebSocket("cancel")
    }

    private fun startGatheringBlocksAnimation() {
        val initialText = "Gathering Blocks"
        val dots = listOf("", ".", "..", "...")
        var dotIndex = 0
        handler.post(object : Runnable {
            override fun run() {
                gatheringBlocksTextView.text = "$initialText${dots[dotIndex]}"
                dotIndex = (dotIndex + 1) % dots.size
                handler.postDelayed(this, 500) // Change dots every 500ms
            }
        })
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
                Log.d("BALANCE", balance.toString())
                Log.d("FEES", fees.toString())

                // Update the TextViews with the received data
                // Convert balance and fees to their correct values
                val formattedBalance = if (chain == "TRON") {
                    String.format("%.6f", balance.toDouble() / 1_000_000)
                } else {
                    String.format("%.8f", balance.toDouble() / 100_000_000)
                }

                val formattedFees = if (chain == "TRON") {
                    String.format("%.6f", fees.toDouble() / 1_000_000)
                } else {
                    String.format("%.8f", fees.toDouble() / 100_000_000)
                }

                // Update the TextViews with the received data
                balanceTextView.text = "Amount: $formattedBalance"
                balanceTextView.visibility = TextView.VISIBLE
                txidTextView.text = "Transaction ID: $txid"
                txidTextView.visibility = TextView.VISIBLE
                feesTextView.text = "Fees: $formattedFees"
                feesTextView.visibility = TextView.VISIBLE
                confirmationsTextView.text = "Confirmations: $confirmations"
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
                    printButton.visibility = View.VISIBLE
                }

                // Show the print button if there is at least one confirmation
//                if (confirmations > 0) {
//                    printButton.visibility = View.VISIBLE
//                }

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
