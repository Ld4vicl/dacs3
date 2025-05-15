package com.app.chefshare.controllers

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.app.chefshare.R
import com.app.chefshare.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val emailFromRegister = intent.getStringExtra("email")
        if (!emailFromRegister.isNullOrEmpty()) {
            binding.etEmail.setText(emailFromRegister)
        }

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null && auth.currentUser!!.isEmailVerified) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập email và mật khẩu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null && user.isEmailVerified) {
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            user?.sendEmailVerification()
                                ?.addOnCompleteListener { verificationTask ->
                                    if (verificationTask.isSuccessful) {
                                        Toast.makeText(
                                            this,
                                            "Email chưa được xác minh. Đã gửi lại email xác minh.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            this,
                                            "Không thể gửi email xác minh: ${verificationTask.exception?.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        Log.e("LoginActivity", "Gửi email xác minh thất bại", verificationTask.exception)
                                    }
                                    auth.signOut()
                                }
                        }
                    } else {
                        Toast.makeText(
                            this,
                            "Đăng nhập thất bại: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        binding.tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.btnGoogleLogin.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        if (task.isSuccessful) {
            val account: GoogleSignInAccount? = task.result
            val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener { firebaseTask ->
                    if (firebaseTask.isSuccessful) {
                        val user = auth.currentUser
                        val accountName = account?.displayName
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(accountName)
                            .setPhotoUri(account?.photoUrl)
                            .build()
                        user?.updateProfile(profileUpdates)

                        val firestore = FirebaseFirestore.getInstance()
                        val uid = user?.uid ?: return@addOnCompleteListener

                        user?.let {
                            val userRef = firestore.collection("users").document(it.uid)
                            userRef.get().addOnSuccessListener { doc ->
                                val name = it.displayName ?: "Ẩn danh"
                                val avatar = it.photoUrl?.toString() ?: ""

                                val updateData = hashMapOf<String, Any>()
                                if (!doc.exists() || doc.getString("photoUrl").isNullOrEmpty()) {
                                    updateData["photoUrl"] = avatar
                                }
                                if (!doc.exists() || doc.getString("name").isNullOrEmpty()) {
                                    updateData["name"] = name
                                }

                                if (updateData.isNotEmpty()) {
                                    userRef.set(updateData, SetOptions.merge())
                                }
                            }
                        }

                        firestore.collection("users").document(uid).get()
                            .addOnSuccessListener { document ->
                                if (!document.exists()) {
                                    val userCode = generateUserCode()
                                    val userData = hashMapOf(
                                        "id" to userCode,
                                        "name" to user?.displayName,
                                        "email" to user?.email
                                    )
                                    firestore.collection("users").document(uid).set(userData)
                                }
                            }

                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Đăng nhập Google thất bại", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            Toast.makeText(this, "Google Sign-In thất bại: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            Log.e("LoginActivity", "Google Sign-In error", task.exception)
        }
    }

    private fun generateUserCode(): String {
        val randomNumber = (1000000..9999999).random()
        return "@ce_$randomNumber"
    }
}
