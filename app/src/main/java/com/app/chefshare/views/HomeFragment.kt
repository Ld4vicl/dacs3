package com.app.chefshare.views

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import com.app.chefshare.PremiumActivity
import com.app.chefshare.R
import com.app.chefshare.RecipeDetailActivity
import com.app.chefshare.SearchResultsActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var db: FirebaseFirestore
    private val popularIngredients = listOf(
        "Trứng", "Thịt bò", "Tôm", "Gà", "Thịt heo", "Bún"
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()

        val glIngredients = view.findViewById<GridLayout>(R.id.glIngredients)
        displayIngredientSquares(glIngredients, popularIngredients)

        val llRecentIngredients = view.findViewById<LinearLayout>(R.id.llRecentIngredients)
        loadRecipes(llRecentIngredients)

        val btnSubscribe = view.findViewById<Button>(R.id.btnSubscribe)
        btnSubscribe.setOnClickListener {
            startActivity(Intent(requireContext(), PremiumActivity::class.java))
        }
    }

    private fun displayIngredientSquares(gridLayout: GridLayout, items: List<String>) {
        val inflater = LayoutInflater.from(requireContext())
        gridLayout.removeAllViews()

        // Đặt số cột là 2 để cân đều
        gridLayout.columnCount = 2

        // Convert dp sang pixel cho bo góc
        val cornerRadius = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            16f,
            resources.displayMetrics
        ).toInt()

        for (item in items) {
            val itemView = inflater.inflate(R.layout.item_ingredient_square, gridLayout, false)
            val tvName = itemView.findViewById<TextView>(R.id.tvIngredientName)
            val imgIngredient = itemView.findViewById<ImageView>(R.id.imgIngredient)

            tvName.text = item

            // Gán ảnh tương ứng
            val imageRes = when (item.lowercase()) {
                "gà" -> R.drawable.chicken
                "tôm" -> R.drawable.shrimp
                "thịt bò" -> R.drawable.beef
                "trứng" -> R.drawable.egg
                "thịt heo" -> R.drawable.pork
                "bún" -> R.drawable.vermicelli
                else -> R.drawable.ic_placeholder
            }

            Glide.with(requireContext())
                .load(imageRes)
                .transform(RoundedCorners(cornerRadius))
                .into(imgIngredient)

            // LayoutParams cho 2 cột cân đều
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(16, 16, 16, 16)
            }
            itemView.layoutParams = params

            itemView.setOnClickListener {
                val intent = Intent(requireContext(), SearchResultsActivity::class.java)
                intent.putExtra("keyword", item)
                startActivity(intent)
            }

            gridLayout.addView(itemView)
        }
    }

    private fun loadRecipes(container: LinearLayout) {
        db.collection("recipes")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { result ->
                val inflater = LayoutInflater.from(requireContext())
                container.removeAllViews()

                for (document in result) {
                    val name = document.getString("name") ?: "Không tên"
                    val mainImage = document.getString("mainImage") ?: ""
                    val userId = document.getString("userId") ?: ""
                    val recipeId = document.id

                    val itemView = inflater.inflate(R.layout.item_ingredient_chip, container, false)
                    val tvName = itemView.findViewById<TextView>(R.id.tvIngredientName)
                    val tvAuthor = itemView.findViewById<TextView>(R.id.tvPostedBy)
                    val imageView = itemView.findViewById<ImageView>(R.id.imgIngredientImage)

                    tvName.text = name
                    Glide.with(requireContext()).load(mainImage).into(imageView)

                    if (userId.isNotEmpty()) {
                        db.collection("users").document(userId)
                            .get()
                            .addOnSuccessListener { userDoc ->
                                val authorName = userDoc.getString("name") ?: "Ẩn danh"
                                tvAuthor.text = authorName
                            }
                            .addOnFailureListener {
                                tvAuthor.text = "Ẩn danh"
                            }
                    } else {
                        tvAuthor.text = "Ẩn danh"
                    }

                    itemView.setOnClickListener {
                        val intent = Intent(requireContext(), RecipeDetailActivity::class.java)
                        intent.putExtra("recipeId", recipeId)
                        startActivity(intent)
                    }

                    container.addView(itemView)
                }
            }
            .addOnFailureListener { exception ->
                Log.w("HomeFragment", "Error getting recipes: ", exception)
            }
    }
}
