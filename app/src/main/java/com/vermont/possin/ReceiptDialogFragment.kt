package com.vermont.possin

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections


class ReceiptDialogFragment : DialogFragment() {

    private lateinit var receiptTitle: TextView
    private lateinit var receiptAddress: TextView
    private lateinit var receiptDetails: TextView
    private lateinit var receiptBalance: TextView
    private lateinit var receiptBaseCurrency: TextView
    private lateinit var receiptBasePrice: TextView
    private lateinit var receiptTxID: TextView
    private lateinit var receivingAddress: TextView
    private lateinit var receiptFees: TextView
    private lateinit var receiptConfirmations: TextView
    private lateinit var receiptChain: TextView
    private lateinit var receiptDeviceID: TextView
    private lateinit var printAgainButton: Button
    private lateinit var homeButton: Button
    private lateinit var receiptLayout: LinearLayout
    private var cleanedTxid: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.window?.setBackgroundDrawableResource(android.R.color.darker_gray)
        val view = inflater.inflate(R.layout.fragment_receipt_dialog, container, false)

        receiptTitle = view.findViewById(R.id.receiptTitle)
        receiptAddress = view.findViewById(R.id.receiptAddress)
        receiptDetails = view.findViewById(R.id.receiptDetails)
        receiptBalance = view.findViewById(R.id.receiptBalance)
        receiptBaseCurrency = view.findViewById(R.id.receiptBaseCurrency)
        receiptBasePrice = view.findViewById(R.id.receiptBasePrice)
        receiptTxID = view.findViewById(R.id.receiptTxID)
        receivingAddress = view.findViewById(R.id.receivingAddress)
        receiptFees = view.findViewById(R.id.receiptFees)
        receiptConfirmations = view.findViewById(R.id.receiptConfirmations)
        receiptChain = view.findViewById(R.id.receiptChain)
        receiptDeviceID = view.findViewById(R.id.receiptDeviceID)
        printAgainButton = view.findViewById(R.id.printAgainButton)
        printAgainButton.visibility = View.GONE
        homeButton = view.findViewById(R.id.homeButton)
        homeButton.visibility = View.GONE
        receiptLayout = view.findViewById(R.id.receiptLayout)

        // Get data from arguments
        val args = arguments
        receiptTitle.text = args?.getString("receiptTitle")
        receiptAddress.text = args?.getString("receiptAddress")
        receiptDetails.text = args?.getString("receiptDetails")
        receiptBalance.text = args?.getString("receiptBalance")
        receiptBaseCurrency.text = args?.getString("receiptSelectedCurrencyCode")
        receiptBasePrice.text = args?.getString("receiptNumericPrice")
        receiptTxID.text = args?.getString("receiptTxID")
        receiptFees.text = args?.getString("receiptFees")
        receiptConfirmations.text = args?.getString("receiptConfirmations")
        receiptChain.text = args?.getString("receiptChain")
        receiptDeviceID.text = args?.getString("receiptDeviceID")
        receivingAddress.text = args?.getString("receivingAddress")
        val receiptTxID1 = args?.getString("receiptTxID") ?: ""
        cleanedTxid = receiptTxID1.replace(Regex(".*\\s"), "")


        performPrintCopies(1, "Customer Copy")


        startPrintAnimation(receiptLayout, ::onFirstAnimationEnd)

        return view
    }

    override fun onResume() {
        super.onResume()
        val width = ViewGroup.LayoutParams.MATCH_PARENT
        val height = ViewGroup.LayoutParams.MATCH_PARENT
        dialog?.window?.setLayout(width, height)
    }

    private fun startPrintAnimation(view: View, onAnimationEnd: () -> Unit) {
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()
        val middleOfScreen = screenHeight / 2
        val translationY = ObjectAnimator.ofFloat(view, "translationY", middleOfScreen, -screenHeight)
        val fadeOut = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(translationY, fadeOut)
        animatorSet.duration = 2000
        animatorSet.start()

        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                onAnimationEnd()
            }
        })
    }

    private fun onFirstAnimationEnd() {
        // Show the Owner's Copy
//        receiptTitle.text = "Owner's Copy"
        receiptLayout.alpha = 1f // Reset alpha
        receiptLayout.translationY = 0f // Reset translation

        // Show the home again button
        homeButton.visibility = View.VISIBLE
        homeButton.setOnClickListener {
            startActivity(Intent(activity, HomeActivity::class.java))
            dismissAllowingStateLoss()
        }

        // Show the print again button
        printAgainButton.visibility = View.VISIBLE
        printAgainButton.setOnClickListener {
            // Delay the printing for 1 second
            Handler(Looper.getMainLooper()).postDelayed({
                performPrintCopies(1, "Owner's Copy")
            }, 1050)
            startPrintAnimation(receiptLayout) {
                Handler(Looper.getMainLooper()).postDelayed({
                    startActivity(Intent(activity, HomeActivity::class.java))
                    dismissAllowingStateLoss()
                }, 2000)
            }
        }
    }


    private fun performPrintCopies(copies: Int, copyType: String, onComplete: () -> Unit = {}) {
        val bluetoothConnection = BluetoothPrintersConnections.selectFirstPaired()
        if (bluetoothConnection == null) {
            Toast.makeText(activity, "No paired Bluetooth printer found", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val printer = EscPosPrinter(bluetoothConnection, 203, 48f, 32)
            for (i in 1..copies) {
                printer.printFormattedText(
                    "[C]<font size='big'>${receiptTitle.text}</font>\n" +
                            "[C]${receiptAddress.text}\n" +
                            "[L]\n" +
                            "[C]-------------------------------\n" +
                            "[L]\n" +
                            "[L]<b>Transaction Details</b>\n" +
                            "[L]\n" +
                            "[L]${receiptBalance.text}\n" +
                            "[L]${receiptBaseCurrency.text}\n" +
                            "[L]${receiptBasePrice.text}\n" +
                            "[L]${receiptTxID.text}\n" +
                            "[C]<qrcode size='20'>$cleanedTxid</qrcode>\n" +
                            "[L]${receivingAddress.text}\n" +
                            "[L]${receiptFees.text}\n" +
                            "[L]${receiptConfirmations.text}\n" +
                            "[L]${receiptChain.text}\n" +
                            "[L]Device ID: ${receiptDeviceID.text}\n" +
                            "[L]\n" +
                            "[C]-------------------------------\n" +
                            "[C]$copyType\n" +
                            "[L]\n" +
                            "[C]Thank you for your payment!\n"
                )
            }
            onComplete()
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle printing errors
        }
    }


}
