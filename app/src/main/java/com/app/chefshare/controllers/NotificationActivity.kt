package com.app.chefshare

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NotificationActivity : AppCompatActivity() {

    private lateinit var notificationListContainer: LinearLayout
    private lateinit var emptyText: TextView
    private lateinit var backButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        notificationListContainer = findViewById(R.id.notificationListContainer)
        emptyText = findViewById(R.id.emptyNotificationText)
        backButton = findViewById(R.id.backButton)

        backButton.setOnClickListener { finish() }

        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(currentUser.uid)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    emptyText.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                emptyText.visibility = View.GONE

                for (doc in snapshot) {
                    val type = doc.getString("type") ?: continue
                    val fromUserId = doc.getString("fromUserId") ?: continue
                    val recipeId = doc.getString("recipeId") ?: continue
                    val message = doc.getString("message")
                    val isRead = doc.getBoolean("isRead") ?: false
                    val notificationId = doc.id

                    val view = LayoutInflater.from(this).inflate(R.layout.item_notification, null)
                    val avatar = view.findViewById<ImageView>(R.id.avatarImageView)
                    val text = view.findViewById<TextView>(R.id.messageTextView)
                    val container = view.findViewById<LinearLayout>(R.id.itemContainer)

                    // Nếu chưa đọc, dùng màu nền sáng hơn
                    if (!isRead) {
                        container.setBackgroundColor(ContextCompat.getColor(this,
                            R.color.unreadBackground
                        ))
                    }

                    text.text = "Đang tải thông báo..."

                    // Click mở recipe và đánh dấu đã đọc
                    view.setOnClickListener {
                        // Mark as read
                        db.collection("users").document(currentUser.uid)
                            .collection("notifications").document(notificationId)
                            .update("isRead", true)

                        // Mở RecipeDetailActivity
                        val intent = Intent(this, RecipeDetailActivity::class.java)
                        intent.putExtra("recipeId", recipeId)
                        startActivity(intent)
                    }

                    // Load user info
                    db.collection("users").document(fromUserId)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            val name = userDoc.getString("name") ?: "Ai đó"
                            val avatarUrl = userDoc.getString("photoUrl")

                            text.text = when (type) {
                                "like" -> "$name đã thích công thức của bạn"
                                "comment" -> {
                                    val preview = if (!message.isNullOrEmpty() && message.length > 100)
                                        message.take(100) + "..."
                                    else
                                        message ?: ""
                                    "$name đã bình luận về bài viết của bạn: $preview"
                                }
                                else -> "$name đã thực hiện hành động"
                            }

                            if (!avatarUrl.isNullOrEmpty()) {
                                Glide.with(this).load(avatarUrl).circleCrop().into(avatar)
                            }
                        }

                    notificationListContainer.addView(view)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Không thể tải thông báo", Toast.LENGTH_SHORT).show()
            }
    }
}
