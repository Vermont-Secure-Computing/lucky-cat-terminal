package com.vermont.possin.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vermont.possin.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction)

    @Update
    suspend fun update(transaction: Transaction)

    @Query("SELECT * FROM transactions")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE txid = :txid LIMIT 1")
    suspend fun getTransactionByTxid(txid: String): Transaction?

    @Query("SELECT * FROM transactions WHERE date BETWEEN :fromDate AND :toDate")
    fun getTransactionsByDateRange(fromDate: String, toDate: String): Flow<List<Transaction>>

}