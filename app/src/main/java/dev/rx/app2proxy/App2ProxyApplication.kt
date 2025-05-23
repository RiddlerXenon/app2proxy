package dev.rx.app2proxy

import android.app.Application
import android.os.Build
import android.util.Log
import com.google.android.material.color.DynamicColors

class App2ProxyApplication : Application() {
    
    companion object {
        private const val TAG = "App2ProxyApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            Log.d(TAG, "=== App2ProxyApplication запущен ===")
            Log.d(TAG, "Android версия: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            Log.d(TAG, "Производитель: ${Build.MANUFACTURER}")
            Log.d(TAG, "Модель: ${Build.MODEL}")
            
            // Инициализация настроек
            initializePreferences()
            
            // Применение Material You для Android 12+
            initializeMaterialYou()
            
            // Специальные настройки для разных версий Android
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM -> {
                    Log.d(TAG, "Обнаружен Android 15, применяем специальные настройки")
                    initializeAndroid15Compatibility()
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                    Log.d(TAG, "Обнаружен Android 14, применяем настройки совместимости")
                    initializeAndroid14Compatibility()
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    Log.d(TAG, "Настройка совместимости с современными версиями Android")
                    initializeModernAndroidCompatibility()
                }
                else -> {
                    Log.d(TAG, "Настройка совместимости с более старыми версиями Android")
                    initializeLegacyAndroidCompatibility()
                }
            }
            
            // Информация о версии приложения
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName ?: "unknown"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toString()
            }
            
            Log.d(TAG, "Версия приложения: $versionName ($versionCode)")
            
        } catch (e: Exception) {
            Log.e(TAG, "Критическая ошибка инициализации приложения", e)
            // Не бросаем исключение, чтобы избежать вылетов при запуске
        }
    }
    
    private fun initializePreferences() {
        try {
            val prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)
            
            // Проверяем, нужна ли первичная инициализация
            if (!prefs.contains("app_initialized")) {
                prefs.edit()
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
    
    private fun initializeModernAndroidCompatibility() {
        try {
            Log.d(TAG, "Настройка совместимости с современными версиями Android")
            
            val prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)
            prefs.edit()
                .putBoolean("modern_android_mode", true)
                .apply()
                
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка настройки современных версий", e)
        }
    }
    
    private fun initializeLegacyAndroidCompatibility() {
        try {
            Log.d(TAG, "Настройка совместимости с устаревшими версиями Android")
            
            val prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)
            prefs.edit()
                .putBoolean("legacy_android_mode", true)
                .apply()
                
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка настройки устаревших версий", e)
        }
    }
}
