package com.example.expansetracker

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private val REQ_ONE_TAP = 2  // Can be any integer unique to this Activity
    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest

    var txtEmail: EditText? = null
    var txtPassword: EditText? = null
    var btnLogin: Button? = null
    var btnGoogleLogin: LinearLayout? = null
    var btnSignUp: TextView? = null

    private val auth = Firebase.auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        txtEmail = findViewById(R.id.edt_email)
        txtPassword = findViewById(R.id.edt_password)
        btnLogin = findViewById(R.id.btn_login)
        btnGoogleLogin = findViewById(R.id.btn_googleLogin)
        btnSignUp = findViewById(R.id.btn_signUp)

        btnLoginClick()
        setupGoogleSignIn()
        btnGoogleLoginClick()
        txtSignupClick()
    }

    private fun txtSignupClick() {
        btnSignUp?.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun setupGoogleSignIn() {
        oneTapClient = Identity.getSignInClient(this)
        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.default_web_client_id)) // ✅ add this in strings.xml from Firebase
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .build()
    }

    private fun btnGoogleLoginClick() {
        btnGoogleLogin?.setOnClickListener {
            oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener { result ->
                    startIntentSenderForResult(
                        result.pendingIntent.intentSender,
                        REQ_ONE_TAP,
                        null, 0, 0, 0
                    )
                }
                .addOnFailureListener { e ->
                    showToast("Google Sign In Failed: ${e.localizedMessage}")
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQ_ONE_TAP) {
            try {
                val credential: SignInCredential = Identity.getSignInClient(this)
                    .getSignInCredentialFromIntent(data)
                val idToken = credential.googleIdToken
                if (idToken != null) {
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(firebaseCredential)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                showToast("Google Sign In Successful ✅")
                                // Go to Main Activity
                                startActivity(Intent(this, SetPasswordActivity::class.java))
                                finish()
                            } else {
                                showToast("Firebase Authentication Failed.")
                            }
                        }
                }
            } catch (e: Exception) {
                Log.e("LoginActivity", "Google Sign-In Failed", e)
            }
        }
    }

    private fun btnLoginClick() {
        btnLogin?.setOnClickListener {
            val email = txtEmail?.text.toString().trim()
            val password = txtPassword?.text.toString().trim()

            if (!isValidEmail(email)) {
                txtEmail?.error = "Please enter a valid email"
                txtEmail?.requestFocus()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                txtPassword?.error = "Please enter password"
                txtPassword?.requestFocus()
                return@setOnClickListener
            }

            if (password.length < 6) {
                txtPassword?.error = "Password must be at least 8 characters"
                txtPassword?.requestFocus()
                return@setOnClickListener
            }

            // Fetch user document from Firestore
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(email).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val storedPassword = document.getString("password")
                        if (storedPassword == password) {
                            showToast("Login Successful ✅")
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            showToast("Incorrect password ❌")
                        }
                    } else {
                        showToast("User not found ❌")
                    }
                }
                .addOnFailureListener {
                    showToast("Login failed: ${it.localizedMessage}")
                }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


}
