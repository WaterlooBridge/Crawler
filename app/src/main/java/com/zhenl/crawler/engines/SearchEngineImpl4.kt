package com.zhenl.crawler.engines

import com.zhenl.crawler.Constants
import com.zhenl.crawler.models.DramasModel
import com.zhenl.crawler.models.MovieModel
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.util.*

/**
 * Created by lin on 2020/10/3.
 */
class SearchEngineImpl4 : SearchEngine() {

    companion object {
        var js: String? = null
    }

    private var prevMovie: String? = null

    override suspend fun search(page: Int, keyword: String?): MutableList<MovieModel> {
        val list: MutableList<MovieModel> = ArrayList()
        val url = "${Constants.API_HOST4}/search.asp?page=$page&searchword=${URLEncoder.encode(keyword, "gb2312")}"
        val document = Jsoup.connect(url).get()
        val elements = document.select(".am-gallery-item a")
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
            model.img = element.select("img").attr("data-original")
            model.title = element.select(".am-gallery-title").text()
            model.date = element.select(".am-gallery-desc").text().split(" ").last()
            list.add(model)
        }
        return list
    }

    override fun detail(url: String?, callback: DetailCallback?) {
        val document = Jsoup.connect(Constants.API_HOST4 + url).get()
        val img = document.select(".am-intro-left img").attr("src")
        val summary = document.select(".txtDesc").text()
        val list: MutableList<DramasModel> = ArrayList()
        document.select(".am-tab-panel").forEach {
            val elements = it.select(".mvlist a")
            for (element in elements) {
                if (element.attr("rel") == "nofollow")
                    continue
                val model = DramasModel()
                model.text = element.text()
                model.url = element.attr("href")
                list.add(model)
            }
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
}