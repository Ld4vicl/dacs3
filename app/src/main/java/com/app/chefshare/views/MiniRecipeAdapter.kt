package com.app.chefshare.views

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.chefshare.R
import com.app.chefshare.models.Recipe
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class MiniRecipeAdapter(
    private val context: Context,
    private val recipeList: MutableList<Recipe>,
    private val onItemClick: (Recipe) -> Unit
) : RecyclerView.Adapter<MiniRecipeAdapter.MiniRecipeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MiniRecipeViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_recipe_mini, parent, false)
        return MiniRecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: MiniRecipeViewHolder, position: Int) {
        val recipe = recipeList[position]
        val imageUrl = recipe.mainImage ?: ""

        Log.d("MiniRecipeAdapter", "Loading image: $imageUrl")

        if (imageUrl.isNotEmpty()) {
            Glide.with(context)
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_placeholder)
                //.error(R.drawable.ic_error) // Nếu có lỗi sẽ hiển thị ảnh này
                .into(holder.ivThumbnail)
        } else {
            holder.ivThumbnail.setImageResource(R.drawable.ic_placeholder)
        }

        holder.tvTitle.text = recipe.name

        holder.itemView.setOnClickListener {
            onItemClick(recipe)
        }
    }

    override fun getItemCount(): Int = recipeList.size

    class MiniRecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivThumbnail: ImageView = itemView.findViewById(R.id.ivThumbnail)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
    }
}
