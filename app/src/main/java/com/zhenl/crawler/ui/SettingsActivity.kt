package com.zhenl.crawler.ui

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.zhenl.crawler.MyApplication
import com.zhenl.crawler.R
import com.zhenl.crawler.base.BaseActivity
import com.zhenl.crawler.databinding.SettingsActivityBinding
import com.zhenl.crawler.utils.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tv.zhenl.media.IPCPlayerControl

class SettingsActivity : BaseActivity<SettingsActivityBinding>() {

    override val layoutRes: Int = R.layout.settings_activity

    override fun initView() {
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        supportActionBar?.setTitle(R.string.settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home)
            finish()
        return super.onOptionsItemSelected(item)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.settings_preferences, rootKey)

            findPreference<ListPreference>("dark_theme")?.setOnPreferenceChangeListener { _, newValue ->
                AppCompatDelegate.setDefaultNightMode(newValue.toString().toInt())
                true
            }

            findPreference<Preference>("clear_video_cache")?.let {
                lifecycleScope.launch { it.summary = calcSummary() }
                it.setOnPreferenceClickListener {
                    AlertDialog.Builder(requireContext())
                            .setTitle("确认清除？")
                            .setPositiveButton("确定") { _, _ ->
                                clearCache(it)
                            }.setNegativeButton("取消", null)
                            .show()
                    true
                }
            }
        }

        private fun clearCache(preference: Preference) {
            lifecycleScope.launch {
                MyApplication.globalLoading.value = true
                withContext(Dispatchers.IO) { IPCPlayerControl.clearCache(context) }
                MyApplication.globalLoading.value = false
                preference.summary = calcSummary()
            }
        }

        private suspend fun calcSummary(): String = withContext(Dispatchers.IO) {
            FileUtil.getFormatSize(IPCPlayerControl.getCacheSize(context).toDouble())
        }
    }
}