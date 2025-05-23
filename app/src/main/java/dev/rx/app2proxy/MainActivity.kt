package dev.rx.app2proxy

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.appbar.MaterialToolbar
import dev.rx.app2proxy.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: AppListAdapter
    private var showSystemApps = false

    // Если появятся dangerous permissions -- добавь их сюда
    // Сейчас массив пустой, так как никаких runtime-разрешений не требуется
    private val permissions = emptyArray<String>()

    private val permissionLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            // Разрешения обработаны, можно обновить список (если нужно)
            updateAppList()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Динамические цвета Material You
        val prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)
        val useMaterialYou = prefs.getBoolean("material_you", false)
        if (useMaterialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            com.google.android.material.color.DynamicColors.applyToActivityIfAvailable(this)
        }

        setTheme(R.style.Theme_App2Proxy)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Корректно добавляем отступ для Toolbar под статус-бар
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { view, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.updatePadding(top = top)
            insets
        }

        // Обязательно явно устанавливаем title и включаем его отображение
        val toolbar: MaterialToolbar = binding.toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.app_name)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        // Подгружаем список приложений
        updateAppList()

        binding.btnApply.setOnClickListener {
            val uids = adapter.getSelectedUids().joinToString(" ")
            IptablesService.applyRules(this, uids)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        menu.findItem(R.id.action_show_system)?.isChecked = showSystemApps
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_show_system -> {
                item.isChecked = !item.isChecked
                showSystemApps = item.isChecked
                updateAppList()
                true
            }
            R.id.action_select_all -> {
                adapter.selectAll()
                saveSelectedUids(adapter.getSelectedUids())
                true
            }
            R.id.action_deselect_all -> {
                adapter.deselectAll()
                saveSelectedUids(adapter.getSelectedUids())
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveSelectedUids(uids: Set<String>) {
        getPrefs().edit().putStringSet("selected_uids", uids).apply()
    }

    private fun updateAppList() {
        val prevSelected = getPrefs().getStringSet("selected_uids", emptySet()) ?: emptySet()
        val apps = getInstalledApps(showSystemApps)
        if (!::adapter.isInitialized) {
            adapter = AppListAdapter(apps, prevSelected) { updatedUids ->
                saveSelectedUids(updatedUids)
            }
            binding.recyclerView.adapter = adapter
        } else {
            adapter.updateData(apps, prevSelected)
        }
    }

    private fun getPrefs() = getSharedPreferences("proxy_prefs", Context.MODE_PRIVATE)

    private fun getInstalledApps(showSystem: Boolean): List<AppInfo> {
        val pm = packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter {
                if (showSystem) true else it.flags and ApplicationInfo.FLAG_SYSTEM == 0
            }
            .map {
                AppInfo(it.loadLabel(pm).toString(), it.packageName, it.uid)
            }
            .sortedBy { it.appName }
    }
}

data class AppInfo(val appName: String, val packageName: String, val uid: Int)
