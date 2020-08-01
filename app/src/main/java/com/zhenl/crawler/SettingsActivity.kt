package com.zhenl.crawler

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.zhenl.crawler.utils.FileUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tv.danmaku.ijk.media.services.IPCPlayerControl

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
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

            findPreference<Preference>("clear_video_cache")?.let {
                lifecycleScope.launch {
                    it.summary = withContext(Dispatchers.IO) { FileUtil.getFormatSize(IPCPlayerControl.getCacheSize(context).toDouble()) }
                }
                it.setOnPreferenceClickListener {
                    AlertDialog.Builder(requireContext())
                            .setTitle("确认清除？")
                            .setPositiveButton("确定") { _, _ ->
                                IPCPlayerControl.clearCache(context)
                                it.summary = FileUtil.getFormatSize(IPCPlayerControl.getCacheSize(context).toDouble())
                            }.setNegativeButton("取消", null)
                            .show()
                    true
                }
            }
        }
    }
}