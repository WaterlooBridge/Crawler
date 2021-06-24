package com.zhenl.crawler.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner

/**
 * Created by lin on 2021/6/19.
 */
abstract class BaseFragment<T : ViewDataBinding> : Fragment() {

    protected lateinit var binding: T
    protected abstract val layoutRes: Int

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate<T>(inflater, layoutRes, container, false).also {
            it.lifecycleOwner = this
        }
        initView()
        return binding.root
    }

    open fun initView() {}

    fun <T : BaseViewModel> getViewModel(modelClass: Class<T>): T {
        return getViewModelInternal(this, modelClass)
    }

    private fun <T : BaseViewModel> getViewModelInternal(
        owner: ViewModelStoreOwner,
        modelClass: Class<T>
    ): T {
        val viewModel = ViewModelProvider(owner).get(modelClass)
        return viewModel
    }
}