package com.zhenl.crawler.utils

import android.content.Intent
import com.zhenl.crawler.Constants
import com.zhenl.crawler.MyApplication
import com.zhenl.crawler.engines.SearchEngineImpl3
import com.zhenl.crawler.services.ACTION_FOO
import com.zhenl.crawler.services.DownloadService
import com.zhenl.crawler.services.EXTRA_PARAM1
import com.zhenl.crawler.ui.SplashActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Created by lin on 2022/1/30.
 */
object PreloadHelper {

    fun preload() {
        GlobalScope.launch(Dispatchers.Main.immediate) {
            val response = withContext(Dispatchers.IO) {
                val baseUrl = "https://gitee.com/lin037/sunmi/raw/master/"
                HttpUtil.getSync(baseUrl + "crawler.json")
            }
            var versionCode = 0
            response?.let {
                val json = JSONObject(it)
                versionCode = json.optInt("versionCode")
                if (Constants.DEBUG)
                    return@let
                Constants.API_HOST = json.optString("crawler_host")
                Constants.API_HOST2 = json.optString("crawler_host2")
                Constants.API_HOST4 = json.optString("crawler_host4")
                SearchEngineImpl3.baseUrl = json.optString("crawler_host3")
            }
            downloadJs(versionCode)
        }
    }

    private fun downloadJs(versionCode: Int) {
        MyApplication.instance.startService(
            Intent(MyApplication.instance, DownloadService::class.java)
                .setAction(ACTION_FOO)
                .putExtra(EXTRA_PARAM1, versionCode)
        )
    }
}