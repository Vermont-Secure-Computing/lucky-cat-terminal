package com.vermont.possin.viewmodel

import com.vermont.possin.utils.CSVExportUtil
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vermont.possin.dao.TransactionDao
import kotlinx.coroutines.launch


class TransactionViewModel(private val transactionDao: TransactionDao) : ViewModel() {

    fun exportTransactions(context: Context, fromDate: String, toDate: String, fileName: String) {
        viewModelScope.launch {
            transactionDao.getTransactionsByDateRange(fromDate, toDate).collect { transactions ->
                val csvFile = CSVExportUtil.exportTransactionsToCSV(context, transactions, fileName)
                // Handle the result, e.g., notify the user or share the file
            }
        }
    }
}
