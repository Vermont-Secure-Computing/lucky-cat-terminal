package com.vermont.possin

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import com.dantsu.escposprinter.exceptions.EscPosBarcodeException
import com.dantsu.escposprinter.exceptions.EscPosConnectionException
import com.dantsu.escposprinter.exceptions.EscPosEncodingException
import com.dantsu.escposprinter.exceptions.EscPosParserException

fun Context.showPrintRecoveryDialog(
    title: String,
    message: String,
    onRetry: () -> Unit
) {
    AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(message)
        .setCancelable(false)
        .setPositiveButton(getString(R.string.retry)) { d, _ -> d.dismiss(); onRetry() }
        .setNegativeButton(getString(R.string.open_bluetooth_settings)) { d, _ ->
            d.dismiss()
            startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))
        }
        .show()
}

fun Throwable.userFacingMessage(ctx: Context): String = when (this) {
    is EscPosConnectionException -> ctx.getString(R.string.connection_error_message)
    is EscPosParserException     -> ctx.getString(R.string.parser_error_message)
    is EscPosEncodingException   -> ctx.getString(R.string.encoding_error_message)
    is EscPosBarcodeException    -> ctx.getString(R.string.barcode_qr_error_message)
    else -> localizedMessage ?: "Unknown print error"
}
