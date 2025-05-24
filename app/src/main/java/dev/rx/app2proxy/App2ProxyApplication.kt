package dev.rx.app2proxy

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors

class App2ProxyApplication : Application() {
    
    companion object {
        private const val TAG = "App2ProxyApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        Log.d(TAG, "🚀 Запуск App2Proxy Application")
        Log.d(TAG, "📱 Android версия: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        
        try {
            // Инициализация настроек приложения
            initializeAppSettings()
            
            // Применение темы
            applyTheme()
            
            // Инициализация Material You
            initializeMaterialYou()
            
            // Специальная инициализация для разных версий Android
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM -> {
                    initializeAndroid15Compatibility()
                    Log.d(TAG, "✅ Инициализация Android 15 завершена")
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                    initializeAndroid14Compatibility()
                    Log.d(TAG, "✅ Инициализация Android 14 завершена")
                }
                else -> {
                    Log.d(TAG, "✅ Стандартная инициализация завершена")
                }
            }
            
            Log.d(TAG, "✅ App2Proxy Application успешно инициализировано")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Критическая ошибка инициализации приложения", e)
        }
    }
    
    private fun applyTheme() {
        try {
            val prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)
            val isDarkTheme = prefs.getBoolean("dark_theme", true)
            
            // Устанавливаем глобальную тему
            AppCompatDelegate.setDefaultNightMode(
                if (isDarkTheme) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
            
            Log.d(TAG, "✅ Тема применена: ${if (isDarkTheme) "Темная" else "Светлая"}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка применения темы", e)
            // Fallback к системной теме
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
    
    private fun initializeAppSettings() {
        try {
            val prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)
            val isFirstLaunch = !prefs.getBoolean("app_initialized", false)
            
            if (isFirstLaunch) {
                Log.d(TAG, "🎉 Первый запуск приложения - инициализируем настройки по умолчанию")
                
                prefs.edit()
                    .putBoolean("autostart", false)
                    .putBoolean("dark_theme", true)
                    .putBoolean("amoled_theme", false) // Новая настройка AMOLED темы
                    .putBoolean("app_initialized", true)
                    .putLong("first_launch_time", System.currentTimeMillis())
                    .putBoolean("material_you", Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    .apply()
                Log.d(TAG, "Выполнена первичная инициализация настроек")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка инициализации настроек", e)
        }
    }
    
    private fun initializeMaterialYou() {
        try {
            val prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)
            val useMaterialYou = prefs.getBoolean("material_you", false)
            
            if (useMaterialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                DynamicColors.applyToActivitiesIfAvailable(this)
                Log.d(TAG, "Material You включен")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка инициализации Material You", e)
        }
    }
    
    private fun initializeAndroid15Compatibility() {
        try {
            Log.d(TAG, "Настройка совместимости с Android 15")
            
            // Специальные настройки для Android 15
            val prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)
            prefs.edit()
                .putBoolean("android_15_mode", true)
                .putBoolean("enhanced_boot_receiver", true)
                .putBoolean("aggressive_service_start", true)
                .apply()
                
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка настройки Android 15", e)
        }
    }
    
    private fun initializeAndroid14Compatibility() {
        try {
            Log.d(TAG, "Настройка совместимости с Android 14")
            
            val prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)
            prefs.edit()
                .putBoolean("android_14_mode", true)
                .putBoolean("foreground_service_special_use", true)
                .apply()
                
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка настройки Android 14", e)
        }
    }
}
