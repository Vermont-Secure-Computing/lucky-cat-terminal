        package com.vermont.possin

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
        import com.dantsu.escposprinter.EscPosPrinter
        import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
        import com.google.zxing.BarcodeFormat
        import com.google.zxing.MultiFormatWriter
        import com.google.zxing.common.BitMatrix
        import com.vermont.possin.database.AppDatabase
        import com.vermont.possin.model.Transaction
        import com.vermont.possin.utils.PropertiesUtil
        import com.vermont.possin.websocket.CustomWebSocketListener
        import kotlinx.coroutines.Dispatchers
        import kotlinx.coroutines.launch
        import okhttp3.Call
        import okhttp3.Callback
        import okhttp3.OkHttpClient
        import okhttp3.Request
        import okhttp3.Response
        import okhttp3.WebSocket
        import org.json.JSONObject
        import pl.droidsonroids.gif.GifDrawable
        import pl.droidsonroids.gif.GifImageView
        import java.io.File
        import java.io.IOException
        import java.math.BigDecimal
        import java.math.RoundingMode
        import java.net.URLEncoder
        import java.text.SimpleDateFormat
        import java.util.Date
        import java.util.Locale
        import java.util.Properties

        class GenerateQRActivity : AppCompatActivity(), CustomWebSocketListener.PaymentStatusCallback {

            private lateinit var timerTextView: TextView
            private lateinit var gatheringBlocksTextView: TextView
            private lateinit var transactionSeenTextView: TextView
            private lateinit var checkImageView: ImageView
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
            private lateinit var baseCurrencyTextView: TextView
            private lateinit var basePriceTextView: TextView
            private lateinit var merchantName: TextView
            private lateinit var merchantAddress: TextView
            private lateinit var merchantPropertiesFile: File

            private lateinit var numericPrice: String
            private lateinit var selectedCurrencyCode: String
            private lateinit var homeText: TextView

            private var previousReceivedAmt: Double = 0.0
            private var initialTxid: String = ""
            private lateinit var address: String
            private lateinit var currency: String

            private lateinit var checkingTransactionsLayout: LinearLayout
            private lateinit var checkingTransactionsGif: ImageView
            private lateinit var checkingTransactionsText: TextView
            private lateinit var addressTextView: TextView
            private lateinit var addressTextViewAddress: TextView
            private lateinit var amountBaseCurrency: TextView
            private lateinit var amountBaseCurrencyPrice: TextView
            private lateinit var amountTextViewAddress: TextView
            private lateinit var amountTextViewAddressChain: TextView
            private var paymentReceived = false

            private lateinit var db: AppDatabase

            companion object {
                private const val PERMISSION_REQUEST_CODE = 1
            }

            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContentView(R.layout.generate_qr)

                window.statusBarColor = ContextCompat.getColor(this, R.color.tapeRed)

                db = AppDatabase.getDatabase(this)

                address = intent.getStringExtra("ADDRESS") ?: getString(R.string.no_address_provided)
                val price = intent.getStringExtra("PRICE") ?: getString(R.string.no_price_provided)
                val logoResId = intent.getIntExtra("LOGO_RES_ID", R.drawable.bitcoin_logo)
                currency = intent.getStringExtra("CURRENCY") ?: "BTC"
                chain = currency
                val addressIndex = intent.getIntExtra("ADDRESS_INDEX", -1)
                val feeStatus = intent.getStringExtra("FEE_STATUS") ?: ""
                val status = intent.getStringExtra("STATUS") ?: ""
                val managerType = intent.getStringExtra("MANAGER_TYPE") ?: "Bitcoin"

                val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                Log.d("RECEIVING ADDRESS", address)

                numericPrice = intent.getStringExtra("NUMERIC_PRICE") ?: ""
                selectedCurrencyCode = intent.getStringExtra("SELECTED_CURRENCY_CODE") ?: ""

                message = intent.getStringExtra("MESSAGE") ?: ""


                val formattedPrice = if (currency == "TRON") {
                    BigDecimal(price).setScale(6, RoundingMode.HALF_UP).toPlainString()
                } else {
                    price
                }


                val uri = when (currency) {
                    "LTC" -> "litecoin:$address?amount=$formattedPrice"
                    "DOGE" -> "dogecoin:${urlEncode(address)}?amount=${urlEncode(formattedPrice)}"
                    "ETH" -> "ethereum:$address?amount=$formattedPrice"
                    "TRON-NETWORK" -> "tron:$address?amount=$formattedPrice"
                    "DASH" -> "dash:$address?amount=$formattedPrice"
                    "BCH" -> "bitcoincash:$address?amount=$formattedPrice"
                    "XMR" -> "monero:$address?amount=$formattedPrice"
                    else -> "bitcoin:$address?amount=$formattedPrice"
                }

                addressTextView = findViewById(R.id.addressTextView)
                addressTextViewAddress = findViewById(R.id.addressTextViewAddress)
                amountBaseCurrency = findViewById(R.id.amountBaseCurrency)
                amountBaseCurrencyPrice = findViewById(R.id.amountBaseCurrencyPrice)
                amountTextViewAddress = findViewById(R.id.amountTextViewAddress)
                amountTextViewAddressChain = findViewById(R.id.amountTextViewAddressChain)
                addressTextView.text = getString(R.string.address_colon, address)
                addressTextViewAddress.visibility = View.GONE
                amountBaseCurrency.text = getString(R.string.base_currency_colon, numericPrice, selectedCurrencyCode)
                amountBaseCurrencyPrice.visibility = View.GONE
                amountTextViewAddress.text = getString(R.string.amount_colon, formattedPrice, currency)
                amountTextViewAddressChain.visibility = View.GONE

                qrCodeImageView = findViewById(R.id.qrCodeImageView)
                val qrCodeBitmap = generateQRCodeWithLogo(uri, logoResId)
        //        val qrCodeBitmap = generateQRCode(uri)
                qrCodeImageView.setImageBitmap(qrCodeBitmap)

                // Initialize the timer TextView
                timerTextView = findViewById(R.id.timerTextView)

                // Initialize the gathering blocks TextView
                gatheringBlocksTextView = findViewById(R.id.gatheringBlocksTextView)
                transactionSeenTextView = findViewById(R.id.transactionSeenTextView)
                checkImageView = findViewById(R.id.checkImageView)

                merchantPropertiesFile = File(filesDir, "merchant.properties")

                // Initialize the new TextViews
                balanceTextView = findViewById(R.id.balanceTextView)
                txidTextView = findViewById(R.id.txidTextView)
                feesTextView = findViewById(R.id.feesTextView)
                confirmationsTextView = findViewById(R.id.confirmationsTextView)
                confirmationsLayout = findViewById(R.id.confirmationsLayout)
                printButton = findViewById(R.id.printButton)
                baseCurrencyTextView = findViewById(R.id.baseCurrencyTextView)
                basePriceTextView = findViewById(R.id.basePriceTextView)
                merchantAddress = findViewById(R.id.merchantAddress)
                merchantName = findViewById(R.id.merchantName)
                printButton.setOnClickListener {
                    handler.removeCallbacksAndMessages(null)
                    gatheringBlocksTextView.visibility = View.GONE
                    showReceiptDialog(deviceId, numericPrice, selectedCurrencyCode, address)
                }

                // Initialize the new views for the GIF and text
                checkingTransactionsLayout = findViewById(R.id.checkingTransactionsLayout)
                checkingTransactionsGif = findViewById(R.id.checkingTransactionsGif)
                checkingTransactionsText = findViewById(R.id.checkingTransactionsText)

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
                startTimer() // 10 minutes in milliseconds

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
                // Handle home click
                homeText = findViewById(R.id.homeText)
                homeText.setOnClickListener {
                    showHomeConfirmationDialog()
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
                        Toast.makeText(this, R.string.bluetooth_permissions_are_required_for_this_app, Toast.LENGTH_SHORT).show()
                    }
                }
            }

            private fun startTimer() {
                val milliseconds = 15 * 60 * 1000L
                countDownTimer = object : CountDownTimer(milliseconds, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        val minutes = millisUntilFinished / 1000 / 60
                        val seconds = millisUntilFinished / 1000 % 60
                        timerTextView.text = String.format(Locale.US, "%02d:%02d", minutes, seconds)
                    }

                    override fun onFinish() {
                        // Timer has ended
                        timerTextView.text = "00:00"

                        // Close WebSocket if it's open
                        if (!paymentReceived && ::webSocket.isInitialized) {
                            webSocket.close(1000, "Time expired without payment")
                            showRetryDialog() // Show retry dialog
                            connectWebSocket("cancel") // Cancel the WebSocket session
                        }
                    }
                }.start()
            }

            private fun showRetryDialog() {
                val dialogView = layoutInflater.inflate(R.layout.retry_dialog, null)
                val dialog = AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setCancelable(false) // Make the dialog modal
                    .create()

                // Set up the "Try Again" button
                dialogView.findViewById<Button>(R.id.btnTryAgain).setOnClickListener {
                    dialog.dismiss()
                    startTimer()
                    connectWebSocket("checkBalance")
                }

                // Set up the "Go Home" button
                dialogView.findViewById<Button>(R.id.btnHome).setOnClickListener {
                    dialog.dismiss()
                    stopRepeatedApiCallsAndNavigateHome()
                }

                dialog.show()
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
                val managerType: String,
                val txid: String?
            )

            private fun initializeWebSocket(address: String, amount: String, chain: String, addressIndex: Int, managerType: String) {
                websocketParams = WebSocketParams(address, amount, chain, addressIndex, managerType, txid = null)
                connectWebSocket("checkBalance")

                checkingTransactionsLayout.visibility = View.VISIBLE
                val gifDrawable = GifDrawable(resources, R.raw.rotating_arc_gradient_thick)
                checkingTransactionsGif.setImageDrawable(gifDrawable)
            }

            private fun connectWebSocket(type: String) {
                val apiKey = PropertiesUtil.getProperty(this, "api_key")

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
                    websocketParams.managerType,
                    websocketParams.txid
                )

                webSocket = client.newWebSocket(request, listener)

            }

            private fun closeWebSocket(reconnect: Boolean = true) {
                if (::webSocket.isInitialized) {
                    webSocket.close(1000, "Payment received")
                }

                if (reconnect) {
                    cancelText.visibility = View.GONE
                    timerTextView.visibility = View.GONE
                    gatheringBlocksTextView.visibility = View.VISIBLE
                    startGatheringBlocksAnimation()
                    connectWebSocket("cancel")
                }
            }


            private fun startGatheringBlocksAnimation() {
                val initialText = getString(R.string.gathering_blocks)
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

            override fun onInsufficientPayment(receivedAmt: Double, totalAmount: Double, difference: Double, txid: String, fees: Double, confirmations: Int, addressIndex: Int, managerType: String) {
                runOnUiThread {
                    previousReceivedAmt = receivedAmt
                    initialTxid = txid
                    showInsufficientPaymentDialog(receivedAmt, totalAmount, difference, txid, intent.getIntExtra("LOGO_RES_ID", R.drawable.bitcoin_logo), fees, confirmations, addressIndex, managerType)
                }
            }

            private fun showInsufficientPaymentDialog(receivedAmt: Double, totalAmount: Double, difference: Double, txid: String, logoResId: Int, fees: Double, confirmations: Int, addressIndex: Int, managerType: String) {
                val dialogView = layoutInflater.inflate(R.layout.insufficient_payment_dialog, null)
                val dialog = AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setCancelable(false) // Make the dialog modal
                    .create()
                cancelText.visibility = View.GONE

                val paidAmountTextView: TextView = dialogView.findViewById(R.id.receivedAmt)
                val requiredAmountTextView: TextView = dialogView.findViewById(R.id.difference)
                val totalAmountTextView: TextView = dialogView.findViewById(R.id.totalAmount)
                val TXIDTextView: TextView = dialogView.findViewById(R.id.txidTextView)

                paidAmountTextView.text = getString(R.string.paid_amount, receivedAmt)
                requiredAmountTextView.text = getString(R.string.required_amount, difference)
                totalAmountTextView.text = getString(R.string.total_amount, totalAmount)
                TXIDTextView.text = getString(R.string.TXID, txid)

                val amountTextViewAddress: TextView = findViewById(R.id.amountTextViewAddress)
                amountTextViewAddress.text = getString(R.string.received, difference, currency)

                dialogView.findViewById<Button>(R.id.btnRetry).setOnClickListener {
                    dialog.dismiss()
                    // Close the previous WebSocket connection without reconnecting with "cancel"
                    closeWebSocket(reconnect = false)

                    // Generate new QR code with the difference amount
                    val uri = when (currency) {
                        "LTC" -> "litecoin:$address?amount=$difference"
                        "DOGE" -> "dogecoin:$address?amount=$difference"
                        "ETH" -> "ethereum:$address?amount=$difference"
                        "TRON-NETWORK" -> "tron:$address?amount=$difference"
                        "DASH" -> "dash:$address?amount=$difference"
                        "BCH" -> "bitcoincash:$address?amount=$difference"
                        "XMR" -> "monero:$address?amount=$difference"
                        else -> "bitcoin:$address?amount=$difference"
                    }

                    val qrCodeBitmap = generateQRCodeWithLogo(uri, logoResId)
                    qrCodeImageView.setImageBitmap(qrCodeBitmap)

                    // Reconnect WebSocket with new parameters
                    connectWebSocketForRetry(address, difference.toString(), currency, txid)
                }

                dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
                    saveTransaction(receivedAmt, difference,  txid, initialTxid, fees, confirmations, chain, message, numericPrice, selectedCurrencyCode, websocketParams.address, "insufficient")
                    printReceipt(receivedAmt, totalAmount, difference, txid, fees, confirmations, chain)
                    saveLastIndex(addressIndex, managerType)
                    dialog.dismiss()
                    navigateToHome()
                }

                dialog.show()
            }


            private fun connectWebSocketForRetry(address: String, amount: String, chain: String, txid: String) {
                println("connect txid: $txid")
                websocketParams = WebSocketParams(address, amount, chain, websocketParams.addressIndex, websocketParams.managerType, txid)
                connectWebSocket("checkBalance")
            }


            override fun onPaymentStatusPaid(status: String, balance: Double, txid: String, fees: Double, confirmations: Int, feeStatus: String, chain: String, addressIndex: Int, managerType: String) {
                runOnUiThread {
                    if (::qrCodeImageView.isInitialized) {
                        qrCodeImageView.setImageBitmap(null)
                        closeWebSocket()

                        addressTextView.visibility = View.GONE
                        addressTextViewAddress.visibility = View.GONE
                        amountTextViewAddress.visibility = View.GONE
                        amountTextViewAddressChain.visibility = View.GONE
                        amountBaseCurrency.visibility = View.GONE
                        amountBaseCurrencyPrice.visibility = View.GONE

                        checkingTransactionsLayout.visibility = View.GONE

                        // Show the home button
                        homeText.visibility = View.VISIBLE

                        transactionSeenTextView.visibility = View.VISIBLE
                        checkImageView.visibility = View.VISIBLE

                        // Accumulate received amount
                        val totalReceivedAmount = previousReceivedAmt + balance

                        // Update the TextViews with the received data
                        // Convert balance and fees to their correct values
                        val formattedBalance = if (chain == "TRON") {
                            String.format("%.6f", convertBalance(totalReceivedAmount))
                        } else {
                            String.format("%.8f", convertBalance(totalReceivedAmount))
                        }

                        val formattedFees = if (chain == "TRON") {
                            String.format("%.6f", convertFee(fees))
                        } else {
                            String.format("%.8f", convertFee(fees))
                        }

                        val merchAddress = getMechantAddress()

                        if (merchAddress.isNullOrEmpty()) {
                            merchantAddress.visibility = TextView.GONE
                        } else {
                            merchantAddress.text = merchAddress
                            merchantAddress.visibility = TextView.VISIBLE
                        }

                        // Update the TextViews with the received data
                        merchantName.text = getMerchantName()
                        merchantName.visibility = TextView.VISIBLE
                        balanceTextView.text = getString(R.string.amount, formattedBalance)
                        balanceTextView.visibility = TextView.VISIBLE
                        baseCurrencyTextView.text = getString(R.string.baseCurrency, selectedCurrencyCode)
                        baseCurrencyTextView.visibility = TextView.VISIBLE
                        basePriceTextView.text = getString(R.string.base_price, numericPrice)
                        basePriceTextView.visibility = TextView.VISIBLE
                        if (initialTxid != "") {
                            txidTextView.text = getString(R.string.transaction_ids, initialTxid, txid)
                        } else {
                            txidTextView.text = getString(R.string.transaction_id, txid)
                        }
                        txidTextView.visibility = TextView.VISIBLE
                        feesTextView.text = getString(R.string.fees, formattedFees)
                        feesTextView.visibility = TextView.VISIBLE
                        confirmationsTextView.text = getString(R.string.confirmations, confirmations)
                        confirmationsTextView.visibility = TextView.VISIBLE
                        confirmationsLayout.visibility = View.VISIBLE

                        // Save the last index if the status is "paid"
                        if (status == "paid") {
                            saveLastIndex(addressIndex, managerType)
                            showPaymentReceivedDialog()
                        }

                        // Store txid and chain for repeated API calls
                        this.txid = txid
                        this.chain = chain

                        startRepeatedApiCalls()

                        if (initialTxid.isNotEmpty()) {
                            saveTransaction(balance, previousReceivedAmt,  txid, initialTxid, fees, confirmations, chain, message, numericPrice, selectedCurrencyCode, websocketParams.address, "from insufficient")
                        } else {
                            saveTransaction(balance, previousReceivedAmt,  txid, initialTxid, fees, confirmations, chain, message, numericPrice, selectedCurrencyCode, websocketParams.address, "paid")
                        }

                        printButton.visibility = View.VISIBLE
                    }
                    if (::countDownTimer.isInitialized) {
                        countDownTimer.cancel()
                    }

                    paymentReceived = true

                    if (::webSocket.isInitialized) {
                        webSocket.close(1000, "Payment received")
                    }
                }
            }

            private fun stopRepeatedApiCallsAndNavigateHome() {
                handler.removeCallbacksAndMessages(null) // Stop repeated API calls
                navigateToHome() // Redirect to HomeActivity
            }

            override fun onPaymentError(error: String) {
                runOnUiThread {
                    // Close the WebSocket connection
                    if (::webSocket.isInitialized) {
                        webSocket.close(1000, "Payment error")
                    }

                    // Create and show an AlertDialog with the error message and OK button
                    val dialogView = layoutInflater.inflate(R.layout.payment_error_dialog, null)
                    val dialog = AlertDialog.Builder(this)
                        .setView(dialogView)
                        .setCancelable(false) // Prevent dismissing the dialog by clicking outside
                        .create()

                    val messageTextView: TextView = dialogView.findViewById(R.id.messageTextView)
                    messageTextView.text = getString(R.string.server_response_error_please_verify_transaction_with_merchant)

                    val okButton: Button = dialogView.findViewById(R.id.okButton)
                    okButton.setOnClickListener {
                        dialog.dismiss()
                        navigateToHome() // Redirect to HomeActivity
                    }

                    dialog.show()
                }
            }

            private fun convertFee(fee: Double): Double {
                return when {
                    fee < 1_000_000 -> fee
                    fee < 100_000_000 -> fee / 1_000_000
                    else -> fee / 100_000_000
                }
            }

            private fun convertBalance(balance: Double): Double {
                return when {
                    balance < 1_000_000 -> balance
                    balance < 100_000_000 -> balance / 1_000_000
                    else -> balance / 100_000_000
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
                    "Tron-network" -> TronManager.PREFS_NAME
                    "Dash" -> DashManager.PREFS_NAME
                    "Bitcoincash" -> BitcoinManager.PREFS_NAME
                    "Monero" -> MoneroManager.PREFS_NAME
                    else -> BitcoinManager.PREFS_NAME
                }
            }

            private fun getLastIndexKey(managerType: String): String {
                return when (managerType) {
                    "Bitcoin" -> BitcoinManager.LAST_INDEX_KEY
                    "Litecoin" -> LitecoinManager.LAST_INDEX_KEY
                    "Ethereum" -> EthereumManager.LAST_INDEX_KEY
                    "Dogecoin" -> DogecoinManager.LAST_INDEX_KEY
                    "Tron-network" -> TronManager.LAST_INDEX_KEY
                    "Dash" -> DashManager.LAST_INDEX_KEY
                    "Bitcoincash" -> BitcoinCashManager.LAST_INDEX_KEY
                    "Monero" -> MoneroManager.LAST_INDEX_KEY
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
                                Log.d("JSON CONFIRMATION", jsonObject.toString())
                                val confirmations = jsonObject.getInt("confirmations")

                                runOnUiThread {
                                    confirmationsTextView.text = getString(R.string.confirmations, confirmations)
                                    for (i in 0 until confirmations.coerceAtMost(6)) {
                                        confirmationBlocks[i].setBackgroundColor(Color.GREEN)
                                    }

                                    if (confirmations >= 10) {
                                        // Stop the repeated API calls
                                        stopRepeatedApiCallsAndNavigateHome()
                                    }
                                }

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

            private fun showReceiptDialog(deviceId: String, numericPrice: String, selectedCurrencyCode: String, address: String) {
                val receiptDialog = ReceiptDialogFragment()

                // Pass data to the dialog fragment
                val args = Bundle()
                args.putString("receiptTitle", "${merchantName.text}")
                args.putString("receiptAddress", "${merchantAddress.text}")
                args.putString("receiptDetails", getString(R.string.transaction_details))
                args.putString("receiptBalance", "${balanceTextView.text}")
                args.putString("receiptTxID", "${txidTextView.text}")
                args.putString("receiptFees", "${feesTextView.text}")
                args.putString("receiptConfirmations", "${confirmationsTextView.text}")
                args.putString("receiptChain", chain)
                args.putString("receiptDeviceID", deviceId)
                args.putString("receiptNumericPrice", getString(R.string.base_price, numericPrice))
                args.putString("receiptSelectedCurrencyCode", getString(R.string.base_currency, selectedCurrencyCode))
                args.putString("receivingAddress", getString(R.string.receivingAddress, address))

                receiptDialog.arguments = args

                receiptDialog.show(supportFragmentManager, "ReceiptDialog")
            }

            private fun saveTransaction(balance: Double? = null, balanceIn: Double? = null, txid: String? = null, txidIn: String? = null, fees: Double? = null, confirmations: Int = 0, chain: String, message: String? = null, numericPrice: String, selectedCurrencyCode: String, address: String, txtype: String) {
                val currentBalance = balance ?: balanceTextView.text.toString().replace("Balance: ", "").toDouble()
                val currentTxid = txid ?: txidTextView.text.toString().replace("Transaction ID: ", "")
                val currentFees = fees ?: feesTextView.text.toString().replace("Fees: ", "").toDouble()
                val confirmationsString = confirmationsTextView.text.toString()
                val currentConfirmations = confirmationsString.replace(Regex("[^0-9]"), "").toInt()
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

                val transaction = Transaction(
                    balance = currentBalance,
                    balanceIn = balanceIn,
                    txid = currentTxid,
                    txidIn = txidIn,
                    fees = currentFees,
                    confirmations = currentConfirmations,
                    date = currentDate,
                    time = currentTime,
                    chain = chain,
                    message = message,
                    numericPrice = numericPrice,
                    selectedCurrencyCode = selectedCurrencyCode,
                    address = address,
                    txtype = txtype
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
                    closeWebSocket()
                }

                dialog.show()
            }

            private fun showHomeConfirmationDialog() {
                val dialogView = layoutInflater.inflate(R.layout.home_cancel_dialog, null)
                val dialog = AlertDialog.Builder(this)
                    .setView(dialogView)
                    .create()

                dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
                    dialog.dismiss()
                }

                dialogView.findViewById<Button>(R.id.btnOkay).setOnClickListener {
                    dialog.dismiss()
                    stopRepeatedApiCallsAndNavigateHome()
                }

                dialog.show()
            }

            private fun navigateToHome() {
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }

            private fun showPaymentReceivedDialog() {
                if (!isFinishing && !isDestroyed) {
                    val dialogView = layoutInflater.inflate(R.layout.payment_received_dialog, null)
                    val dialog = AlertDialog.Builder(this)
                        .setView(dialogView)
                        .create()

                    val gifImageView = dialogView.findViewById<GifImageView>(R.id.gifImageView)
                    val gifDrawable = GifDrawable(resources, R.raw.green_check_circle_animation_300x300)

                    // Set the ImageView size to make it smaller
                    val layoutParams = gifImageView.layoutParams
                    layoutParams.width = 200 // Set width to 200 pixels
                    layoutParams.height = 200 // Set height to 200 pixels
                    gifImageView.layoutParams = layoutParams

                    gifImageView.setImageDrawable(gifDrawable)

                    val mediaPlayer = MediaPlayer.create(this, R.raw.coins_received)
                    mediaPlayer.start()

                    // Get the duration of the GIF animation
                    val gifDuration = gifDrawable.duration

                    // Dismiss the dialog and release the media player after the animation completes
                    handler.postDelayed({
                        if (!isFinishing && !isDestroyed) {
                            // Replace GIF with PNG image
                            gifImageView.setImageResource(R.drawable.green_check_circle_animation_300x300)

                            // Show the dialog for an additional 2 seconds
                            handler.postDelayed({
                                dialog.dismiss()
                                mediaPlayer.release()
                            }, 1800) // 2 seconds
                        }
                    }, gifDuration.toLong())

                    dialog.show()
                }
            }



            private fun printReceipt(receivedAmt: Double, totalAmount: Double, difference: Double, txid: String, fees: Double, confirmations: Int, chain: String) {
                val receiptTitle = getMerchantName() ?: "Merchant Name"
                val receiptAddress = getMechantAddress() ?: "Merchant Address"
                val address = websocketParams.address
                val deviceID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

                val bluetoothConnection = BluetoothPrintersConnections.selectFirstPaired()
                if (bluetoothConnection == null) {
                    Toast.makeText(this, "No paired Bluetooth printer found", Toast.LENGTH_SHORT).show()
                    return
                }

                val printer = EscPosPrinter(bluetoothConnection, 203, 48f, 32)

                printer.printFormattedText(
                    "[C]<font size='big'>$receiptTitle</font>\n" +
                            "[C]$receiptAddress\n" +
                            "[L]\n" +
                            "[C]-------------------------------\n" +
                            "[L]\n" +
                            "[L]<b>Insufficient Payment</b>\n" +
                            "[L]\n" +
                            "[L]Received: $receivedAmt\n" +
                            "[L]Total: $totalAmount\n" +
                            "[L]Difference: $difference\n" +
                            "[L]TXID: $txid\n" +
                            "[L]Address: $address\n" +
                            "[L]Fees: $fees\n" +
                            "[L]Confirmations: $confirmations\n" +
                            "[L]Chain: $chain\n" +
                            "[L]Device ID: $deviceID\n" +
                            "[L]Time: $time\n" +
                            "[L]Date: $date\n" +
                            "[L]\n" +
                            "[C]-------------------------------\n" +
                            "[L]\n" +
                            "[C]Thank you for your payment!\n"
                )
            }

        }
