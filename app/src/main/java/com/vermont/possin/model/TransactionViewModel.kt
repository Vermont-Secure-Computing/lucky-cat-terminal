package com.vermont.possin.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.vermont.possin.database.AppDatabase

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val transactionDao = AppDatabase.getDatabase(application).transactionDao()

    val allTransactions: LiveData<List<Transaction>> = transactionDao.getAllTransactions().asLiveData()
}