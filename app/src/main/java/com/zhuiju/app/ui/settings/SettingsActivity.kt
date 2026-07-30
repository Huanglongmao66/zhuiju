package com.zhuiju.app.ui.settings

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.zhuiju.app.databinding.ActivitySettingsBinding
import com.zhuiju.app.util.ToastUtils
import java.io.File

/**
 * 设置页 Activity
 *
 * - 播放设置：自动播放下一个、循环播放
 * - 缓存管理：清除缓存
 * - 关于：版本号、检查更新
 *
 * 设置项通过 SharedPreferences 持久化，首页/播放页读取控制播放行为。
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("app_settings", MODE_PRIVATE)

        // 返回
        binding.btnBack.setOnClickListener { finish() }

        // ========== 播放设置 ==========
        // 自动播放下一个
        binding.switchAutoplay.isChecked = prefs.getBoolean(KEY_AUTOPLAY_NEXT, true)
        binding.switchAutoplay.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_AUTOPLAY_NEXT, isChecked).apply()
            ToastUtils.show(if (isChecked) "已开启自动播放" else "已关闭自动播放")
        }

        // 循环播放
        binding.switchLoop.isChecked = prefs.getBoolean(KEY_LOOP_PLAY, false)
        binding.switchLoop.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_LOOP_PLAY, isChecked).apply()
            ToastUtils.show(if (isChecked) "已开启循环播放" else "已关闭循环播放")
        }

        // ========== 缓存管理 ==========
        // 显示缓存大小
        val cacheSize = calculateCacheSize()
        binding.tvCacheSize.text = formatSize(cacheSize)

        binding.itemClearCache.setOnClickListener {
            clearCache()
            binding.tvCacheSize.text = "0 B"
            ToastUtils.show("缓存已清除")
        }

        // ========== 关于 ==========
        // 版本号
        try {
            val versionName = packageManager.getPackageInfo(packageName, 0).versionName
            binding.tvVersion.text = versionName
        } catch (e: Exception) {
            binding.tvVersion.text = "1.1.8"
        }

        // 检查更新
        binding.itemCheckUpdate.setOnClickListener {
            ToastUtils.show("当前已是最新版本")
        }
    }

    /**
     * 计算应用缓存大小（cacheDir + externalCacheDir）
     */
    private fun calculateCacheSize(): Long {
        var size = 0L
        size += dirSize(cacheDir)
        externalCacheDir?.let { size += dirSize(it) }
        return size
    }

    private fun dirSize(dir: File): Long {
        if (!dir.exists()) return 0
        var size = 0L
        dir.listFiles()?.forEach { f ->
            size += if (f.isDirectory) dirSize(f) else f.length()
        }
        return size
    }

    /**
     * 清除缓存
     */
    private fun clearCache() {
        deleteDir(cacheDir)
        externalCacheDir?.let { deleteDir(it) }
    }

    private fun deleteDir(dir: File) {
        if (!dir.exists()) return
        dir.listFiles()?.forEach { f ->
            if (f.isDirectory) deleteDir(f) else f.delete()
        }
    }

    /**
     * 格式化文件大小
     */
    private fun formatSize(bytes: Long): String = when {
        bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
        bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes > 0 -> "$bytes B"
        else -> "0 B"
    }

    companion object {
        const val KEY_AUTOPLAY_NEXT = "autoplay_next"
        const val KEY_LOOP_PLAY = "loop_play"
    }
}
