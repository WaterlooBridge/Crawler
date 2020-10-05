package com.zhenl.crawler.adapter

import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import com.bumptech.glide.Glide
import com.zhenl.crawler.R
import com.zhenl.crawler.models.MovieModel
import com.zhenl.violet.base.BasePagedListAdapter
import com.zhenl.violet.base.BaseViewHolder

/**
 * Created by lin on 2020/10/3.
 */
class MovieAdapter : BasePagedListAdapter<MovieModel>(MOVIE_COMPARATOR) {

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

    override fun getLayoutResId(viewType: Int): Int = R.layout.item_movie

    override fun convert(holder: BaseViewHolder, item: MovieModel) {
        val iv = holder.getView<ImageView>(R.id.iv)
        Glide.with(iv.context).load(item.img).centerCrop().into(iv)
        holder.setText(R.id.tv_title, item.title)
        holder.setText(R.id.tv_date, item.date)
    }
}