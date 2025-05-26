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
    
    companion object {
        private const val TAG = "SettingsActivity"
        private const val DEFAULT_PROXY_PORT = 12345
        private const val DEFAULT_DNS_PORT = 10853
    }
    
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        // Применяем тему до создания контента
        applySelectedTheme()
        super.onCreate(savedInstanceState)
        
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Настройка edge-to-edge
        setupEdgeToEdge()
        
        // Инициализируем SharedPreferences
        prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)
        
        // Настройка toolbar
        setupToolbar()

        // Настройка переключателей и полей
        setupSwitches()
        setupNetworkFields()
        
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
            
            // Применяем AMOLED фон к AppBarLayout как в MainActivity
            binding.appBarLayout.setBackgroundColor(android.graphics.Color.BLACK)
            
            Log.d(TAG, "✅ AMOLED стиль применен к Settings (включая Toolbar и AppBarLayout)")
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
        // Проверяем поддержку Material You для динамических цветов
        val supportsMaterialYou = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        
        if (supportsMaterialYou) {
            Log.d(TAG, "✅ Material You поддерживается (Android 12+)")
        } else {
            Log.d(TAG, "⚠️ Material You не поддерживается (Android < 12)")
        }
        
        // Автозапуск
        binding.switchAutostart.isChecked = prefs.getBoolean("autostart", false)
        binding.switchAutostart.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("autostart", isChecked).apply()
            Log.d(TAG, "Автозапуск ${if (isChecked) "включен" else "отключен"}")
            
            val message = if (isChecked) {
                "Автозапуск включен. Правила будут восстановлены после перезагрузки."
            } else {
                "Автозапуск отключен."
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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
        binding.switchMaterialYou.isEnabled = supportsMaterialYou
        binding.switchMaterialYou.isChecked = prefs.getBoolean("material_you", false)
        binding.switchMaterialYou.setOnCheckedChangeListener { _, isChecked ->
            if (supportsMaterialYou) {
                prefs.edit().putBoolean("material_you", isChecked).apply()
                
                Log.d(TAG, "Material You ${if (isChecked) "включен" else "отключен"}")

                // Показываем уведомление пользователю
                val message = if (isChecked) {
                    if (prefs.getBoolean("amoled_theme", false)) {
                        "Material You включен для AMOLED темы.\nДинамические цвета применятся к элементам интерфейса,\nфон и AppBar останутся черными."
                    } else {
                        "Material You включен. Цвета будут извлечены из ваших обоев."
                    }
                } else {
                    "Material You отключен. Используются стандартные цвета."
                }
                
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()

                // Перезапускаем приложение с полной очисткой стека
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                binding.switchMaterialYou.isChecked = false
                Toast.makeText(this, "Material You недоступен на Android версии ниже 12", Toast.LENGTH_SHORT).show()
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
                    "AMOLED тема включена с поддержкой Material You.\nДинамические цвета будут применены к элементам интерфейса,\nфон и AppBar станут черными для экономии батареи."
                } else {
                    "AMOLED тема включена.\nФон и AppBar станут абсолютно черными для экономии батареи OLED экранов."
                }
            } else {
                "AMOLED тема отключена. Используется стандартная темная тема."
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

    private fun setupNetworkFields() {
        // Загружаем сохраненные порты
        val proxyPort = prefs.getInt("proxy_port", DEFAULT_PROXY_PORT)
        val dnsPort = prefs.getInt("dns_port", DEFAULT_DNS_PORT)
        
        binding.editTextProxyPort.setText(proxyPort.toString())
        binding.editTextDnsPort.setText(dnsPort.toString())
        
        // Настраиваем валидацию и сохранение
        binding.editTextProxyPort.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateAndSaveProxyPort()
            }
        }
        
        binding.editTextDnsPort.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateAndSaveDnsPort()
            }
        }
        
        Log.d(TAG, "✅ Сетевые поля настроены: прокси=$proxyPort, DNS=$dnsPort")
    }
    
    private fun validateAndSaveProxyPort() {
        val portText = binding.editTextProxyPort.text.toString().trim()
        val port = portText.toIntOrNull()
        
        when {
            port == null -> {
                binding.textInputLayoutProxyPort.error = "Введите корректный номер порта"
                binding.editTextProxyPort.setText(prefs.getInt("proxy_port", DEFAULT_PROXY_PORT).toString())
            }
            port < 1024 || port > 65535 -> {
                binding.textInputLayoutProxyPort.error = "Порт должен быть в диапазоне 1024-65535"
                binding.editTextProxyPort.setText(prefs.getInt("proxy_port", DEFAULT_PROXY_PORT).toString())
            }
            port == prefs.getInt("dns_port", DEFAULT_DNS_PORT) -> {
                binding.textInputLayoutProxyPort.error = "Порт не должен совпадать с портом DNS"
                binding.editTextProxyPort.setText(prefs.getInt("proxy_port", DEFAULT_PROXY_PORT).toString())
            }
            else -> {
                binding.textInputLayoutProxyPort.error = null
                val oldPort = prefs.getInt("proxy_port", DEFAULT_PROXY_PORT)
                
                // Если порт действительно изменился, очищаем старые правила
                if (oldPort != port) {
                    clearOldRulesAndUpdatePort("proxy_port", oldPort, port)
                    Log.d(TAG, "Порт прокси изменен с $oldPort на $port")
                    Toast.makeText(this, "Порт прокси изменен на $port. Старые правила очищены.", Toast.LENGTH_LONG).show()
                } else {
                    Log.d(TAG, "Порт прокси не изменился: $port")
                }
            }
        }
    }
    
    private fun validateAndSaveDnsPort() {
        val portText = binding.editTextDnsPort.text.toString().trim()
        val port = portText.toIntOrNull()
        
        when {
            port == null -> {
                binding.textInputLayoutDnsPort.error = "Введите корректный номер порта"
                binding.editTextDnsPort.setText(prefs.getInt("dns_port", DEFAULT_DNS_PORT).toString())
            }
            port < 1024 || port > 65535 -> {
                binding.textInputLayoutDnsPort.error = "Порт должен быть в диапазоне 1024-65535"
                binding.editTextDnsPort.setText(prefs.getInt("dns_port", DEFAULT_DNS_PORT).toString())
            }
            port == prefs.getInt("proxy_port", DEFAULT_PROXY_PORT) -> {
                binding.textInputLayoutDnsPort.error = "Порт не должен совпадать с портом прокси"
                binding.editTextDnsPort.setText(prefs.getInt("dns_port", DEFAULT_DNS_PORT).toString())
            }
            else -> {
                binding.textInputLayoutDnsPort.error = null
                val oldPort = prefs.getInt("dns_port", DEFAULT_DNS_PORT)
                
                // Если порт действительно изменился, очищаем старые правила
                if (oldPort != port) {
                    clearOldRulesAndUpdatePort("dns_port", oldPort, port)
                    Log.d(TAG, "Порт DNS изменен с $oldPort на $port")
                    Toast.makeText(this, "Порт DNS изменен на $port. Старые правила очищены.", Toast.LENGTH_LONG).show()
                } else {
                    Log.d(TAG, "Порт DNS не изменился: $port")
                }
            }
        }
    }

    /**
     * Очищает старые правила и обновляет порт в настройках
     */
    private fun clearOldRulesAndUpdatePort(portType: String, oldPort: Int, newPort: Int) {
        try {
            // Получаем список активных UID
            val selectedUids = prefs.getStringSet("selected_uids", emptySet()) ?: emptySet()
            
            if (selectedUids.isNotEmpty()) {
                val uidsString = selectedUids.joinToString(" ")
                
                // Получаем текущие порты (один старый, один новый)
                val oldProxyPort = if (portType == "proxy_port") oldPort else prefs.getInt("proxy_port", DEFAULT_PROXY_PORT)
                val oldDnsPort = if (portType == "dns_port") oldPort else prefs.getInt("dns_port", DEFAULT_DNS_PORT)
                
                Log.d(TAG, "🧹 Очистка старых правил перед сменой $portType с $oldPort на $newPort")
                Log.d(TAG, "Активные UID для очистки: $uidsString")
                Log.d(TAG, "Старые порты для очистки: прокси=$oldProxyPort, DNS=$oldDnsPort")
                
                // Очищаем правила со старыми портами
                IptablesService.clearRulesWithOldPorts(this, uidsString, oldProxyPort, oldDnsPort)
                
                // Сохраняем новый порт
                prefs.edit().putInt(portType, newPort).apply()
                
                // Применяем правила с новыми портами
                Log.d(TAG, "🔄 Применяем правила с новыми портами")
                IptablesService.applyRules(this, uidsString)
                
                Log.d(TAG, "✅ Обновление портов завершено успешно")
            } else {
                // Если нет активных UID, просто сохраняем новый порт
                prefs.edit().putInt(portType, newPort).apply()
                Log.d(TAG, "📝 Порт сохранен без очистки правил (нет активных UID)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка при обновлении портов", e)
            Toast.makeText(this, "Ошибка при обновлении портов: ${e.message}", Toast.LENGTH_LONG).show()
        }
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
                // Для более старых версий используем WindowInsetsControllerCompat
                val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
                windowInsetsController.isAppearanceLightStatusBars = !isDarkTheme
                windowInsetsController.isAppearanceLightNavigationBars = !isDarkTheme
            }

            // Применяем отступы для системных UI аналогично MainActivity
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
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
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка настройки edge-to-edge", e)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
    
    override fun onPause() {
        super.onPause()
        // Сохраняем порты при выходе из активности
        validateAndSaveProxyPort()
        validateAndSaveDnsPort()
    }
}
