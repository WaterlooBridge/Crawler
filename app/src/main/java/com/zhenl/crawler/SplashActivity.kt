package com.zhenl.crawler

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.zhenl.crawler.services.ACTION_FOO
import com.zhenl.crawler.services.DownloadService
import com.zhenl.crawler.services.EXTRA_PARAM1
import com.zhenl.crawler.utils.HttpUtil
import com.zhenl.violet.core.Dispatcher
import org.json.JSONObject

class SplashActivity : AppCompatActivity() {

    @SuppressLint("HandlerLeak")
    internal var handler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            startActivity(Intent(this@SplashActivity, HomeActivity::class.java))
            startService(Intent(this@SplashActivity, DownloadService::class.java)
                    .setAction(ACTION_FOO)
                    .putExtra(EXTRA_PARAM1, msg.what))
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*set it to be no title*/
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        /*set it to be full screen*/
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)

        setContentView(R.layout.activity_splash)

        load()
    }

    private fun load() {
        Dispatcher.getInstance().enqueue {
            val baseUrl = "https://raw.githubusercontent.com/WaterlooBridge/Crawler/master/"
            val response = HttpUtil.getSync(baseUrl + "config.json")
            var versionCode = 0
            response?.let {
                val json = JSONObject(it)
                versionCode = json.optInt("versionCode")
                Constants.API_HOST = json.optString("crawler_host")
                Constants.API_HOST2 = json.optString("crawler_host2")
                Constants.API_HOST3 = json.optString("crawler_host3")
            }
            handler.sendEmptyMessage(versionCode)
        }
    }
}