package com.app.chefshare

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.chefshare.controllers.MainActivity
import com.app.chefshare.models.Recipe
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore

class SearchResultsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SearchResultAdapter
    private val db = FirebaseFirestore.getInstance()
    private val results = mutableListOf<Recipe>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_results)

        // Back button
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        recyclerView = findViewById(R.id.rvSearchResults)
        recyclerView.layoutManager = GridLayoutManager(this, 1)

        val keyword = intent.getStringExtra("keyword") ?: ""
        adapter = SearchResultAdapter(results)
        recyclerView.adapter = adapter

        fetchSearchResults(keyword)

        // FAB
        val fab = findViewById<View>(R.id.fabContainer)
        val fabAddRecipe = fab.findViewById<FloatingActionButton>(R.id.fabAddRecipe)
        fabAddRecipe.setOnClickListener {
            startActivity(Intent(this, UploadRecipeActivity::class.java))
        }

        // Bottom nav
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

    private fun fetchSearchResults(keyword: String) {
        db.collection("recipes")
            .get()
            .addOnSuccessListener { documents ->
                results.clear()
                for (doc in documents) {
                    val name = doc.getString("name") ?: ""
                    val description = doc.getString("description") ?: ""
                    val ingredients = doc.get("ingredients") as? List<String> ?: listOf()

                    // Kiểm tra keyword có trong tên, mô tả hoặc nguyên liệu
                    if (name.contains(keyword, true)
                        || description.contains(keyword, true)
                        || ingredients.any { it.contains(keyword, true) }
                    ) {
                        val recipe = Recipe(
                            id = doc.id,
                            name = name,
                            description = description,
                            portion = doc.getString("portion") ?: "",
                            cookingTime = doc.getString("cookingTime") ?: "",
                            ingredients = ingredients,
                            steps = doc.get("steps") as? List<String> ?: listOf(),
                            mainImage = doc.getString("mainImage") ?: "",
                            stepImages = doc.get("stepImages") as? List<String> ?: listOf(),
                            userId = doc.getString("userId") ?: ""
                        )
                        results.add(recipe)
                    }
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi khi tải dữ liệu: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("SearchResults", "Firestore error", e)
            }
    }
}
