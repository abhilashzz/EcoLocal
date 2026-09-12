package com.ecolocal.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ecolocal.app.databinding.ItemMarketplaceCategoryBinding
import com.ecolocal.app.model.MarketCategory

class MarketplaceCategoryAdapter(
    private val categories: List<MarketCategory>,
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
        holder.binding.tvCatLabel.text = item.title
        holder.binding.ivCatIcon.setImageResource(item.iconRes)
        holder.binding.root.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = categories.size
}
