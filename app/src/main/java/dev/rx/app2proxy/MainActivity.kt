package dev.rx.app2proxy

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import dev.rx.app2proxy.databinding.ActivityMainBinding

class MainActivity : ComponentActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: AppListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val apps = getInstalledApps()
        val selectedUids = getPrefs().getStringSet("selected_uids", emptySet()) ?: emptySet()
        adapter = AppListAdapter(apps, selectedUids) { updatedUids ->
            getPrefs().edit().putStringSet("selected_uids", updatedUids).apply()
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.btnApply.setOnClickListener {
            val uids = adapter.getSelectedUids().joinToString(" ")
            IptablesService.applyRules(this, uids)
        }
        binding.btnClear.setOnClickListener {
            val uids = adapter.getSelectedUids().joinToString(" ")
            IptablesService.clearRules(this, uids)
        }
    }

    private fun getPrefs() = getSharedPreferences("proxy_prefs", Context.MODE_PRIVATE)

    private fun getInstalledApps(): List<AppInfo> {
        val pm = packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .map {
                AppInfo(it.loadLabel(pm).toString(), it.packageName, it.uid)
            }.sortedBy { it.appName }
    }
}

data class AppInfo(val appName: String, val packageName: String, val uid: Int)
