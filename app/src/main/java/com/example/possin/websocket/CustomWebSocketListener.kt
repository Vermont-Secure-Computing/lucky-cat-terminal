package com.example.possin.websocket

import android.util.Log
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject

class CustomWebSocketListener(
    private val address: String,
    private val amount: String,
    private val chain: String,
    private val checkBalance: String,
    private val callback: PaymentStatusCallback,
    private val addressIndex: Int,
    private val managerType: String
) : WebSocketListener() {

    interface PaymentStatusCallback {
        fun onPaymentStatusPaid(status: String, balance: Long, txid: String, fees: Long, confirmations: Int, feeStatus: String, chain: String, addressIndex: Int, managerType: String)
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        super.onOpen(webSocket, response)
        Log.d("WebSocket", "Connected to WebSocket server")

        // Construct the JSON object with the parameters
        val jsonObject = JSONObject().apply {
            put("address", address)
            put("amount", amount)
            put("chain", chain)
            put("type", checkBalance)
        }

        // Send the JSON object as a string
        webSocket.send(jsonObject.toString())
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d("WebSocket", "Received message: $text")
        // Handle the received message
        val jsonObject = JSONObject(text)
        val status = jsonObject.getString("status")
        if (status == "paid") {
            val balance = jsonObject.getLong("balance")
            val txid = jsonObject.getString("txid")
            val fees = jsonObject.getLong("fees")
            val confirmations = jsonObject.getInt("confirmations")
            val feeStatus = jsonObject.getString("feeStatus")
            callback.onPaymentStatusPaid(status, balance, txid, fees, confirmations, feeStatus, chain, addressIndex, managerType)
        }
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        super.onMessage(webSocket, bytes)
        Log.d("WebSocket", "Received bytes: $bytes")
        // Handle the received bytes
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosing(webSocket, code, reason)
        Log.d("WebSocket", "Closing WebSocket: $code / $reason")
        webSocket.close(1000, null)
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        super.onClosed(webSocket, code, reason)
        Log.d("WebSocket", "Closed WebSocket: $code / $reason")
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        super.onFailure(webSocket, t, response)
        Log.e("WebSocket", "WebSocket error", t)
    }
}