package dev.rx.app2proxy

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.appbar.MaterialToolbar
import dev.rx.app2proxy.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        // Применяем тему перед вызовом super.onCreate()
        applySelectedTheme()
        
        super.onCreate(savedInstanceState)
        
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Настраиваем edge-to-edge режим
        setupEdgeToEdge()

        // Настройка toolbar
        val toolbar: MaterialToolbar = binding.toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = getString(R.string.action_settings)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
        }

        // Настройка переключателей
        setupSwitches()
    }

    private fun applySelectedTheme() {
        prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)
        val useMaterialYou = prefs.getBoolean("material_you", false)
        val useAmoledTheme = prefs.getBoolean("amoled_theme", false)
        val isDarkTheme = prefs.getBoolean("dark_theme", true)
        
        // Применяем Material You если включен
        if (useMaterialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            com.google.android.material.color.DynamicColors.applyToActivityIfAvailable(this)
        }

        // Выбираем тему
        when {
            useAmoledTheme && isDarkTheme -> setTheme(R.style.Theme_App2Proxy_Amoled)
            else -> setTheme(R.style.Theme_App2Proxy)
        }
    }

    private fun setupSwitches() {
        // Автозапуск
        binding.switchAutostart.isChecked = prefs.getBoolean("autostart", false)
        binding.switchAutostart.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("autostart", isChecked).apply()
        }

        // Темная тема
        binding.switchTheme.isChecked = prefs.getBoolean("dark_theme", true)
        binding.switchTheme.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_theme", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
            
            // Если выключается темная тема, отключаем AMOLED
            if (!isChecked && binding.switchAmoledTheme.isChecked) {
                binding.switchAmoledTheme.isChecked = false
                prefs.edit().putBoolean("amoled_theme", false).apply()
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
                
                // Показываем сообщение о перезапуске приложения
                Toast.makeText(this, "🎨 Перезапуск приложения для применения Material You...", Toast.LENGTH_SHORT).show()
                
                // Перезапускаем приложение с полной очисткой стека
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                binding.switchMaterialYou.isChecked = false
                Toast.makeText(this, "Material You доступен только на Android 12 и выше", Toast.LENGTH_SHORT).show()
            }
        }

        // AMOLED тема
        binding.switchAmoledTheme.isChecked = prefs.getBoolean("amoled_theme", false)
        binding.switchAmoledTheme.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !binding.switchTheme.isChecked) {
                // Если пытаются включить AMOLED без темной темы
                binding.switchAmoledTheme.isChecked = false
                Toast.makeText(this, "Для AMOLED темы необходимо включить темную тему", Toast.LENGTH_SHORT).show()
                return@setOnCheckedChangeListener
            }
            
            prefs.edit().putBoolean("amoled_theme", isChecked).apply()
            
            // Показываем сообщение о применении темы
            val message = if (isChecked) {
                "🌃 Перезапуск приложения для применения AMOLED темы..."
            } else {
                "☀️ Перезапуск приложения для отключения AMOLED темы..."
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            
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
        }
    }

    private fun restartActivity() {
        // Перезапускаем текущую активность для применения темы
        val intent = intent
        finish()
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun setupEdgeToEdge() {
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
            // Для более старых версий используем WindowInsetsControllerCompat
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.isAppearanceLightStatusBars = !isDarkTheme
            windowInsetsController.isAppearanceLightNavigationBars = !isDarkTheme
        }

        // Устанавливаем обработчик для системных отступов
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Применяем отступы с учетом системных баров
            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            
            insets
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
