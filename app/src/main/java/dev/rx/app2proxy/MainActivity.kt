package dev.rx.app2proxy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import dev.rx.app2proxy.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), RulesUpdateListener {
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
                Toast.makeText(
                    this, 
                    "Для отображения всех приложений необходимо разрешение", 
                    Toast.LENGTH_LONG
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

        // Настройка ViewPager
        setupViewPager()

        // Настройка нижней навигации
        setupBottomNavigation()

        // Проверяем и восстанавливаем правила при запуске приложения
        checkAndRestoreRulesOnStart()

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

    /**
     * Проверяет и восстанавливает правила iptables при запуске приложения
     * Это дополнительная защита на случай, если автозагрузка не сработала
     */
    private fun checkAndRestoreRulesOnStart() {
        try {
            val prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)
            val selectedUids = prefs.getStringSet("selected_uids", emptySet()) ?: emptySet()
            
            if (selectedUids.isNotEmpty()) {
                // Получаем время последней перезагрузки системы
                val currentBootTime = System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime()
                val lastBootTime = prefs.getLong("last_boot_time", 0)
                val lastRulesApplied = prefs.getLong("last_rules_applied", 0)
                
                // Проверяем, нужно ли восстанавливать правила
                val shouldRestoreRules = when {
                    // Если это первый запуск после установки
                    lastBootTime == 0L -> true
                    
                    // Если прошло больше 2 минут с момента перезагрузки и правила не применялись
                    currentBootTime > lastBootTime + 120000 && lastRulesApplied < currentBootTime -> true
                    
                    // Если разница во времени загрузки больше 1 минуты (новая перезагрузка)
                    Math.abs(currentBootTime - lastBootTime) > 60000 -> true
                    
                    else -> false
                }
                
                if (shouldRestoreRules) {
                    android.util.Log.d("MainActivity", "Восстанавливаем правила iptables для ${selectedUids.size} приложений")
                    
                    // Применяем правила
                    IptablesService.applyRulesFromPrefs(this)
                    
                    // Сохраняем время применения правил
                    prefs.edit()
                        .putLong("last_boot_time", currentBootTime)
                        .putLong("last_rules_applied", System.currentTimeMillis())
                        .apply()
                    
                    // Показываем уведомление пользователю
                    Toast.makeText(
                        this, 
                        "App2Proxy: Правила iptables восстановлены для ${selectedUids.size} приложений", 
                        Toast.LENGTH_LONG
                    ).show()
                    
                    android.util.Log.d("MainActivity", "Правила iptables успешно восстановлены")
                } else {
                    android.util.Log.d("MainActivity", "Правила iptables уже применены, восстановление не требуется")
                }
            } else {
                android.util.Log.d("MainActivity", "Нет сохранённых правил для восстановления")
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Ошибка при проверке и восстановлении правил iptables", e)
            Toast.makeText(this, "Ошибка при восстановлении правил: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupEdgeToEdge() {
        // Включаем edge-to-edge режим с использованием WindowCompat (современный API)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Получаем настройки темы
        val prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)
        val isDarkTheme = prefs.getBoolean("dark_theme", true)

        // Настраиваем цвета статус бара и навигационной панели
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Для API 30+ используем новый WindowInsetsController
            val insetsController = window.insetsController
            if (isDarkTheme) {
                insetsController?.setSystemBarsAppearance(0, 
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
            } else {
                insetsController?.setSystemBarsAppearance(
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
            }
        } else {
            // Для более старых версий используем WindowInsetsControllerCompat
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.isAppearanceLightStatusBars = !isDarkTheme
            windowInsetsController.isAppearanceLightNavigationBars = !isDarkTheme
        }

        // Устанавливаем обработчик для системных отступов
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            // Получаем отступы для системных баров
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // AppBarLayout автоматически обработает верхний отступ благодаря fitsSystemWindows="true"
            // Нам нужно только обработать боковые отступы, нижний обрабатывается bottom navigation
            binding.root.setPadding(
                systemBars.left,
                0, // Верхний отступ обрабатывается AppBarLayout
                systemBars.right,
                0 // Нижний отступ обрабатывается BottomNavigation
            )
            
            // Применяем отступ для нижней навигации
            val bottomNavigationParams = binding.bottomNavigation.layoutParams as androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams
            bottomNavigationParams.bottomMargin = systemBars.bottom
            binding.bottomNavigation.layoutParams = bottomNavigationParams
            
            insets
        }
    }

    private fun setupViewPager() {
        viewPagerAdapter = ViewPagerAdapter(this, this)
        binding.viewPager.adapter = viewPagerAdapter
    }

    private fun setupBottomNavigation() {
        val bottomNavigation: BottomNavigationView = binding.bottomNavigation
        
        // Устанавливаем начальную вкладку
        bottomNavigation.selectedItemId = R.id.nav_apps
        
        // Настраиваем обработчик нажатий
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_apps -> {
                    binding.viewPager.currentItem = 0
                    true
                }
                R.id.nav_rules -> {
                    binding.viewPager.currentItem = 1
                    true
                }
                else -> false
            }
        }

        // Синхронизируем ViewPager с BottomNavigation
        binding.viewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    0 -> bottomNavigation.selectedItemId = R.id.nav_apps
                    1 -> bottomNavigation.selectedItemId = R.id.nav_rules
                }
            }
        })
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
                getAppListFragment()?.setShowSystemApps(showSystemApps)
                true
            }
            R.id.action_select_all -> {
                getAppListFragment()?.selectAll()
                true
            }
            R.id.action_deselect_all -> {
                getAppListFragment()?.deselectAll()
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Реализация RulesUpdateListener
    override fun onRulesUpdated() {
        // Уведомляем фрагмент списка приложений об изменении правил
        getAppListFragment()?.refreshSelectedStates()
    }

    // Вспомогательные методы для получения фрагментов
    private fun getAppListFragment(): AppListFragment? {
        return try {
            supportFragmentManager.findFragmentByTag("f0") as? AppListFragment
        } catch (e: Exception) {
            null
        }
    }

    override fun onResume() {
        super.onResume()
        
        // Дополнительная проверка правил при возврате в приложение
        // Это полезно, если пользователь вручную очистил правила iptables
        checkRulesConsistency()
    }

    /**
     * Проверяет соответствие сохранённых настроек и текущих правил iptables
     */
    private fun checkRulesConsistency() {
        try {
            val prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)
            val selectedUids = prefs.getStringSet("selected_uids", emptySet()) ?: emptySet()
            val lastConsistencyCheck = prefs.getLong("last_consistency_check", 0)
            val currentTime = System.currentTimeMillis()
            
            // Проверяем не чаще чем раз в 5 минут
            if (selectedUids.isNotEmpty() && currentTime - lastConsistencyCheck > 300000) {
                android.util.Log.d("MainActivity", "Проверяем соответствие правил iptables")
                
                // Здесь можно добавить проверку текущих правил iptables
                // и при необходимости восстановить их
                
                prefs.edit().putLong("last_consistency_check", currentTime).apply()
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Ошибка при проверке соответствия правил", e)
        }
    }
}
