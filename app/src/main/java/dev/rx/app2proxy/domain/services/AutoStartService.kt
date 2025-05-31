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
        Log.d(TAG, "AutoStartService created")
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "=== AutoStartService started ===")
                
        // Start a coroutine to restore iptables rules
        serviceScope.launch {
            try {
                restoreIptablesRules()
            } finally {
                // Stopping the foreground service
                stopSelf()
            }
        }
        
        return START_NOT_STICKY 
    }
    
    private suspend fun restoreIptablesRules() {
        try {
            Log.d(TAG, "Starting restoration of iptables rules")
            
            val prefs = getSharedPreferences("proxy_prefs", Context.MODE_PRIVATE)
            val selectedUids = prefs.getStringSet("selected_uids", emptySet()) ?: emptySet()
            
            if (selectedUids.isEmpty()) {
                Log.d(TAG, "No saved rules for restoration")
                delay(3000)
                return
            }
            
            Log.d(TAG, "Found ${selectedUids.size} rules to restore")
                       
            val uidsString = selectedUids.joinToString(" ")
            IptablesService.applyRules(this@AutoStartService, uidsString)

            // Saving the fact that the service has successfully restored rules
            prefs.edit()
                .putLong("last_service_restore", System.currentTimeMillis())
                .putBoolean("service_restore_success", true)
                .apply()
            
            Log.d(TAG, "✅ Rules restored successfully via service")            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error restoring rules via service", e)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "=== AutoStartService destroyed ===")
    }
}
