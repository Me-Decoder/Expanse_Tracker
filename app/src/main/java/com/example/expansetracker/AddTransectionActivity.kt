package com.example.expansetracker

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.expansetracker.databinding.ActivityAddTransectionBinding
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class AddTransectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTransectionBinding
    private var selectedCategory = ""
    private var selectedCategoryCard: MaterialCardView? = null
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddTransectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back button
        binding.backIcon.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // Date Picker
        binding.edtDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val themedContext = ContextThemeWrapper(this, R.style.DatePickerLightOrange)
            val datePickerDialog = DatePickerDialog(
                themedContext,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val dateStr = "${selectedDay.toString().padStart(2, '0')}/${(selectedMonth + 1).toString().padStart(2, '0')}/$selectedYear"
                    binding.edtDate.setText(dateStr)
                },
                year, month, day
            )

            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
            datePickerDialog.show()
        }

        // Category selection
        setupCategorySelection()

        // Save button click
        binding.btnTransaction.setOnClickListener {
            saveTransaction()
        }
    }

    private fun setupCategorySelection() {
        val categoryMap = mapOf(
            binding.cardFood to "Food",
            binding.cardTravel to "Travel",
            binding.cardBill to "Bill",
            binding.cardRent to "Rent",
            binding.cardShopping to "Shopping",
            binding.cardOther to "Other"
        )

        categoryMap.forEach { (card, categoryName) ->
            card.setOnClickListener {
                selectedCategoryCard?.strokeColor = getColor(R.color.lightGray)
                card.strokeColor = getColor(R.color.orange)
                selectedCategoryCard = card
                selectedCategory = categoryName
            }
        }
    }

    private fun saveTransaction() {
        val title = binding.edtTitle.text.toString().trim()
        val amountStr = binding.edtAmount.text.toString().trim()
        val date = binding.edtDate.text.toString().trim()
        val type = binding.edtCategory.text.toString().trim().lowercase()
        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            showToast("User not logged in")
            return
        }

        when {
            title.isEmpty() -> binding.edtTitle.error = "Enter title"
            amountStr.isEmpty() -> binding.edtAmount.error = "Enter amount"
            date.isEmpty() -> binding.edtDate.error = "Select date"
            type.isEmpty() -> binding.edtCategory.error = "Enter type"
            type != "income" && type != "expense" -> binding.edtCategory.error = "Only 'Income' or 'Expense' allowed"
            selectedCategory.isEmpty() -> showToast("Please select category")
            else -> {
                val amount = amountStr.toDoubleOrNull()
                if (amount == null || amount <= 0) {
                    binding.edtAmount.error = "Invalid amount"
                    return
                }

                val transaction = hashMapOf(
                    "uid" to user.uid,  // ✅ Store user ID
                    "title" to title,
                    "amount" to amount,
                    "date" to date,
                    "type" to type.capitalize(),
                    "category" to selectedCategory
                )

                db.collection("transactions")
                    .add(transaction)
                    .addOnSuccessListener {
                        showToast("Transaction added")
                        finish()
                    }
                    .addOnFailureListener {
                        showToast("Error: ${it.message}")
                    }
            }
        }
    }


    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
