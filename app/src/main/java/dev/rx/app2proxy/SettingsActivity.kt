package dev.rx.app2proxy

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.appbar.MaterialToolbar
import dev.rx.app2proxy.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        // Применяем Material You если включен
        val prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)
        val useMaterialYou = prefs.getBoolean("material_you", false)
        if (useMaterialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            com.google.android.material.color.DynamicColors.applyToActivityIfAvailable(this)
        }

        setTheme(R.style.Theme_App2Proxy)
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        this.prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)

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
        binding.switchAutostart.isChecked = prefs.getBoolean("autostart", false)
        binding.switchAutostart.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("autostart", isChecked).apply()
        }

        binding.switchTheme.isChecked = prefs.getBoolean("dark_theme", true)
        binding.switchTheme.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_theme", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        // Переключатель Material You с автоперезапуском
        binding.switchMaterialYou.isEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        binding.switchMaterialYou.isChecked = prefs.getBoolean("material_you", false)
        binding.switchMaterialYou.setOnCheckedChangeListener { _, isChecked ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                prefs.edit().putBoolean("material_you", isChecked).apply()
                
                // Автоматический перезапуск приложения
                Toast.makeText(this, "Перезапуск приложения для применения Material You...", Toast.LENGTH_SHORT).show()
                
                // Перезапускаем приложение
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                binding.switchMaterialYou.isChecked = false
                Toast.makeText(this, "Material You доступен только на Android 12 и выше", Toast.LENGTH_SHORT).show()
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
