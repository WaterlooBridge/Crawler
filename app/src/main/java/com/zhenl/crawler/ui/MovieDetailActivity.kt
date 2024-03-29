package com.zhenl.crawler.ui

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.paging.PagingData
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import com.zhenl.crawler.R
import com.zhenl.crawler.adapter.DramasViewHolder
import com.zhenl.crawler.adapter.HeaderAdapter
import com.zhenl.crawler.base.BaseActivity
import com.zhenl.crawler.databinding.ActivityMovieDetailBinding
import com.zhenl.crawler.databinding.ItemMovieSummaryBinding
import com.zhenl.crawler.models.DramasModel
import com.zhenl.crawler.models.VideoModel
import com.zhenl.crawler.vm.MovieViewModel
import com.zhenl.violet.base.SimplePagingDataAdapter
import kotlin.math.max
import kotlin.math.min

/**
 * Created by lin on 2018/6/8.
 */
class MovieDetailActivity : BaseActivity<ActivityMovieDetailBinding>() {

    private var url: String? = null

    private lateinit var viewModel: MovieViewModel
    private lateinit var adapter: SimplePagingDataAdapter<DramasModel, DramasViewHolder>

    override val layoutRes: Int = R.layout.activity_movie_detail

    override fun initView() {
        url = intent.getStringExtra("url")
        val title = intent.getStringExtra("title")
        setTitle(title)

        viewModel = getViewModel(MovieViewModel::class.java)

        val header = DataBindingUtil.inflate<ItemMovieSummaryBinding>(
            layoutInflater,
            R.layout.item_movie_summary,
            null,
            false
        )
        header.lifecycleOwner = this
        header.vm = viewModel

        adapter = SimplePagingDataAdapter({ parent -> DramasViewHolder(parent, title, adapter) })
        binding.gv.adapter = ConcatAdapter().apply {
            addAdapter(HeaderAdapter(header.root))
            addAdapter(adapter)
        }
        binding.gv.layoutManager = GridLayoutManager(this, 4).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (position == 0) 4 else 1
                }
            }
        }
    }

    override fun initData() {
        viewModel.dsList.observe(this) {
            adapter.submitData(lifecycle, PagingData.from(it))
        }

        viewModel.loadMovieDetail(url ?: return)
    }

    companion object {
        fun start(context: Context, title: String?, url: String?) {
            val intent = Intent(context, MovieDetailActivity::class.java)
            intent.putExtra("title", title)
            intent.putExtra("url", url)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        }
    }
}