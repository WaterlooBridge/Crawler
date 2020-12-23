package com.zhenl.crawler.ui

import android.content.DialogInterface
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import com.zhenl.crawler.R
import com.zhenl.crawler.adapter.MovieAdapter
import com.zhenl.crawler.base.BaseActivity
import com.zhenl.crawler.databinding.ActivityHomeBinding
import com.zhenl.crawler.engines.SearchEngineFactory
import com.zhenl.crawler.vm.HomeViewModel
import com.zhenl.violet.base.BasePagedListAdapter
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class HomeActivity : BaseActivity<ActivityHomeBinding>() {

    private val TAG = javaClass.simpleName
    private val types = intArrayOf(7, 1, 2, 4, 6, 9)

    private lateinit var viewModel: HomeViewModel
    private lateinit var adapter: MovieAdapter

    private var pos = 0

    override val layoutRes: Int = R.layout.activity_home

    override fun initView() {
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeAsUpIndicator(R.drawable.ic_menu)
        }
        viewModel = getViewModel(HomeViewModel::class.java)

        adapter = MovieAdapter()
        adapter.isEnableLoadMore = true
        binding.recyclerView.adapter = adapter
        adapter.setOnItemClickListener { _: BasePagedListAdapter<*>?, view: View, position: Int ->
            val model = adapter.getDefItem(position)
            SearchEngineFactory.type = 1
            MovieDetailActivity.start(view.context, model!!.title, model.url)
        }
        lifecycleScope.launchWhenCreated {
            adapter.loadStateFlow.collectLatest {
                binding.refreshLayout.isRefreshing = it.refresh is LoadState.Loading
            }
        }
        lifecycleScope.launchWhenCreated {
            adapter.loadStateFlow.distinctUntilChangedBy { it.refresh }
                    .filter { it.refresh is LoadState.NotLoading }
                    .collect { binding.recyclerView.layoutManager?.onAdapterChanged(null, null) }
        }

        binding.navView.setNavigationItemSelectedListener { menuItem: MenuItem ->
            val id = menuItem.itemId
            if (id == R.id.downloads) {
                val intent = Intent(application, DownloadActivity::class.java)
                startActivity(intent)
            } else if (id == R.id.settings) {
                val intent = Intent(application, SettingsActivity::class.java)
                startActivity(intent)
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            false
        }

        binding.refreshLayout.setOnRefreshListener {
            viewModel.refresh()
        }
    }

    override fun initData() {
        lifecycleScope.launchWhenCreated {
            viewModel.movies.collectLatest {
                adapter.submitData(it)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val moreItem = menu.add(Menu.NONE, Menu.FIRST, Menu.FIRST, "SEARCH")
        moreItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == Menu.FIRST) {
            showSearchEngines()
        } else if (id == android.R.id.home) {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showSearchEngines() {
        val items = arrayOf("Search Engine 1", "Search Engine 2", "Search Engine 3", "Anime")
        val builder = AlertDialog.Builder(this)
        builder.setItems(items) { dialog: DialogInterface, which: Int ->
            dialog.dismiss()
            SearchEngineFactory.type = which + 1
            startActivity(Intent(this@HomeActivity, SearchActivity::class.java))
        }
        builder.create().show()
    }

    fun onClick(view: View?) {
        pos = ++pos % types.size
        viewModel.type = types[pos]
    }
}