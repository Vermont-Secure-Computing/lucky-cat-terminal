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
package com.vermont.possin.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vermont.possin.R
import com.vermont.possin.model.Transaction

class TransactionDividerAdapter(
    private val context: Context,
    private val transactions: List<Transaction>
) : RecyclerView.Adapter<TransactionDividerAdapter.TransactionViewHolder>() {

    override fun getItemCount() = transactions.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction_divider, parent, false)
        return TransactionViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.bind(transaction)
    }

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val chainTextView: TextView = itemView.findViewById(R.id.chainTextView)
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val balanceTextView: TextView = itemView.findViewById(R.id.balanceTextView)

        fun bind(transaction: Transaction) {
            chainTextView.text = transaction.chain
            dateTextView.text = transaction.date
            balanceTextView.text = "${transaction.balance}"
        }
    }
}
