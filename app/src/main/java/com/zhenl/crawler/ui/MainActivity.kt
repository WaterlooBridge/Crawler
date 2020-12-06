package com.zhenl.crawler.ui

import android.Manifest
import android.app.Dialog
import android.content.*
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.RemoteException
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.zhenl.crawler.Constants
import com.zhenl.crawler.MyApplication
import com.zhenl.crawler.R
import com.zhenl.crawler.adapter.DoubleSpeedAdapter
import com.zhenl.crawler.base.BaseActivity
import com.zhenl.crawler.core.RecordAgent
import com.zhenl.crawler.databinding.ActivityMainBinding
import com.zhenl.crawler.databinding.DialogVideoPlaySettingBinding
import com.zhenl.crawler.download.VideoDownloadService.Companion.downloadVideo
import com.zhenl.crawler.engines.SearchEngine
import com.zhenl.crawler.engines.SearchEngineFactory
import com.zhenl.crawler.models.VideoModel
import com.zhenl.crawler.views.FloatVideoView.Companion.getVideoView
import com.zhenl.crawler.views.FloatVideoView.Companion.isFloatWindowOpAllowed
import com.zhenl.crawler.views.FloatVideoView.Companion.showFloatWindow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tv.danmaku.ijk.media.player.AVOptions
import tv.danmaku.ijk.media.player.IIjkMediaPlayer
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.IjkMediaPlayer
import tv.danmaku.ijk.media.widget.AndroidMediaController
import tv.danmaku.ijk.media.widget.IPCVideoView
import tv.danmaku.ijk.media.widget.VideoControlHelper
import java.net.URLEncoder

class MainActivity : BaseActivity<ActivityMainBinding>(), IPCVideoView.OnInfoListener, IPCVideoView.OnPreparedListener {

    private val TAG = javaClass.simpleName

    private lateinit var videoModel: VideoModel
    private lateinit var videoParent: ViewGroup
    private lateinit var mVideoView: IPCVideoView
    private lateinit var controller: AndroidMediaController
    private lateinit var controlHelper: VideoControlHelper

    private var url: String? = null
    private var engine: SearchEngine? = null
    private var mStopped = false
    private var isPlaying = false
    private var isLock = false
    private var bgEnable = false

    override val layoutRes: Int = R.layout.activity_main

    override fun initView() {
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        mOrientationListener = OrientationListener(this)
        supportActionBar!!.setBackgroundDrawable(ColorDrawable(0x40000000))
        videoParent = binding.buffer

        val isFromFloatWindow = intent.getBooleanExtra("isFromFloatWindow", false)
        if (isFromFloatWindow) {
            val videoView = getVideoView(intent.getStringExtra("viewId")!!)
            if (videoView == null) {
                super.finish()
                return
            }
            mVideoView = videoView
            val parent = mVideoView.parent as ViewGroup
            parent.removeView(mVideoView)
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            wm.removeViewImmediate(parent)
        } else {
            mVideoView = IPCVideoView(MyApplication.instance)
            if ("1" == PreferenceManager.getDefaultSharedPreferences(MyApplication.instance).getString("decoding_settings", null))
                mVideoView.setMediaCodecEnable(true)
        }
        videoParent.addView(mVideoView, -1, -1)
        val options = AVOptions()
        options.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "user_agent", Constants.USER_AGENT)
        options.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "soundtouch", 1)
        mVideoView.setOptions(options)
        mVideoView.setOnErrorListener { mp: IIjkMediaPlayer, what: Int, _: Int ->
            isLock = false
            try {
                record(mp.duration.toInt(), mp.currentPosition.toInt())
            } catch (e: RemoteException) {
                e.printStackTrace()
            }
            if (what == -10000) {
                AlertDialog.Builder(this).setMessage("播放异常，是否尝试浏览器播放")
                        .setNegativeButton("否") { _: DialogInterface?, _: Int -> finish() }
                        .setPositiveButton("是") { _: DialogInterface?, _: Int -> jumpBrowser() }.create().show()
                return@setOnErrorListener true
            }
            false
        }

        controller = AndroidMediaController(this, false)
        controller.setInstantSeeking(false)
        controller.setSupportActionBar(supportActionBar)
        controller.setOnFullscreenClickListener {
            requestedOrientation = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mOrientationListener.disable()
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        }
        controlHelper = VideoControlHelper(mVideoView, window)
        controlHelper.setMediaController(controller)
        mVideoView.setBufferingIndicator(binding.probar)
        binding.btnLock.setOnClickListener {
            isLock = !isLock
            binding.btnLock.setImageResource(if (isLock) R.drawable.ic_lock_24dp else R.drawable.ic_lock_open_24dp)
            controller.isLock = isLock
            if (isLock) controller.hide() else controller.show()
        }

        val adapter = DoubleSpeedAdapter(lifecycle)
        binding.rvDoubleSpeed.adapter = adapter
        adapter.setOnItemClickListener { _, _, position ->
            binding.flDoubleSpeed.visibility = View.GONE
            mVideoView.setSpeed(adapter.getDefItem(position)!!.toFloat())
        }

        videoModel = intent.getParcelableExtra("video") ?: return
        title = videoModel.title
        supportActionBar!!.subtitle = videoModel.subtitle
        url = videoModel.url

        if (isFromFloatWindow) {
            mVideoView.setMediaController(controller)
            mVideoView.setControlHelper(controlHelper)
            mVideoView.setOnInfoListener(this)
            mVideoView.setOnPreparedListener(this)
            binding.probar.visibility = View.GONE
            return
        }

        intent.data?.let {
            handleVideoFileIntent(it)
            return
        }

        Log.e(TAG, "[INFO:CONSOLE]$url")
        engine = SearchEngineFactory.create()
        engine?.load(url, object : SearchEngine.Callback {
            override fun play(path: String) {
                mVideoView.setCacheEnable(true)
                this@MainActivity.play(path)
            }

            override fun finish() {
                this@MainActivity.finish()
            }
        })
    }

    private fun handleVideoFileIntent(uri: Uri) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE_FILE)
            return
        }
        url = uri.toString()
        Log.e(TAG, url)
        play(uri)
    }

    private fun play(path: String) {
        videoModel.videoPath = path
        val uri = Uri.parse(path)
        play(uri)
    }

    private fun play(uri: Uri?) {
        mVideoView.setVideoURI(uri)
        mVideoView.setMediaController(controller)
        mVideoView.setControlHelper(controlHelper)
        mVideoView.requestFocus()
        mVideoView.setOnInfoListener(this)
        mVideoView.setOnPreparedListener(this)

        val pos = RecordAgent.getInstance().getRecord(url)
        if (pos > 0) mVideoView.seekTo(pos)
    }

    override fun onInfo(mp: IIjkMediaPlayer, what: Int, extra: Int): Boolean {
        when (what) {
            IMediaPlayer.MEDIA_INFO_BUFFERING_START -> if (mVideoView.isPlaying) {
                mVideoView.pause()
            }
            IMediaPlayer.MEDIA_INFO_BUFFERING_END -> if (!mStopped || bgEnable) mVideoView.start()
        }
        return true
    }

    override fun onPrepared(mp: IIjkMediaPlayer) {
        try {
            if (!mStopped || bgEnable) mp.start() else mp.pause()
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    override fun onBackPressed() {
        if (isLock) return
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mOrientationListener.disable()
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val moreItem = menu.add(Menu.NONE, Menu.FIRST, Menu.FIRST, "PIP")
        moreItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        val moreMenu = menu.add(Menu.NONE, MENU_MORE, MENU_MORE, null)
        moreMenu.setIcon(R.drawable.ic_more_vert_24dp)
        moreMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == Menu.FIRST) {
            openFloat()
        } else if (id == MENU_MORE) {
            showSettingDialog()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openFloat() {
        if (!isFloatWindowOpAllowed()) return
        bgEnable = true
        mVideoView.setBufferingIndicator(null)
        mVideoView.setMediaController(null)
        mVideoView.setControlHelper(null)
        mVideoView.setOnInfoListener(null)
        mVideoView.setOnPreparedListener(null)
        videoParent.removeView(mVideoView)
        showFloatWindow(mVideoView, videoModel)
        engine?.destroy()
        controller.release()
        super.finish()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            controller.onFullscreenChanged(true)
            mOrientationListener.enable()
        } else {
            controller.onFullscreenChanged(false)
            mOrientationListener.disable()
        }
    }

    override fun finish() {
        engine?.destroy()
        record(mVideoView.duration, mVideoView.currentPosition)
        mVideoView.release(true)
        controller.release()
        super.finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_FILE) play(intent.data)
    }

    private fun record(duration: Int, curPos: Int) {
        RecordAgent.getInstance().record(url, duration, curPos)
    }

    override fun onStart() {
        super.onStart()
        mStopped = false
        if (!bgEnable && isPlaying) mVideoView.start()
    }

    override fun onResume() {
        super.onResume()
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mOrientationListener.enable()
        }
    }

    override fun onPause() {
        mOrientationListener.disable()
        super.onPause()
    }

    override fun onStop() {
        mStopped = true
        isPlaying = mVideoView.isPlaying
        if (!bgEnable) mVideoView.pause()
        super.onStop()
    }

    private var fadeoutJob: Job? = null

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_UP) {
            if (isLock && binding.btnLock.visibility == View.GONE || controller.isShowing) {
                binding.btnLock.visibility = View.VISIBLE
                fadeoutJob?.cancel()
                fadeoutJob = lifecycleScope.launch {
                    delay(3000)
                    binding.btnLock.visibility = View.GONE
                }
            } else binding.btnLock.visibility = View.GONE
            if (binding.flDoubleSpeed.visibility == View.VISIBLE)
                binding.flDoubleSpeed.visibility = View.GONE
        }
        return super.dispatchTouchEvent(ev)
    }

    private lateinit var mOrientationListener: OrientationEventListener

    internal inner class OrientationListener(context: Context?) : OrientationEventListener(context) {
        override fun onOrientationChanged(orientation: Int) {
            Log.e(TAG, "Orientation changed to $orientation")
            if (orientation in 81..99) { //90度
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            } else if (orientation in 261..279) { //270度
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        }
    }

    private fun showSettingDialog() {
        settingDialog.show()
    }

    private val settingDialog by lazy {
        Dialog(this, R.style.TransparentDialog).also { dialog ->
            val binding = DataBindingUtil.inflate<DialogVideoPlaySettingBinding>(layoutInflater, R.layout.dialog_video_play_setting, null, false)
            dialog.setContentView(binding.root)
            binding.switchPlayBackground.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean -> bgEnable = isChecked }
            binding.viewDoubleSpeed.setOnClickListener {
                this.binding.flDoubleSpeed.visibility = View.VISIBLE
                dialog.dismiss()
            }
            binding.viewOpenBrowser.setOnClickListener {
                dialog.dismiss()
                jumpBrowser()
            }
            binding.viewCopyLink.setOnClickListener {
                dialog.dismiss()
                val manager = getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
                if (manager == null || TextUtils.isEmpty(videoModel.videoPath)) return@setOnClickListener
                manager.setPrimaryClip(ClipData.newPlainText("link", generateUrl()))
                Toast.makeText(applicationContext, "Link Copied", Toast.LENGTH_SHORT).show()
            }
            if (TextUtils.isEmpty(videoModel.videoPath) || !videoModel.videoPath!!.startsWith("http")) {
                binding.viewDownload.visibility = View.GONE
                return@also
            }
            binding.viewDownload.setOnClickListener {
                dialog.dismiss()
                downloadVideo(videoModel)
            }
        }
    }

    private fun jumpBrowser() {
        if (TextUtils.isEmpty(videoModel.videoPath)) return
        try {
            val uri = Uri.parse(generateUrl())
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun generateUrl(): String {
        return "https://waterloobridge.github.io/smile/video.html?path=" + URLEncoder.encode(videoModel.videoPath)
    }

    companion object {
        fun start(context: Context, model: VideoModel?) {
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra("video", model)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        }

        private const val MENU_MORE = 2
        private const val REQUEST_CODE_FILE = 101
    }
}