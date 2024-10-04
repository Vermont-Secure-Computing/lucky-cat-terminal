package com.vermont.possin.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import android.widget.Toast
import com.vermont.possin.R
import com.vermont.possin.model.Transaction
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


object CSVExportUtil {

    fun exportTransactionsToCSV(context: Context, transactions: List<Transaction>, baseFileName: String): File? {
        // Get the current date
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val currentDate = dateFormat.format(Date())

        // Create the filename with date
        val fileName = "$baseFileName-$currentDate.csv"

        // Save the file to the Downloads directory
        val csvFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
        Log.d("CSVExport", "File saved at: ${csvFile.absolutePath}")

        try {
            FileWriter(csvFile).use { writer ->
                writer.append("ID,Balance,BalanceIn,TxID,TxIDIn,Fees,Confirmations,Date,Time,Chain,Coin,Message,NumericPrice,SelectedCurrencyCode,Address,TxType\n")
                for (transaction in transactions) {
                    writer.append("${transaction.id},${transaction.balance},${transaction.balanceIn},${transaction.txid},${transaction.txidIn},${transaction.fees},${transaction.confirmations},${transaction.date},${transaction.time},${transaction.chain},${transaction.coin},${transaction.message},${transaction.numericPrice},${transaction.selectedCurrencyCode},${transaction.address},${transaction.txtype}\n")
                }
            }
            // Show success message
            val filePath = csvFile.absolutePath
            val message = context.getString(R.string.file_saved_at, filePath)
            Toast.makeText(context,  message, Toast.LENGTH_LONG).show()
            return csvFile
        } catch (e: IOException) {
            e.printStackTrace()
            // Show error message
            Toast.makeText(context, R.string.failed_to_save_the_CSV_file, Toast.LENGTH_SHORT).show()
        }
        return null
    }
}
