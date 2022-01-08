package com.zhenl.crawler.engines

import android.text.TextUtils
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import com.zhenl.crawler.Constants
import com.zhenl.crawler.MyApplication
import com.zhenl.crawler.models.DramasModel
import com.zhenl.crawler.models.MovieModel
import com.zhenl.crawler.utils.UrlHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.internal.StringUtil
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.resume

/**
 * Created by lin on 2018/8/25.
 */
class SearchEngineImpl3 internal constructor() : SearchEngine() {

    companion object {
        private const val TAG = "SearchEngineImpl3"
        private var js: String? = null

        @JvmField
        @Volatile
        var baseUrl: String? = null
    }

    private suspend fun loadLink(): String? = withContext(Dispatchers.Main.immediate) {
        suspendCoroutineUninterceptedOrReturn { c: Continuation<String?> ->
            val wv = WebView(MyApplication.instance)
            wv.settings.javaScriptEnabled = true
            wv.settings.blockNetworkImage = true
            wv.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    wv.evaluateJavascript("document.getElementsByClassName('link')[0].innerHTML") { html: String ->
                        GlobalScope.launch { c.resume(ping(html)) }
                    }
                }
            }
            wv.loadUrl(Constants.API_HOST3)
            COROUTINE_SUSPENDED
        }
    }

    private suspend fun ping(html: String): String? = withContext(Dispatchers.IO) {
        val decodeHtml = html.replace("\\u003C", "<")
        Log.e("SearchEngineImpl3", decodeHtml)
        val elements = Jsoup.parse(decodeHtml).select("a")
        for (e in elements) {
            val url = e.attr("href").replace("\\\"", "")
            try {
                val res = Jsoup.connect(url).timeout(5000).execute()
                if (res.statusCode() == 200)
                    return@withContext url
            } catch (t: Throwable) {
                Log.e(TAG, "", t)
            }
        }
        null
    }

    override suspend fun search(page: Int, keyword: String?): List<MovieModel> {
        if (TextUtils.isEmpty(baseUrl))
            baseUrl = loadLink()
        if (TextUtils.isEmpty(baseUrl))
            throw RuntimeException()
        val url = "$baseUrl/s/$keyword/$page.html"
        var res = Jsoup.connect(url).method(Connection.Method.GET)
            .userAgent(Constants.USER_AGENT)
            .followRedirects(false).execute()
        while (res.hasHeader("Location")) {
            var location = res.header("Location")
            if (location != null && location.startsWith("http:/") && location[6] != '/') location =
                location.substring(6)
            val redir = StringUtil.resolve(url, location)
            val connection = Jsoup.connect(redir).method(Connection.Method.GET).data("wd", keyword)
                .data("page", page.toString())
                .userAgent(Constants.USER_AGENT)
            for ((key, value) in res.cookies()) connection.cookie(key, value)
            res = connection.followRedirects(false).execute()
        }
        val document = res.parse()
        val elements = document.select(".p1.m1")
        val list: MutableList<MovieModel> = ArrayList()
        for (element in elements) {
            val model = MovieModel()
            model.url = element.select("a").attr("href")
            model.img = element.select("img").attr("data-original").replace("\n", "")
            model.title = element.select(".name").text()
            model.date = element.select(".actor").first().text()
            if (filter(model)) list.add(model)
        }
        return list
    }

    override fun detail(url: String?, callback: DetailCallback?) {
        val document = Jsoup.connect(baseUrl + url).userAgent(Constants.USER_AGENT).get()
        val img = document.select(".ct-l").select("img").attr("data-original")
            .replace("\n", "")
        val summary = document.select(".tab-jq.ctc").text()
        val elements = document.select(".show_player_gogo a")
        val list: MutableList<DramasModel> = ArrayList()
        for (element in elements) {
            val model = DramasModel()
            model.text = element.text()
            model.url = UrlHelper.makeAbsoluteUrl(baseUrl!!, element.attr("href"))
            list.add(model)
        }
        callback?.onSuccess(img, summary, list)
    }

    override fun load(url: String?, callback: Callback?) {
        this.url = url
        this.callback = callback
        load(url)
    }

    override fun loadJs(): String {
        if (js == null)
            js = loadJs("inject3")
        return js!!
    }

    private fun filter(model: MovieModel): Boolean {
        if ("VIP".equals(model.date, true) || "美女图片" == model.date)
            return false
        return true
    }
}