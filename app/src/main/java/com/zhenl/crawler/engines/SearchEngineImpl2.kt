package com.zhenl.crawler.engines

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zhenl.crawler.Constants
import com.zhenl.crawler.models.DramasModel
import com.zhenl.crawler.models.MovieModel
import com.zhenl.crawler.utils.HttpUtil
import com.zhenl.crawler.utils.UrlHelper
import org.jsoup.Jsoup
import java.util.*

/**
 * Created by lin on 2018/8/22.
 */
class SearchEngineImpl2 : SearchEngine() {

    companion object {

        private const val SEARCH = "my_search='"
        private var searchUrl: String? = null
        private var js: String? = null

        private fun extractSearchUrl() {
            val document = Jsoup.connect("${Constants.API_HOST2}/search.php").post()
            document.select(".wrap").select("script").forEach {
                var script = it.data()
                if (script.contains(SEARCH)) {
                    script = script.substring(script.indexOf(SEARCH) + SEARCH.length)
                    searchUrl = script.substring(0, script.indexOf("'"))
                    return
                }
            }
        }
    }

    override suspend fun search(page: Int, keyword: String?): List<MovieModel> {
        val list: MutableList<MovieModel> = ArrayList()
        if (page > 1)
            return list
        if (searchUrl == null)
            extractSearchUrl()
        if (searchUrl == null)
            return list
        val json = HttpUtil.getSync("${searchUrl}?q=${keyword}") ?: return list
        Gson().fromJson<List<SearchModel>>(json, (object : TypeToken<List<SearchModel>>() {}).type)
            .forEach {
                val model = MovieModel()
                model.url = it.url
                model.img = it.thumb
                model.title = it.title
                model.date = if (it.lianzaijs.isNullOrEmpty()) it.beizhu else "连载至${it.lianzaijs}集"
                list.add(model)
            }
        return list
    }

    override fun detail(url: String?, callback: DetailCallback?) {
        val detailUrl = UrlHelper.makeAbsoluteUrl(Constants.API_HOST2, url ?: return)
        val document = Jsoup.connect(detailUrl).get()
        val img = document.select(".content .pic img").attr("data-original")
        val summary = document.select("meta[name=description]").attr("content")
        val elements = document.select(".urlli a")
        val list: MutableList<DramasModel> = ArrayList()
        elements.forEach {
            val href = it.attr("href")
            if (href.isNullOrEmpty() || it.attr("target") == "_self")
                return@forEach
            val model = DramasModel()
            model.text = it.text()
            model.url = UrlHelper.makeAbsoluteUrl(detailUrl, href)
            list.add(model)
        }
        callback?.onSuccess(img, summary, list)
    }

    override fun load(url: String?, callback: Callback?) {
        this.url = url
        this.callback = callback
        load(url)
    }

    private data class SearchModel(
        val url: String?,
        val thumb: String?,
        val title: String?,
        val lianzaijs: String?,
        val beizhu: String?
    )
}