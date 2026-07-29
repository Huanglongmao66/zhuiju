package com.zhuiju.app.ui.discover

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zhuiju.app.data.Video
import com.zhuiju.app.databinding.ItemVideoCardBinding

class DiscoverAdapter(
    private val onItemClick: (Video) -> Unit = {}
) : ListAdapter<Video, DiscoverAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVideoCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemVideoCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Video) {
            binding.tvTitle.text = item.title
            binding.tvPlayCount.text = formatCount(item.playCount)
            binding.tvRating.text = String.format("%.1f", item.rating)
            Glide.with(binding.ivCover).load(item.coverUrl).into(binding.ivCover)
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    private fun formatCount(count: Long): String = when {
        count >= 10000 -> String.format("%.1f万", count / 10000.0)
        else -> count.toString()
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Video>() {
            override fun areItemsTheSame(a: Video, b: Video) = a.id == b.id
            override fun areContentsTheSame(a: Video, b: Video) = a == b
        }
    }
}
