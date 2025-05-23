package dev.rx.app2proxy

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*

class AutoStartService : Service() {
    
    companion object {
        private const val TAG = "AutoStartService"
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Сервис автозагрузки запущен")
        
        // Запускаем применение правил в корутине
        serviceScope.launch {
            applyRulesWithRetry()
        }
        
        // Возвращаем START_STICKY для автоматического перезапуска сервиса
        return START_STICKY
    }
    
    private suspend fun applyRulesWithRetry() {
        var attempts = 0
        val maxAttempts = 3
        
        while (attempts < maxAttempts) {
            try {
                attempts++
                Log.d(TAG, "Попытка $attempts применить правила iptables")
                
                // Задержка для полной загрузки системы
                delay(5000L * attempts) // Увеличиваем задержку с каждой попыткой
                
                val prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)
                val selectedUids = prefs.getStringSet("selected_uids", emptySet()) ?: emptySet()
                
                if (selectedUids.isNotEmpty()) {
                    IptablesService.applyRulesFromPrefs(this@AutoStartService)
                    Log.d(TAG, "Правила iptables успешно применены для ${selectedUids.size} приложений")
                    break // Успешно применили, выходим из цикла
                } else {
                    Log.d(TAG, "Нет сохранённых правил для применения")
                    break
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при применении правил (попытка $attempts)", e)
                if (attempts >= maxAttempts) {
                    Log.e(TAG, "Не удалось применить правила после $maxAttempts попыток")
                }
            }
        }
        
        // Останавливаем сервис после выполнения задачи
        stopSelf()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "Сервис автозагрузки остановлен")
    }
}
