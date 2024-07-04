package com.example.possin

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import android.os.CountDownTimer
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.print.PrintManager
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.possin.websocket.CustomWebSocketListener
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

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

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.generate_qr)

        val address = intent.getStringExtra("ADDRESS") ?: "No address provided"
        val price = intent.getStringExtra("PRICE") ?: "No price provided"
        val logoResId = intent.getIntExtra("LOGO_RES_ID", R.drawable.bitcoin_logo)
        val currency = intent.getStringExtra("CURRENCY") ?: "BTC"

        val uri = when (currency) {
            "LTC" -> "litecoin:$address?amount=$price"
            "DOGE" -> "dogecoin:$address?amount=$price"
            else -> "bitcoin:$address?amount=$price"
        }

        val addressTextView: TextView = findViewById(R.id.addressTextView)
        addressTextView.text = "Address: $address\nAmount: $price $currency"

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
        printButton.setOnClickListener { printReceipt() }

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
        startTimer(10 * 60 * 1000) // 1 minute in milliseconds

        // Initialize WebSocket connection
        initializeWebSocket(address, price, currency)
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

    private fun overlayBitmap(qrBitmap: Bitmap, logo: Bitmap, overlaySize: Int): Bitmap {
        val combined = Bitmap.createBitmap(qrBitmap.width, qrBitmap.height, qrBitmap.config)
        val canvas = Canvas(combined)
        canvas.drawBitmap(qrBitmap, 0f, 0f, null)

        val left = (qrBitmap.width - overlaySize) / 2
        val top = (qrBitmap.height - overlaySize) / 2
        val rect = Rect(left, top, left + overlaySize, top + overlaySize)
        canvas.drawBitmap(logo, null, rect, null)

        return combined
    }

    private fun initializeWebSocket(address: String, amount: String, chain: String) {
        client = OkHttpClient()

        val request = Request.Builder()
//            .url("ws://10.0.2.2:3200/ws") // Replace with your WebSocket server URL
//            .url("ws://localhost:3200/ws") // Replace with your WebSocket server URL
            .url("ws://198.7.125.183/ws")
            .build()

        val listener = CustomWebSocketListener("address", "amount", chain, "checkBalance", this)

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

    override fun onPaymentStatusPaid(balance: Long, txid: String, fees: Long, confirmations: Int, chain: String) {
        runOnUiThread {
            if (::qrCodeImageView.isInitialized) {
                qrCodeImageView.setImageBitmap(null)
                closeWebSocket()

                // Update the TextViews with the received data
                balanceTextView.text = "Balance: $balance"
                balanceTextView.visibility = TextView.VISIBLE
                txidTextView.text = "Transaction ID: $txid"
                txidTextView.visibility = TextView.VISIBLE
                feesTextView.text = "Fees: $fees"
                feesTextView.visibility = TextView.VISIBLE
                confirmationsTextView.text = "Confirmations: 0"
                confirmationsTextView.visibility = TextView.VISIBLE
                confirmationsLayout.visibility = View.VISIBLE


                // Store txid and chain for repeated API calls
                this.txid = txid
                this.chain = chain

                // Start repeated API calls
                startRepeatedApiCalls()

                // Show the print button if there is at least one confirmation
                if (confirmations > 0) {
                    printButton.visibility = View.VISIBLE
                }
            }
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
//        val url = "http://10.0.2.2:3200/terminal/tx_confirmations/$chain/$txid"
//        val url = "http://localhost:3200/terminal/tx_confirmations/$chain/$txid"
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

    private fun printReceipt() {
        val bluetoothConnection = BluetoothPrintersConnections.selectFirstPaired()
        if (bluetoothConnection == null) {
            Toast.makeText(this, "No paired Bluetooth printer found", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val printer = EscPosPrinter(bluetoothConnection, 203, 48f, 32)
            printer.printFormattedText(
                "[C]<u><font size='big'>RECEIPT</font></u>\n" +
                        "[L]\n" +
                        "[L]Balance: ${balanceTextView.text}\n" +
                        "[L]Transaction ID: ${txidTextView.text}\n" +
                        "[L]Fees: ${feesTextView.text}\n" +
                        "[L]Confirmations: ${confirmationsTextView.text}\n" +
                        "[L]\n" +
                        "[C]-------------------------------\n" +
                        "[L]\n" +
                        "[C]<qrcode size='20'>${txidTextView.text}</qrcode>\n"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle printing errors
        }
    }

    private fun getReceiptContent(): String {
        return """
            Receipt
            ---------
            Balance: ${balanceTextView.text}
            Transaction ID: ${txidTextView.text}
            Fees: ${feesTextView.text}
            Confirmations: ${confirmationsTextView.text}
        """.trimIndent()
    }
}