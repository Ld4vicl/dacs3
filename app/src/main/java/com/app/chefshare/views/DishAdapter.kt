package com.app.chefshare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.chefshare.R
import com.app.chefshare.models.Dish
import com.bumptech.glide.Glide

class DishAdapter(private val dishes: List<Dish>) :
    RecyclerView.Adapter<DishAdapter.DishViewHolder>() {

    inner class DishViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        val tvAuthor: TextView = itemView.findViewById(R.id.tvAuthor)
        val imgDish: ImageView = itemView.findViewById(R.id.imgDish)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DishViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_dish, parent, false)
        return DishViewHolder(view)
    }

    override fun onBindViewHolder(holder: DishViewHolder, position: Int) {
        val dish = dishes[position]
        holder.tvTitle.text = dish.title
        holder.tvDescription.text = dish.description
        holder.tvAuthor.text = dish.author

        Glide.with(holder.itemView.context)
            .load(dish.imageUrl)
            .into(holder.imgDish)
    }


    override fun getItemCount(): Int = dishes.size
}
