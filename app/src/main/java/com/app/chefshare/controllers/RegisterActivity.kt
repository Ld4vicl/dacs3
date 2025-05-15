package com.app.chefshare.controllers

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.app.chefshare.R
import com.app.chefshare.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.tvBackToLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val fullName = binding.etFullName.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || fullName.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(fullName)
                            .build()

                        user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                            val firestore = FirebaseFirestore.getInstance()
                            val uid = user?.uid ?: return@addOnCompleteListener
                            val userCode = generateUserCode()

                            val userData = hashMapOf(
                                "id" to userCode,
                                "name" to fullName,
                                "email" to email
                            )

                            firestore.collection("users").document(uid).set(userData)

                            // Gửi email xác minh
                            user.sendEmailVerification()
                                .addOnCompleteListener { verifyTask ->
                                    if (verifyTask.isSuccessful) {
                                        Toast.makeText(
                                            this,
                                            "Đăng ký thành công! Vui lòng xác minh email trước khi đăng nhập.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            this,
                                            "Không thể gửi email xác minh: ${verifyTask.exception?.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }

                                    auth.signOut()
                                    val intent = Intent(this, LoginActivity::class.java)
                                    intent.putExtra("email", email)
                                    startActivity(intent)
                                    finish()
                                }
                        }
                    } else {
                        Toast.makeText(this, "Đăng ký thất bại: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    private fun generateUserCode(): String {
        val randomNumber = (1000000..9999999).random()
        return "@ce_$randomNumber"
    }
}
