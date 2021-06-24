package com.zhenl.crawler.ui

import android.view.View
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.zhenl.crawler.R
import com.zhenl.crawler.adapter.MovieAdapter
import com.zhenl.crawler.base.BaseActivity
import com.zhenl.crawler.databinding.ActivitySearchBinding
import com.zhenl.crawler.views.MaterialSearchView
import com.zhenl.crawler.vm.SearchViewModel
import com.zhenl.violet.base.BasePagedListAdapter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/**
 * Created by lin on 2018/6/9.
 */
class SearchActivity : BaseActivity<ActivitySearchBinding>() {

    private lateinit var viewModel: SearchViewModel
    private lateinit var adapter: MovieAdapter

    override val layoutRes: Int = R.layout.activity_search

    override fun initView() {
        viewModel = getViewModel(SearchViewModel::class.java)

        adapter = MovieAdapter()
        binding.rv.adapter = adapter
        adapter.isEnableLoadMore = true
        adapter.setOnItemClickListener { _: BasePagedListAdapter<*>?, view: View, position: Int ->
            val model = adapter.getDefItem(position)
            MovieDetailActivity.start(view.context, model!!.title, model.url)
        }

        supportActionBar?.hide()
        binding.msv.setOnQueryTextListener(object : MaterialSearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.keyword = newText ?: return false
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