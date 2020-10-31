package com.zhenl.crawler.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.zhenl.crawler.Constants
import com.zhenl.crawler.MyApplication
import com.zhenl.crawler.R
import com.zhenl.crawler.base.BaseActivity
import com.zhenl.crawler.databinding.ActivitySplashBinding
import com.zhenl.crawler.services.ACTION_FOO
import com.zhenl.crawler.services.DownloadService
import com.zhenl.crawler.services.EXTRA_PARAM1
import com.zhenl.crawler.utils.HttpUtil
import kotlinx.coroutines.*
import org.json.JSONObject

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

        load(startHome)

        lifecycleScope.launch {
            delay(5000)
            startHome.value = true
        }
    }

    companion object {

        private fun downloadJs(versionCode: Int) {
            MyApplication.instance.startService(Intent(MyApplication.instance, DownloadService::class.java)
                    .setAction(ACTION_FOO)
                    .putExtra(EXTRA_PARAM1, versionCode))
        }

        private fun load(startHome: MutableLiveData<Boolean>) {
            GlobalScope.launch(Dispatchers.Main.immediate) {
                val response = withContext(Dispatchers.IO) {
                    val baseUrl = "https://raw.githubusercontent.com/WaterlooBridge/Crawler/master/"
                    HttpUtil.getSync(baseUrl + "config.json")
                }
                var versionCode = 0
                response?.let {
                    val json = JSONObject(it)
                    versionCode = json.optInt("versionCode")
                    if (Constants.DEBUG)
                        return@let
                    Constants.API_HOST = json.optString("crawler_host")
                    Constants.API_HOST2 = json.optString("crawler_host2")
                    Constants.API_HOST3 = json.optString("crawler_host3")
                    Constants.API_HOST4 = json.optString("crawler_host4")
                }
                startHome.value = true
                downloadJs(versionCode)
            }
        }
    }

}
