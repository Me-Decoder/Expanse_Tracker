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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class SignupActivity : AppCompatActivity() {


    private val REQ_ONE_TAP = 3  // Can be any integer unique to this Activity
    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest



    var txtName: EditText? = null
    var txtEmail: EditText? = null
    var txtPassword: EditText? = null
    var txtCPassword: EditText? = null
    var btnSignUp: Button? = null
    var btnGoogleSignup: LinearLayout? = null

    private val auth = Firebase.auth
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup)

        txtName = findViewById(R.id.edt_Username)
        txtEmail = findViewById(R.id.edt_Email)
        txtPassword = findViewById(R.id.edt_password)
        txtCPassword = findViewById(R.id.edt_cpassword)
        btnSignUp = findViewById(R.id.btn_Signup)
        btnGoogleSignup = findViewById(R.id.btn_googleSignup)


        btnSignupClick()
        setupGoogleSignIn()
        btn_Google_Signup_Click()

    }
    private fun btnSignupClick() {
        btnSignUp?.setOnClickListener {
            val name = txtName?.text.toString().trim()
            val email = txtEmail?.text.toString().trim()
            val password = txtPassword?.text.toString().trim()
            val cPassword = txtCPassword?.text.toString().trim()

            when {
                name.isEmpty() -> {
                    txtName?.error = "Username is required"
                    txtName?.requestFocus()
                }
                email.isEmpty() -> {
                    txtEmail?.error = "Email is required"
                    txtEmail?.requestFocus()
                }
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    txtEmail?.error = "Enter a valid email"
                    txtEmail?.requestFocus()
                }
                password.isEmpty() -> {
                    txtPassword?.error = "Password is required"
                    txtPassword?.requestFocus()
                }
                password.length < 6 -> {
                    txtPassword?.error = "Password must be at least 6 characters"
                    txtPassword?.requestFocus()
                }
                cPassword.isEmpty() -> {
                    txtCPassword?.error = "Confirm your password"
                    txtCPassword?.requestFocus()
                }
                password != cPassword -> {
                    txtCPassword?.error = "Passwords do not match"
                    txtCPassword?.requestFocus()
                }
                else -> {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val userId = auth.currentUser?.uid

                                val user = hashMapOf(
                                    "uid" to userId,
                                    "name" to name,
                                    "email" to email,
                                    "password" to password
                                )

                                db.collection("users").document(email)
                                    .set(user)
                                    .addOnSuccessListener {
                                        showToast("Signup Successful 🎉")
                                        // Navigate to another screen (e.g., login or dashboard)
                                        startActivity(Intent(this, LoginActivity::class.java))
                                        finish()
                                    }
                                    .addOnFailureListener { e ->
                                        showToast("User data save failed: ${e.localizedMessage}")
                                    }
                            } else {
                                showToast("Signup Failed: ${task.exception?.localizedMessage}")
                            }
                        }
                }
            }
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
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    private fun btn_Google_Signup_Click() {
        btnGoogleSignup?.setOnClickListener {
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


}