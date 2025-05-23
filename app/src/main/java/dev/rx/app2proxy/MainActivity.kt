package dev.rx.app2proxy

import android.Manifest
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

    // Разрешения для Android 11+ (API 30+)
    private val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        arrayOf(Manifest.permission.QUERY_ALL_PACKAGES)
    } else {
        emptyArray()
    }

    private val permissionLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.all { it.value }
            if (allGranted || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                updateAppList()
            } else {
                // Показать сообщение о необходимости разрешения
                android.widget.Toast.makeText(
                    this, 
                    "Для отображения всех приложений необходимо разрешение", 
                    android.widget.Toast.LENGTH_LONG
                ).show()
                updateAppList() // Все равно попробуем загрузить список
            }
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

        // Обработка system bars
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                left = systemBars.left,
                right = systemBars.right,
                bottom = systemBars.bottom
            )
            insets
        }

        val toolbar: MaterialToolbar = binding.toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.app_name)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        // Настраиваем SwipeRefreshLayout
        binding.swipeRefresh.setOnRefreshListener {
            updateAppList()
            binding.swipeRefresh.isRefreshing = false
        }

        // Запрашиваем разрешения если нужно
        if (permissions.isNotEmpty()) {
            val needsPermission = permissions.any { 
                checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED 
            }
            if (needsPermission) {
                permissionLauncher.launch(permissions)
            } else {
                updateAppList()
            }
        } else {
            updateAppList()
        }

        binding.btnApply.setOnClickListener {
            val uids = adapter.getSelectedUids().joinToString(" ")
            if (uids.isNotEmpty()) {
                IptablesService.applyRules(this, uids)
            } else {
                android.widget.Toast.makeText(this, "Выберите хотя бы одно приложение", android.widget.Toast.LENGTH_SHORT).show()
            }
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
                if (::adapter.isInitialized) {
                    adapter.selectAll()
                    saveSelectedUids(adapter.getSelectedUids())
                }
                true
            }
            R.id.action_deselect_all -> {
                if (::adapter.isInitialized) {
                    adapter.deselectAll()
                    saveSelectedUids(adapter.getSelectedUids())
                }
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
        
        if (apps.isEmpty()) {
            android.widget.Toast.makeText(this, "Не удалось загрузить список приложений", android.widget.Toast.LENGTH_LONG).show()
            return
        }
        
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
        return try {
            val pm = packageManager
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter {
                    if (showSystem) true else (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0
                }
                .mapNotNull { appInfo ->
                    try {
                        AppInfo(
                            appInfo.loadLabel(pm).toString(),
                            appInfo.packageName,
                            appInfo.uid
                        )
                    } catch (e: Exception) {
                        null // Пропускаем приложения, которые не удается обработать
                    }
                }
                .sortedBy { it.appName }
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Ошибка загрузки приложений: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            emptyList()
        }
    }
}

data class AppInfo(val appName: String, val packageName: String, val uid: Int)
