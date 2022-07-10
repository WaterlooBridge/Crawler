package com.zhenl.crawler.adapter

import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.Glide
import com.zhenl.crawler.R
import com.zhenl.crawler.engines.SearchEngineFactory
import com.zhenl.crawler.models.MovieModel
import com.zhenl.crawler.ui.MovieDetailActivity
import com.zhenl.violet.base.BaseViewHolder
import com.zhenl.violet.ktx.getItemView

/**
 * Created by lin on 2022/7/9.
 */
class MovieViewHolder(parent: ViewGroup, home: Boolean = false) : BaseViewHolder<MovieModel>(
    parent.getItemView(R.layout.item_movie)
) {

    companion object {
        val MOVIE_COMPARATOR = object : DiffUtil.ItemCallback<MovieModel>() {
            override fun areItemsTheSame(oldItem: MovieModel, newItem: MovieModel): Boolean {
                return oldItem.title == newItem.title
            }

            override fun areContentsTheSame(oldItem: MovieModel, newItem: MovieModel): Boolean {
                return oldItem.url == newItem.url
            }
        }
    }

    init {
        itemView.setOnClickListener {
            this.item?.apply {
                if (home) SearchEngineFactory.type = 1
                MovieDetailActivity.start(itemView.context, title, url)
            }
        }
    }

    override fun bind(item: MovieModel?) {
        item ?: return
        val iv = getView<ImageView>(R.id.iv)
        Glide.with(iv.context).load(item.img).centerCrop().into(iv)
        setText(R.id.tv_title, item.title)
        setText(R.id.tv_date, item.date)
    }
}