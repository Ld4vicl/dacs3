package com.app.chefshare

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.app.chefshare.controllers.LoginActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        title = "Cài đặt"

        val backButton = findViewById<ImageView>(R.id.icBack)
        backButton.setOnClickListener {
            onBackPressed() // hoặc finish()
        }

        auth = FirebaseAuth.getInstance()

        // Khởi tạo GoogleSignInClient để đăng xuất khỏi Google
        googleSignInClient = GoogleSignIn.getClient(
            this,
            com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
            )
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        )

        val logoutButton = findViewById<Button>(R.id.buttonLogout)
        logoutButton.setOnClickListener {
            // Đăng xuất Firebase
            auth.signOut()

            // Đăng xuất Google nếu cần
            googleSignInClient.signOut().addOnCompleteListener {
                // Xoá sharedPreferences nếu có
                val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
                sharedPreferences.edit().clear().apply()

                // Điều hướng về màn hình đăng nhập và xoá backstack
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
    }
}

