package com.zhenl.crawler.ui.home

import android.content.DialogInterface
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.zhenl.crawler.R
import com.zhenl.crawler.base.BaseActivity
import com.zhenl.crawler.databinding.ActivityHomeBinding
import com.zhenl.crawler.engines.SearchEngineFactory
import com.zhenl.crawler.ui.DownloadActivity
import com.zhenl.crawler.ui.SearchActivity
import com.zhenl.crawler.ui.SettingsActivity

class HomeActivity : BaseActivity<ActivityHomeBinding>() {

    override val layoutRes: Int = R.layout.activity_home

    override fun initView() {
        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setHomeAsUpIndicator(R.drawable.ic_menu)
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

        binding.vp.adapter = CategoryAdapter(this)
        TabLayoutMediator(binding.tabLayout, binding.vp) { tab, position ->
            tab.text = resources.getStringArray(R.array.home_category)[position]
        }.attach()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val moreItem = menu.add(Menu.NONE, Menu.FIRST, Menu.FIRST, R.string.home_search)
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
        val builder = AlertDialog.Builder(this)
        builder.setItems(R.array.home_search_engine) { dialog: DialogInterface, which: Int ->
            dialog.dismiss()
            SearchEngineFactory.type = which + 1
            startActivity(Intent(this@HomeActivity, SearchActivity::class.java))
        }
        builder.create().show()
    }

    private class CategoryAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

        private val types = intArrayOf(4, 1, 2, 3, 6)

        override fun getItemCount(): Int = types.size

        override fun createFragment(position: Int): Fragment {
            return HomeFragment.newInstance(types[position])
        }
    }
}