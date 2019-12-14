package com.zhenl.crawler.views

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.*
import android.widget.FrameLayout
import com.zhenl.crawler.MainActivity
import com.zhenl.crawler.MyApplication
import com.zhenl.crawler.R
import com.zhenl.crawler.core.RecordAgent
import kotlinx.android.synthetic.main.layout_float_video.view.*
import tv.danmaku.ijk.media.widget.IPCVideoView
import kotlin.math.abs

/**
 * Created by lin on 19-12-9.
 */
class FloatVideoView : FrameLayout, View.OnClickListener {

    companion object {
        private val IC_MEDIA_PAUSE_ID = Resources.getSystem().getIdentifier("ic_media_pause", "drawable", "android")
        private val IC_MEDIA_PLAY_ID = Resources.getSystem().getIdentifier("ic_media_play", "drawable", "android")

        fun isFloatWindowOpAllowed(): Boolean {
            val context = MyApplication.getInstance()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.packageName))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return false
            }
            return true
        }

        fun showFloatWindow(videoView: IPCVideoView, url: String) {
            val context = MyApplication.getInstance()
            val floatView = FloatVideoView(context)
            floatView.showFloatWindow(videoView, url)
        }

        private fun windowType(): Int {
            return when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                else -> WindowManager.LayoutParams.TYPE_TOAST
            }
        }

        private val videoMap = HashMap<String, IPCVideoView>()

        fun getVideoView(id: String): IPCVideoView? {
            return videoMap.remove(id)
        }
    }

    private lateinit var videoView: IPCVideoView
    private lateinit var wm: WindowManager
    private lateinit var wmParams: WindowManager.LayoutParams
    private val controller: View
    private var url: String? = null
    private var mTouchSlop = 3
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
        controller = LayoutInflater.from(context).inflate(R.layout.layout_float_video, this, false)
        addView(controller)
        setOnClickListener(this)
        iv_close.setOnClickListener(this)
        iv_fullscreen.setOnClickListener(this)
        btn_play.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.iv_close -> {
                release()
            }
            R.id.iv_fullscreen -> {
                videoMap[videoView.toString()] = videoView
                val context = MyApplication.getInstance()
                val intent = Intent(context, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra("isFromFloatWindow", true)
                intent.putExtra("viewId", videoView.toString())
                intent.putExtra("url", url)
                context.startActivity(intent)
            }
            R.id.btn_play -> {
                if (videoView.isPlaying) {
                    btn_play.setImageResource(IC_MEDIA_PLAY_ID)
                    videoView.pause()
                } else {
                    btn_play.setImageResource(IC_MEDIA_PAUSE_ID)
                    videoView.start()
                    fadeOut.run()
                }
                handler.removeCallbacks(fadeOut)
            }
            else -> {
                controller.visibility = if (controller.visibility == View.GONE) View.VISIBLE else View.GONE
                btn_play.setImageResource(if (videoView.isPlaying) IC_MEDIA_PAUSE_ID else IC_MEDIA_PLAY_ID)
                handler.removeCallbacks(fadeOut)
                handler.postDelayed(fadeOut, 3000)
            }
        }
    }

    fun showFloatWindow(videoView: IPCVideoView, url: String?) {
        this.url = url
        this.videoView = videoView
        addView(videoView, 0)
        wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wmParams = WindowManager.LayoutParams()
        wmParams.type = windowType()
        wmParams.format = PixelFormat.RGBA_8888
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        wmParams.gravity = Gravity.CENTER
        val dm = DisplayMetrics()
        wm.defaultDisplay.getMetrics(dm)
        val width = (if (dm.heightPixels < dm.widthPixels) dm.heightPixels else dm.widthPixels) / 5 * 4
        wmParams.width = width
        wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        wm.addView(this, wmParams)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null)
            return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                lastX = downX
                downY = event.rawY
                lastY = downY
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = (event.rawX - lastX).toInt()
                val deltaY = (event.rawY - lastY).toInt()
                wmParams.x += deltaX
                wmParams.y += deltaY
                wm.updateViewLayout(this, wmParams)
                lastX = event.rawX
                lastY = event.rawY
            }
            MotionEvent.ACTION_UP -> if (isClickable && abs(event.rawX - downX) < mTouchSlop
                    && abs(event.rawY - downY) < mTouchSlop)
                performClick()
        }
        return true
    }

    private fun release() {
        handler.removeCallbacks(fadeOut)
        RecordAgent.getInstance().record(url, videoView.duration, videoView.currentPosition)
        videoMap.remove(videoView.toString())
        videoView.release(true)
        wm.removeViewImmediate(this)
    }

    private val fadeOut = Runnable {
        controller.visibility = View.GONE
    }
}