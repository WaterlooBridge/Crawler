package com.zhenl.crawler.vm

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.zhenl.crawler.base.BaseViewModel
import com.zhenl.crawler.paging.SearchPagingSource

/**
 * Created by lin on 2020/10/3.
 */
class SearchViewModel : BaseViewModel() {

    private var latestSource: SearchPagingSource? = null

    var keyword = ""
        set(value) {
            field = value
            latestSource?.invalidate()
        }

    val movies = Pager(
            PagingConfig(10, 5), 1
    ) {
        SearchPagingSource(keyword).apply {
            latestSource = this
        }
    }.flow
}