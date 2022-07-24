package com.zhenl.crawler.vm

import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.zhenl.crawler.base.BaseViewModel
import com.zhenl.crawler.paging.HomePagingSource

/**
 * Created by lin on 2020/10/3.
 */
class HomeViewModel : BaseViewModel() {

    private var latestSource: HomePagingSource? = null

    var type = 4
        set(value) {
            field = value
            latestSource?.invalidate()
        }

    val movies = Pager(
            PagingConfig(10, 5), 1
    ) {
        HomePagingSource(type).apply {
            latestSource = this
        }
    }.flow

    fun refresh() {
        latestSource?.invalidate()
    }
}