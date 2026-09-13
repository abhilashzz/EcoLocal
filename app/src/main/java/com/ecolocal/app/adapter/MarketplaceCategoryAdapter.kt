package com.ecolocal.app.adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ecolocal.app.R
import com.ecolocal.app.databinding.ItemMarketplaceCategoryBinding
import com.ecolocal.app.model.MarketCategory

class MarketplaceCategoryAdapter(
    private val categories: List<MarketCategory>,
    private var selectedCategory: String? = null,
    private val onItemClick: (MarketCategory) -> Unit = {}
) : RecyclerView.Adapter<MarketplaceCategoryAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemMarketplaceCategoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMarketplaceCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = categories[position]
        val context = holder.binding.root.context
        val isSelected = selectedCategory != null && item.title.equals(selectedCategory, ignoreCase = true)

        holder.binding.tvCatLabel.text = item.title
        holder.binding.ivCatIcon.setImageResource(item.iconRes)

        if (isSelected) {
            holder.binding.flCatCircle.setBackgroundResource(R.drawable.bg_mkt_cat_circle_selected)
            holder.binding.ivCatIcon.setColorFilter(ContextCompat.getColor(context, R.color.eco_chip_selected_text))
            holder.binding.tvCatLabel.setTextColor(ContextCompat.getColor(context, R.color.eco_green_primary))
            holder.binding.tvCatLabel.setTypeface(null, Typeface.BOLD)
        } else {
            holder.binding.flCatCircle.setBackgroundResource(R.drawable.bg_mkt_cat_circle)
            holder.binding.ivCatIcon.clearColorFilter()
            holder.binding.tvCatLabel.setTextColor(ContextCompat.getColor(context, R.color.eco_secondary_dark))
            holder.binding.tvCatLabel.setTypeface(null, Typeface.NORMAL)
        }

        holder.binding.root.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = categories.size

    fun setSelectedCategory(category: String?) {
        selectedCategory = category
        notifyDataSetChanged()
    }
}
