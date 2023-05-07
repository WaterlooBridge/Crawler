package com.zhenl.crawler.ui

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatEditText
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.media3.exoplayer.offline.Download
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.zhenl.crawler.MyApplication
import com.zhenl.crawler.R
import com.zhenl.crawler.base.BaseActivity
import com.zhenl.crawler.databinding.ActivityDownloadBinding
import com.zhenl.crawler.download.VideoDownloadEntity
import com.zhenl.crawler.download.VideoDownloader
import com.zhenl.crawler.download.toVideoDownloadEntity
import com.zhenl.crawler.models.VideoModel
import com.zhenl.crawler.utils.FileUtil
import kotlinx.android.synthetic.main.activity_download.*
import kotlinx.android.synthetic.main.item_download_list.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tv.zhenl.media.IPCPlayerControl
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by lin on 20-1-20.
 */
class DownloadActivity : BaseActivity<ActivityDownloadBinding>() {
    override val layoutRes: Int = R.layout.activity_download

    private lateinit var adapter: VideoDownloadAdapter

    override fun initView() {
        supportActionBar?.setTitle(R.string.downloads)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = VideoDownloadAdapter()
        rv_downloads.adapter = adapter

        fab_btn.setOnClickListener { newDownload() }
    }

    override fun initData() {
        refreshData()
        lifecycleScope.launch {
            VideoDownloader.downloadCallback.asFlow().collect { onProgress(it) }
        }
        lifecycleScope.launch {
            flow {
                while (true) {
                    delay(1000)
                    emit(adapter.list.firstOrNull { it.downloadTask.state == Download.STATE_DOWNLOADING } != null)
                }
            }.collect {
                if (it) adapter.notifyDataSetChanged()
            }
        }
    }

    private fun refreshData(needDelay: Boolean = false) {
        lifecycleScope.launch {
            if (needDelay) delay(1000)
            val list = arrayListOf<VideoDownloadEntity>()
            withContext(Dispatchers.IO) {
                VideoDownloader.loadDownloads().forEach {
                    val entity = it.toVideoDownloadEntity() ?: return@forEach
                    list.add(entity)
                }
                list.sort()
            }
            adapter.setData(list)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home)
            finish()
        return super.onOptionsItemSelected(item)
    }

    private fun onProgress(download: Download) {
        val videoList = adapter.list
        for ((index, item) in videoList.withIndex()) {
            if (item.originalUrl == download.request.uri.toString()) {
                if (item.downloadTask != download)
                    item.downloadTask = download
                adapter.notifyItemChanged(index, 0)
                break
            }
        }
    }

    private fun newDownload() {
        val editText = AppCompatEditText(this)
        editText.hint = "请输入下载地址"
        MaterialAlertDialogBuilder(this)
            .setView(editText)
            .setTitle("新建下载")
            .setPositiveButton("确定") { _, _ ->
                val url = editText.text.toString()
                if (!url.matches(Regex("(http|ftp|https)://.+"))) {
                    return@setPositiveButton
                }
                val name = FileUtil.getFileNameFromUrl(url)
                val model = VideoModel(name, null, url, url)
                VideoDownloader.downloadVideo(model)
                refreshData(true)
            }.show()
    }

    class VideoDownloadAdapter(var list: MutableList<VideoDownloadEntity> = arrayListOf()) :
        RecyclerView.Adapter<VideoDownloadAdapter.ViewHolder>() {

        companion object {
            private val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        }

        fun setData(videoList: MutableList<VideoDownloadEntity>) {
            list = videoList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.item_download_list, parent, false
                )
            )
        }

        override fun getItemCount() = list.size

        override fun onBindViewHolder(
            holder: ViewHolder,
            position: Int,
            payloads: MutableList<Any>
        ) {
            if (payloads.isEmpty()) {
                super.onBindViewHolder(holder, position, payloads)
            } else {
                holder.updateProgress(list[position].downloadTask)
            }
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.setData(list[position])
        }

        inner class ViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
            private val title = view.title
            private val subtitle = view.subtitle
            private val currentSize = view.current_size
            private val speed = view.speed
            private val date = view.date
            private val download = view.download

            private var data: VideoDownloadEntity? = null

            init {
                view.setOnClickListener { view ->
                    val it = data ?: return@setOnClickListener
                    val items =
                        Array(if (it.downloadTask.state != Download.STATE_COMPLETED) 3 else 2) { pos ->
                            when (pos) {
                                0 -> when (it.downloadTask.state) {
                                    Download.STATE_DOWNLOADING -> "暂停下载"
                                    Download.STATE_COMPLETED -> "播放"
                                    else -> "开始下载"
                                }
                                1 -> "删除"
                                else -> "边下边播"
                            }
                        }

                    val builder = MaterialAlertDialogBuilder(view.context)
                    builder.setItems(items) { dialog: DialogInterface, which: Int ->
                        dialog.dismiss()
                        when (which) {
                            0 -> when (it.downloadTask.state) {
                                Download.STATE_DOWNLOADING -> VideoDownloader.stopDownload(it.downloadTask)
                                Download.STATE_COMPLETED -> play(it)
                                else -> VideoDownloader.startDownload(it.downloadTask)
                            }
                            1 -> MaterialAlertDialogBuilder(view.context)
                                .setTitle("确认删除？")
                                .setPositiveButton("确定") { _, _ ->
                                    VideoDownloader.removeDownload(it.downloadTask)
                                    list.remove(it)
                                    notifyItemRemoved(absoluteAdapterPosition)
                                }.setNegativeButton("取消", null)
                                .show()
                            else -> play(it)
                        }
                    }
                    builder.create().show()
                }
            }

            private fun play(entity: VideoDownloadEntity) {
                val context = MyApplication.instance
                val intent = Intent(context, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val url = IPCPlayerControl.DOWNLOAD_URI_SCHEME + entity.originalUrl
                intent.data = Uri.parse(url)
                intent.putExtra("video", VideoModel(entity.name, entity.subName, url))
                context.startActivity(intent)
            }

            fun setData(data: VideoDownloadEntity?) {
                if (data == null) {
                    return
                }
                this.data = data
                val context = view.context
                title.text =
                    data.name.ifEmpty { context.getString(R.string.unknown_movie) }
                subtitle.text = data.subName
                date.text = df.format(Date(data.downloadTask.startTimeMs))
                updateProgress(data.downloadTask)
            }

            fun updateProgress(downloadTask: Download) {
                currentSize.text = FileUtil.getFormatSize(downloadTask.bytesDownloaded.toDouble())
                val text = DecimalFormat("#.##%").format(downloadTask.percentDownloaded / 100)
                speed.text = text

                when (downloadTask.state) {
                    Download.STATE_QUEUED -> {
                        currentSize.setText(R.string.wait_download)
                        speed.visibility = View.GONE
                        download.visibility = View.GONE
                    }
                    Download.STATE_DOWNLOADING -> {
                        speed.visibility = View.VISIBLE
                        download.visibility = View.VISIBLE
                        download.setText(R.string.downloading)
                    }
                    Download.STATE_STOPPED -> {
                        speed.visibility = View.VISIBLE
                        download.visibility = View.VISIBLE
                        download.setText(R.string.already_paused)
                    }
                    Download.STATE_COMPLETED -> {
                        speed.visibility = View.GONE
                        download.visibility = View.GONE
                    }
                    Download.STATE_RESTARTING -> {
                        currentSize.setText(R.string.wait_download)
                        speed.visibility = View.GONE
                        download.visibility = View.VISIBLE
                        download.setText(R.string.preparing)
                    }
                    Download.STATE_FAILED -> {
                        currentSize.setText(R.string.download_error)
                        speed.visibility = View.GONE
                        download.visibility = View.GONE
                    }
                }
            }
        }
    }
}