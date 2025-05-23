package dev.rx.app2proxy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayoutMediator
import dev.rx.app2proxy.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewPagerAdapter: ViewPagerAdapter
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
            if (!allGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Показать сообщение о необходимости разрешения
                android.widget.Toast.makeText(
                    this, 
                    "Для отображения всех приложений необходимо разрешение", 
                    android.widget.Toast.LENGTH_LONG
                ).show()
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

        // Настраиваем edge-to-edge режим
        setupEdgeToEdge()

        val toolbar: MaterialToolbar = binding.toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.app_name)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        // Настройка ViewPager и TabLayout
        setupViewPager()

        // Запрашиваем разрешения если нужно
        if (permissions.isNotEmpty()) {
            val needsPermission = permissions.any { 
                checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED 
            }
            if (needsPermission) {
                permissionLauncher.launch(permissions)
            }
        }
    }

    private fun setupEdgeToEdge() {
        // Включаем edge-to-edge режим
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = 
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }

        // Настраиваем цвета статус бара в зависимости от темы
        val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView)
        val prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)
        val isDarkTheme = prefs.getBoolean("dark_theme", true)
        
        windowInsetsController?.isAppearanceLightStatusBars = !isDarkTheme
        windowInsetsController?.isAppearanceLightNavigationBars = !isDarkTheme

        // Устанавливаем обработчик для системных отступов
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            // Получаем отступы для системных баров
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // AppBarLayout автоматически обработает верхний отступ благодаря fitsSystemWindows="true"
            // Нам нужно только обработать боковые и нижний отступы
            binding.root.setPadding(
                systemBars.left,
                0, // Верхний отступ обрабатывается AppBarLayout
                systemBars.right,
                systemBars.bottom
            )
            
            insets
        }
    }

    private fun setupViewPager() {
        viewPagerAdapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = viewPagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Приложения"
                1 -> "Правила"
                else -> ""
            }
        }.attach()
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
                // Уведомляем фрагмент о изменении
                val fragment = supportFragmentManager.findFragmentByTag("f0") as? AppListFragment
                fragment?.setShowSystemApps(showSystemApps)
                true
            }
            R.id.action_select_all -> {
                val fragment = supportFragmentManager.findFragmentByTag("f0") as? AppListFragment
                fragment?.selectAll()
                true
            }
            R.id.action_deselect_all -> {
                val fragment = supportFragmentManager.findFragmentByTag("f0") as? AppListFragment
                fragment?.deselectAll()
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
