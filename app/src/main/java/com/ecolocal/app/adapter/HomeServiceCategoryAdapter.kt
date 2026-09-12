package com.ecolocal.app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ecolocal.app.databinding.ItemHomeServiceCategoryBinding
import com.ecolocal.app.model.ServiceCategory

class HomeServiceCategoryAdapter(
    private val categories: List<ServiceCategory>,
    private val onItemClick: (ServiceCategory) -> Unit = {}
) : RecyclerView.Adapter<HomeServiceCategoryAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemHomeServiceCategoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomeServiceCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = categories[position]
        holder.binding.tvCategoryTitle.text = item.title
        holder.binding.ivCategoryIcon.setImageResource(item.iconRes)
        holder.binding.root.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = categories.size
}
