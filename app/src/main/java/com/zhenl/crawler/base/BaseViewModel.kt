package com.zhenl.crawler.base

import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhenl.crawler.MyApplication
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

/**
 * Created by lin on 2020/10/3.
 */
abstract class BaseViewModel : ViewModel() {

    private val uiScope = viewModelScope.plus(CoroutineExceptionHandler { _, t ->
        Toast.makeText(MyApplication.instance, t.toString(), Toast.LENGTH_SHORT).show()
    })

    internal fun launch(block: suspend CoroutineScope.() -> Unit) {
        uiScope.launch {
            block()
        }
    }
}