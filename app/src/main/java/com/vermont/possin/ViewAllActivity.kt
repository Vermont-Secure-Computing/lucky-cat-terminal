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
package com.vermont.possin

import android.os.Bundle
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vermont.possin.adapter.ViewAllTransactionAdapter
import com.vermont.possin.model.Transaction
import com.vermont.possin.model.TransactionViewModel

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
