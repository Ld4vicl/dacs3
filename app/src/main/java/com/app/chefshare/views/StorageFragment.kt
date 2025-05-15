package com.app.chefshare.views

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.chefshare.R
import com.app.chefshare.RecipeDetailActivity
import com.app.chefshare.models.Recipe
import com.app.chefshare.views.MiniRecipeAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class StorageFragment : Fragment(R.layout.fragment_storage) {

    private lateinit var rvRecentRecipes: RecyclerView
    private lateinit var rvSavedRecipes: RecyclerView
    private lateinit var tvSavedCount: TextView
    private lateinit var adapter: MiniRecipeAdapter
    private lateinit var savedAdapter: MiniRecipeAdapter
    private val recentRecipes = mutableListOf<Recipe>()
    private val savedRecipes = mutableListOf<Recipe>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvRecentRecipes = view.findViewById(R.id.rvRecentRecipes)
        rvSavedRecipes = view.findViewById(R.id.rvSavedRecipes)
        tvSavedCount = view.findViewById(R.id.tvSavedCount)

        adapter = MiniRecipeAdapter(requireContext(), recentRecipes) { recipe ->
            val intent = Intent(requireContext(), RecipeDetailActivity::class.java)
            intent.putExtra("recipeId", recipe.id)
            startActivity(intent)
        }

        savedAdapter = MiniRecipeAdapter(requireContext(), savedRecipes) { recipe ->
            val intent = Intent(requireContext(), RecipeDetailActivity::class.java)
            intent.putExtra("recipeId", recipe.id)
            startActivity(intent)
        }

        rvRecentRecipes.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvRecentRecipes.adapter = adapter

        rvSavedRecipes.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rvSavedRecipes.adapter = savedAdapter

        loadRecentRecipes()
        loadSavedRecipes()
    }

    private fun loadRecentRecipes() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .document(user.uid)
            .collection("recentRecipes")
            .orderBy("viewedAt", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { documents ->
                recentRecipes.clear()
                for (document in documents) {
                    val name = document.getString("name") ?: ""
                    val mainImage = document.getString("mainImage") ?: ""
                    val recipeId = document.id

                    recentRecipes.add(
                        Recipe(
                            id = recipeId,
                            name = name,
                            mainImage = mainImage
                        )
                    )
                }

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Lỗi khi tải món ăn gần đây: $exception", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadSavedRecipes() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .document(user.uid)
            .collection("savedRecipes")
            .orderBy("savedAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                savedRecipes.clear()
                for (document in documents) {
                    val name = document.getString("name") ?: ""
                    val mainImage = document.getString("mainImage") ?: ""
                    val recipeId = document.id

                    savedRecipes.add(
                        Recipe(
                            id = recipeId,
                            name = name,
                            mainImage = mainImage
                        )
                    )
                }

                savedAdapter.notifyDataSetChanged()
                tvSavedCount.text = "Đã lưu ${savedRecipes.size} món"
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Lỗi khi tải món đã lưu: $exception", Toast.LENGTH_SHORT).show()
            }
    }
}
