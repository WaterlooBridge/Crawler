package com.zhenl.crawler

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.zhenl.violet.core.ResMgr

/**
 * Created by lin on 2018/8/23.
 */
class MyApplication : Application() {

    companion object {
        lateinit var instance: MyApplication
        val globalLoading = MutableLiveData<Boolean>()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        ResMgr.init(this)
    }
}