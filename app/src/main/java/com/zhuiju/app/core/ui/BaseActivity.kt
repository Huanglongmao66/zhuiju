package com.zhuiju.app.core.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.zhuiju.app.core.player.PowerManager
import com.zhuiju.app.util.LeakDetector
import com.zhuiju.app.util.LogUtils
import com.zhuiju.app.util.ToastUtils
import kotlinx.coroutines.launch

/**
 * Activity 基类
 *
 * - 封装 ViewBinding 绑定（子类实现 [inflateBinding]）
 * - 收集 ViewModel 全局事件（Toast、跳转、加载框）
 * - 统一状态栏、沉浸式适配入口
 * - View 层只做 UI 刷新、控件绑定、用户事件分发，禁止写业务逻辑
 *
 * 使用方式：
 * ```
 * class MainActivity : BaseActivity<ActivityMainBinding>() {
 *     override fun inflateBinding(): ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)
 *     override fun onCreate(savedInstanceState: Bundle?) { ... }
 * }
 * ```
 */
abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    protected lateinit var binding: VB
        private set

    private val TAG = this.javaClass.simpleName

    /**
     * 子类实现：创建 ViewBinding
     */
    protected abstract fun inflateBinding(): VB

    /**
     * 子类实现：初始化视图（在 [onCreate] 中调用）
     */
    protected abstract fun initViews()

    /**
     * 子类实现：初始化数据（在 [onCreate] 中调用）
     */
    protected abstract fun initData()

    /**
     * 子类实现：收集 ViewModel 状态（在 [onCreate] 中调用）
     */
    protected abstract fun collectState()

    /**
     * 可选重写：是否启用沉浸式状态栏
     */
    protected open val enableImmersive: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = inflateBinding()
        setContentView(binding.root)

        LogUtils.i("$TAG onCreate", TAG)

        // 监控 Activity 内存泄漏（仅 Debug 包生效）
        LeakDetector.watch(this, TAG)

        if (enableImmersive) {
            setupImmersive()
        }

        initViews()
        initData()
        collectState()
    }

    /**
     * 收集 ViewModel 的全局事件
     */
    protected fun bindViewModelEvents(viewModel: BaseViewModel) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.commonEvent.collect { event ->
                    when (event) {
                        is CommonEvent.Toast -> ToastUtils.show(event.message)
                        is CommonEvent.ShowLoading -> showLoading(event.message)
                        is CommonEvent.HideLoading -> hideLoading()
                        is CommonEvent.Finish -> finish()
                        is CommonEvent.Navigate -> navigateTo(event)
                    }
                }
            }
        }
    }

    /**
     * 沉浸式状态栏适配（子类可重写自定义）
     */
    protected open fun setupImmersive() {
        // 默认实现可后续扩展：透明状态栏、状态栏图标颜色等
    }

    /**
     * 显示加载框（子类可重写实现自定义 Loading）
     */
    internal open fun showLoading(message: String) {
        // 默认空实现，后续阶段补充自定义 LoadingDialog
    }

    /**
     * 隐藏加载框
     */
    internal open fun hideLoading() {
        // 默认空实现
    }

    /**
     * 页面跳转（子类可重写实现 Navigation 路由）
     */
    internal open fun navigateTo(event: CommonEvent.Navigate) {
        LogUtils.i("navigateTo: ${event.route}", TAG)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 退出 Activity 时释放屏幕常亮，避免 WakeLock 泄漏
        PowerManager.releaseScreenOn()
        LogUtils.i("$TAG onDestroy", TAG)
    }
}
