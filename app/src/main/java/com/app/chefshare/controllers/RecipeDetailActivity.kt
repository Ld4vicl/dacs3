package com.app.chefshare

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.app.chefshare.controllers.MainActivity
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class RecipeDetailActivity : AppCompatActivity() {

    private var isSaved = false
    private var isLiked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail)

        val recipeId = intent.getStringExtra("recipeId") ?: return

        val imgMain = findViewById<ImageView>(R.id.imgMain)
        val tvName = findViewById<TextView>(R.id.tvName)
        val tvDescription = findViewById<TextView>(R.id.tvDescription)
        val tvPortion = findViewById<TextView>(R.id.tvPortion)
        val tvTime = findViewById<TextView>(R.id.tvTime)
        val ingredientContainer = findViewById<LinearLayout>(R.id.ingredientContainer)
        val stepsContainer = findViewById<LinearLayout>(R.id.stepsContainer)
        val btnSave = findViewById<ImageView>(R.id.btnSaveRecipe)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnLike = findViewById<ImageView>(R.id.btnLikeRecipe)
        val uploaderAvatar = findViewById<ImageView>(R.id.imgUploaderAvatar)
        val uploaderName = findViewById<TextView>(R.id.tvUploaderName)
        val etComment = findViewById<EditText>(R.id.edtComment)
        val btnSendComment = findViewById<ImageButton>(R.id.btnSendComment)
        val commentListContainer = findViewById<LinearLayout>(R.id.commentListContainer)

        val user = FirebaseAuth.getInstance().currentUser

        btnBack.setOnClickListener { finish() }

        if (user != null) {
            val saveRef = FirebaseFirestore.getInstance()
                .collection("users").document(user.uid)
                .collection("savedRecipes").document(recipeId)

            saveRef.get().addOnSuccessListener { doc ->
                isSaved = doc.exists()
                btnSave.setImageResource(if (isSaved) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark)
            }
        }

        FirebaseFirestore.getInstance().collection("recipes")
            .document(recipeId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val data = doc.data ?: return@addOnSuccessListener
                    val name = data["name"] as? String ?: ""
                    val description = data["description"] as? String ?: ""
                    val portion = data["portion"] as? String ?: ""
                    val cookingTime = data["cookingTime"] as? String ?: ""
                    val mainImage = data["mainImage"] as? String ?: ""
                    val userId = data["userId"] as? String ?: ""

                    addToRecentRecipes(recipeId, name, mainImage)

                    tvName.text = name
                    tvDescription.text = description
                    tvPortion.text = portion
                    tvTime.text = cookingTime
                    Glide.with(this).load(mainImage).into(imgMain)

                    // Lưu / Gỡ lưu
                    btnSave.setOnClickListener {
                        if (user != null) {
                            val saveRef = FirebaseFirestore.getInstance()
                                .collection("users").document(user.uid)
                                .collection("savedRecipes").document(recipeId)

                            if (isSaved) {
                                saveRef.delete().addOnSuccessListener {
                                    isSaved = false
                                    btnSave.setImageResource(R.drawable.ic_bookmark)
                                    showToast("❌ Đã gỡ lưu món ăn!")
                                }
                            } else {
                                val saveData = hashMapOf(
                                    "name" to name,
                                    "mainImage" to mainImage,
                                    "savedAt" to FieldValue.serverTimestamp()
                                )
                                saveRef.set(saveData).addOnSuccessListener {
                                    isSaved = true
                                    btnSave.setImageResource(R.drawable.ic_bookmark_filled)
                                    showToast("✅ Đã lưu món ăn!")
                                }
                            }
                        }
                    }

                    // Nguyên liệu
                    val ingredients = data["ingredients"] as? List<*> ?: emptyList<Any>()
                    ingredients.forEach {
                        val tv = TextView(this)
                        tv.text = "• ${it.toString()}"
                        ingredientContainer.addView(tv)
                    }

                    // Các bước làm
                    val steps = data["steps"] as? List<Map<String, Any>> ?: emptyList()
                    for ((index, step) in steps.withIndex()) {
                        val stepLayout = LinearLayout(this)
                        stepLayout.orientation = LinearLayout.HORIZONTAL
                        stepLayout.setPadding(0, 12, 0, 12)

                        val numberCircle = TextView(this)
                        numberCircle.text = (index + 1).toString()
                        numberCircle.textSize = 16f
                        numberCircle.setTextColor(Color.WHITE)
                        numberCircle.gravity = Gravity.CENTER
                        numberCircle.setBackgroundResource(R.drawable.bg_circle)
                        val params = LinearLayout.LayoutParams(70, 70)
                        numberCircle.layoutParams = params

                        val stepDesc = TextView(this)
                        stepDesc.text = step["text"] as? String ?: ""
                        stepDesc.textSize = 16f
                        stepDesc.setPadding(16, 0, 0, 0)

                        stepLayout.addView(numberCircle)
                        stepLayout.addView(stepDesc)
                        stepsContainer.addView(stepLayout)

                        val images = step["images"] as? List<String> ?: emptyList()
                        images.forEachIndexed { imgIndex, imageUrl ->
                            val image = ImageView(this)
                            val imageParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, 400
                            )
                            image.scaleType = ImageView.ScaleType.CENTER_CROP
                            imageParams.setMargins(0, if (imgIndex == 0) 30 else 25, 0, 0)
                            image.layoutParams = imageParams

                            Glide.with(this).load(imageUrl).into(image)
                            stepsContainer.addView(image)
                        }
                    }

                    // THẢ TIM ❤️
                    val tvLikeCount = findViewById<TextView>(R.id.tvLikeCount)

                    if (user != null) {
                        val likesRef = FirebaseFirestore.getInstance()
                            .collection("recipes").document(recipeId)

                        likesRef.get().addOnSuccessListener { doc ->
                            val likes = doc.get("likes") as? List<*> ?: emptyList<String>()
                            isLiked = user.uid in likes
                            btnLike.setImageResource(
                                if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart
                            )
                            tvLikeCount.text = likes.size.toString()
                        }

                        btnLike.setOnClickListener {
                            val update = if (isLiked) {
                                mapOf("likes" to FieldValue.arrayRemove(user.uid))
                            } else {
                                mapOf("likes" to FieldValue.arrayUnion(user.uid))
                            }

                            FirebaseFirestore.getInstance().collection("recipes")
                                .document(recipeId)
                                .update(update)
                                .addOnSuccessListener {
                                    isLiked = !isLiked
                                    btnLike.setImageResource(
                                        if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart
                                    )
                                    showToast(
                                        if (isLiked) "❤️ Bạn đã thích công thức này!"
                                        else "💔 Đã bỏ thích."
                                    )

                                    // ✅ Cập nhật lại số lượt thích
                                    FirebaseFirestore.getInstance().collection("recipes")
                                        .document(recipeId).get()
                                        .addOnSuccessListener { updatedDoc ->
                                            val updatedLikes = updatedDoc.get("likes") as? List<*> ?: emptyList<String>()
                                            tvLikeCount.text = updatedLikes.size.toString()
                                        }

                                    // ✅ Gửi thông báo nếu là hành động LIKE (không phải unlike)
                                    if (isLiked && user.uid != userId) {
                                        val notifData = hashMapOf(
                                            "type" to "like",
                                            "fromUserId" to user.uid,
                                            "recipeId" to recipeId,
                                            "timestamp" to FieldValue.serverTimestamp()
                                        )

                                        FirebaseFirestore.getInstance()
                                            .collection("users").document(userId)
                                            .collection("notifications")
                                            .add(notifData)
                                    }
                                }
                        }
                    }

                    // Hiển thị người đăng
                    if (userId.isNotBlank()) {
                        val currentUser = FirebaseAuth.getInstance().currentUser

                        if (userId == currentUser?.uid) {
                            FirebaseFirestore.getInstance().collection("users")
                                .document(userId)
                                .get()
                                .addOnSuccessListener { userDoc ->
                                    uploaderName.text = userDoc.getString("name") ?: "Bạn"
                                    val avatarUrl = userDoc.getString("photoUrl")
                                    if (!avatarUrl.isNullOrEmpty()) {
                                        Glide.with(this).load(avatarUrl).circleCrop().into(uploaderAvatar)
                                    } else {
                                        uploaderAvatar.setImageResource(R.drawable.ic_person)
                                    }
                                }
                                .addOnFailureListener {
                                    uploaderName.text = "Bạn"
                                    uploaderAvatar.setImageResource(R.drawable.ic_person)
                                }
                        } else {
                            // ✅ Người dùng khác → lấy từ Firestore
                            FirebaseFirestore.getInstance().collection("users")
                                .document(userId)
                                .get()
                                .addOnSuccessListener { userDoc ->
                                    if (userDoc.exists()) {
                                        val uploader = userDoc.data
                                        uploaderName.text = uploader?.get("name") as? String ?: "Ẩn danh"
                                        val avatarUrl = uploader?.get("photoUrl") as? String
                                        if (!avatarUrl.isNullOrEmpty()) {
                                            Glide.with(this).load(avatarUrl).circleCrop().into(uploaderAvatar)
                                        } else {
                                            uploaderAvatar.setImageResource(R.drawable.ic_person)
                                        }
                                    } else {
                                        uploaderName.text = "Ẩn danh"
                                        uploaderAvatar.setImageResource(R.drawable.ic_person)
                                    }
                                }
                                .addOnFailureListener {
                                    uploaderName.text = "Ẩn danh"
                                    uploaderAvatar.setImageResource(R.drawable.ic_person)
                                }
                        }
                    }

                    // XỬ LÝ BÌNH LUẬN
                    loadComments(recipeId)

                    btnSendComment.setOnClickListener {
                        val content = etComment.text.toString().trim()
                        if (content.isNotEmpty() && user != null) {
                            val commentData = hashMapOf(
                                "userId" to user.uid,
                                "content" to content,
                                "timestamp" to FieldValue.serverTimestamp()
                            )

                            FirebaseFirestore.getInstance()
                                .collection("recipes").document(recipeId)
                                .collection("comments").add(commentData)
                                .addOnSuccessListener {
                                    showToast("✅ Bình luận đã được gửi!")
                                    etComment.text.clear()
                                    loadComments(recipeId)

                                    // ✅ Gửi thông báo nếu bình luận vào công thức của người khác
                                    if (user.uid != userId) {
                                        val notifData = hashMapOf(
                                            "type" to "comment",
                                            "fromUserId" to user.uid,
                                            "recipeId" to recipeId,
                                            "content" to content,
                                            "timestamp" to FieldValue.serverTimestamp()
                                        )

                                        FirebaseFirestore.getInstance()
                                            .collection("users").document(userId)
                                            .collection("notifications")
                                            .add(notifData)
                                    }
                                }
                                .addOnFailureListener {
                                    showToast("❌ Gửi bình luận thất bại!")
                                }
                        }
                    }

                } else {
                    showToast("❌ Không tìm thấy công thức!")
                }
            }
            .addOnFailureListener {
                showToast("❌ Lỗi tải công thức từ Firestore!")
            }

        val fab = findViewById<View>(R.id.fabContainer)
        fab.findViewById<FloatingActionButton>(R.id.fabAddRecipe).setOnClickListener {
            startActivity(Intent(this, UploadRecipeActivity::class.java))
        }

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_storage
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("SELECTED_TAB", R.id.nav_home)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    true
                }
                R.id.nav_ai -> {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        putExtra("navigateTo", "ai")
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    })
                    true
                }
                R.id.nav_storage -> {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        putExtra("navigateTo", "storage")
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    })
                    true
                }
                else -> false
            }
        }
    }

    private fun loadComments(recipeId: String) {
        val db = FirebaseFirestore.getInstance()
        val container = findViewById<LinearLayout>(R.id.commentListContainer)
        val currentUser = FirebaseAuth.getInstance().currentUser

        container.removeAllViews()

        db.collection("recipes").document(recipeId)
            .collection("comments")
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot) {
                    val content = doc.getString("content") ?: ""
                    val userId = doc.getString("userId") ?: ""

                    val commentLayout = LinearLayout(this)
                    commentLayout.orientation = LinearLayout.HORIZONTAL
                    commentLayout.setPadding(0, 16, 0, 16)

                    val avatar = ImageView(this)
                    val avatarParams = LinearLayout.LayoutParams(100, 100)
                    avatarParams.setMargins(0, 0, 16, 0)
                    avatar.layoutParams = avatarParams
                    avatar.setImageResource(R.drawable.ic_person)
                    avatar.clipToOutline = true

                    val textLayout = LinearLayout(this)
                    textLayout.orientation = LinearLayout.VERTICAL

                    val nameView = TextView(this)
                    nameView.text = "Đang tải tên..."
                    nameView.setTextColor(Color.BLACK)
                    nameView.textSize = 14f

                    val contentView = TextView(this)
                    contentView.text = content
                    contentView.setTextColor(Color.DKGRAY)
                    contentView.textSize = 15f

                    textLayout.addView(nameView)
                    textLayout.addView(contentView)

                    commentLayout.addView(avatar)
                    commentLayout.addView(textLayout)
                    container.addView(commentLayout)

                    // ✅ Nếu là user hiện tại → lấy từ FirebaseAuth
                    if (userId == currentUser?.uid) {
                        // ✅ Lấy từ Firestore ngay cả khi là chính mình
                        FirebaseFirestore.getInstance().collection("users")
                            .document(userId)
                            .get()
                            .addOnSuccessListener { userDoc ->
                                nameView.text = userDoc.getString("name") ?: "Bạn"
                                val avatarUrl = userDoc.getString("photoUrl")
                                if (!avatarUrl.isNullOrEmpty()) {
                                    Glide.with(this).load(avatarUrl).circleCrop().into(avatar)
                                }
                            }
                            .addOnFailureListener {
                                nameView.text = "Bạn"
                            }
                    } else {
                        // ✅ Lấy từ Firestore user profile nếu là người khác
                        FirebaseFirestore.getInstance().collection("users")
                            .document(userId)
                            .get()
                            .addOnSuccessListener { userDoc ->
                                if (userDoc.exists()) {
                                    val userName = userDoc.getString("name") ?: "Ẩn danh"
                                    nameView.text = userName
                                    val avatarUrl = userDoc.getString("photoUrl")
                                    if (!avatarUrl.isNullOrEmpty()) {
                                        Glide.with(this).load(avatarUrl).circleCrop().into(avatar)
                                    }
                                }
                            }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "❌ Không thể tải bình luận", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addToRecentRecipes(recipeId: String, name: String, mainImage: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        val recentRef = db.collection("users").document(user.uid)
            .collection("recentRecipes").document(recipeId)

        val data = hashMapOf(
            "name" to name,
            "mainImage" to mainImage,
            "viewedAt" to FieldValue.serverTimestamp()
        )

        recentRef.set(data)
            .addOnSuccessListener {
                Log.d("Firestore", "Đã lưu vào recentRecipes")
            }
            .addOnFailureListener {
                Log.e("Firestore", "Lỗi khi lưu recent", it)
            }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
