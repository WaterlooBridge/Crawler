package com.zhenl.crawler.paging

import androidx.paging.PagingSource
import com.zhenl.crawler.engines.SearchEngineFactory
import com.zhenl.crawler.models.MovieModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.HttpStatusException

/**
 * Created by lin on 2020/10/3.
 */
class SearchPagingSource(private val keyword: String) : PagingSource<Int, MovieModel>() {

    private val engine = SearchEngineFactory.create()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MovieModel> = withContext(Dispatchers.IO) {
        try {
            val list = engine.search(params.key ?: 1, keyword)
            LoadResult.Page(list, null, if (list.isNullOrEmpty()) null else params.key?.plus(1))
        } catch (e: Exception) {
            if (e is HttpStatusException && e.statusCode == 404)
                return@withContext LoadResult.Page(emptyList(), null, null)
            LoadResult.Error(e)
        }
    }
}