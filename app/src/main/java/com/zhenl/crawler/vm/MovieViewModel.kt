package com.zhenl.crawler.vm

import androidx.lifecycle.MutableLiveData
import com.zhenl.crawler.base.BaseViewModel
import com.zhenl.crawler.engines.SearchEngineFactory
import com.zhenl.crawler.models.DramasModel
import com.zhenl.crawler.models.MovieModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Created by lin on 2020/10/3.
 */
class MovieViewModel : BaseViewModel() {

    val img = MutableLiveData<String>()
    val summary = MutableLiveData<String>()
    val dsList = MutableLiveData<List<DramasModel>>()

    fun loadMovieDetail(url: String) {
        launch {
            withContext(Dispatchers.IO) {
                SearchEngineFactory.create().detail(url) { _img: String?, _summary: String?, list: List<DramasModel>? ->
                    img.postValue(MovieModel.handleImg(_img))
                    summary.postValue(_summary)
                    if (list != null)
                        dsList.postValue(list)
                }
            }
        }
    }
}