package com.example.possin

import android.os.Bundle
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.possin.adapter.ViewAllTransactionAdapter
import com.example.possin.model.Transaction
import com.example.possin.model.TransactionViewModel

class ViewAllActivity : AppCompatActivity() {

    private val transactionViewModel: TransactionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_all)

        window.statusBarColor = ContextCompat.getColor(this, R.color.tapeRed)

        // Set up back arrow
        val backArrow = findViewById<ImageView>(R.id.back_arrow)
        backArrow.setOnClickListener {
            finish()
        }

        // Set up RecyclerView
        val transactionsRecyclerView = findViewById<RecyclerView>(R.id.transactionsRecyclerView)
        transactionsRecyclerView.layoutManager = LinearLayoutManager(this)

        // Add divider item decoration
//        val dividerItemDecoration = DividerItemDecoration(
//            transactionsRecyclerView.context,
//            (transactionsRecyclerView.layoutManager as LinearLayoutManager).orientation
//        )
//        transactionsRecyclerView.addItemDecoration(dividerItemDecoration)

        transactionViewModel.allTransactions.observe(this, Observer { transactions ->
            transactions?.let {
                val sortedTransactions = it.sortedWith(
                    compareByDescending<Transaction> { it.date }
                        .thenByDescending { it.time }
                )
                val adapter = ViewAllTransactionAdapter(sortedTransactions)
                transactionsRecyclerView.adapter = adapter
            }
        })
    }
}
