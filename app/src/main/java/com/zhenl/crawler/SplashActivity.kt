package com.zhenl.crawler

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
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
            startHome.run()
            startService(Intent(MyApplication.getInstance(), DownloadService::class.java)
                    .setAction(ACTION_FOO)
                    .putExtra(EXTRA_PARAM1, msg.what))
        }
    }

    internal var startHome = object : Runnable {
        var hasStart = false

        override fun run() {
            if (hasStart)
                return
            hasStart = true
            startActivity(Intent(this@SplashActivity, HomeActivity::class.java))
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*set it to be no title*/
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        /*set it to be full screen*/
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN or WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES

        setContentView(R.layout.activity_splash)

        load()

        handler.postDelayed(startHome, 5000)
    }

    private fun load() {
        Dispatcher.getInstance().enqueue {
            val baseUrl = "https://raw.githubusercontent.com/WaterlooBridge/Crawler/master/"
            val response = HttpUtil.getSync(baseUrl + "config.json")
            var versionCode = 0
            response?.let {
                val json = JSONObject(it)
                versionCode = json.optInt("versionCode")
                if (Constants.DEBUG)
                    return@let
                Constants.API_HOST = json.optString("crawler_host")
                Constants.API_HOST2 = json.optString("crawler_host2")
                Constants.API_HOST3 = json.optString("crawler_host3")
            }
            handler.sendEmptyMessage(versionCode)
        }
    }
}
