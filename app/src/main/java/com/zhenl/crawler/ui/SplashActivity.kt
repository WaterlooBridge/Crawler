package com.zhenl.crawler.ui

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import com.zhenl.crawler.R
import com.zhenl.crawler.base.BaseActivity
import com.zhenl.crawler.databinding.ActivitySplashBinding
import com.zhenl.crawler.ui.home.HomeActivity
import com.zhenl.crawler.utils.PreloadHelper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : BaseActivity<ActivitySplashBinding>() {

    override val layoutRes: Int = R.layout.activity_splash

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val startHome = MutableLiveData<Boolean>().also {
            it.observe(this) {
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }
        }

        if (PreloadHelper.hasPreload) {
            startHome.value = true
            return
        }

        PreloadHelper.preload()

        binding.tvSkip.setOnClickListener { startHome.value = true }

        GlobalScope.launch {
            delay(5000)
            startHome.postValue(true)
        }
    }

}
