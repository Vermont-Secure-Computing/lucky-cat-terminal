package com.example.possin.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val balance: Double,
    val balanceIn: Double? = null,
    val txid: String,
    val txidIn: String? = null,
    val fees: Double,
    val confirmations: Int,
    val date: String,
    val time: String,
    val chain: String,
    val message: String? = null,
    val numericPrice: String,
    val selectedCurrencyCode: String,
    val address: String,
    val txtype: String
) : Parcelable