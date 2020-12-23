package com.zhenl.crawler.engines

import com.zhenl.crawler.Constants
import com.zhenl.crawler.models.DramasModel
import com.zhenl.crawler.models.MovieModel
import org.jsoup.Jsoup
import java.util.*

/**
 * Created by lin on 2018/8/22.
 */
class SearchEngineImpl2 : SearchEngine() {

    private var prevMovie: String? = null

    override suspend fun search(page: Int, keyword: String?): List<MovieModel> {
        val document = Jsoup.connect(Constants.API_HOST2 + "/search.php").data("searchword", keyword)
                .data("page", page.toString()).get()
        val elements = document.select(".v-thumb.stui-vodlist__thumb")
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
            model.img = element.attr("data-original")
            model.title = element.attr("title")
            model.date = element.select(".pic-text").text()
            list.add(model)
        }
        return list
    }

    override fun detail(url: String?, callback: DetailCallback?) {
        val document = Jsoup.connect(Constants.API_HOST2 + url).get()
        val img = document.select(".stui-vodlist__thumb img").attr("data-original")
        val summary = document.select("meta[name=description]").attr("content")
        val elements = document.select(".stui-content__playlist a")
        val list: MutableList<DramasModel> = ArrayList()
        for (element in elements) {
            val model = DramasModel()
            model.text = element.text()
            model.url = element.attr("href")
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
            js = loadJs("inject2")
        }
        return js!!
    }

    companion object {
        private var js: String? = null
    }
}