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
import com.zhenl.crawler.services.ACTION_FOO
import com.zhenl.crawler.services.DownloadService
import com.zhenl.crawler.services.EXTRA_PARAM1
import com.zhenl.crawler.ui.home.HomeActivity
import com.zhenl.crawler.utils.HttpUtil
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

        load(startHome)

        binding.videoView.let {
            val file = File(externalCacheDir, "splash_video.mp4")
            if (!file.exists())
                file.writeBytes(assets.open("violet.mp4").readBytes())
            it.setVideoURI(file.toUri())
            it.setOnPreparedListener { mp -> mp.setVolume(0f, 0f) }
            it.setOnCompletionListener { startHome.value = true }
        }

        binding.tvSkip.setOnClickListener { startHome.value = true }
    }

    override fun onDestroy() {
        binding.videoView.release(true)
        super.onDestroy()
    }

    companion object {

        private fun downloadJs(versionCode: Int) {
            MyApplication.instance.startService(
                Intent(MyApplication.instance, DownloadService::class.java)
                    .setAction(ACTION_FOO)
                    .putExtra(EXTRA_PARAM1, versionCode)
            )
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
                downloadJs(versionCode)
            }
        }
    }

}
