package com.zhenl.crawler.services

import android.app.IntentService
import android.content.Context
import android.content.Intent
import com.zhenl.crawler.MyApplication
import com.zhenl.crawler.utils.HttpUtil
import org.json.JSONObject


// TODO: Rename actions, choose action names that describe tasks that this
// IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
const val ACTION_FOO = "com.zhenl.crawler.services.action.FOO"
const val ACTION_BAZ = "com.zhenl.crawler.services.action.BAZ"

// TODO: Rename parameters
const val EXTRA_PARAM1 = "com.zhenl.crawler.services.extra.PARAM1"
const val EXTRA_PARAM2 = "com.zhenl.crawler.services.extra.PARAM2"

/**
 * An [IntentService] subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * TODO: Customize class - update intent actions and extra parameters.
 */
class DownloadService : IntentService("DownloadService") {

    override fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_FOO -> {
                val param1 = intent.getIntExtra(EXTRA_PARAM1, 0)
                val param2 = intent.getStringExtra(EXTRA_PARAM2)
                handleActionFoo(param1, param2)
            }
            ACTION_BAZ -> {
                val param1 = intent.getStringExtra(EXTRA_PARAM1)
                val param2 = intent.getStringExtra(EXTRA_PARAM2)
                handleActionBaz(param1, param2)
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private fun handleActionFoo(originVersionCode: Int, param2: String?) {
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

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private fun handleActionBaz(param1: String, param2: String) {
        TODO("Handle action Baz")
    }
}
