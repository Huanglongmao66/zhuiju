package com.zhuiju.app.ui.player

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zhuiju.app.databinding.ItemShortVideoBinding

/**
 * 短视频 Feed 流适配器
 *
 * - 配合 ViewPager2 实现上下滑动切换
 * - 每个 Item 持有独立 TextureView（配合播放器池）
 * - 极简布局，保证 60 帧滑动
 */
class ShortVideoFeedAdapter :
    ListAdapter<ShortVideoItem, ShortVideoFeedAdapter.ViewHolder>(DIFF_CALLBACK) {

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

        fun bind(item: ShortVideoItem) {
            binding.tvAuthor.text = "@${item.author}"
            binding.tvDesc.text = item.description
            // TODO: 加载头像、设置点赞状态
            // 视频播放由 Fragment/Activity 根据 ViewPager2 页面切换回调统一管理
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ShortVideoItem>() {
            override fun areItemsTheSame(oldItem: ShortVideoItem, newItem: ShortVideoItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ShortVideoItem, newItem: ShortVideoItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}

/**
 * 短视频数据模型
 */
data class ShortVideoItem(
    val id: String,
    val videoUrl: String,
    val coverUrl: String,
    val author: String,
    val avatarUrl: String,
    val description: String,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val shareCount: Int = 0,
    val isLiked: Boolean = false
)
