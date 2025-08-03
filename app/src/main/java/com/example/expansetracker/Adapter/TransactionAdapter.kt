package com.example.expansetracker.Adapter

import Transaction
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.expansetracker.R

class TransactionAdapter(
    private val transactionList: MutableList<Transaction>
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val circle: View = itemView.findViewById(R.id.viewCategoryCircle)
        val title: TextView = itemView.findViewById(R.id.tvTitle)
        val date: TextView = itemView.findViewById(R.id.tvDate)
        val amount: TextView = itemView.findViewById(R.id.tvAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactionList[position]

        holder.title.text = transaction.title
        holder.date.text = transaction.date
        holder.amount.text = if (transaction.type.lowercase() == "expense")
            "-₹%.2f".format(transaction.amount)
        else
            "+₹%.2f".format(transaction.amount)

        holder.circle.background.setTint(
            getCategoryColor(holder.itemView.context, transaction.category)
        )
    }

    override fun getItemCount(): Int = transactionList.size

    private fun getCategoryColor(context: Context, category: String): Int {
        return when (category.lowercase()) {
            "food" -> ContextCompat.getColor(context, R.color.green)
            "travel" -> ContextCompat.getColor(context, R.color.yellow)
            "bill" -> ContextCompat.getColor(context, R.color.red)
            "rent" -> ContextCompat.getColor(context, R.color.blue)
            "shopping" -> ContextCompat.getColor(context, R.color.purple)
            else -> ContextCompat.getColor(context, R.color.gray)
        }
    }

    // ✅ Add this method to update data
    fun setData(newList: List<Transaction>) {
        transactionList.clear()
        transactionList.addAll(newList)
        notifyDataSetChanged()
    }
}
