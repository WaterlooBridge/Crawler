package com.zhenl.crawler.core

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.zhenl.crawler.MyApplication
import com.zhenl.crawler.preference.PreferenceModel

/**
 * Created by lin on 20-10-30.
 */
object Config : PreferenceModel {

    override val context: Context = MyApplication.instance

    var darkTheme by preference("dark_theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM.toString())
}