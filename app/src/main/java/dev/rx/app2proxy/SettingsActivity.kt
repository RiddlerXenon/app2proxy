package dev.rx.app2proxy

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.DynamicColors
import dev.rx.app2proxy.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences
    
    companion object {
        private const val TAG = "SettingsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Применяем тему перед вызовом super.onCreate()
        applySelectedTheme()
        
        super.onCreate(savedInstanceState)
        
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Настраиваем edge-to-edge режим
        setupEdgeToEdge()

        // Настройка toolbar
        setupToolbar()

        // Настройка переключателей
        setupSwitches()
        
        // Применяем AMOLED фон после создания интерфейса
        applyAmoledThemeIfNeeded()
    }

    private fun applySelectedTheme() {
        prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)
        val useMaterialYou = prefs.getBoolean("material_you", false)
        val useAmoledTheme = prefs.getBoolean("amoled_theme", false)
        val isDarkTheme = prefs.getBoolean("dark_theme", true)
        
        Log.d(TAG, "🎨 Применяем тему в Settings: MaterialYou=$useMaterialYou, AMOLED=$useAmoledTheme, Dark=$isDarkTheme")
        
        // Сначала выбираем базовую тему
        when {
            useAmoledTheme && isDarkTheme -> setTheme(R.style.Theme_App2Proxy_Amoled)
            else -> setTheme(R.style.Theme_App2Proxy)
        }
        
        // Применяем динамические цвета Material You только если включен и поддерживается
        if (useMaterialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (useAmoledTheme && isDarkTheme) {
                // Для AMOLED темы с Material You применяем специальную логику
                Log.d(TAG, "🎨 Применяем AMOLED + Material You")
                AmoledDynamicColorScheme.applyAmoledDynamicColors(this)
            } else {
                // Для обычных тем применяем Material You стандартно
                Log.d(TAG, "🎨 Применяем стандартный Material You")
                DynamicColors.applyToActivityIfAvailable(this)
            }
        }
    }
    
    private fun applyAmoledThemeIfNeeded() {
        val useAmoledTheme = prefs.getBoolean("amoled_theme", false)
        val isDarkTheme = prefs.getBoolean("dark_theme", true)
        
        if (useAmoledTheme && isDarkTheme) {
            // Применяем черный фон только к корневому контейнеру
            AmoledDynamicColorScheme.applyAmoledBackgroundToView(binding.root)
            
            // Применяем AMOLED стиль к Toolbar
            AmoledDynamicColorScheme.applyAmoledToolbarStyle(binding.toolbar, this)
            
            Log.d(TAG, "✅ AMOLED стиль применен к Settings (включая Toolbar)")
        }
    }

    override fun onResume() {
        super.onResume()
        
        // Повторно применяем AMOLED стиль если нужно
        applyAmoledThemeIfNeeded()
    }

    private fun setupToolbar() {
        try {
            val toolbar: MaterialToolbar = binding.toolbar
            setSupportActionBar(toolbar)
            supportActionBar?.apply {
                title = getString(R.string.action_settings)
                setDisplayHomeAsUpEnabled(true)
                setDisplayShowTitleEnabled(true)
            }
            Log.d(TAG, "✅ Toolbar настроен")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка настройки toolbar", e)
        }
    }

    private fun setupSwitches() {
        // Автозапуск
        binding.switchAutostart.isChecked = prefs.getBoolean("autostart", false)
        binding.switchAutostart.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("autostart", isChecked).apply()
            Log.d(TAG, "Автозапуск ${if (isChecked) "включен" else "отключен"}")
        }

        // Темная тема
        binding.switchTheme.isChecked = prefs.getBoolean("dark_theme", true)
        binding.switchTheme.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_theme", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
            
            Log.d(TAG, "Тема изменена на ${if (isChecked) "темную" else "светлую"}")
            
            // Если выключается темная тема, отключаем AMOLED
            if (!isChecked && binding.switchAmoledTheme.isChecked) {
                binding.switchAmoledTheme.isChecked = false
                prefs.edit().putBoolean("amoled_theme", false).apply()
                Log.d(TAG, "AMOLED тема отключена при переключении на светлую тему")
            }
            
            // Обновляем доступность AMOLED переключателя
            updateAmoledSwitchState()
            
            // Перезапускаем активность для применения темы
            restartActivity()
        }

        // Material You
        binding.switchMaterialYou.isEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        binding.switchMaterialYou.isChecked = prefs.getBoolean("material_you", false)
        binding.switchMaterialYou.setOnCheckedChangeListener { _, isChecked ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                prefs.edit().putBoolean("material_you", isChecked).apply()
                
                Log.d(TAG, "Material You ${if (isChecked) "включен" else "отключен"}")

                // Показываем уведомление пользователю
                val message = if (isChecked) {
                    if (prefs.getBoolean("amoled_theme", false)) {
                        "Material You включен для AMOLED темы.\nДинамические цвета применятся к элементам интерфейса,\nфон и AppBar останутся черными."
                    } else {
                        "Material You включен"
                    }
                } else {
                    "Material You отключен"
                }
                
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()

                // Перезапускаем приложение с полной очисткой стека
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                binding.switchMaterialYou.isChecked = false
                Toast.makeText(this, "Material You недоступен на этой версии Android", Toast.LENGTH_SHORT).show()
                Log.w(TAG, "Попытка включить Material You на неподдерживаемой версии Android")
            }
        }

        // AMOLED тема
        binding.switchAmoledTheme.isChecked = prefs.getBoolean("amoled_theme", false)
        binding.switchAmoledTheme.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !binding.switchTheme.isChecked) {
                // Если пытаются включить AMOLED без темной темы
                binding.switchAmoledTheme.isChecked = false
                Toast.makeText(this, "AMOLED тема доступна только в темном режиме", Toast.LENGTH_SHORT).show()
                Log.w(TAG, "Попытка включить AMOLED тему без темного режима")
                return@setOnCheckedChangeListener
            }
            
            prefs.edit().putBoolean("amoled_theme", isChecked).apply()
            
            val message = if (isChecked) {
                if (prefs.getBoolean("material_you", false)) {
                    "AMOLED тема включена с поддержкой Material You.\nДинамические цвета будут применены к элементам интерфейса,\nфон и AppBar останутся черными для экономии батареи."
                } else {
                    "AMOLED тема включена.\nФон и AppBar станут черными для экономии батареи."
                }
            } else {
                "AMOLED тема отключена"
            }
            
            Log.d(TAG, message)
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            
            // Перезапускаем приложение аналогично Material You
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Обновляем состояние AMOLED переключателя
        updateAmoledSwitchState()
    }

    private fun updateAmoledSwitchState() {
        val isDarkTheme = binding.switchTheme.isChecked
        binding.switchAmoledTheme.isEnabled = isDarkTheme
        
        if (!isDarkTheme && binding.switchAmoledTheme.isChecked) {
            binding.switchAmoledTheme.isChecked = false
            prefs.edit().putBoolean("amoled_theme", false).apply()
            Log.d(TAG, "AMOLED переключатель отключен и сброшен из-за светлой темы")
        }
    }

    private fun restartActivity() {
        Log.d(TAG, "Перезапуск Settings активности для применения темы")
        // Перезапускаем текущую активность для применения темы
        val intent = intent
        finish()
        startActivity(intent)
        
        // Используем современный API для анимации переходов
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ (API 34+) - новый API
            overrideActivityTransition(
                OVERRIDE_TRANSITION_CLOSE,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
        } else {
            // Старые версии Android - устаревший но рабочий API
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun setupEdgeToEdge() {
        try {
            // Включаем edge-to-edge режим с использованием WindowCompat (современный API)
            WindowCompat.setDecorFitsSystemWindows(window, false)

            // Получаем настройки темы
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
                // Для более старых версий используем системные флаги
                val flags = if (isDarkTheme) {
                    0 // Светлые иконки на темном фоне
                } else {
                    // Темные иконки на светлом фоне
                    @Suppress("DEPRECATION")
                    android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or 
                    @Suppress("DEPRECATION")
                    android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                }
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = flags
            }

            // Применяем отступы для системных UI
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
                insets
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка настройки edge-to-edge", e)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
