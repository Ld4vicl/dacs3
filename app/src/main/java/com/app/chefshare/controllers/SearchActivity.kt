package com.app.chefshare

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.app.chefshare.controllers.MainActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class SearchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_empty)

        val etSearch = findViewById<EditText>(R.id.etSearch)
        val btnFilter = findViewById<ImageView>(R.id.btnFilter)

        btnFilter.setOnClickListener {
            Toast.makeText(this, "Tính năng lọc đang phát triển", Toast.LENGTH_SHORT).show()
        }

        //sự kiện cho back
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish() // Quay lại activity trước đó
        }

        // Khi người dùng nhấn Enter/Search
        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
                val keyword = etSearch.text.toString()
                if (keyword.isNotBlank()) {
                    val intent = Intent(this, SearchResultsActivity::class.java)
                    intent.putExtra("keyword", keyword)
                    startActivity(intent)
                    finish()
                }
                true
            } else {
                false
            }
        }

        // Tuỳ chọn: Khi người dùng tap vào EditText (nếu có text thì cũng có thể chuyển luôn)
        etSearch.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && etSearch.text.isNotEmpty()) {
                // Có thể để trống hoặc điều hướng nếu muốn
            }
        }

        val fab = findViewById<View>(R.id.fabContainer)
        val fabAddRecipe = fab.findViewById<FloatingActionButton>(R.id.fabAddRecipe)
        fabAddRecipe.setOnClickListener {
            val intent = Intent(this, UploadRecipeActivity::class.java)
            startActivity(intent)
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
}
