package com.zhenl.crawler.views

import android.content.Context
import android.view.View
import android.widget.ImageView
import com.zhenl.crawler.R
import kotlinx.android.synthetic.main.layout_media_controller.view.*
import tv.danmaku.ijk.media.widget.AndroidMediaController

/**
 * Created by lin on 2022/1/30.
 */
open class AppMediaController(context: Context) : AndroidMediaController(context, false) {

    protected var mFullscreenRes = R.drawable.ic_fullscreen
    protected var mFullscreenExitRes = R.drawable.ic_fullscreen_exit

    private var mFullscreen: ImageView? = null
    private var mStepForward: View? = null
    private var mDoubleSpeed: View? = null

    private var mFullscreenClickListener: OnClickListener? = null
    private var mStepForwardClickListener: OnClickListener? = null
    private var mDoubleSpeedClickListener: OnClickListener? = null

    init {
        mMediaPlayRes = R.drawable.ic_media_play
        mMediaPauseRes = R.drawable.ic_media_pause
    }

    override fun getControllerLayoutId(): Int {
        return R.layout.layout_media_controller
    }

    override fun onViewCreated(view: View) {
        mFullscreen = view.controller_fullscreen
        mFullscreenClickListener?.let {
            mFullscreen?.setOnClickListener(it)
        }

        mStepForward = view.controller_step_forward
        mStepForwardClickListener?.let {
            mStepForward?.setOnClickListener(it)
        }

        mDoubleSpeed = view.controller_double_speed
        mDoubleSpeedClickListener?.let {
            mDoubleSpeed?.setOnClickListener(it)
        }
    }

    fun setOnFullscreenClickListener(listener: OnClickListener) {
        mFullscreenClickListener = listener
        mFullscreen?.setOnClickListener(listener)
    }

    fun onFullscreenChanged(fullscreen: Boolean) {
        hide()
        mFullscreen?.setImageResource(if (fullscreen) mFullscreenExitRes else mFullscreenRes)
    }

    fun setOnStepForwardClickListener(listener: OnClickListener) {
        mStepForwardClickListener = listener
        mStepForward?.setOnClickListener(listener)
    }

    fun setOnDoubleSpeedClickListener(listener: OnClickListener) {
        mDoubleSpeedClickListener = listener
        mDoubleSpeed?.setOnClickListener(listener)
    }
}