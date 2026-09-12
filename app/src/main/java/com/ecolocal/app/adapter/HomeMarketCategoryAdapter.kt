package com.ecolocal.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ecolocal.app.databinding.ItemHomeMarketCategoryBinding
import com.ecolocal.app.model.MarketCategory

class HomeMarketCategoryAdapter(
    private val categories: List<MarketCategory>,
    private val onItemClick: (MarketCategory) -> Unit = {}
) : RecyclerView.Adapter<HomeMarketCategoryAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemHomeMarketCategoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomeMarketCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = categories[position]
        holder.binding.tvMarketTitle.text = item.title
        holder.binding.ivMarketIcon.setImageResource(item.iconRes)
        holder.binding.root.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = categories.size
}
