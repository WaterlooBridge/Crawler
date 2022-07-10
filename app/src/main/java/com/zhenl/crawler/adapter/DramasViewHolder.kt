package com.zhenl.crawler.adapter

import android.view.ViewGroup
import com.zhenl.crawler.R
import com.zhenl.crawler.models.DramasModel
import com.zhenl.crawler.models.VideoModel
import com.zhenl.crawler.ui.MainActivity
import com.zhenl.violet.base.BaseViewHolder
import com.zhenl.violet.base.SimplePagingDataAdapter
import com.zhenl.violet.ktx.getItemView
import kotlin.math.max
import kotlin.math.min

/**
 * Created by lin on 2022/7/9.
 */
class DramasViewHolder(
    parent: ViewGroup,
    title: String?,
    adapter: SimplePagingDataAdapter<DramasModel, DramasViewHolder>
) : BaseViewHolder<DramasModel>(
    parent.getItemView(R.layout.item_movie_dramas)
) {

    init {
        itemView.setOnClickListener {
            var current: VideoModel? = null
            val position = bindingAdapterPosition
            val list = ArrayList<VideoModel>()
            val start = max(position - 100, 0)
            val end = min(position + 100, adapter.itemCount)
            for (i in start until end) {
                val model = adapter.getDefItem(i) ?: continue
                list.add(VideoModel(title, model.text, model.url).apply {
                    if (i == position)
                        current = this
                })
            }
            MainActivity.start(itemView.context, current ?: return@setOnClickListener, list)
        }
    }

    override fun bind(item: DramasModel?) {
        setText(R.id.tv, item?.text)
    }
}