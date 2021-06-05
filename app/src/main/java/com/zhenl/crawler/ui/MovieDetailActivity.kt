package com.zhenl.crawler.ui

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.paging.PagingData
import com.zhenl.crawler.R
import com.zhenl.crawler.adapter.DramasAdapter
import com.zhenl.crawler.base.BaseActivity
import com.zhenl.crawler.databinding.ActivityMovieDetailBinding
import com.zhenl.crawler.engines.SearchEngineFactory
import com.zhenl.crawler.models.VideoModel
import com.zhenl.crawler.ui.MovieDetailActivity
import com.zhenl.crawler.vm.MovieViewModel
import com.zhenl.violet.base.BasePagedListAdapter

/**
 * Created by lin on 2018/6/8.
 */
class MovieDetailActivity : BaseActivity<ActivityMovieDetailBinding>() {

    private var url: String? = null

    private lateinit var viewModel: MovieViewModel
    private lateinit var adapter: DramasAdapter

    override val layoutRes: Int = R.layout.activity_movie_detail

    override fun initView() {
        url = intent.getStringExtra("url")
        val title = intent.getStringExtra("title")
        setTitle(title)

        viewModel = getViewModel(MovieViewModel::class.java)
        binding.vm = viewModel

        adapter = DramasAdapter()
        binding.gv.adapter = adapter
        binding.gv.isNestedScrollingEnabled = false
        adapter.setOnItemClickListener { _: BasePagedListAdapter<*>?, view: View, position: Int ->
            val model = adapter.getDefItem(position)
            MainActivity.start(view.context, VideoModel(title, model!!.text, model.url))
        }
    }

    override fun initData() {
        viewModel.dsList.observe(this, {
            adapter.submitData(lifecycle, PagingData.from(it))
        })

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