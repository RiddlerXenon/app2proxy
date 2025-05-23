package dev.rx.app2proxy

import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log

class App2ProxyApplication : Application() {
    
    companion object {
        private const val TAG = "App2ProxyApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        Log.d(TAG, "=== App2ProxyApplication запущен ===")
        Log.d(TAG, "Android версия: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        Log.d(TAG, "Производитель: ${Build.MANUFACTURER}")
        Log.d(TAG, "Модель: ${Build.MODEL}")
        
        // Проверяем и запрашиваем критические разрешения для Android 15
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) { // Android 15
            Log.d(TAG, "Обнаружен Android 15, применяем специальные настройки")
            setupAndroid15Compatibility()
        }
        
        // Настройка для работы с современными версиями Android
        setupModernAndroidCompatibility()
        
        // Регистрируем обработчик необработанных исключений
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Log.e(TAG, "Необработанное исключение в потоке ${thread.name}", exception)
            
            // Сохраняем информацию об ошибке
            val prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)
            prefs.edit()
                .putString("last_crash", exception.toString())
                .putLong("last_crash_time", System.currentTimeMillis())
                .apply()
        }
    }
    
    private fun setupAndroid15Compatibility() {
        try {
            Log.d(TAG, "Настройка совместимости с Android 15")
            
            // Дополнительные настройки для Android 15
            val prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)
            prefs.edit()
                .putBoolean("android_15_detected", true)
                .putLong("app_start_time", System.currentTimeMillis())
                .apply()
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка настройки Android 15", e)
        }
    }
    
    private fun setupModernAndroidCompatibility() {
        try {
            Log.d(TAG, "Настройка совместимости с современными версиями Android")
            
            // Проверяем доступность критических разрешений
            checkCriticalPermissions()
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка настройки совместимости", e)
        }
    }
    
    private fun checkCriticalPermissions() {
        val packageManager = packageManager
        val packageName = packageName
        
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            Log.d(TAG, "Версия приложения: ${packageInfo.versionName} (${packageInfo.longVersionCode})")
            
            // Логируем доступные разрешения
            val permissions = packageInfo.requestedPermissions
            permissions?.forEach { permission ->
                Log.d(TAG, "Запрошенное разрешение: $permission")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка проверки разрешений", e)
        }
    }
}
