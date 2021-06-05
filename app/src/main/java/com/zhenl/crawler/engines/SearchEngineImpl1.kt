package com.zhenl.crawler.engines

import android.os.Build
import android.webkit.WebResourceResponse
import android.webkit.WebView
import com.zhenl.crawler.BuildConfig
import com.zhenl.crawler.Constants
import com.zhenl.crawler.models.DramasModel
import com.zhenl.crawler.models.MovieModel
import com.zhenl.crawler.utils.UrlHelper
import com.zhenl.violet.core.Dispatcher
import org.jsoup.Jsoup
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

/**
 * Created by lin on 2018/8/22.
 */
class SearchEngineImpl1 : SearchEngine() {

    private var prevMovie: String? = null

    override suspend fun search(page: Int, keyword: String?): List<MovieModel> {
        val url = if (page > 1) Constants.API_HOST + "/s/" + keyword + "/" + page + ".html" else Constants.API_HOST + "/search?wd=" + keyword
        val document = Jsoup.connect(url).userAgent(Constants.USER_AGENT).get()
        val elements = document.select(".vbox>a")
        val list: MutableList<MovieModel> = ArrayList()
        if (elements.size == 0)
            return list
        val first = elements[0].attr("href")
        if (page == 1)
            prevMovie = null
        if (prevMovie == first)
            return list
        prevMovie = first
        for (element in elements) {
            val model = MovieModel()
            model.url = element.attr("href")
            model.img = element.attr("style").findBackgroundImage()
            model.title = element.attr("title")
            model.date = element.text()
            list.add(model)
        }
        return list
    }

    override fun detail(url: String?, callback: DetailCallback?) {
        val document = Jsoup.connect(Constants.API_HOST + url).userAgent(Constants.USER_AGENT).get()
        val img = document.select(".dbox .img").attr("style").findBackgroundImage()
        val summary = document.select(".tbox_js").text()
        val elements = document.select(".show a")
        val list: MutableList<DramasModel> = ArrayList()
        for (element in elements) {
            val model = DramasModel()
            model.text = element.text()
            model.url = UrlHelper.makeAbsoluteUrl(Constants.API_HOST, element.attr("href"))
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
        if (js == null) {
            js = loadJs("inject")
        }
        return js!!
    }

    @Throws(Exception::class)
    private fun loadData() {
        referer = url
        var document = Jsoup.connect(url).userAgent(Constants.USER_AGENT).get()
        url = document.select("iframe").attr("src")
        document = Jsoup.connect(url).referrer(referer).get()
        val elements = document.select("iframe")
        if (elements != null && elements.size > 0) url = document.select("iframe").attr("src")
    }

    override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
        val suffix = "package=" + BuildConfig.APPLICATION_ID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && url.endsWith(suffix)) {
            try {
                val uri = URL(url.substring(0, url.length - suffix.length - 1))
                val connection = uri.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                if (connection.responseCode == 200) {
                    val `is` = connection.inputStream
                    val map: MutableMap<String, String> = HashMap()
                    map["Access-Control-Allow-Origin"] = "*"
                    val response = WebResourceResponse("application/json", "utf-8", `is`)
                    response.responseHeaders = map
                    return response
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return super.shouldInterceptRequest(view, url)
    }

    companion object {
        private var js: String? = null
    }
}