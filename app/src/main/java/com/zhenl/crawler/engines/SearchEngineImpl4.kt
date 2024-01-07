package com.zhenl.crawler.engines

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.zhenl.crawler.Constants
import com.zhenl.crawler.models.DramasModel
import com.zhenl.crawler.models.MovieModel
import com.zhenl.crawler.utils.HttpUtil
import java.net.URLEncoder

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
        val query = if (keyword.isNullOrEmpty()) " " else keyword
        val url = "${Constants.API_HOST4}/search?page=$page&query=${URLEncoder.encode(query)}"
        val document = HttpUtil.getSync(url) ?: return list
        val json = Gson().fromJson(document, JsonObject::class.java)
        val elements = json.getAsJsonObject("data").getAsJsonArray("videos")
        if (elements.size() == 0)
            return list
        val first = elements[0].asJsonObject.get("id").asString
        if (page == 1)
            prevMovie = null
        if (prevMovie == first)
            return list
        prevMovie = first
        for (element in elements) {
            val model = MovieModel()
            val item = element.asJsonObject
            model.url = item.get("id").asString
            model.img = item.get("cover").asString
            model.title = item.get("name").asString
            model.date = item.get("uptodate").asString
            list.add(model)
        }
        return list
    }

    override fun detail(url: String?, callback: DetailCallback?) {
        val document = HttpUtil.getSync("${Constants.API_HOST4}/detail/$url") ?: return
        val json = Gson().fromJson(document, JsonObject::class.java).getAsJsonObject("video")
        val img = json.get("cover").asString
        val summary = json.get("intro").asString
        val list: MutableList<DramasModel> = ArrayList()
        json.getAsJsonObject("playlists").entrySet().forEach {
            val site = if (it.key == "xigua") "vip" else "zj"
            val elements = it.value.asJsonArray
            for (element in elements) {
                val model = DramasModel()
                val item = element.asJsonArray
                model.text = item[0].asString
                model.url = "https://$site.sp-flv.com:8443/?url=${item[1].asString}"
                list.add(model)
            }
        }
        callback?.onSuccess(img, summary, list)
    }

    override fun load(url: String?, callback: Callback?) {
        this.url = url
        this.referer = "https://m.agemys.org/"
        this.callback = callback
        load(url)
    }
}