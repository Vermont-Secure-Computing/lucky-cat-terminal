package com.example.possin
import android.os.CountDownTimer
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.possin.websocket.CustomWebSocketListener
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket

class GenerateQRActivity : AppCompatActivity() {

    private lateinit var timerTextView: TextView
    private lateinit var countDownTimer: CountDownTimer
    private lateinit var client: OkHttpClient
    private lateinit var webSocket: WebSocket
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.generate_qr)

        val address = intent.getStringExtra("ADDRESS") ?: "No address provided"
        val price = intent.getStringExtra("PRICE") ?: "No price provided"
        val logoResId = intent.getIntExtra("LOGO_RES_ID", R.drawable.bitcoin_logo)
        val currency = intent.getStringExtra("CURRENCY") ?: "BTC"

        val uri = when (currency) {
            "LTC" -> "litecoin:$address?amount=$price"
            else -> "bitcoin:$address?amount=$price"
        }

        val addressTextView: TextView = findViewById(R.id.addressTextView)
        addressTextView.text = "Address: $address\nAmount: $price $currency"

        val qrCodeImageView: ImageView = findViewById(R.id.qrCodeImageView)
        val qrCodeBitmap = generateQRCodeWithLogo(uri, logoResId)
        qrCodeImageView.setImageBitmap(qrCodeBitmap)

        // Initialize the timer TextView
        timerTextView = findViewById(R.id.timerTextView)

        // Start the 30-minute countdown timer
        startTimer(1 * 60 * 1000) // 1 minute in milliseconds

        // Initialize WebSocket connection
        initializeWebSocket(address, price, currency)
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
            .url("ws://10.0.2.2:3200/ws") // Replace with your WebSocket server URL
            .build()

        val listener = CustomWebSocketListener(address, amount, chain)

        webSocket = client.newWebSocket(request, listener)

        client.dispatcher.executorService.shutdown()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel the timer if the activity is destroyed
        countDownTimer.cancel()
    }
}