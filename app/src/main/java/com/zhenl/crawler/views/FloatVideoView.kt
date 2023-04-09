package com.zhenl.crawler.views

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.*
import android.widget.FrameLayout
import android.widget.OverScroller
import androidx.core.view.ViewCompat
import com.zhenl.crawler.MyApplication
import com.zhenl.crawler.R
import com.zhenl.crawler.core.RecordAgent
import com.zhenl.crawler.models.VideoModel
import com.zhenl.crawler.ui.MainActivity
import kotlinx.android.synthetic.main.layout_float_video.view.*
import tv.zhenl.media.VideoPlayerView


/**
 * Created by lin on 19-12-9.
 */
class FloatVideoView : FrameLayout, View.OnClickListener {

    companion object {

        fun isFloatWindowOpAllowed(): Boolean {
            val context = MyApplication.instance
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                try {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + context.packageName)
                    )
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return false
            }
            return true
        }

        fun showFloatWindow(
            videoView: VideoPlayerView,
            model: VideoModel,
            playlist: ArrayList<VideoModel>? = null
        ) {
            val context = MyApplication.instance
            val floatView = FloatVideoView(context)
            floatView.showFloatWindow(videoView, model, playlist)
        }

        private fun windowType(): Int {
            return when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                else -> WindowManager.LayoutParams.TYPE_TOAST
            }
        }

        private val videoMap = HashMap<String, VideoPlayerView>()

        fun getVideoView(id: String): VideoPlayerView? {
            return videoMap.remove(id)
        }
    }

    private val mMediaPauseRes = R.drawable.ic_media_pause
    private val mMediaPlayRes = R.drawable.ic_media_play

    private lateinit var videoView: VideoPlayerView
    private lateinit var wm: WindowManager
    private lateinit var wmParams: WindowManager.LayoutParams
    private val controller: View =
        LayoutInflater.from(context).inflate(R.layout.layout_float_video, this, false)
    private var videoModel: VideoModel? = null
    private var playlist: ArrayList<VideoModel>? = null
    private var initialWidth = -2

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
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
                videoView.useController = true
                videoMap[videoView.toString()] = videoView
                val context = MyApplication.instance
                val intent = Intent(context, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra("isFromFloatWindow", true)
                intent.putExtra("viewId", videoView.toString())
                intent.putExtra("video", videoModel)
                intent.putParcelableArrayListExtra("playlist", playlist)
                context.startActivity(intent)
            }
            R.id.btn_play -> {
                if (videoView.isPlaying) {
                    btn_play.setImageResource(mMediaPlayRes)
                    videoView.pause()
                } else {
                    btn_play.setImageResource(mMediaPauseRes)
                    videoView.start()
                    fadeOut.run()
                }
                handler.removeCallbacks(fadeOut)
            }
            else -> {
                controller.visibility =
                    if (controller.visibility == View.GONE) View.VISIBLE else View.GONE
                btn_play.setImageResource(if (videoView.isPlaying) mMediaPauseRes else mMediaPlayRes)
                handler.removeCallbacks(fadeOut)
                handler.postDelayed(fadeOut, 3000)
            }
        }
    }

    fun showFloatWindow(
        videoView: VideoPlayerView,
        model: VideoModel?,
        playlist: ArrayList<VideoModel>? = null
    ) {
        this.videoModel = model
        this.playlist = playlist
        this.videoView = videoView
        videoView.useController = false
        addView(videoView, 0)
        wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wmParams = WindowManager.LayoutParams()
        wmParams.type = windowType()
        wmParams.format = PixelFormat.RGBA_8888
        wmParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        wmParams.gravity = Gravity.CENTER
        val dm = DisplayMetrics()
        wm.defaultDisplay.getMetrics(dm)
        initialWidth =
            (if (dm.heightPixels < dm.widthPixels) dm.heightPixels else dm.widthPixels) / 5 * 4
        wmParams.width = initialWidth
        wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        wm.addView(this, wmParams)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null)
            return false

        event.offsetLocation(event.rawX - event.x, event.rawY - event.y)
        mScaleGestureDetector.onTouchEvent(event)
        mGestureDetector.onTouchEvent(event)
        return true
    }

    private val mGestureDetector =
        GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                if (isClickable)
                    return performClick()
                return super.onSingleTapUp(e)
            }

            override fun onScroll(
                e1: MotionEvent,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                wmParams.x -= distanceX.toInt()
                wmParams.y -= distanceY.toInt()
                wm.updateViewLayout(this@FloatVideoView, wmParams)
                return true
            }

            override fun onFling(
                e1: MotionEvent,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                fling(velocityX.toInt(), velocityY.toInt())
                return true
            }
        })

    private val mScaleGestureDetector =
        ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {

            private var mScaleFactor = 1f

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                mScaleFactor *= detector.scaleFactor
                wmParams.width = (initialWidth * mScaleFactor).toInt()
                wm.updateViewLayout(this@FloatVideoView, wmParams)
                return true
            }
        })

    private fun fling(velocityX: Int, velocityY: Int) {
        if (mFlingRunnable != null) {
            removeCallbacks(mFlingRunnable)
            mFlingRunnable = null
        }
        val startX = wmParams.x
        val startY = wmParams.y
        val dm = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(dm)
        mScroller.fling(
            startX, startY, velocityX, velocityY,
            (width - dm.widthPixels) / 2, (dm.widthPixels - width) / 2,
            (height - dm.heightPixels) / 2, (dm.heightPixels - height) / 2
        )
        if (mScroller.computeScrollOffset()) {
            mFlingRunnable = FlingRunnable().also {
                ViewCompat.postOnAnimation(this, it)
            }
        }
    }

    private val mScroller = OverScroller(context)
    private var mFlingRunnable: FlingRunnable? = null

    inner class FlingRunnable : Runnable {
        override fun run() {
            if (mScroller.computeScrollOffset()) {
                wmParams.x = mScroller.currX
                wmParams.y = mScroller.currY
                wm.updateViewLayout(this@FloatVideoView, wmParams)
                ViewCompat.postOnAnimation(this@FloatVideoView, this)
            }
        }
    }

    private fun release() {
        handler.removeCallbacks(fadeOut)
        RecordAgent.getInstance()
            .record(videoModel?.url, videoView.duration, videoView.currentPosition)
        videoMap.remove(videoView.toString())
        videoView.release(true)
        wm.removeViewImmediate(this)
    }

    private val fadeOut = Runnable {
        controller.visibility = View.GONE
    }
}