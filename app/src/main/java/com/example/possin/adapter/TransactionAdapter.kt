package com.example.possin.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.possin.R
import com.example.possin.ViewAllActivity
import com.example.possin.model.Transaction

class TransactionAdapter(private val context: Context, private val transactions: List<Transaction>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val ITEM_VIEW_TYPE_TRANSACTION = 0
        private const val ITEM_VIEW_TYPE_VIEW_ALL = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == transactions.size) ITEM_VIEW_TYPE_VIEW_ALL else ITEM_VIEW_TYPE_TRANSACTION
    }

    override fun getItemCount() = transactions.size + 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM_VIEW_TYPE_TRANSACTION) {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_transaction, parent, false)
            TransactionViewHolder(itemView)
        } else {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_view_all, parent, false)
            ViewAllViewHolder(itemView)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType == ITEM_VIEW_TYPE_TRANSACTION) {
            val transaction = transactions[position]
            (holder as TransactionViewHolder).bind(transaction)
        } else {
            (holder as ViewAllViewHolder).bind()
        }
    }

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val chainTextView: TextView = itemView.findViewById(R.id.chainTextView)
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val balanceTextView: TextView = itemView.findViewById(R.id.balanceTextView)

        fun bind(transaction: Transaction) {
            chainTextView.text = transaction.chain
            dateTextView.text = transaction.date
            balanceTextView.text = "Amount: ${transaction.balance}"
        }
    }

    inner class ViewAllViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val viewAllButton: Button = itemView.findViewById(R.id.viewAllButton)

        fun bind() {
            viewAllButton.setOnClickListener {
                val intent = Intent(context, ViewAllActivity::class.java)
                context.startActivity(intent)
            }
        }
    }
}