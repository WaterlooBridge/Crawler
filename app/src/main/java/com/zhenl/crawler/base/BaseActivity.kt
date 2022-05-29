package com.zhenl.crawler.base

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.zhenl.crawler.MyApplication
import com.zhenl.crawler.R
import com.zhenl.crawler.core.Config
import com.zhenl.violet.widget.CircleImageView
import com.zhenl.violet.widget.CircularProgressDrawable

/**
 * Created by lin on 20-1-20.
 */
abstract class BaseActivity<T : ViewDataBinding> : AppCompatActivity() {

    protected lateinit var binding: T
    protected abstract val layoutRes: Int

    init {
        val theme = Config.darkTheme
        AppCompatDelegate.setDefaultNightMode(theme.toInt())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView<T>(this, layoutRes).also {
            it.lifecycleOwner = this
        }
        initView()
        initData()

        MyApplication.globalLoading.observe(this) {
            if (it) showLoading() else hideLoading()
        }
    }

    open fun initView() {}

    open fun initData() {}

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

    private lateinit var mProgress: CircularProgressDrawable

    private val loadingDialog: Dialog by lazy {
        Dialog(this, R.style.loadingDialogStyle).also {
            val circleImageView = CircleImageView(this, Color.WHITE)
            mProgress = CircularProgressDrawable(this)
            mProgress.setStyle(CircularProgressDrawable.LARGE);
            circleImageView.setImageDrawable(mProgress)
            it.setContentView(circleImageView)
            it.setCanceledOnTouchOutside(false)
            val size = resources.getDimensionPixelSize(R.dimen.loading_dialog_size)
            it.window?.apply {
                attributes.width = size
                attributes.height = size
            }
        }
    }

    private fun showLoading() {
        loadingDialog.show()
        mProgress.start()
    }

    private fun hideLoading() {
        loadingDialog.dismiss()
        mProgress.stop()
    }
}