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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.expansetracker.Adapter.TransactionAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TransactionAdapter
    private val transactionList = mutableListOf<Transaction>()

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

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

        loadTransactions()
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

                for (document in documents) {
                    val transaction = document.toObject(Transaction::class.java)
                    newTransactionList.add(transaction)
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
