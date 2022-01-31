package com.zhenl.crawler.ui

import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import com.zhenl.crawler.Constants
import com.zhenl.crawler.MyApplication
import com.zhenl.crawler.R
import com.zhenl.crawler.base.BaseActivity
import com.zhenl.crawler.databinding.ActivitySplashBinding
import com.zhenl.crawler.engines.SearchEngineImpl3
import com.zhenl.crawler.services.ACTION_FOO
import com.zhenl.crawler.services.DownloadService
import com.zhenl.crawler.services.EXTRA_PARAM1
import com.zhenl.crawler.ui.home.HomeActivity
import com.zhenl.crawler.utils.HttpUtil
import com.zhenl.crawler.utils.PreloadHelper
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File

class SplashActivity : BaseActivity<ActivitySplashBinding>() {

    override val layoutRes: Int = R.layout.activity_splash

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val startHome = MutableLiveData<Boolean>().also {
            it.observe(this, {
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            })
        }

        PreloadHelper.preload()

        binding.tvSkip.setOnClickListener { startHome.value = true }

        GlobalScope.launch {
            delay(5000)
            startHome.postValue(true)
        }
    }

}
