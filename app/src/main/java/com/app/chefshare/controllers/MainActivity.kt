package com.app.chefshare.controllers

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.app.chefshare.NotificationActivity
import com.app.chefshare.PremiumActivity
import com.app.chefshare.ProfileActivity
import com.app.chefshare.R
import com.app.chefshare.RecentDishesActivity
import com.app.chefshare.SearchActivity
import com.app.chefshare.SettingsActivity
import com.app.chefshare.UploadRecipeActivity
import com.app.chefshare.controllers.LoginActivity
import com.app.chefshare.views.AIFragment
import com.app.chefshare.views.HomeFragment
import com.app.chefshare.views.StorageFragment
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var imgHeaderAvatar: ImageView
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var fabAddRecipe: FloatingActionButton
    private lateinit var notificationDot: View
    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        setupViews()
        setupNavigationDrawer()
        setupViewPager()
        setupBottomNavigation()

        findViewById<ImageButton>(R.id.btnNotification).setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        checkUnreadNotifications()

        // Kiểm tra xem giá trị navigateTo có tồn tại không
        val destination = intent?.getStringExtra("navigateTo")

        // Nếu có giá trị navigateTo, chọn tab tương ứng
        if (destination != null) {
            when (destination) {
                "home" -> bottomNavigation.selectedItemId = R.id.nav_home
                "ai" -> bottomNavigation.selectedItemId = R.id.nav_ai
                "storage" -> bottomNavigation.selectedItemId = R.id.nav_storage
            }
        }

        // Xóa giá trị "navigateTo" để tránh việc sử dụng lại trong lần tiếp theo
        intent?.removeExtra("navigateTo")
    }

    private fun setupViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        imgHeaderAvatar = findViewById(R.id.imgHeaderAvatar)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        fabAddRecipe = findViewById(R.id.fabAddRecipe)
        viewPager = findViewById(R.id.viewPager)

        val headerRoot = findViewById<LinearLayout>(R.id.headerSearchWrapper)
        notificationDot = headerRoot.findViewById(R.id.notificationDot)
        notificationDot.visibility = View.GONE

        val user = FirebaseAuth.getInstance().currentUser
        user?.photoUrl?.let { url ->
            Glide.with(this).load(url).circleCrop().into(imgHeaderAvatar)
        } ?: run {
            imgHeaderAvatar.setImageResource(R.drawable.ic_person)
        }

        imgHeaderAvatar.setOnClickListener {
            drawerLayout.openDrawer(navigationView)
        }

        fabAddRecipe.setOnClickListener {
            startActivity(Intent(this, UploadRecipeActivity::class.java))
        }

        val etSearchFake = headerRoot.findViewById<View>(R.id.etSearchFake)
        etSearchFake.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }

        val header = navigationView.getHeaderView(0)
        bindUserHeader(header)
    }

    private fun setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_profile -> startActivity(Intent(this, ProfileActivity::class.java))
                R.id.nav_notifications -> showToast("Thông báo")
                R.id.nav_stats -> showToast("Thống kê bếp")
                R.id.nav_recent -> startActivity(Intent(this, RecentDishesActivity::class.java))
                R.id.nav_premium -> startActivity(Intent(this, PremiumActivity::class.java))
                R.id.nav_challenges -> showToast("Thử thách")
                R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
                R.id.nav_faq -> showToast("FAQ")
                R.id.nav_feedback -> showToast("Phản hồi")
            }
            drawerLayout.closeDrawer(navigationView)
            true
        }
    }

    private fun setupViewPager() {
        viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 3

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> HomeFragment()
                    1 -> AIFragment()
                    2 -> StorageFragment()
                    else -> HomeFragment()
                }
            }
        }

        viewPager.isUserInputEnabled = false
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    setHeaderVisibility(true)
                    viewPager.currentItem = 0
                    true
                }
                R.id.nav_ai -> {
                    setHeaderVisibility(false)
                    viewPager.currentItem = 1
                    true
                }
                R.id.nav_storage -> {
                    setHeaderVisibility(true)
                    viewPager.currentItem = 2
                    true
                }
                else -> false
            }
        }
    }

    private fun setHeaderVisibility(show: Boolean) {
        findViewById<View>(R.id.headerSearchWrapper).isVisible = show
        if (show) fabAddRecipe.show() else fabAddRecipe.hide()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun bindUserHeader(header: View) {
        val imgAvatar = header.findViewById<ImageView>(R.id.imgAvatar)
        val tvUsername = header.findViewById<TextView>(R.id.tvUsername)
        val tvHandle = header.findViewById<TextView>(R.id.tvHandle)
        val tvUserId = header.findViewById<TextView>(R.id.tvUserId)

        val user = FirebaseAuth.getInstance().currentUser
        val displayName = user?.displayName ?: "Ẩn danh"
        val email = user?.email ?: "Không rõ"
        val uid = user?.uid ?: return

        tvUsername.text = displayName
        tvHandle.text = email
        tvUserId.text = "ID: Đang tải..."

        // Load avatar
        val photoUrl = user.photoUrl
        if (photoUrl != null) {
            Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.ic_person)
                .circleCrop()
                .into(imgAvatar)
        } else {
            imgAvatar.setImageResource(R.drawable.ic_person)
            imgAvatar.setColorFilter(ContextCompat.getColor(this, android.R.color.white))
        }

        // 🔥 Truy vấn Firestore để lấy id người dùng
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val customId = document.getString("id") ?: "Không rõ"
                    tvUserId.text = "ID: $customId"
                } else {
                    tvUserId.text = "ID: Không tìm thấy"
                }
            }
            .addOnFailureListener {
                tvUserId.text = "ID: Lỗi tải"
            }
    }


    private fun checkUnreadNotifications() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .document(currentUser.uid)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .addOnSuccessListener { documents ->
                val hasUnread = documents.any { it.getBoolean("isRead") != true }
                notificationDot.visibility = if (hasUnread) View.VISIBLE else View.GONE
            }
            .addOnFailureListener {
                notificationDot.visibility = View.GONE
            }
    }
}
