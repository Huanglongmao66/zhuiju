package com.zhuiju.app.ui.player

import android.view.LayoutInflater
import android.view.TextureView
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zhuiju.app.data.ShortVideo
import com.zhuiju.app.databinding.ItemShortVideoBinding
import com.zhuiju.app.util.ToastUtils

/**
 * 短视频 Feed 流适配器
 *
 * - 配合 ViewPager2 实现上下滑动切换
 * - 每个 Item 持有独立 TextureView（配合播放器池）
 * - 极简布局，保证 60 帧滑动
 *
 * 注意：播放逻辑由 [com.zhuiju.app.ui.home.HomeFragment] 通过 ViewPager2
 * 页面切换回调统一接管，Adapter 仅负责 UI 绑定与 TextureView 暴露。
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

    /**
     * 根据 Adapter position 查找当前已 attach 的 ViewHolder
     *
     * 供 HomeFragment 在 [androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback]
     * 中拿到当前页的 TextureView，绑定到全局 ExoPlayer。
     */
    fun findAttachedViewHolder(recyclerView: RecyclerView, position: Int): ViewHolder? {
        val rv = recyclerView.getChildAt(0)?.parent as? RecyclerView ?: recyclerView
        val holder = rv.findViewHolderForAdapterPosition(position) as? ViewHolder
        return holder
    }

    class ViewHolder(val binding: ItemShortVideoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        /** 当前 Item 绑定的视频数据 */
        var boundItem: ShortVideo? = null
            private set

        /** 暴露 TextureView 供 HomeFragment 绑定 ExoPlayer */
        val textureView: TextureView
            get() = binding.textureViewShort

        /** 本地点赞状态（ShortVideo.isLiked 是只读的，需维护副本） */
        private var isLiked = false
        private var likeCount = 0

        fun bind(item: ShortVideo) {
            boundItem = item
            binding.tvAuthor.text = "@${item.author}"
            binding.tvDesc.text = item.description
            // 加载头像
            Glide.with(binding.ivAvatar)
                .load(item.avatarUrl)
                .circleCrop()
                .into(binding.ivAvatar)

            // 互动数据
            isLiked = item.isLiked
            likeCount = item.likeCount
            updateLikeUI()
            binding.tvCommentCount.text = formatCount(item.commentCount)
            binding.tvShareCount.text = formatCount(item.shareCount)

            // 点赞：切换状态 + 缩放动画
            binding.btnLike.setOnClickListener {
                isLiked = !isLiked
                likeCount = if (isLiked) likeCount + 1 else (likeCount - 1).coerceAtLeast(0)
                updateLikeUI()
                animateLike()
                if (isLiked) ToastUtils.show("已点赞")
            }

            // 评论
            binding.btnComment.setOnClickListener {
                ToastUtils.show("评论功能开发中")
            }

            // 分享
            binding.btnShare.setOnClickListener {
                ToastUtils.show("分享功能开发中")
            }
        }

        /** 更新点赞图标和数字 */
        private fun updateLikeUI() {
            binding.btnLike.setImageResource(
                if (isLiked) android.R.drawable.btn_star_big_on
                else android.R.drawable.btn_star_big_off
            )
            binding.tvLikeCount.text = formatCount(likeCount)
        }

        /** 点赞缩放动画 */
        private fun animateLike() {
            binding.btnLike.animate()
                .scaleX(1.3f)
                .scaleY(1.3f)
                .setDuration(100)
                .withEndAction {
                    binding.btnLike.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .setInterpolator(OvershootInterpolator())
                        .start()
                }
                .start()
        }

        private fun formatCount(count: Int): String = when {
            count >= 10000 -> String.format("%.1f万", count / 10000.0)
            count > 0 -> count.toString()
            else -> ""
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
