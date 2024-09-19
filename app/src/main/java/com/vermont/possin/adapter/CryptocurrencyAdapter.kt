package com.vermont.possin.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vermont.possin.R
import com.vermont.possin.Cryptocurrency

class CryptocurrencyAdapter(
    private val context: Context,
    private val cryptocurrencies: List<Cryptocurrency>,
    private val clickListener: (Cryptocurrency) -> Unit
) : RecyclerView.Adapter<CryptocurrencyAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val logo: ImageView = itemView.findViewById(R.id.logo)
        val name: TextView = itemView.findViewById(R.id.name)
        val shortname: TextView = itemView.findViewById(R.id.shortname)
        val chain: TextView = itemView.findViewById(R.id.chain)

        fun bind(cryptocurrency: Cryptocurrency, clickListener: (Cryptocurrency) -> Unit) {
            name.text = cryptocurrency.name
            shortname.text = cryptocurrency.shortname
            chain.text = cryptocurrency.chain
            val resourceId = context.resources.getIdentifier(cryptocurrency.logo, "drawable", context.packageName)
            if (resourceId != 0) {
                logo.setImageResource(resourceId)
            } else {
                logo.setImageResource(R.drawable.logo) // A default logo if not found
            }

            itemView.setOnClickListener { clickListener(cryptocurrency) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.list_item_cryptocurrency, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(cryptocurrencies[position], clickListener)
    }

    override fun getItemCount(): Int {
        return cryptocurrencies.size
    }
}
