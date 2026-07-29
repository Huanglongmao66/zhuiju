package com.zhuiju.app.ui.rank

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zhuiju.app.data.RankItem
import com.zhuiju.app.databinding.ItemRankBinding

class RankAdapter(
    private val onItemClick: (RankItem) -> Unit = {}
) : ListAdapter<RankItem, RankAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRankBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemRankBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: RankItem) {
            binding.tvRank.text = item.rank.toString()
            // 前三名特殊颜色
            binding.tvRank.setTextColor(when (item.rank) {
                1 -> 0xFFFFD700.toInt()  // 金
                2 -> 0xFFC0C0C0.toInt()  // 银
                3 -> 0xFFCD7F32.toInt()  // 铜
                else -> 0xFF86909C.toInt()
            })
            binding.tvTitle.text = item.video.title
            binding.tvHot.text = "热度 ${formatCount(item.hotValue)}"
            binding.tvCategory.text = item.video.category
            Glide.with(binding.ivCover).load(item.video.coverUrl).into(binding.ivCover)
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    private fun formatCount(count: Long): String = when {
        count >= 10000 -> String.format("%.1f万", count / 10000.0)
        else -> count.toString()
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<RankItem>() {
            override fun areItemsTheSame(a: RankItem, b: RankItem) = a.rank == b.rank
            override fun areContentsTheSame(a: RankItem, b: RankItem) = a == b
        }
    }
}
