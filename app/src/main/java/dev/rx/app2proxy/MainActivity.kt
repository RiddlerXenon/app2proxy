package dev.rx.app2proxy

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import dev.rx.app2proxy.databinding.ActivityMainBinding
import java.io.File
import com.google.android.material.color.DynamicColors

class MainActivity : AppCompatActivity(), RulesUpdateListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private var showSystemApps = false

    companion object {
        private const val TAG = "MainActivity"
    }

    // Разрешения с учетом Android 15
    private val permissions = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            add(Manifest.permission.QUERY_ALL_PACKAGES)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            add(Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE)
        }
    }.toTypedArray()

    private val permissionLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            handlePermissionResults(permissions)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            Log.d(TAG, "=== MainActivity onCreate (Android ${Build.VERSION.SDK_INT}) ===")
            
            // Применяем тему в зависимости от версии Android
            applyThemeForAndroidVersion()
            
            super.onCreate(savedInstanceState)
            
            // Безопасная инициализация binding
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Настраиваем edge-to-edge режим
            setupEdgeToEdge()

            // Настройка toolbar
            setupToolbar()

            // Настройка ViewPager
            setupViewPager()

            // Настройка нижней навигации
            setupBottomNavigation()

            // Специальная проверка для Android 15 (без Toast)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                performAndroid15Setup()
            } else {
                performStandardSetup()
            }

            // Запрашиваем разрешения
            requestNecessaryPermissions()
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Критическая ошибка в onCreate", e)
            
            // Показываем диалог с ошибкой
            try {
                AlertDialog.Builder(this)
                    .setTitle("Ошибка запуска")
                    .setMessage("Произошла ошибка при запуске приложения: ${e.message}")
                    .setPositiveButton("OK") { _, _ -> 
                        finish()
                    }
                    .show()
            } catch (dialogError: Exception) {
                Log.e(TAG, "Ошибка показа диалога", dialogError)
                finish()
            }
        }
    }
    

    private fun applyThemeForAndroidVersion() {
        try {
            val prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)
            val useMaterialYou = prefs.getBoolean("material_you", false)
            val useAmoledTheme = prefs.getBoolean("amoled_theme", false)
            val isDarkTheme = prefs.getBoolean("dark_theme", true)
            
            Log.d(TAG, "🎨 Применяем тему в MainActivity: MaterialYou=$useMaterialYou, AMOLED=$useAmoledTheme, Dark=$isDarkTheme")

            // Выбираем тему в зависимости от настроек
            when {
                useAmoledTheme && isDarkTheme -> {
                    setTheme(R.style.Theme_App2Proxy_Amoled)
                    Log.d(TAG, "✅ AMOLED тема применена в MainActivity")
                }
                isDarkTheme -> {
                    setTheme(R.style.Theme_App2Proxy)
                    Log.d(TAG, "✅ Темная тема применена в MainActivity")
                }
                else -> {
                    setTheme(R.style.Theme_App2Proxy)
                    Log.d(TAG, "✅ Светлая тема применена в MainActivity")
                }
            }

            // Material You только для Android 12+
            if (useMaterialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    if (useAmoledTheme && isDarkTheme) {
                        // Для AMOLED темы с Material You применяем специальную логику
                        Log.d(TAG, "🎨 Применяем AMOLED + Material You в MainActivity")
                        AmoledDynamicColorScheme.applyAmoledDynamicColors(this)
                    } else {
                        // Для обычных тем применяем стандартный Material You
                        Log.d(TAG, "🎨 Применяем стандартный Material You в MainActivity")
                        DynamicColors.applyToActivityIfAvailable(this)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка применения Material You", e)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Общая ошибка применения темы в MainActivity", e)
            setTheme(R.style.Theme_App2Proxy) // Fallback тема
        }
    }
    
    private fun applyAmoledThemeIfNeeded() {
        val prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)
        val useAmoledTheme = prefs.getBoolean("amoled_theme", false)
        val isDarkTheme = prefs.getBoolean("dark_theme", true)
        
        if (useAmoledTheme && isDarkTheme) {
            // Применяем черный фон только к корневому контейнеру
            // Дочерние элементы сохранят динамические цвета
            AmoledDynamicColorScheme.applyAmoledBackgroundToView(binding.root)

            // Применяем AMOLED стиль к Toolbar
            AmoledDynamicColorScheme.applyAmoledToolbarStyle(binding.toolbar, this)
            
            // Также применяем черный фон к TabLayout для единообразия
            binding.bottomNavigation.setBackgroundColor(android.graphics.Color.BLACK)

            // Также применяем AMOLED фон к AppBarLayout
            binding.appBarLayout.setBackgroundColor(android.graphics.Color.BLACK)
            Log.d(TAG, "✅ AMOLED фон применен к корневому контейнеру MainActivity")
        }
    }
                
    private fun setupToolbar() {
        try {
            val toolbar: MaterialToolbar = binding.toolbar
            setSupportActionBar(toolbar)
            supportActionBar?.title = getString(R.string.app_name)
            supportActionBar?.setDisplayShowTitleEnabled(true)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка настройки toolbar", e)
        }
    }
    
    private fun performAndroid15Setup() {
        Log.d(TAG, "🔥 Выполняем настройку для Android 15")
        
        try {
            // Сохраняем информацию о версии Android
            val prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)
            prefs.edit()
                .putBoolean("is_android_15", true)
                .putInt("android_api_level", Build.VERSION.SDK_INT)
                .putString("android_version", Build.VERSION.RELEASE)
                .apply()
            
            // Специальная диагностика для Android 15 (без Toast)
            performAndroid15Diagnostics()
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка настройки Android 15", e)
            // Fallback к стандартной настройке
            performStandardSetup()
        }
    }
    
    private fun performStandardSetup() {
        Log.d(TAG, "📱 Выполняем стандартную настройку")
        
        try {
            // Расширенная диагностика автозагрузки (без Toast)
            performExtendedBootDiagnostics()
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка стандартной настройки", e)
        }
    }
    
    private fun performAndroid15Diagnostics() {
        Log.d(TAG, "🔥 Android 15 диагностика")
        
        try {
            val prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)
            val selectedUids = prefs.getStringSet("selected_uids", emptySet()) ?: emptySet()
            
            if (selectedUids.isEmpty()) {
                Log.d(TAG, "❌ Нет правил для диагностики Android 15")
                return
            }
            
            Log.d(TAG, "📋 Android 15: Диагностика ${selectedUids.size} правил")
            
            // Проверяем статус Android 15 автозагрузки
            val android15BootHandled = prefs.getBoolean("android_15_boot_handled", false)
            val lastAndroid15Restore = prefs.getLong("last_android_15_restore", 0)
            val android15Success = prefs.getBoolean("android_15_success", false)
            val android15Result = prefs.getString("android_15_result", "no_result")
            
            Log.d(TAG, "🔥 Android 15 boot обработан: $android15BootHandled")
            Log.d(TAG, "🔥 Последнее восстановление: $lastAndroid15Restore")
            Log.d(TAG, "🔥 Успех: $android15Success")
            Log.d(TAG, "🔥 Результат: $android15Result")
            
            // Читаем логи Android 15
            readAndroid15Logs()
            
            // Определяем нужно ли ручное восстановление
            val currentTime = System.currentTimeMillis()
            val currentBootTime = currentTime - android.os.SystemClock.elapsedRealtime()
            val timeSinceRestore = currentTime - lastAndroid15Restore
            
            val needsRestore = when {
                !android15BootHandled && (currentTime - currentBootTime) > 300000 -> {
                    Log.d(TAG, "🔧 Android 15: BootReceiver не сработал")
                    true
                }
                android15BootHandled && !android15Success && timeSinceRestore > 180000 -> {
                    Log.d(TAG, "🔧 Android 15: Восстановление не удалось")
                    true
                }
                android15Success && timeSinceRestore < 600000 -> {
                    Log.d(TAG, "✅ Android 15: Автозагрузка работает")
                    false
                }
                else -> {
                    Log.d(TAG, "🔧 Android 15: Статус неопределён, восстанавливаем")
                    true
                }
            }
            
            if (needsRestore) {
                Log.d(TAG, "🔧 Требуется ручное восстановление для Android 15")
                
                // Применяем правила
                IptablesService.applyRulesFromPrefs(this)
                
                // Сохраняем информацию
                prefs.edit()
                    .putLong("last_manual_restore_android_15", currentTime)
                    .putBoolean("manual_restore_android_15", true)
                    .apply()
            }
            
            // Очищаем временные флаги
            prefs.edit()
                .putBoolean("android_15_boot_handled", false)
                .apply()
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка Android 15 диагностики", e)
            
            // Аварийное восстановление
            try {
                IptablesService.applyRulesFromPrefs(this)
            } catch (restoreError: Exception) {
                Log.e(TAG, "❌ Ошибка аварийного восстановления Android 15", restoreError)
            }
        }
    }
    
    private fun readAndroid15Logs() {
        try {
            val bootLogFile = File(filesDir, "boot_receiver_log.txt")
            if (bootLogFile.exists()) {
                val log = bootLogFile.readText()
                Log.d(TAG, "📄 Android 15 Boot Log:\n$log")
            }
            
            val serviceLogFile = File(filesDir, "service_log.txt")
            if (serviceLogFile.exists()) {
                val log = serviceLogFile.readText()
                Log.d(TAG, "📄 Android 15 Service Log:\n$log")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка чтения логов Android 15", e)
        }
    }
    
    private fun performExtendedBootDiagnostics() {
        // Существующий код диагностики для старых версий Android (без Toast)
        try {
            Log.d(TAG, "=== СТАНДАРТНАЯ ДИАГНОСТИКА АВТОЗАГРУЗКИ ===")
            
            val prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)
            val selectedUids = prefs.getStringSet("selected_uids", emptySet()) ?: emptySet()
            
            if (selectedUids.isEmpty()) {
                Log.d(TAG, "❌ Нет сохранённых правил")
                return
            }
            
            // Стандартная логика диагностики...
            val bootReceiverActivated = prefs.getBoolean("boot_receiver_activated", false)
            val serviceRestoreSuccess = prefs.getBoolean("service_restore_success", false)
            
            if (!bootReceiverActivated) {
                Log.d(TAG, "🔧 Требуется ручное восстановление")
                IptablesService.applyRulesFromPrefs(this)
            } else if (!serviceRestoreSuccess) {
                Log.d(TAG, "🔧 BootReceiver сработал, но восстановление не удалось")
                IptablesService.applyRulesFromPrefs(this)
            } else {
                Log.d(TAG, "✅ Автозагрузка работает")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка стандартной диагностики", e)
        }
    }
    
    private fun requestNecessaryPermissions() {
        if (permissions.isEmpty()) return
        
        val missingPermissions = permissions.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isNotEmpty()) {
            Log.d(TAG, "Запрашиваем разрешения: $missingPermissions")
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }
    
    private fun handlePermissionResults(results: Map<String, Boolean>) {
        val grantedPermissions = results.filterValues { it }.keys
        val deniedPermissions = results.filterValues { !it }.keys
        
        Log.d(TAG, "Разрешены: $grantedPermissions")
        Log.d(TAG, "Отклонены: $deniedPermissions")
        
        if (deniedPermissions.isNotEmpty()) {
            val message = buildString {
                append("Некоторые разрешения отклонены:\n")
                deniedPermissions.forEach { permission ->
                    when (permission) {
                        Manifest.permission.QUERY_ALL_PACKAGES -> 
                            append("• Просмотр всех приложений\n")
                        Manifest.permission.POST_NOTIFICATIONS -> 
                            append("• Уведомления\n")
                        Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE -> 
                            append("• Фоновый сервис\n")
                    }
                }
                append("\nЭто может повлиять на работу автозагрузки.")
            }
            
            AlertDialog.Builder(this)
                .setTitle("Разрешения отклонены")
                .setMessage(message)
                .setPositiveButton("Настройки") { _, _ ->
                    openAppSettings()
                }
                .setNegativeButton("Продолжить", null)
                .show()
        }
    }
    
    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка открытия настроек", e)
            Toast.makeText(this, "Не удалось открыть настройки", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupEdgeToEdge() {
        try {
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
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка настройки edge-to-edge", e)
        }
    }

    private fun setupViewPager() {
        try {
            viewPagerAdapter = ViewPagerAdapter(this, this)
            binding.viewPager.adapter = viewPagerAdapter
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка настройки ViewPager", e)
        }
    }

    private fun setupBottomNavigation() {
        try {
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
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка настройки BottomNavigation", e)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return try {
            menuInflater.inflate(R.menu.menu_main, menu)
            menu.findItem(R.id.action_show_system)?.isChecked = showSystemApps
            true
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка создания меню", e)
            false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return try {
            when (item.itemId) {
                R.id.action_show_system -> {
                    item.isChecked = !item.isChecked
                    showSystemApps = item.isChecked
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
                    try {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    } catch (e: Exception) {
                        Log.e(TAG, "Ошибка открытия настроек", e)
                        Toast.makeText(this, "Ошибка открытия настроек", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка обработки меню", e)
            false
        }
    }

    // Реализация RulesUpdateListener
    override fun onRulesUpdated() {
        try {
            getAppListFragment()?.refreshSelectedStates()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка обновления правил", e)
        }
    }

    private fun getAppListFragment(): AppListFragment? {
        return try {
            supportFragmentManager.findFragmentByTag("f0") as? AppListFragment
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка получения AppListFragment", e)
            null
        }
    }

    override fun onResume() {
        super.onResume()

        applyAmoledThemeIfNeeded()
    }

    private fun checkRulesConsistency() {
        try {
            val prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)
            val selectedUids = prefs.getStringSet("selected_uids", emptySet()) ?: emptySet()
            val lastConsistencyCheck = prefs.getLong("last_consistency_check", 0)
            val currentTime = System.currentTimeMillis()
            
            // Проверяем не чаще чем раз в 5 минут
            if (selectedUids.isNotEmpty() && currentTime - lastConsistencyCheck > 300000) {
                Log.d(TAG, "🔍 Проверяем соответствие правил iptables")
                
                prefs.edit()
                    .putLong("last_consistency_check", currentTime)
                    .apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при проверке соответствия правил", e)
        }
    }
    
    private fun checkComponentsStatus() {
        try {
            val packageManager = packageManager
            
            // Проверяем состояние BootReceiver
            val bootReceiverComponent = ComponentName(this, BootReceiver::class.java)
            val bootReceiverState = packageManager.getComponentEnabledSetting(bootReceiverComponent)
            Log.d(TAG, "BootReceiver состояние: $bootReceiverState")
            
            // Проверяем состояние AutoStartService
            val serviceComponent = ComponentName(this, AutoStartService::class.java)
            val serviceState = packageManager.getComponentEnabledSetting(serviceComponent)
            Log.d(TAG, "AutoStartService состояние: $serviceState")
            
            // Проверяем состояние alias активности
            try {
                val aliasComponent = ComponentName(this, "dev.rx.app2proxy.AppInfoActivity")
                val aliasState = packageManager.getComponentEnabledSetting(aliasComponent)
                Log.d(TAG, "AppInfoActivity alias состояние: $aliasState")
            } catch (e: Exception) {
                Log.w(TAG, "Alias активность не найдена: ${e.message}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка проверки состояния компонентов", e)
        }
    }
    
    override fun onDestroy() {
        try {
            Log.d(TAG, "MainActivity уничтожается")
            super.onDestroy()
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка в onDestroy", e)
            super.onDestroy()
        }
    }
}
