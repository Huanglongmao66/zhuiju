package com.zhuiju.app.core.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.zhuiju.app.util.LogUtils
import kotlinx.coroutines.launch

/**
 * Fragment 基类
 *
 * - 封装 ViewBinding 绑定
 * - 支持懒加载（[onLazyInit]），仅在页面首次可见时触发
 * - 收集 ViewModel 状态
 * - View 层只做 UI 刷新、控件绑定，禁止写业务逻辑
 */
abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null
    protected val binding: VB get() = _binding!!

    private val TAG = this.javaClass.simpleName

    /** 是否已执行过懒加载 */
    private var isLazyLoaded = false

    /**
     * 子类实现：创建 ViewBinding
     */
    protected abstract fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    /**
     * 子类实现：初始化视图
     */
    protected abstract fun initViews()

    /**
     * 子类实现：初始化数据（每次页面可见都会调用）
     */
    protected abstract fun initData()

    /**
     * 子类实现：懒加载数据（仅页面首次可见时调用一次，适合网络请求）
     */
    protected abstract fun onLazyInit()

    /**
     * 子类实现：收集 ViewModel 状态
     */
    protected abstract fun collectState()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = inflateBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        LogUtils.i("$TAG onViewCreated", TAG)
        initViews()
        collectState()
    }

    override fun onResume() {
        super.onResume()
        if (!isLazyLoaded) {
            isLazyLoaded = true
            onLazyInit()
            LogUtils.i("$TAG 首次懒加载", TAG)
        }
        initData()
    }

    /**
     * 收集 ViewModel 的全局事件
     */
    protected fun bindViewModelEvents(viewModel: BaseViewModel) {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.commonEvent.collect { event ->
                    when (event) {
                        is CommonEvent.Toast -> com.zhuiju.app.util.ToastUtils.show(event.message)
                        is CommonEvent.ShowLoading -> (activity as? BaseActivity<*>)?.showLoading(event.message)
                        is CommonEvent.HideLoading -> (activity as? BaseActivity<*>)?.hideLoading()
                        is CommonEvent.Finish -> activity?.finish()
                        is CommonEvent.Navigate -> (activity as? BaseActivity<*>)?.navigateTo(event)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        LogUtils.i("$TAG onDestroyView", TAG)
        _binding = null
    }
}
