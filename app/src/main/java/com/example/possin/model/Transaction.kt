package com.example.possin.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val balance: Long,
    val txid: String,
    val fees: Long,
    val confirmations: Int,
    val date: String,
    val time: String,
    val chain: String,
    val message: String? = null  // Added message field
) : Parcelable