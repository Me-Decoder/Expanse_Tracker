package com.example.expansetracker

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SetPasswordActivity : AppCompatActivity() {



    private lateinit var txtUsername: EditText
    private lateinit var txtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var edtCpassword: EditText
    private lateinit var btnSetPassword: LinearLayout

    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_set_password)

        txtEmail = findViewById(R.id.edt_email)
        edtPassword = findViewById(R.id.edt_password)
        edtCpassword = findViewById(R.id.edt_cpassword)
        btnSetPassword = findViewById(R.id.btn_setpassword)
        txtEmail = findViewById(R.id.edt_email)
        txtUsername = findViewById(R.id.edt_username)


        val currentUser = FirebaseAuth.getInstance().currentUser
        val email = currentUser?.email

        //if User Not Loggein
        if (currentUser != null) {
            txtEmail.setText(currentUser.email)
            txtEmail.isEnabled = false
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish() // or redirect to login
        }

        firestore = FirebaseFirestore.getInstance()

        // Disable email editing (assumes it's prefilled after Google Sign-in)
        txtEmail.setText(email)
        txtEmail.isEnabled = false


        btnSetPassword.setOnClickListener {
            // Get user input
            val username = txtUsername.text.toString().trim()
            val email = txtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()
            val confirmPassword = edtCpassword.text.toString().trim()

            if (!isValidEmail(email)) {
                toast("Invalid email address")
            } else if (password.isEmpty()) {
                toast("Password cannot be empty")
            } else if (password.length < 6) {
                toast("Password must be at least 6 characters")
            } else if (password != confirmPassword) {
                toast("Passwords do not match")
            }else if (username.isEmpty()) {
                toast("Username cannot be empty")
            } else if (username.length < 3) {
                toast("Username must be at least 3 characters")
            }
            else {
                saveUserToFirestore(username,email, password)
            }
        }

    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun saveUserToFirestore(username: String,email: String, password: String) {
        val userMap = hashMapOf(
            "username" to username,
            "email" to email,
            "password" to password
        )

        firestore.collection("users").document(email)
            .set(userMap)
            .addOnSuccessListener {
                toast("Password set successfully")
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                toast("Failed to save. Try again.")
            }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }


}