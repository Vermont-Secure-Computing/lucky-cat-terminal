package com.example.possin.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.example.possin.database.AppDatabase
import com.example.possin.model.Transaction

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val transactionDao = AppDatabase.getDatabase(application).transactionDao()

    val allTransactions: LiveData<List<Transaction>> = transactionDao.getAllTransactions().asLiveData()
}