package com.zhenl.crawler.ui

import androidx.lifecycle.lifecycleScope
import com.zhenl.crawler.BuildConfig
import com.zhenl.crawler.Constants
import com.zhenl.crawler.R
import com.zhenl.crawler.adapter.MovieViewHolder
import com.zhenl.crawler.base.BaseActivity
import com.zhenl.crawler.databinding.ActivitySearchBinding
import com.zhenl.crawler.models.MovieModel
import com.zhenl.crawler.views.MaterialSearchView
import com.zhenl.crawler.vm.SearchViewModel
import com.zhenl.violet.base.SimpleLoadStateAdapter
import com.zhenl.violet.base.SimplePagingDataAdapter
import com.zhenl.violet.ktx.withNetworkLoadStateFooter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/**
 * Created by lin on 2018/6/9.
 */
class SearchActivity : BaseActivity<ActivitySearchBinding>() {

    private lateinit var viewModel: SearchViewModel
    private lateinit var adapter: SimplePagingDataAdapter<MovieModel, MovieViewHolder>

    override val layoutRes: Int = R.layout.activity_search

    override fun initView() {
        viewModel = getViewModel(SearchViewModel::class.java)

        adapter = SimplePagingDataAdapter(
            { parent -> MovieViewHolder(parent) },
            MovieViewHolder.MOVIE_COMPARATOR
        )
        binding.rv.adapter = adapter.withNetworkLoadStateFooter()

        supportActionBar?.hide()
        binding.msv.setOnQueryTextListener(object : MaterialSearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                if (BuildConfig.APPLICATION_ID == query) Constants.SPIRITED_AWAY = true
                viewModel.keyword = query
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                viewModel.keyword = newText
                return true
            }
        })
        binding.msv.setOnSearchViewListener(object : MaterialSearchView.SearchViewListener {
            override fun onSearchViewShown() {
            }

            override fun onSearchViewClosed() {
                finish()
            }
        })

        lifecycleScope.launchWhenResumed {
            delay(200)
            binding.msv.showSearch(true)
        }
    }

    override fun initData() {
        lifecycleScope.launchWhenCreated {
            viewModel.movies.collectLatest {
                adapter.submitData(it)
            }
        }
    }
}