package dev.rx.app2proxy

import android.app.Application
import android.content.Context
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
            // Проверяем доступность зашифрованного хранилища
            if (isDeviceProtectedStorageAvailable()) {
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
            } else {
                Log.w(TAG, "⚠️ Пользователь еще не разблокирован, отложенная инициализация")
                // Применяем безопасные настройки по умолчанию
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                Log.d(TAG, "✅ Базовая инициализация завершена")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Критическая ошибка инициализации приложения", e)
            // Fallback инициализация
            try {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                Log.d(TAG, "✅ Fallback инициализация завершена")
            } catch (fallbackError: Exception) {
                Log.e(TAG, "❌ Fallback инициализация также провалилась", fallbackError)
            }
        }
    }
    
    private fun isDeviceProtectedStorageAvailable(): Boolean {
        return try {
            // Попытка получить доступ к SharedPreferences
            getSharedPreferences("test_prefs", MODE_PRIVATE)
            true
        } catch (e: IllegalStateException) {
            // Пользователь еще не разблокирован
            false
        } catch (e: Exception) {
            Log.e(TAG, "Неожиданная ошибка проверки хранилища", e)
            false
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
            
            Log.d(TAG, "✅ Глобальная тема применена: ${if (isDarkTheme) "Темная" else "Светлая"}")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка применения глобальной темы", e)
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
                Log.d(TAG, "✅ Выполнена первичная инициализация настроек")
            } else {
                Log.d(TAG, "ℹ️ Приложение уже инициализировано ранее")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка инициализации настроек", e)
        }
    }
    
    private fun initializeMaterialYou() {
        try {
            val prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)
            val useMaterialYou = prefs.getBoolean("material_you", false)
            
            if (useMaterialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Применяем динамические цвета ко всем активностям приложения
                DynamicColors.applyToActivitiesIfAvailable(this)
                Log.d(TAG, "✅ Material You включен глобально для всех активностей")
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                Log.d(TAG, "ℹ️ Material You недоступен на Android ${Build.VERSION.RELEASE}")
            } else {
                Log.d(TAG, "ℹ️ Material You отключен пользователем")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка инициализации Material You", e)
        }
    }
    
    private fun initializeAndroid15Compatibility() {
        try {
            Log.d(TAG, "🔥 Выполняем настройку для Android 15")
            
            // Специальные настройки для Android 15
            val prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)
            prefs.edit()
                .putBoolean("android_15_mode", true)
                .putBoolean("enhanced_boot_receiver", true)
                .putBoolean("aggressive_service_start", true)
                .apply()
                
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка настройки Android 15", e)
        }
    }
    
    private fun initializeAndroid14Compatibility() {
        try {
            Log.d(TAG, "🔄 Настройка совместимости с Android 14")
            
            val prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)
            prefs.edit()
                .putBoolean("android_14_mode", true)
                .putBoolean("foreground_service_special_use", true)
                .apply()
                
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка настройки Android 14", e)
        }
    }
}
