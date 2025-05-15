package com.app.chefshare.views

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.chefshare.R
import com.app.chefshare.RecipeDetailActivity
import com.app.chefshare.models.Recipe
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class RecipeAdapter(
    private val context: Context,
    private val recipes: List<Recipe>,
    param: (Any) -> Unit
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val imgDish: ImageView = itemView.findViewById(R.id.imgDish)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe_card, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipes[position]

        holder.tvTitle.text = recipe.name
        holder.tvDescription.text = recipe.description

        val imageUrl = recipe.mainImage ?: ""

        Log.d("RecipeAdapter", "Loading image: $imageUrl")

        if (imageUrl.isNotEmpty()) {
            Glide.with(context)
                .clear(holder.imgDish) // Clear view trước khi load mới
            Glide.with(context)
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(holder.imgDish)
        } else {
            holder.imgDish.setImageResource(R.drawable.ic_placeholder)
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(context, RecipeDetailActivity::class.java)
            intent.putExtra("recipeId", recipe.id)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = recipes.size
}
