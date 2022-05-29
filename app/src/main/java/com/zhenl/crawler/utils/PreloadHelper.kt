package com.zhenl.crawler.utils

import android.content.Context
import com.zhenl.crawler.Constants
import com.zhenl.crawler.MyApplication
import com.zhenl.crawler.engines.SearchEngineImpl3
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Created by lin on 2022/1/30.
 */
object PreloadHelper {

    private var _hasPreload = false
    val hasPreload: Boolean
        get() = _hasPreload

    fun preload() {
        if (Constants.DEBUG) {
            _hasPreload = true
            return
        }
        GlobalScope.launch(Dispatchers.Main.immediate) {
            val response = withContext(Dispatchers.IO) {
                val baseUrl = "https://gitee.com/lin037/sunmi/raw/master/"
                HttpUtil.getSync(baseUrl + "crawler.json")
            }
            response?.let {
                try {
                    val json = JSONObject(it)
                    Constants.API_HOST = json.optString("crawler_host")
                    Constants.API_HOST2 = json.optString("crawler_host2")
                    Constants.API_HOST4 = json.optString("crawler_host4")
                    SearchEngineImpl3.baseUrl = json.optString("crawler_host3")
                    _hasPreload = true
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun downloadJs(originVersionCode: Int) {
        val baseUrl = "https://raw.githubusercontent.com/WaterlooBridge/Crawler/master/"
        val sp = MyApplication.instance.getSharedPreferences("search_engine", Context.MODE_PRIVATE)
        val versionCode = sp.getInt("versionCode", 0)
        if (originVersionCode > versionCode) {
            val inject = HttpUtil.getSync(baseUrl + "app/src/main/res/raw/inject.js")
            val inject2 = HttpUtil.getSync(baseUrl + "app/src/main/res/raw/inject2.js")
            val inject3 = HttpUtil.getSync(baseUrl + "app/src/main/res/raw/inject3.js")
            if (inject != null && inject2 != null && inject3 != null)
                sp.edit().putInt("versionCode", originVersionCode)
                    .putString("inject", inject)
                    .putString("inject2", inject2)
                    .putString("inject3", inject3)
                    .apply()
        }
    }
}