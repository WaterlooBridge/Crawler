package com.zhenl.crawler.ui.home

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import com.zhenl.crawler.R
import com.zhenl.crawler.adapter.MovieViewHolder
import com.zhenl.crawler.base.BaseFragment
import com.zhenl.crawler.databinding.FragmentHomeBinding
import com.zhenl.crawler.models.MovieModel
import com.zhenl.crawler.vm.HomeViewModel
import com.zhenl.violet.base.SimpleLoadStateAdapter
import com.zhenl.violet.base.SimplePagingDataAdapter
import com.zhenl.violet.ktx.withNetworkLoadStateFooter
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter

/**
 * Created by lin on 2021/6/19.
 */
class HomeFragment : BaseFragment<FragmentHomeBinding>() {

    companion object {

        const val KEY_CATEGORY = "category"

        fun newInstance(category: Int): HomeFragment {
            return HomeFragment().apply {
                arguments = Bundle().also {
                    it.putInt(KEY_CATEGORY, category)
                }
            }
        }
    }

    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: SimplePagingDataAdapter<MovieModel, MovieViewHolder>

    override val layoutRes: Int = R.layout.fragment_home

    override fun initView() {
        viewModel = getViewModel(HomeViewModel::class.java)
        arguments?.getInt(KEY_CATEGORY)?.let { viewModel.type = it }

        adapter = SimplePagingDataAdapter(
            { parent -> MovieViewHolder(parent, true) },
            MovieViewHolder.MOVIE_COMPARATOR
        )
        binding.recyclerView.adapter = adapter.withNetworkLoadStateFooter()
        lifecycleScope.launchWhenResumed {
            adapter.loadStateFlow.collectLatest {
                if (it.refresh is LoadState.Loading)
                    binding.refreshLayout.autoRefreshAnimationOnly()
                else
                    binding.refreshLayout.finishRefresh()
            }
        }
        lifecycleScope.launchWhenResumed {
            adapter.loadStateFlow.distinctUntilChangedBy { it.refresh }
                .filter { it.refresh is LoadState.NotLoading }
                .collect { binding.recyclerView.layoutManager?.onAdapterChanged(null, null) }
        }

        binding.refreshLayout.setPrimaryColorsId(R.color.transparent)
        binding.refreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }

        lifecycleScope.launchWhenResumed {
            viewModel.movies.collectLatest {
                adapter.submitData(it)
            }
        }
    }
}