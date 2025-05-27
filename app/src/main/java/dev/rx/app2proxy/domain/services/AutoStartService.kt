package dev.rx.app2proxy

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class AutoStartService : Service() {
    
    companion object {
        private const val TAG = "AutoStartService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "auto_start_channel"
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AutoStartService создан")
        createNotificationChannel()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "=== AutoStartService запущен ===")
        
        // Создаём уведомление для foreground service
        val notification = createNotification("Восстановление правил iptables...")
        startForeground(NOTIFICATION_ID, notification)
        
        // Запускаем восстановление правил
        serviceScope.launch {
            try {
                restoreIptablesRules()
            } finally {
                // Останавливаем сервис после выполнения
                stopSelf()
            }
        }
        
        return START_NOT_STICKY // Не перезапускать автоматически
    }
    
    private suspend fun restoreIptablesRules() {
        try {
            Log.d(TAG, "Начинаем восстановление правил iptables")
            
            val prefs = getSharedPreferences("proxy_prefs", Context.MODE_PRIVATE)
            val selectedUids = prefs.getStringSet("selected_uids", emptySet()) ?: emptySet()
            
            if (selectedUids.isEmpty()) {
                Log.d(TAG, "Нет сохранённых правил для восстановления")
                updateNotification("Нет правил для восстановления")
                delay(3000)
                return
            }
            
            Log.d(TAG, "Найдено ${selectedUids.size} правил для восстановления")
            updateNotification("Найдено ${selectedUids.size} правил")
            
            // Даём системе время загрузиться
            for (i in 15 downTo 1) {
                updateNotification("Ожидание загрузки системы... $i сек")
                delay(1000)
            }
            
            // Применяем правила
            updateNotification("Применяем правила iptables...")
            
            val uidsString = selectedUids.joinToString(" ")
            IptablesService.applyRules(this@AutoStartService, uidsString)
            
            // Сохраняем информацию об успешном применении
            prefs.edit()
                .putLong("last_service_restore", System.currentTimeMillis())
                .putBoolean("service_restore_success", true)
                .apply()
            
            updateNotification("✅ Правила успешно восстановлены!")
            Log.d(TAG, "✅ Правила iptables успешно восстановлены через сервис")
            
            delay(5000) // Показываем уведомление 5 секунд
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка восстановления правил через сервис", e)
            updateNotification("❌ Ошибка восстановления правил")
            delay(5000)
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Автозапуск App2Proxy",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Уведомления о восстановлении правил iptables при загрузке"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("App2Proxy")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun updateNotification(text: String) {
        val notification = createNotification(text)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "AutoStartService уничтожен")
    }
}
