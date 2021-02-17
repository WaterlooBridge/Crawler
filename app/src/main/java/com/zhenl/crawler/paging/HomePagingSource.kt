package com.zhenl.crawler.paging

import android.net.Uri
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.zhenl.crawler.Constants
import com.zhenl.crawler.engines.SearchEngine
import com.zhenl.crawler.engines.SearchEngine.Companion.findBackgroundImage
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
                val elements = document.select(".vbox>a")
                if (elements.size == 0)
                    return@withContext LoadResult.Error(RuntimeException())
                val list: MutableList<MovieModel> = ArrayList()
                for (element in elements) {
                    val model = MovieModel()
                    model.url = element.attr("href")
                    model.img = element.attr("style").findBackgroundImage()
                    model.title = element.attr("title")
                    model.date = element.text()
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

    override fun getRefreshKey(state: PagingState<Int, MovieModel>): Int? = null
}