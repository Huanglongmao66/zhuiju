package com.zhuiju.app.ui.discover

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zhuiju.app.data.Banner
import com.zhuiju.app.databinding.ItemBannerBinding

class BannerAdapter(
    private val onItemClick: (Banner) -> Unit = {}
) : ListAdapter<Banner, BannerAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBannerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemBannerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Banner) {
            binding.tvTitle.text = item.title
            Glide.with(binding.ivBanner).load(item.imageUrl).into(binding.ivBanner)
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Banner>() {
            override fun areItemsTheSame(a: Banner, b: Banner) = a.id == b.id
            override fun areContentsTheSame(a: Banner, b: Banner) = a == b
        }
    }
}
