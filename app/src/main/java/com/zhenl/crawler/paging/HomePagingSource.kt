package com.zhenl.crawler.paging

import android.net.Uri
import androidx.paging.PagingSource
import com.zhenl.crawler.Constants
import com.zhenl.crawler.models.MovieModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.lang.Exception
import java.util.*

/**
 * Created by lin on 2020/10/3.
 */
class HomePagingSource(private val type: Int) : PagingSource<Int, MovieModel>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MovieModel> = withContext(Dispatchers.IO) {
        try {
            val document = Jsoup.connect(Constants.API_HOST + "/type/" + type + "/" + params.key + ".html")
                    .userAgent(Constants.USER_AGENT).get()
            if (document.location().startsWith(Constants.API_HOST)) {
                val elements = document.select(".p1")
                val list: MutableList<MovieModel> = ArrayList()
                for (element in elements) {
                    val model = MovieModel()
                    model.url = element.select("a").attr("href")
                    model.img = element.select("img").attr("src")
                    model.title = element.select(".name").text()
                    model.date = element.select(".other i").text()
                    list.add(model)
                }
                LoadResult.Page(list, null, params.key?.plus(1))
            } else {
                val uri = Uri.parse(document.location())
                Constants.API_HOST = uri.scheme + "://" + uri.host
                load(params)
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}