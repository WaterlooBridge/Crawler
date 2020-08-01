package com.zhenl.crawler

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.zhenl.crawler.base.BaseActivity
import com.zhenl.crawler.download.*
import com.zhenl.crawler.download.VideoDownloadEntity
import com.zhenl.crawler.models.VideoModel
import com.zhenl.crawler.utils.FileUtil
import com.zhenl.violet.core.Dispatcher
import kotlinx.android.synthetic.main.activity_download.*
import kotlinx.android.synthetic.main.item_download_list.view.*
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by lin on 20-1-20.
 */
class DownloadActivity : BaseActivity() {
    override val layoutRes: Int = R.layout.activity_download

    private lateinit var adapter: VideoDownloadAdapter
    private val videoList = arrayListOf<VideoDownloadEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setTitle(R.string.downloads)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = VideoDownloadAdapter(videoList)
        rv_downloads.adapter = adapter

        fab_btn.setOnClickListener { newDownload() }

        FileDownloader.downloadCallback.observe(this, Observer {
            onProgress(it)
        })

        load()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home)
            finish()
        return super.onOptionsItemSelected(item)
    }

    private fun load() {
        Dispatcher.getInstance().enqueue {
            FileDownloader.getBaseDownloadPath().listFiles().forEach {
                val file = File(it, "video.config")
                if (file.exists()) {
                    val text = file.readText()
                    if (text.isNotEmpty()) {
                        val data = Gson().fromJson<VideoDownloadEntity>(
                                text,
                                VideoDownloadEntity::class.java
                        )
                        if (data != null) {
                            if (data.status == DELETE) {
                                it.deleteRecursively()
                            } else {
                                if (data.status == DOWNLOADING && data.downloadContext == null)
                                    data.status = PAUSE
                                videoList.add(data)
                            }
                        }
                    }
                }
            }

            videoList.sort()

            runOnUiThread { adapter.notifyDataSetChanged() }
        }
    }

    private fun onProgress(entity: VideoDownloadEntity) {
        for ((index, item) in videoList.withIndex()) {
            if (item.originalUrl == entity.originalUrl) {
                if (item != entity)
                    videoList[index] = entity
                adapter.notifyItemChanged(index, 0)
                break
            }
        }
    }

    private fun newDownload() {
        val editText = AppCompatEditText(this)
        editText.hint = "请输入下载地址"
        AlertDialog.Builder(this)
                .setView(editText)
                .setTitle("新建下载")
                .setPositiveButton("确定") { _, _ ->
                    if (editText.text.isNullOrEmpty()) {
                        return@setPositiveButton
                    }
                    val url = editText.text.toString()
                    val name = FileUtil.getFileNameFromUrl(url)
                    val entity = VideoDownloadEntity(url, name)
                    entity.toFile()
                    videoList.add(0, entity)
                    adapter.notifyItemInserted(0)
                    rv_downloads.scrollToPosition(0)
                    VideoDownloadService.downloadVideo(entity)
                }.show()
    }

    class VideoDownloadAdapter(private val list: MutableList<VideoDownloadEntity>) :
            RecyclerView.Adapter<VideoDownloadAdapter.ViewHolder>() {

        companion object {
            private val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                            R.layout.item_download_list, parent, false
                    )
            )
        }

        override fun getItemCount() = list.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
            if (payloads.isNullOrEmpty()) {
                super.onBindViewHolder(holder, position, payloads)
            } else {
                holder.updateProgress(list[position])
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
                    val it = data!!
                    val items = arrayListOf(
                            if (it.status == DOWNLOADING) "暂停下载"
                            else if (it.status == COMPLETE) "播放"
                            else "开始下载", "删除")
                            .apply {
                                if (it.status != COMPLETE)
                                    add("边下边播")
                            }.toArray(arrayOf(""))

                    val builder = AlertDialog.Builder(view.context)
                    builder.setItems(items) { dialog: DialogInterface, which: Int ->
                        dialog.dismiss()
                        if (which == 0) {
                            if (it.status == DOWNLOADING) {
                                it.downloadContext?.stop()
                            } else if (it.status == COMPLETE) {
                                play(it)
                            } else {
                                VideoDownloadService.downloadVideo(it)
                            }
                        } else if (which == 1) {
                            AlertDialog.Builder(view.context)
                                    .setTitle("确认删除？")
                                    .setPositiveButton("确定") { _, _ ->
                                        FileDownloader.deleteVideo(it)
                                        list.remove(it)
                                        notifyItemRemoved(adapterPosition)
                                    }.setNegativeButton("取消", null)
                                    .show()
                        } else {
                            play(it)
                        }
                    }
                    builder.create().show()
                }
            }

            private fun play(entity: VideoDownloadEntity) {
                val context = MyApplication.getInstance()
                val intent = Intent(context, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.data = Uri.fromFile(FileDownloader.getLocalPlayFile(entity.originalUrl))
                intent.putExtra("video", VideoModel(entity.name, entity.subName, intent.data.toString()))
                context.startActivity(intent)
            }

            fun setData(data: VideoDownloadEntity?) {
                if (data == null) {
                    return
                }
                this.data = data
                val context = view.context
                title.text = if (data.name.isNotEmpty()) data.name else context.getString(R.string.unknown_movie)
                subtitle.text = data.subName
                date.text = df.format(Date(data.createTime))
                updateProgress(data)
            }

            fun updateProgress(data: VideoDownloadEntity) {
                this.data = data
                currentSize.text = FileUtil.getFormatSize(data.currentSize.toDouble())
                speed.text = "${DecimalFormat("#.##%").format(data.currentProgress)}|${data.currentSpeed}"

                when (data.status) {
                    NO_START -> {
                        currentSize.setText(R.string.wait_download)
                        speed.visibility = View.GONE
                        download.visibility = View.GONE
                    }
                    DOWNLOADING -> {
                        speed.visibility = View.VISIBLE
                        download.visibility = View.VISIBLE
                        download.setText(R.string.downloading)
                    }
                    PAUSE -> {
                        speed.visibility = View.VISIBLE
                        download.visibility = View.VISIBLE
                        download.setText(R.string.already_paused)
                    }
                    COMPLETE -> {
                        speed.visibility = View.GONE
                        download.visibility = View.GONE
                    }
                    PREPARE -> {
                        currentSize.setText(R.string.wait_download)
                        speed.visibility = View.GONE
                        download.visibility = View.VISIBLE
                        download.setText(R.string.preparing)
                    }
                    ERROR -> {
                        currentSize.setText(R.string.download_error)
                        speed.visibility = View.GONE
                        download.visibility = View.GONE
                    }
                }
            }
        }
    }
}