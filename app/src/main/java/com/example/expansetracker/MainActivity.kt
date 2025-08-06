package com.example.expansetracker

import Transaction
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.expansetracker.Adapter.TransactionAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import androidx.recyclerview.widget.ItemTouchHelper

class MainActivity : AppCompatActivity() {

    private lateinit var tvIncome: TextView
    private lateinit var tvExpense: TextView
    private lateinit var tvTotalBalance: TextView

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TransactionAdapter
    private val transactionList = mutableListOf<Transaction>()

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    var id: String = ""  // Make it mutable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)


        auth = FirebaseAuth.getInstance()
        val email = auth.currentUser?.email
        val userId = auth.currentUser?.uid

        val tvUsername = findViewById<TextView>(R.id.tv_username)
        getAndDisplayUsername(email, tvUsername)

        val btnAddTransaction = findViewById<Button>(R.id.btn_add_transaction)
        btnAddTransaction.setOnClickListener {
            startActivity(Intent(this, AddTransectionActivity::class.java))
        }

        recyclerView = findViewById(R.id.rv_transactions)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = TransactionAdapter(transactionList)
        recyclerView.adapter = adapter

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            loadTransactions()
        }

        tvIncome = findViewById(R.id.tv_income)
        tvExpense = findViewById(R.id.tv_expense)
        tvTotalBalance = findViewById(R.id.tv_total_balance)

        loadTransactions()
        swipeLeftToDelete()

    }

    private fun swipeLeftToDelete() {



        // Import this

        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val transaction = transactionList[position]

                // Firestore delete
                db.collection("transactions").document(transaction.id).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this@MainActivity, "Deleted", Toast.LENGTH_SHORT).show()
                        transactionList.removeAt(position)
                        adapter.notifyItemRemoved(position)

                        // Recalculate totals after delete
                        loadTransactions()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this@MainActivity, "Failed to delete", Toast.LENGTH_SHORT).show()
                        adapter.notifyItemChanged(position)
                    }
            }
        }

        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView)

    }

    private fun getAndDisplayUsername(email: String?, tvUsername: TextView) {
        if (email != null) {
            db.collection("users").document(email).get()
                .addOnSuccessListener { doc ->
                    val name = doc.getString("name")
                    tvUsername.text = name ?: "User"
                }
                .addOnFailureListener {
                    tvUsername.text = "User"
                }
        } else {
            tvUsername.text = "Guest"
        }
    }

    private fun loadTransactions() {
        swipeRefreshLayout.isRefreshing = true
        val userId = auth.currentUser?.uid ?: return

        db.collection("transactions")
            .whereEqualTo("uid", userId)
            .get()
            .addOnSuccessListener { documents ->
                val newTransactionList = mutableListOf<Transaction>()
                var totalIncome = 0.0
                var totalExpense = 0.0

                for (document in documents) {
                    val transaction = document.toObject(Transaction::class.java)
                    if (transaction.type == "Income") {
                        totalIncome += transaction.amount
                    } else if (transaction.type == "Expense") {
                        totalExpense += transaction.amount
                    }
                    transaction.id = document.id
                    newTransactionList.add(transaction)

                }
                val totalBalance = totalIncome - totalExpense

                // Update the UI
                tvIncome.text = "₹ %.2f".format(totalIncome)
                tvExpense.text = "₹ %.2f".format(totalExpense)
                tvTotalBalance.text = "₹ %.2f".format(totalBalance)

                if (totalBalance <= 0) {
                    tvTotalBalance.setTextColor(ContextCompat.getColor(this, R.color.red))
                }

                adapter.setData(newTransactionList)
                swipeRefreshLayout.isRefreshing = false
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load: ${e.message}", Toast.LENGTH_SHORT).show()

                Log.e("MainActivity", "Failed to load transactions: ${e.message}")
                swipeRefreshLayout.isRefreshing = false
            }
    }
}
