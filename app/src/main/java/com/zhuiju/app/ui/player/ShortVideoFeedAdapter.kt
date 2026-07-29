package com.zhuiju.app.ui.player

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zhuiju.app.data.ShortVideo
import com.zhuiju.app.databinding.ItemShortVideoBinding

/**
 * 短视频 Feed 流适配器
 *
 * - 配合 ViewPager2 实现上下滑动切换
 * - 每个 Item 持有独立 TextureView（配合播放器池）
 * - 极简布局，保证 60 帧滑动
 */
class ShortVideoFeedAdapter :
    ListAdapter<ShortVideo, ShortVideoFeedAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemShortVideoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemShortVideoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ShortVideo) {
            binding.tvAuthor.text = "@${item.author}"
            binding.tvDesc.text = item.description
            // 加载头像
            Glide.with(binding.ivAvatar)
                .load(item.avatarUrl)
                .circleCrop()
                .into(binding.ivAvatar)
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ShortVideo>() {
            override fun areItemsTheSame(oldItem: ShortVideo, newItem: ShortVideo): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ShortVideo, newItem: ShortVideo): Boolean {
                return oldItem == newItem
            }
        }
    }
}
