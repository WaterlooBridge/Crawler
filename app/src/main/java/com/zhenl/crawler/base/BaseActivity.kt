package com.zhenl.crawler.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Created by lin on 20-1-20.
 */
abstract class BaseActivity : AppCompatActivity() {

    protected abstract val layoutRes: Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutRes)
    }
}