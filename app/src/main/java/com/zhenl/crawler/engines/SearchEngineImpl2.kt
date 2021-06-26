package com.zhenl.crawler.engines

import com.zhenl.crawler.Constants
import com.zhenl.crawler.models.DramasModel
import com.zhenl.crawler.models.MovieModel
import com.zhenl.crawler.utils.UrlHelper
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.util.*

/**
 * Created by lin on 2018/8/22.
 */
class SearchEngineImpl2 : SearchEngine() {

    private var prevMovie: String? = null

    override suspend fun search(page: Int, keyword: String?): List<MovieModel> {
        val wd = URLEncoder.encode(keyword, "UTF-8")
        val document = Jsoup.connect("${Constants.API_HOST2}/vod/search/-pg-1-wd-${wd}.html").get()
        val elements = document.select(".itemBox")
        val list: MutableList<MovieModel> = ArrayList()
        if (elements.size == 0)
            return list
        val first = elements[0].select(".itemImg a").attr("href")
        if (page == 1)
            prevMovie = null
        if (prevMovie == first)
            return list
        prevMovie = first
        for (element in elements) {
            val model = MovieModel()
            model.url = element.select(".itemImg a").attr("href")
            model.img = element.select(".itemImg img").attr("src")
            model.title = element.select(".itemTxt .title").text()
            model.date = element.select(".itemTxt .date").text()
            list.add(model)
        }
        return list
    }

    override fun detail(url: String?, callback: DetailCallback?) {
        val document = Jsoup.connect(Constants.API_HOST2 + url).get()
        val img = document.select(".Introduct_Sub .pic img").attr("src")
        val summary = document.select("meta[name=description]").attr("content")
        val elements = document.select(".Drama a")
        val list: MutableList<DramasModel> = ArrayList()
        for (element in elements) {
            val model = DramasModel()
            model.text = element.text()
            model.url = UrlHelper.makeAbsoluteUrl(Constants.API_HOST2, element.attr("href"))
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
            js = loadJs("inject3")
        }
        return js!!
    }

    companion object {
        private var js: String? = null
    }
}