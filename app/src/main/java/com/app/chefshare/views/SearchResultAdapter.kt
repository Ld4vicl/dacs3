package com.app.chefshare

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.chefshare.models.Recipe
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class SearchResultAdapter(private val recipes: List<Recipe>) :
    RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgDish: ImageView = itemView.findViewById(R.id.imgDish)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tvAuthor)
        private val imgAvatar: ImageView = itemView.findViewById(R.id.imgAvatar)

        fun bind(recipe: Recipe) {
            tvTitle.text = recipe.name
            tvDescription.text = recipe.description

            Glide.with(itemView.context)
                .load(recipe.mainImage)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(imgDish)

            val cleanedUserId = recipe.userId?.trim()
            if (!cleanedUserId.isNullOrEmpty()) {
                loadUserInfo(cleanedUserId, tvAuthor, imgAvatar)
            } else {
                tvAuthor.text = "Ẩn danh"
                imgAvatar.setImageResource(R.drawable.ic_person)
            }

            itemView.setOnClickListener {
                val intent = Intent(itemView.context, RecipeDetailActivity::class.java)
                intent.putExtra("recipeId", recipe.id)
                itemView.context.startActivity(intent)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(recipes[position])
    }

    override fun getItemCount(): Int = recipes.size

    private fun loadUserInfo(userId: String, tvAuthor: TextView, imgAvatar: ImageView) {
        FirebaseFirestore.getInstance().collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->
                val data = doc.data
                if (data != null) {
                    val name = data["name"] as? String ?: "Ẩn danh"
                    val photoUrl = data["photoUrl"] as? String

                    tvAuthor.text = name

                    if (!photoUrl.isNullOrEmpty()) {
                        Glide.with(tvAuthor.context)
                            .load(photoUrl)
                            .circleCrop()
                            .error(R.drawable.ic_person)
                            .fallback(R.drawable.ic_person)
                            .into(imgAvatar)
                    } else {
                        imgAvatar.setImageResource(R.drawable.ic_person)
                    }
                } else {
                    tvAuthor.text = "Ẩn danh"
                    imgAvatar.setImageResource(R.drawable.ic_person)
                }
            }
            .addOnFailureListener {
                tvAuthor.text = "Ẩn danh"
                imgAvatar.setImageResource(R.drawable.ic_person)
            }
    }
}
