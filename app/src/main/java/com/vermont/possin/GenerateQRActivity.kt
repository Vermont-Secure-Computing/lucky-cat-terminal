package com.vermont.possin

import android.Manifest
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
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.dantsu.escposprinter.exceptions.EscPosConnectionException
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.vermont.possin.database.AppDatabase
import com.vermont.possin.model.Transaction
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

class GenerateQRActivity : BaseNetworkActivity(), CustomWebSocketListener.PaymentStatusCallback {

    // ===== Helpers / constants =====
    private fun toNetworkChain(ticker: String): String = when (ticker.uppercase()) {
        "USDC", "SOL", "SOLANA" -> "SOL"
        "TRON", "TRON-NETWORK", "USDT-TRON" -> "TRON"
        "BTC", "BITCOIN" -> "BTC"
        "BCH", "BITCOINCASH" -> "BCH"
        "LTC", "LITECOIN" -> "LTC"
        "DOGE", "DOGECOIN" -> "DOGE"
        "DASH" -> "DASH"
        "ETH", "ETHEREUM" -> "ETH"
        "XMR", "MONERO" -> "XMR"
        else -> ticker.uppercase()
    }
    private fun normalizeChain(chain: String): String = when (chain.uppercase()) {
        "USDC", "SOLANA", "SOL" -> "SOL"
        "TRON-NETWORK", "USDT-TRON" -> "TRON"
        "BITCOIN" -> "BTC"
        "BITCOINCASH" -> "BCH"
        "LITECOIN" -> "LTC"
        "DOGECOIN" -> "DOGE"
        "ETHEREUM" -> "ETH"
        "MONERO" -> "XMR"
        else -> chain.uppercase()
    }
    private fun inferManagerType(networkChain: String): String = when (networkChain) {
        "SOL" -> "Solana"
        "TRON" -> "Tron-network"
        "ETH" -> "Ethereum"
        "BTC" -> "Bitcoin"
        "BCH" -> "Bitcoincash"
        "LTC" -> "Litecoin"
        "DOGE" -> "Dogecoin"
        "DASH" -> "Dash"
        "XMR" -> "Monero"
        else -> "Bitcoin"
    }
    private fun useSixDecimals(ticker: String, networkChain: String): Boolean {
        val norm = normalizeChain(networkChain)
        return ticker.equals("USDC", true) || norm.equals("TRON", true)
    }

    private fun buildPaymentUri(ticker: String, address: String, amountStr: String): String {
        return when (ticker.uppercase()) {
            "BTC", "BITCOIN" -> "bitcoin:$address?amount=$amountStr"
            "BCH", "BITCOINCASH" -> "bitcoincash:$address?amount=$amountStr"
            "LTC", "LITECOIN" -> "litecoin:$address?amount=$amountStr"
            "DOGE", "DOGECOIN" -> "dogecoin:${urlEncode(address)}?amount=${urlEncode(amountStr)}"
            "DASH" -> "dash:$address?amount=$amountStr"
            "ETH", "ETHEREUM" -> "ethereum:$address?amount=$amountStr"
            "TRON", "TRON-NETWORK", "USDT-TRON" -> "tron:$address?amount=$amountStr"
            "XMR", "MONERO" -> "monero:$address?amount=$amountStr"
            "SOL", "SOLANA" -> "solana:$address?amount=$amountStr"
            "USDC" -> "solana:$address?amount=$amountStr&spl-token=$USDC_SOL_MINT"
            else -> "bitcoin:$address?amount=$amountStr"
        }
    }
    private fun logC(tag: String, msg: String) = Log.d(tag, msg)

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
        private const val USDC_SOL_MINT = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"
    }

    // ===== Views / state =====
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
    private lateinit var wsChain: String

    // Dialog refs to avoid leaks
    private var paymentDialog: AlertDialog? = null
    private var postPaymentDialog: AlertDialog? = null

    private lateinit var websocketParams: WebSocketParams
    data class WebSocketParams(
        val address: String,
        val amount: String,
        val chain: String,
        val addressIndex: Int,
        val managerType: String,
        val txid: String?
    )

    // === Network-aware timer / reconnection ===
    private var remainingMs: Long = 15 * 60 * 1000L
    private var timerRunning = false
    private var lastWebSocketType: String = "checkBalance"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.generate_qr)
        window.statusBarColor = ContextCompat.getColor(this, R.color.tapeRed)

        // Shared banner monitor
        setupNetworkMonitoring(R.id.networkBanner)

        db = AppDatabase.getDatabase(this)

        // --- Inputs / derived network ---
        address = intent.getStringExtra("ADDRESS") ?: getString(R.string.no_address_provided)
        val price = intent.getStringExtra("PRICE") ?: getString(R.string.no_price_provided)
        currency = intent.getStringExtra("CURRENCY") ?: "BTC"
        val networkChain = toNetworkChain(currency)
        chain = networkChain
        wsChain = chainForSocket(currency)

        val logoResId = when (currency) {
            "USDC" -> R.drawable.usdc
            "SOL" -> R.drawable.solana
            else -> intent.getIntExtra("LOGO_RES_ID", R.drawable.bitcoin_logo)
        }
        val addressIndex = intent.getIntExtra("ADDRESS_INDEX", -1)
        val feeStatus = intent.getStringExtra("FEE_STATUS") ?: ""
        val status = intent.getStringExtra("STATUS") ?: ""
        val managerType = intent.getStringExtra("MANAGER_TYPE") ?: inferManagerType(networkChain)

        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        Log.d("RECEIVING ADDRESS", address)

        numericPrice = intent.getStringExtra("NUMERIC_PRICE") ?: ""
        selectedCurrencyCode = intent.getStringExtra("SELECTED_CURRENCY_CODE") ?: ""
        message = intent.getStringExtra("MESSAGE") ?: ""

        val formattedPrice =
            if (useSixDecimals(currency, networkChain)) BigDecimal(price).setScale(6, RoundingMode.HALF_UP).toPlainString()
            else price

        // --- Views ---
        addressTextView = findViewById(R.id.addressTextView)
        addressTextViewAddress = findViewById(R.id.addressTextViewAddress)
        amountBaseCurrency = findViewById(R.id.amountBaseCurrency)
        amountBaseCurrencyPrice = findViewById(R.id.amountBaseCurrencyPrice)
        amountTextViewAddress = findViewById(R.id.amountTextViewAddress)
        amountTextViewAddressChain = findViewById(R.id.amountTextViewAddressChain)
        qrCodeImageView = findViewById(R.id.qrCodeImageView)
        timerTextView = findViewById(R.id.timerTextView)
        gatheringBlocksTextView = findViewById(R.id.gatheringBlocksTextView)
        transactionSeenTextView = findViewById(R.id.transactionSeenTextView)
        checkImageView = findViewById(R.id.checkImageView)
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
        merchantPropertiesFile = File(filesDir, "merchant.properties")

        // --- Static text / initial visibilities ---
        addressTextView.text = getString(R.string.address_colon, address)
        addressTextViewAddress.visibility = View.GONE
        amountBaseCurrency.text = getString(R.string.base_currency_colon, numericPrice, selectedCurrencyCode)
        amountBaseCurrencyPrice.visibility = View.GONE
        amountTextViewAddress.text = getString(R.string.amount_colon, formattedPrice, currency)
        amountTextViewAddressChain.visibility = View.GONE

        // Ensure confirmations UI visible and reset at start
        confirmationsLayout.visibility = View.VISIBLE
        confirmationsTextView.visibility = View.VISIBLE
        confirmationBlocks.forEach { it.setBackgroundColor(Color.LTGRAY) }

        // --- QR code with correct URI ---
        val uri = buildPaymentUri(currency, address, formattedPrice)
        val qrCodeBitmap = generateQRCodeWithLogo(uri, logoResId)
        qrCodeImageView.setImageBitmap(qrCodeBitmap)

        printButton.setOnClickListener {
            handler.removeCallbacksAndMessages(null)
            gatheringBlocksTextView.visibility = View.GONE
            showReceiptDialog(deviceId, numericPrice, selectedCurrencyCode, address)
        }

        requestBluetoothPermissions()
        startTimer() // uses remainingMs

        // WebSocket
        initializeWebSocket(address, formattedPrice, wsChain, addressIndex, managerType)

        if (status == "paid") {
            saveLastIndex(addressIndex, managerType)
        }

        lifecycleScope.launch {
            db.transactionDao().getAllTransactions().collect { transactions ->
                transactions.forEach { Log.d("GenerateQRActivity", "Transaction: $it") }
            }
        }

        cancelText = findViewById(R.id.cancelText)
        cancelText.setOnClickListener { showCancelDialog() }

        homeText = findViewById(R.id.homeText)
        homeText.setOnClickListener { showHomeConfirmationDialog() }
    }

    private fun urlEncode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT),
                    PERMISSION_REQUEST_CODE
                )
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Granted
            } else {
                Toast.makeText(this, R.string.bluetooth_permissions_are_required_for_this_app, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // === Timer: pause/resume-safe ===
    private fun startTimer(durationMs: Long = remainingMs) {
        if (::countDownTimer.isInitialized) countDownTimer.cancel()
        timerRunning = true
        countDownTimer = object : CountDownTimer(durationMs, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMs = millisUntilFinished
                val minutes = millisUntilFinished / 1000 / 60
                val seconds = millisUntilFinished / 1000 % 60
                timerTextView.text = String.format(Locale.US, "%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                timerRunning = false
                timerTextView.text = "00:00"
                if (!paymentReceived && ::webSocket.isInitialized) {
                    webSocket.close(1000, "Time expired without payment")
                    showRetryDialog()
                    connectWebSocket("cancel")
                }
            }
        }.start()
    }

    private fun pauseTimer() {
        if (timerRunning && ::countDownTimer.isInitialized) {
            countDownTimer.cancel()
            timerRunning = false
        }
    }

    private fun resumeTimerIfNeeded() {
        if (!paymentReceived && !timerRunning && remainingMs > 1000L) {
            startTimer(remainingMs)
        }
    }

    private fun chainForSocket(input: String): String {
        val norm = normalizeChain(input)
        return if (norm == "TRON") "TRON-NETWORK" else norm
    }


    private fun showRetryDialog() {
        val dialogView = layoutInflater.inflate(R.layout.retry_dialog, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<Button>(R.id.btnTryAgain).setOnClickListener {
            dialog.dismiss()
            startTimer()
            connectWebSocket("checkBalance")
        }

        dialogView.findViewById<Button>(R.id.btnHome).setOnClickListener {
            dialog.dismiss()
            stopRepeatedApiCallsAndNavigateHome()
        }

        if (!isFinishing && !isDestroyed) dialog.show()
    }

    private fun generateQRCodeWithLogo(text: String, logoResId: Int): Bitmap {
        val size = 500
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        val qrCodeBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) for (y in 0 until size) {
            qrCodeBitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
        }
        val logo = BitmapFactory.decodeResource(resources, logoResId)
        val overlaySize = size / 5
        return overlayBitmap(qrCodeBitmap, logo, overlaySize)
    }

    private fun generateQRCode(text: String): Bitmap {
        val size = 600
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        val qrCodeBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) for (y in 0 until size) {
            qrCodeBitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
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

    private fun initializeWebSocket(address: String, amount: String, chain: String, addressIndex: Int, managerType: String) {
        websocketParams = WebSocketParams(address, amount, chain, addressIndex, managerType, txid = null)
        connectWebSocket("checkBalance")
        checkingTransactionsLayout.visibility = View.VISIBLE
        val gifDrawable = GifDrawable(resources, R.raw.rotating_arc_gradient_thick)
        checkingTransactionsGif.setImageDrawable(gifDrawable)
    }

    private fun connectWebSocket(type: String) {
        lastWebSocketType = type
        val apiKey = ApiKeyStore.get(this) ?: ""
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
                handler.postDelayed(this, 500)
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::countDownTimer.isInitialized) countDownTimer.cancel()
        handler.removeCallbacksAndMessages(null)
        runCatching { paymentDialog?.dismiss() }
        runCatching { postPaymentDialog?.dismiss() }
        paymentDialog = null
        postPaymentDialog = null
    }

    private fun getProperty(key: String): String? {
        val properties = Properties()
        if (merchantPropertiesFile.exists()) {
            properties.load(merchantPropertiesFile.inputStream())
        }
        return properties.getProperty(key)
    }

    fun getMerchantName(): String? = getProperty("merchant_name")
    fun getMechantAddress(): String? = getProperty("address")

    override fun onInsufficientPayment(
        receivedAmt: Double,
        totalAmount: Double,
        difference: Double,
        txid: String,
        fees: Double,
        confirmations: Int,
        addressIndex: Int,
        managerType: String
    ) {
        runOnUiThread {
            previousReceivedAmt = receivedAmt
            initialTxid = txid
            showInsufficientPaymentDialog(
                receivedAmt, totalAmount, difference, txid,
                intent.getIntExtra("LOGO_RES_ID", R.drawable.bitcoin_logo),
                fees, confirmations, addressIndex, managerType
            )
        }
    }

    private fun showInsufficientPaymentDialog(
        receivedAmt: Double,
        totalAmount: Double,
        difference: Double,
        txid: String,
        logoResId: Int,
        fees: Double,
        confirmations: Int,
        addressIndex: Int,
        managerType: String
    ) {
        val dialogView = layoutInflater.inflate(R.layout.insufficient_payment_dialog, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
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
            closeWebSocket(reconnect = false)

            val uri = buildPaymentUri(
                currency,
                address,
                if (useSixDecimals(currency, chain))
                    BigDecimal(difference).setScale(6, RoundingMode.HALF_UP).toPlainString()
                else difference.toString()
            )

            val qrCodeBitmap = generateQRCodeWithLogo(uri, logoResId)
            qrCodeImageView.setImageBitmap(qrCodeBitmap)

            connectWebSocketForRetry(address, difference.toString(), wsChain, txid)
        }

        val coin = intent.getStringExtra("SHORTNAME") ?: "UnknownCoin"

        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            saveTransaction(
                receivedAmt, difference, txid, initialTxid, fees, confirmations,
                chain, coin, message, numericPrice, selectedCurrencyCode, websocketParams.address, "insufficient"
            )
            printReceipt(receivedAmt, totalAmount, difference, txid, fees, confirmations, chain)
            saveLastIndex(addressIndex, managerType)
            dialog.dismiss()
            navigateToHome()
        }

        if (!isFinishing && !isDestroyed) dialog.show()
    }

    private fun connectWebSocketForRetry(address: String, amount: String, chain: String, txid: String) {
        websocketParams = WebSocketParams(address, amount, chain, websocketParams.addressIndex, websocketParams.managerType, txid)
        connectWebSocket("checkBalance")
    }

    override fun onPaymentStatusPaid(
        status: String,
        balance: Double,
        txid: String,
        fees: Double,
        confirmations: Int,
        feeStatus: String,
        chain: String,
        addressIndex: Int,
        managerType: String
    ) {
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

                homeText.visibility = View.VISIBLE

                transactionSeenTextView.visibility = View.VISIBLE
                checkImageView.visibility = View.VISIBLE

                val totalReceivedAmount = previousReceivedAmt + balance

                this.chain = normalizeChain(chain)

                val sixDp = useSixDecimals(currency, this.chain)
                val formattedBalance = if (sixDp) {
                    String.format("%.6f", convertBalance(totalReceivedAmount))
                } else {
                    String.format("%.8f", convertBalance(totalReceivedAmount))
                }
                val formattedFees = if (sixDp) {
                    String.format("%.6f", convertFee(fees))
                } else {
                    String.format("%.8f", convertFee(fees))
                }

                val merchAddress = getMechantAddress()
                merchantAddress.visibility = if (merchAddress.isNullOrEmpty()) View.GONE else View.VISIBLE
                if (!merchAddress.isNullOrEmpty()) merchantAddress.text = merchAddress

                merchantName.text = getMerchantName()
                merchantName.visibility = View.VISIBLE
                balanceTextView.text = getString(R.string.amount, formattedBalance)
                balanceTextView.visibility = View.VISIBLE
                baseCurrencyTextView.text = getString(R.string.baseCurrency, selectedCurrencyCode)
                baseCurrencyTextView.visibility = View.VISIBLE
                basePriceTextView.text = getString(R.string.base_price, numericPrice)
                basePriceTextView.visibility = View.VISIBLE
                if (initialTxid != "") {
                    txidTextView.text = getString(R.string.transaction_ids, initialTxid, txid)
                } else {
                    txidTextView.text = getString(R.string.transaction_id, txid)
                }
                txidTextView.visibility = View.VISIBLE
                feesTextView.text = getString(R.string.fees, formattedFees)
                feesTextView.visibility = View.VISIBLE

                confirmationsLayout.visibility = View.VISIBLE
                confirmationsTextView.visibility = View.VISIBLE
                confirmationBlocks.forEach { it.setBackgroundColor(Color.LTGRAY) }
                confirmationsTextView.text = getString(R.string.confirmations, confirmations)
                for (i in 0 until confirmations.coerceAtMost(6)) {
                    confirmationBlocks[i].setBackgroundColor(Color.GREEN)
                }

                if (status == "paid") {
                    saveLastIndex(addressIndex, managerType)
                    showPaymentReceivedDialog()
                }

                this.txid = txid
                startRepeatedApiCalls()

                val coin = intent.getStringExtra("SHORTNAME") ?: "UnknownCoin"

                if (initialTxid.isNotEmpty()) {
                    saveTransaction(
                        balance, previousReceivedAmt, txid, initialTxid, fees, confirmations,
                        this.chain, coin, message, numericPrice, selectedCurrencyCode, websocketParams.address, "from insufficient"
                    )
                } else {
                    saveTransaction(
                        balance, previousReceivedAmt, txid, initialTxid, fees, confirmations,
                        this.chain, coin, message, numericPrice, selectedCurrencyCode, websocketParams.address, "paid"
                    )
                }

                printButton.visibility = View.VISIBLE
            }
            if (::countDownTimer.isInitialized) countDownTimer.cancel()

            paymentReceived = true

            if (::webSocket.isInitialized) {
                webSocket.close(1000, "Payment received")
            }
        }
    }

    private fun stopPollingAndMaybePromptToPrint() {
        handler.removeCallbacksAndMessages(null)

        if (postPaymentDialog?.isShowing == true) return

        if (printButton.visibility == View.VISIBLE) {
            val dlg = AlertDialog.Builder(this)
                .setTitle(getString(R.string.payment_confirmed))
                .setMessage(getString(R.string.would_you_like_to_print_a_receipt))
                .setPositiveButton(getString(R.string.print)) { _, _ ->
                    printButton.performClick()
                }
                .setNegativeButton(getString(R.string.home)) { _, _ ->
                    navigateToHome()
                }
                .setNeutralButton(getString(R.string.stay)) { d, _ ->
                    d.dismiss()
                }
                .create()
            dlg.setCanceledOnTouchOutside(false)
            dlg.setOnDismissListener { postPaymentDialog = null }
            postPaymentDialog = dlg
            if (!isFinishing && !isDestroyed) dlg.show()
        } else {
            navigateToHome()
        }
    }

    private fun stopRepeatedApiCallsAndNavigateHome() {
        handler.removeCallbacksAndMessages(null)
        paymentDialog?.let { dlg ->
            if (dlg.isShowing) {
                dlg.setOnDismissListener {
                    navigateToHome()
                }
                dlg.dismiss()
                return
            }
        }
        navigateToHome()
    }

    override fun onPaymentError(error: String) {
        runOnUiThread {
            if (::webSocket.isInitialized) {
                webSocket.close(1000, "Payment error")
            }

            if (error.contains("API key has expired", ignoreCase = true)) {
                showExpiredDialog()
                stopRepeatedApiCallsAndNavigateHome()
                return@runOnUiThread
            }

            val dialogView = layoutInflater.inflate(R.layout.payment_error_dialog, null)
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create()

            val messageTextView: TextView = dialogView.findViewById(R.id.messageTextView)
            messageTextView.text = getString(R.string.server_response_error_please_verify_transaction_with_merchant)

            val okButton: Button = dialogView.findViewById(R.id.okButton)
            okButton.setOnClickListener {
                dialog.dismiss()
                navigateToHome()
            }

            if (!isFinishing && !isDestroyed) dialog.show()
        }
    }

    private fun convertFee(fee: Double): Double = when {
        fee < 1_000_000 -> fee
        fee < 100_000_000 -> fee / 1_000_000
        else -> fee / 100_000_000
    }

    private fun convertBalance(balance: Double): Double = when {
        balance < 1_000_000 -> balance
        balance < 100_000_000 -> balance / 1_000_000
        else -> balance / 100_000_000
    }

    private fun saveLastIndex(addressIndex: Int, managerType: String) {
        val sharedPreferences = getSharedPreferences(getPrefsName(managerType), Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putInt(getLastIndexKey(managerType), addressIndex)
            apply()
        }
    }

    private fun getPrefsName(managerType: String): String = when (managerType) {
        "Bitcoin" -> BitcoinManager.PREFS_NAME
        "Litecoin" -> LitecoinManager.PREFS_NAME
        "Ethereum" -> EthereumManager.PREFS_NAME
        "Dogecoin" -> DogecoinManager.PREFS_NAME
        "Tron-network" -> TronManager.PREFS_NAME
        "Dash" -> DashManager.PREFS_NAME
        "Bitcoincash" -> BitcoinManager.PREFS_NAME
        "Monero" -> MoneroManager.PREFS_NAME
        "Solana" -> SolanaManager.PREFS_NAME
        "USDC" -> SolanaManager.PREFS_NAME
        else -> BitcoinManager.PREFS_NAME
    }

    private fun getLastIndexKey(managerType: String): String = when (managerType) {
        "Bitcoin" -> BitcoinManager.LAST_INDEX_KEY
        "Litecoin" -> LitecoinManager.LAST_INDEX_KEY
        "Ethereum" -> EthereumManager.LAST_INDEX_KEY
        "Dogecoin" -> DogecoinManager.LAST_INDEX_KEY
        "Tron-network" -> TronManager.LAST_INDEX_KEY
        "Dash" -> DashManager.LAST_INDEX_KEY
        "Bitcoincash" -> BitcoinCashManager.LAST_INDEX_KEY
        "Monero" -> MoneroManager.LAST_INDEX_KEY
        "Solana" -> SolanaManager.LAST_INDEX_KEY
        "USDC" -> SolanaManager.LAST_INDEX_KEY
        else -> BitcoinManager.LAST_INDEX_KEY
    }

    private fun startRepeatedApiCalls() {
        val delay = if (chain == "BTC" || chain == "BCH") {
            10 * 60 * 1000L
        } else {
            60 * 1000L
        }

        handler.post(object : Runnable {
            override fun run() {
                getConfirmations(chain, txid)
                handler.postDelayed(this, delay)
            }
        })
    }

    private fun getConfirmations(chain: String, txid: String) {
        // Skip polling when not connected
        if (lastObservedNetworkStatus != NetworkStatus.CONNECTED) return

        val norm = normalizeChain(chain)
        val apiChain = if (norm == "TRON") "TRON-NETWORK" else norm

        val url = "https://dogpay.mom/terminal/tx_confirmations/$apiChain/$txid"
        val apiKey = ApiKeyStore.get(this) ?: ""

        logC("CONFIRM_API", "URL=$url")
        logC("CONFIRM_API", "CHAIN_REQ=$norm TXID=$txid")

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .addHeader("x-api-key", apiKey)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    if (response.code == 403) {
                        val raw = response.body?.string().orEmpty()
                        val msg = try {
                            if (raw.isNotEmpty()) JSONObject(raw).optString("message", "") else ""
                        } catch (_: Exception) { "" }
                        if (msg.equals("API key has expired", ignoreCase = true)) {
                            runOnUiThread {
                                showExpiredDialog()
                                stopRepeatedApiCallsAndNavigateHome()
                            }
                            return
                        }
                    }
                    return
                }

                val responseData = response.body?.string()
                responseData?.let {
                    val jsonObject = JSONObject(it)
                    Log.d("JSON CONFIRMATION", jsonObject.toString())
                    val confirmations = jsonObject.optInt("confirmations", 0)

                    runOnUiThread {
                        confirmationsLayout.visibility = View.VISIBLE
                        confirmationsTextView.visibility = View.VISIBLE
                        confirmationBlocks.forEach { block -> block.setBackgroundColor(Color.LTGRAY) }

                        if (norm == "SOL" && confirmations >= 1) {
                            confirmationsTextView.text = "Finalized (MAX confirmations)"
                            confirmationBlocks.forEach { it.setBackgroundColor(Color.GREEN) }
                        } else {
                            confirmationsTextView.text = getString(R.string.confirmations, confirmations)
                            for (i in 0 until confirmations.coerceAtMost(6)) {
                                confirmationBlocks[i].setBackgroundColor(Color.GREEN)
                            }
                        }

                        val terminal = when (norm) {
                            "SOL" -> confirmations >= 1
                            "BTC","BCH","LTC","DOGE","DASH" -> confirmations >= 6
                            "ETH","TRON","XMR" -> confirmations >= 1
                            else -> confirmations >= 1
                        }
                        if (terminal) {
                            stopPollingAndMaybePromptToPrint()
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
        })
    }

    private fun showReceiptDialog(deviceId: String, numericPrice: String, selectedCurrencyCode: String, address: String) {
        val receiptDialog = ReceiptDialogFragment()
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
        args.putString("message", message)
        receiptDialog.arguments = args
        receiptDialog.show(supportFragmentManager, "ReceiptDialog")
    }

    private fun saveTransaction(
        balance: Double? = null,
        balanceIn: Double? = null,
        txid: String? = null,
        txidIn: String? = null,
        fees: Double? = null,
        confirmations: Int = 0,
        chain: String,
        coin: String,
        message: String? = null,
        numericPrice: String,
        selectedCurrencyCode: String,
        address: String,
        txtype: String
    ) {
        val currentBalance = balance ?: balanceTextView.text.toString().replace("Balance: ", "").toDouble()
        val currentTxid = txid ?: txidTextView.text.toString().replace("Transaction ID: ", "")
        val currentFees = fees ?: feesTextView.text.toString().replace("Fees: ", "").toDouble()
        val confirmationsString = confirmationsTextView.text.toString()
        val currentConfirmations = confirmationsString.replace(Regex("[^0-9]"), "").toInt()
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        Log.d("COIN", coin)

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
            coin = coin,
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

        if (!isFinishing && !isDestroyed) dialog.show()
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

        if (!isFinishing && !isDestroyed) dialog.show()
    }

    private fun navigateToHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        when {
            cancelText.visibility == View.VISIBLE -> showCancelDialog()
            homeText.visibility == View.VISIBLE -> showHomeConfirmationDialog()
            else -> super.onBackPressed()
        }
    }

    private fun showPaymentReceivedDialog() {
        if (isFinishing || isDestroyed) return

        val dialogView = layoutInflater.inflate(R.layout.payment_received_dialog, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create().also {
                it.setCanceledOnTouchOutside(false)
            }

        paymentDialog = dialog

        val gifImageView = dialogView.findViewById<GifImageView>(R.id.gifImageView)
        val gifDrawable = GifDrawable(resources, R.raw.green_check_circle_animation_300x300)
        gifImageView.setImageDrawable(gifDrawable)

        val mediaPlayer = MediaPlayer.create(this, R.raw.coins_received)
        dialog.setOnDismissListener {
            runCatching { mediaPlayer.release() }
            paymentDialog = null
        }

        val animMs = (gifDrawable.duration.takeIf { it > 0 } ?: 1200)

        dialog.setOnShowListener {
            runCatching { mediaPlayer.start() }
            gifImageView.postDelayed({
                if (!isFinishing && !isDestroyed && dialog.isShowing) {
                    gifImageView.setImageResource(R.drawable.green_check_circle_animation_300x300)
                    gifImageView.postDelayed({
                        if (!isFinishing && !isDestroyed && dialog.isShowing) {
                            dialog.dismiss()
                        }
                    }, 1000L)
                }
            }, animMs.toLong())
        }

        dialog.show()
    }

    private fun printReceipt(
        receivedAmt: Double,
        totalAmount: Double,
        difference: Double,
        txid: String,
        fees: Double,
        confirmations: Int,
        chain: String
    ) {
        val receiptTitle = getMerchantName() ?: "Merchant Name"
        val receiptAddress = getMechantAddress() ?: "Merchant Address"
        val address = websocketParams.address
        val deviceID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        val bluetoothConnection = BluetoothPrintersConnections.selectFirstPaired()
        if (bluetoothConnection == null) {
            showPrintRecoveryDialog(
                title = getString(R.string.printer_not_found),
                message = getString(R.string.no_paired_printer_message),
                onRetry = { printReceipt(receivedAmt, totalAmount, difference, txid, fees, confirmations, chain) }
            )
            return
        }

        try {
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
        } catch (e: EscPosConnectionException) {
            showPrintRecoveryDialog(
                title = getString(R.string.printer_issue_title),
                message = getString(R.string.paper_or_jam_message),
                onRetry = { printReceipt(receivedAmt, totalAmount, difference, txid, fees, confirmations, chain) }
            )
        } catch (e: Exception) {
            showPrintRecoveryDialog(
                title = getString(R.string.printer_issue_title),
                message = e.userFacingMessage(this),
                onRetry = { printReceipt(receivedAmt, totalAmount, difference, txid, fees, confirmations, chain) }
            )
        }
    }

    private fun showExpiredDialog() {
        val dlg = AlertDialog.Builder(this)
            .setTitle("API Key Expired")
            .setMessage("Your API key has expired. Please renew it on the server to continue.")
            .setCancelable(false)
            .setPositiveButton("OK") { d, _ -> d.dismiss() }
            .create()
        if (!isFinishing && !isDestroyed) dlg.show()
    }

    // === React to network changes from BaseNetworkActivity ===
    override fun onNetworkStatusChanged(status: NetworkStatus) {
        val overlay = findViewById<View>(R.id.offlineOverlay)
        val label = findViewById<TextView?>(R.id.offlineOverlayText)

        when (status) {
            NetworkStatus.CONNECTED -> {
                overlay?.visibility = View.GONE
                printButton.isEnabled = true
                printButton.alpha = 1f
                resumeTimerIfNeeded()
                if (!paymentReceived) {
                    runCatching { if (::webSocket.isInitialized) webSocket.cancel() }
                    connectWebSocket(lastWebSocketType)
                }
            }
            NetworkStatus.LIMITED -> {
                label?.text = getString(R.string.internet_is_unstable_try_again)
                overlay?.visibility = View.VISIBLE
                printButton.isEnabled = false
                printButton.alpha = 0.5f
                pauseTimer()
                runCatching { if (::webSocket.isInitialized) webSocket.close(1001, "Network limited") }
            }
            NetworkStatus.OFFLINE -> {
                label?.text = getString(R.string.no_internet_connection)
                overlay?.visibility = View.VISIBLE
                printButton.isEnabled = false
                printButton.alpha = 0.5f
                pauseTimer()
                runCatching { if (::webSocket.isInitialized) webSocket.close(1001, "Network offline") }
            }
        }
    }
}
