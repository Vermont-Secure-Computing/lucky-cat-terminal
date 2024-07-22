package com.example.possin

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections

class ReceiptDialogFragment : DialogFragment() {

    private lateinit var receiptTitle: TextView
    private lateinit var receiptDetails: TextView
    private lateinit var receiptBalance: TextView
    private lateinit var receiptTxID: TextView
    private lateinit var receiptFees: TextView
    private lateinit var receiptConfirmations: TextView
    private lateinit var receiptChain: TextView
    private lateinit var receiptDeviceID: TextView
    private lateinit var printAgainButton: Button
    private lateinit var receiptLayout: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.window?.setBackgroundDrawableResource(android.R.color.darker_gray)
        val view = inflater.inflate(R.layout.fragment_receipt_dialog, container, false)

        receiptTitle = view.findViewById(R.id.receiptTitle)
        receiptDetails = view.findViewById(R.id.receiptDetails)
        receiptBalance = view.findViewById(R.id.receiptBalance)
        receiptTxID = view.findViewById(R.id.receiptTxID)
        receiptFees = view.findViewById(R.id.receiptFees)
        receiptConfirmations = view.findViewById(R.id.receiptConfirmations)
        receiptChain = view.findViewById(R.id.receiptChain)
        receiptDeviceID = view.findViewById(R.id.receiptDeviceID)
        printAgainButton = view.findViewById(R.id.printAgainButton)
        printAgainButton.visibility = View.GONE
        receiptLayout = view.findViewById(R.id.receiptLayout)

        // Get data from arguments
        val args = arguments
        receiptTitle.text = args?.getString("receiptTitle")
        receiptDetails.text = args?.getString("receiptDetails")
        receiptBalance.text = args?.getString("receiptBalance")
        receiptTxID.text = args?.getString("receiptTxID")
        receiptFees.text = args?.getString("receiptFees")
        receiptConfirmations.text = args?.getString("receiptConfirmations")
        receiptChain.text = args?.getString("receiptChain")
        receiptDeviceID.text = args?.getString("receiptDeviceID")

        // Print the first copy (Customer Copy)
        performPrintCopies(1, "Customer Copy")

        // Start the first animation
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
        receiptTitle.text = "Owner's Copy"
        receiptLayout.alpha = 1f // Reset alpha
        receiptLayout.translationY = 0f // Reset translation

        // Show the print again button
        printAgainButton.visibility = View.VISIBLE
        printAgainButton.setOnClickListener {
            performPrintCopies(1, "Owner's Copy")
            printAgainButton.visibility = View.GONE // Hide the print again button
            startPrintAnimation(receiptLayout, ::dismiss)
        }
    }

    private fun performPrintCopies(copies: Int, copyType: String) {
        val bluetoothConnection = BluetoothPrintersConnections.selectFirstPaired()
        if (bluetoothConnection == null) {
            Toast.makeText(activity, "No paired Bluetooth printer found", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val printer = EscPosPrinter(bluetoothConnection, 203, 48f, 32)
            for (i in 1..copies) {
                printer.printFormattedText(
                    "[C]<u><font size='big'>RECEIPT</font></u>\n" +
                            "[L]\n" +
                            "[C]-------------------------------\n" +
                            "[L]\n" +
                            "[L]<b>Transaction Details</b>\n" +
                            "[L]\n" +
                            "[L]${receiptBalance.text}\n" +
                            "[L]${receiptTxID.text}\n" +
                            "[L]${receiptFees.text}\n" +
                            "[L]${receiptConfirmations.text}\n" +
                            "[L]Chain: ${receiptChain.text}\n" +
                            "[L]Device ID: ${receiptDeviceID.text}\n" +
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
