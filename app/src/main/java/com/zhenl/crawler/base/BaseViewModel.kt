package com.zhenl.crawler.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Created by lin on 2020/10/3.
 */
abstract class BaseViewModel : ViewModel() {

    private val uiScope = viewModelScope

    internal fun launch(block: suspend CoroutineScope.() -> Unit) {
        uiScope.launch {
            block()
        }
    }
}