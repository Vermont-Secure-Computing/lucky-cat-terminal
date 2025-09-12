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
import com.dantsu.escposprinter.exceptions.EscPosConnectionException
import java.io.File
import java.io.FileInputStream
import java.util.Locale
import java.util.Properties

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
    private lateinit var merchantProperties: Properties

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.window?.setBackgroundDrawableResource(android.R.color.darker_gray)
        val view = inflater.inflate(R.layout.fragment_receipt_dialog, container, false)

        // Initialize views
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
        homeButton = view.findViewById(R.id.homeButton)
        receiptLayout = view.findViewById(R.id.receiptLayout)

        // Load merchant properties
        loadMerchantProperties()

        // Get data from arguments
        val args = arguments
        receiptTitle.text = args?.getString("receiptTitle")
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

    private fun loadMerchantProperties() {
        merchantProperties = Properties()
        val file = File(requireContext().filesDir, "merchant.properties")
        if (file.exists()) {
            FileInputStream(file).use {
                merchantProperties.load(it)
            }
        }
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
        receiptLayout.alpha = 1f // Reset alpha
        receiptLayout.translationY = 0f // Reset translation

        homeButton.visibility = View.VISIBLE
        homeButton.setOnClickListener {
            startActivity(Intent(activity, HomeActivity::class.java))
            dismissAllowingStateLoss()
        }

        printAgainButton.visibility = View.VISIBLE
        printAgainButton.setOnClickListener {
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

    private fun getUSString(resourceId: Int, vararg formatArgs: Any): String {
        val config = resources.configuration
        val originalLocale = config.locale
        config.setLocale(Locale.US)
        val localizedContext = requireContext().createConfigurationContext(config)
        val localizedResources = localizedContext.resources
        val result = localizedResources.getString(resourceId, *formatArgs)
        config.setLocale(originalLocale) // Restore original locale
        return result
    }

    private fun performPrintCopies(copies: Int, copyType: String, onComplete: () -> Unit = {}) {
        val bluetoothConnection = BluetoothPrintersConnections.selectFirstPaired()
        if (bluetoothConnection == null) {
            Toast.makeText(activity, R.string.no_paired_Bluetooth_printer_found, Toast.LENGTH_SHORT).show()
            requireContext().showPrintRecoveryDialog(
                title = getString(R.string.printer_not_found),
                message = getString(R.string.no_paired_printer_message),
                onRetry = { performPrintCopies(copies, copyType, onComplete) }
            )
            return
        }

        try {
            val printer = EscPosPrinter(bluetoothConnection, 203, 48f, 32)
            val receiptContent = StringBuilder()

            // Header
            receiptContent.append("[C]<font size='big'>${receiptTitle.text}</font>\n")

            // Optional merchant fields
            val address = merchantProperties.getProperty("address", "")
            val city = merchantProperties.getProperty("city", "")
            val state = merchantProperties.getProperty("state", "")
            val zipCode = merchantProperties.getProperty("zip_code", "")
            val country = merchantProperties.getProperty("country", "")
            val phone = merchantProperties.getProperty("phone", "")
            val email = merchantProperties.getProperty("email", "")

            if (address.isNotEmpty()) receiptContent.append("[L]Address: $address\n")
            if (city.isNotEmpty()) receiptContent.append("[L]City: $city\n")
            if (state.isNotEmpty()) receiptContent.append("[L]State: $state\n")
            if (zipCode.isNotEmpty()) receiptContent.append("[L]Zip Code: $zipCode\n")
            if (country.isNotEmpty()) receiptContent.append("[L]Country: $country\n")
            if (phone.isNotEmpty()) receiptContent.append("[L]Phone: $phone\n")
            if (email.isNotEmpty()) receiptContent.append("[L]Email: $email\n")

            receiptContent.append("[C]-------------------------------\n")
            receiptContent.append("[L]<b>Transaction Details</b>\n")

            // Transaction details
            if (receiptBalance.text.isNotEmpty()) {
                val regex = Regex(".*?[：:\\s]+(\\d+(?:[.,]\\d+)?)")
                val amountValue = regex.find(receiptBalance.text)?.groupValues?.get(1)?.trim() ?: receiptBalance.text
                receiptContent.append("[L]Amount: $amountValue\n")
            }

            if (receiptBaseCurrency.text.isNotEmpty()) {
                val regex = Regex(".*?[：:]\\s*([a-zA-Z0-9]+)")
                val baseCurrencyValue = regex.find(receiptBaseCurrency.text)?.groupValues?.get(1)?.trim() ?: receiptBaseCurrency.text
                receiptContent.append("[L]Base Currency: $baseCurrencyValue\n")
            }

            if (receiptBasePrice.text.isNotEmpty()) {
                val regex = Regex(".*?[：:]?\\s*(\\d*\\.\\d+|\\d+)")
                val basePriceValue = regex.find(receiptBasePrice.text)?.groupValues?.get(1)?.trim() ?: receiptBasePrice.text
                receiptContent.append("[L]Base Price: $basePriceValue\n")
            }

            if (receiptTxID.text.isNotEmpty()) {
                val regex = Regex(".*?[：:]\\s*([a-fA-F0-9]+)")
                val txIDValue = regex.find(receiptTxID.text)?.groupValues?.get(1)?.trim() ?: receiptTxID.text
                receiptContent.append("[L]Transaction ID: $txIDValue\n")
                receiptContent.append("[C]<qrcode size='20'>$cleanedTxid</qrcode>\n")
            }

            if (receivingAddress.text.isNotEmpty()) {
                val regex = Regex(".*?[：:]\\s*([a-zA-Z0-9]+)")
                val addressValue = regex.find(receivingAddress.text)?.groupValues?.get(1)?.trim() ?: receivingAddress.text
                receiptContent.append("[L]Address: $addressValue\n")
            }

            if (receiptFees.text.isNotEmpty()) {
                val regex = Regex(".*?[：:\\s]+(\\d+(?:[.,]\\d+)?)")
                val feesValue = regex.find(receiptFees.text)?.groupValues?.get(1)?.trim() ?: receiptFees.text
                receiptContent.append("[L]Fees: $feesValue\n")
            }

            if (receiptConfirmations.text.isNotEmpty()) {
                val regex = Regex(".*?[：:]\\s*(\\d+)")
                val confirmationsValue = regex.find(receiptConfirmations.text)?.groupValues?.get(1)?.trim() ?: "0"
                receiptContent.append("[L]Confirmations: $confirmationsValue\n")
            }

            if (receiptChain.text.isNotEmpty()) {
                val regex = Regex(".*?[：:]\\s*([a-zA-Z0-9]+)")
                val chainValue = regex.find(receiptChain.text)?.groupValues?.get(1)?.trim() ?: receiptChain.text
                receiptContent.append("[L]${getUSString(R.string.chainText, chainValue)}\n")
            }

            if (receiptDeviceID.text.isNotEmpty()) {
                receiptContent.append("[L]Device ID: ${receiptDeviceID.text}\n")
            }

            val message = arguments?.getString("message") ?: ""
            if (message.isNotEmpty()) {
                receiptContent.append("[L]$message\n")
            }

            receiptContent.append("[C]-------------------------------\n")
            receiptContent.append("[C]$copyType\n")
            receiptContent.append("[L]\n")
            receiptContent.append("[C]Thank you for your payment!\n")

            // IMPORTANT: convert to String
            val escpos = receiptContent.toString()
            for (i in 1..copies) {
                printer.printFormattedText(escpos)
            }

            onComplete()
        } catch (e: EscPosConnectionException) {
            requireContext().showPrintRecoveryDialog(
                title = getString(R.string.printer_issue_title),
                message = getString(R.string.paper_or_jam_message),
                onRetry = { performPrintCopies(copies, copyType, onComplete) }
            )
        } catch (e: Exception) {
            requireContext().showPrintRecoveryDialog(
                title = getString(R.string.printer_issue_title),
                message = e.userFacingMessage(requireContext()),
                onRetry = { performPrintCopies(copies, copyType, onComplete) }
            )
        }
    }
}
