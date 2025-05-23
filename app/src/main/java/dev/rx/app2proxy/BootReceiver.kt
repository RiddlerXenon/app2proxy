package dev.rx.app2proxy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Получен intent: ${intent.action}")
        
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED -> {
                Log.d(TAG, "Система загружена, запускаем восстановление правил iptables")
                startAutoStartService(context)
            }
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.d(TAG, "Приложение обновлено, запускаем восстановление правил iptables")
                startAutoStartService(context)
            }
            Intent.ACTION_PACKAGE_REPLACED -> {
                intent.data?.let { data ->
                    if (data.schemeSpecificPart == context.packageName) {
                        Log.d(TAG, "Наше приложение обновлено, запускаем восстановление правил iptables")
                        startAutoStartService(context)
                    }
                }
            }
            Intent.ACTION_USER_PRESENT -> {
                Log.d(TAG, "Пользователь разблокировал устройство, проверяем правила iptables")
                startAutoStartService(context)
            }
        }
    }
    
    private fun startAutoStartService(context: Context) {
        try {
            val serviceIntent = Intent(context, AutoStartService::class.java)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            
            Log.d(TAG, "Сервис автозагрузки запущен")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при запуске сервиса автозагрузки", e)
            
            // Fallback: применяем правила напрямую
            try {
                IptablesService.applyRulesFromPrefs(context)
            } catch (fallbackError: Exception) {
                Log.e(TAG, "Ошибка при применении правил через fallback", fallbackError)
            }
        }
    }
}
