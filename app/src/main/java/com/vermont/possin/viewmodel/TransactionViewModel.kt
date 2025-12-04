/*
 * Copyright 2024–2025 Vermont Secure Computing and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
http://www.apache.org/licenses/LICENSE-2.0

 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
