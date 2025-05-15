package com.app.chefshare

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.chefshare.models.Recipe
import com.app.chefshare.views.RecipeAdapter
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView

class ProfileActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var btnEditProfile: Button
    private lateinit var imgAvatar: CircleImageView
    private lateinit var tvFullName: TextView
    private lateinit var tvUserId: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var rvMyRecipes: RecyclerView
    private lateinit var recipeAdapter: RecipeAdapter
    private val recipeList = mutableListOf<Recipe>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Ánh xạ view
        btnBack = findViewById(R.id.btnBack)
        btnEditProfile = findViewById(R.id.btnEditProfile)
        imgAvatar = findViewById(R.id.imgAvatar)
        tvFullName = findViewById(R.id.tvFullName)
        tvUserId = findViewById(R.id.tvUserId)
        progressBar = findViewById(R.id.progressBar)
        rvMyRecipes = findViewById(R.id.rvMyRecipes)

        rvMyRecipes.layoutManager = LinearLayoutManager(this)
        recipeAdapter = RecipeAdapter(this, recipeList) { recipe ->
            val intent = Intent(this, RecipeDetailActivity::class.java)
            intent.putExtra("recipeId", recipe.javaClass)
            startActivity(intent)
        }
        rvMyRecipes.adapter = recipeAdapter

        val user = UserManager.currentUser
        if (user != null) {
            tvFullName.text = user.displayName ?: "Ẩn danh"

            val avatarUrl = user.photoUrl?.toString() ?: ""
            Log.d("ProfileActivity", "Avatar URL: $avatarUrl")

            if (avatarUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_person)
                    .circleCrop()
                    .into(imgAvatar)
            } else {
                imgAvatar.setImageResource(R.drawable.ic_person)
            }

            val userId = user.uid ?: return
            displayCustomUserId(userId)
        } else {
            Toast.makeText(this, "Không có thông tin người dùng!", Toast.LENGTH_SHORT).show()
        }

        btnBack.setOnClickListener { finish() }
        btnEditProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        // Hiển thị công thức
        progressBar.visibility = ProgressBar.VISIBLE
        val userId = user?.uid ?: return

        FirebaseFirestore.getInstance().collection("recipes")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                recipeList.clear()
                for (doc in documents) {
                    val name = doc.getString("name") ?: continue
                    val desc = doc.getString("description") ?: ""
                    val image = doc.getString("mainImage") ?: ""
                    val id = doc.getString("id") ?: doc.id

                    val recipe = Recipe(
                        id = id,
                        name = name,
                        description = desc,
                        mainImage = image
                    )
                    recipeList.add(recipe)
                }
                recipeAdapter.notifyDataSetChanged()
                progressBar.visibility = ProgressBar.GONE

                if (recipeList.isEmpty()) {
                    Toast.makeText(this, "Không có công thức nào!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                progressBar.visibility = ProgressBar.GONE
                Toast.makeText(this, "Lỗi khi lấy dữ liệu công thức!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun displayCustomUserId(uid: String) {
        val db = FirebaseFirestore.getInstance()
        val userDoc = db.collection("users").document(uid)

        userDoc.get()
            .addOnSuccessListener { document ->
                val customId = document.getString("id")
                tvUserId.text = if (customId.isNullOrEmpty()) {
                    "ID: chưa có"
                } else {
                    "$customId"
                }
            }
            .addOnFailureListener {
                tvUserId.text = "ID: lỗi tải"
            }
    }
}
