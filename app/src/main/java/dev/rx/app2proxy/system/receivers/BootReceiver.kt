package dev.rx.app2proxy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "App2ProxyBootReceiver"
        private const val PREF_KEY_SERVICE_STARTED = "autostart_service_already_started"
        private const val PREF_KEY_BOOT_SESSION = "current_boot_session"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val timestamp = System.currentTimeMillis()
        val action = intent.action ?: "null"
        
        Log.d(TAG, "=== BootReceiver ACTIVATED ===")
        Log.d(TAG, "Action: $action")
        Log.d(TAG, "Android version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        Log.d(TAG, "Timestamp: $timestamp")
        
        // Checking if the action is relevant to our boot event handling
        if (!isRelevantBootEvent(action)) {
            Log.d(TAG, "Event $action is not relevant, ignoring")
            return
        }
        
        try {
            // Check if the user is unlocked
            if (!isUserUnlocked(context)) {
                Log.d(TAG, "⚠️ User is not unlocked yet, delaying processing")
                return
            }
            
            // Main logic to handle boot events
            handleBootEvent(context, action, timestamp)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Critical error in onReceive", e)
        }
    }
    
    private fun isRelevantBootEvent(action: String): Boolean {
        return when (action) {
            Intent.ACTION_BOOT_COMPLETED -> true
            Intent.ACTION_USER_PRESENT -> true
            Intent.ACTION_USER_UNLOCKED -> true 
            else -> false
        }
    }
    
    private fun handleBootEvent(context: Context, action: String, timestamp: Long) {
        val prefs = context.getSharedPreferences("proxy_prefs", Context.MODE_PRIVATE)
        
        // Getting the current boot time
        val currentBootTime = System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime()
        val lastBootSession = prefs.getLong(PREF_KEY_BOOT_SESSION, 0)
        val serviceAlreadyStarted = prefs.getBoolean(PREF_KEY_SERVICE_STARTED, false)
        
        // Check if this is a new boot session
        val isNewBootSession = Math.abs(currentBootTime - lastBootSession) > 30000
                
        Log.d(TAG, "Current boot time: $currentBootTime")
        Log.d(TAG, "Last boot session: $lastBootSession")
        Log.d(TAG, "Is new boot session: $isNewBootSession")
        Log.d(TAG, "Service already started: $serviceAlreadyStarted")
        
        // If this is a new boot session, reset the flags
        if (isNewBootSession) {
            Log.d(TAG, "🔄 New boot session detected, resetting flags")
            prefs.edit()
                .putLong(PREF_KEY_BOOT_SESSION, currentBootTime)
                .putBoolean(PREF_KEY_SERVICE_STARTED, false)
                .apply()
        }
        
        // Check if we should start the service
        val shouldStartService = when {
            isNewBootSession -> {
                Log.d(TAG, "✅ New boot session detected - can start service")
                true
            }
            !serviceAlreadyStarted && (action == Intent.ACTION_USER_PRESENT || action == Intent.ACTION_USER_UNLOCKED) -> {
                Log.d(TAG, "✅ User present or unlocked and service not started - can start service")
                true
            }
            else -> {
                Log.d(TAG, "❌ Service already started in this session or conditions not met")
                false
            }
        }
        
        if (!shouldStartService) {
            return
        }
        
        // Checking autostart preference
        val autostartEnabled = prefs.getBoolean("autostart", false)
        if (!autostartEnabled) {
            Log.d(TAG, "❌ Autostart is disabled in settings")
            return
        }
        
        // Checking for saved rules
        val selectedUids = prefs.getStringSet("selected_uids", emptySet()) ?: emptySet()
        if (selectedUids.isEmpty()) {
            Log.d(TAG, "❌ No saved rules for restoration")
            return
        }
        
        Log.d(TAG, "🚀 Starting AutoStartService")
        Log.d(TAG, "📋 Found ${selectedUids.size} rules to restore")

        // Saving the fact that the service has been started
        prefs.edit()
            .putBoolean(PREF_KEY_SERVICE_STARTED, true)
            .putLong("last_service_start_attempt", timestamp)
            .putString("last_boot_action", action)
            .apply()
        
        // Start the AutoStartService
        try {
            val serviceIntent = Intent(context, AutoStartService::class.java).apply {
                putExtra("event_type", action)
                putExtra("uid_count", selectedUids.size)
                putExtra("android_version", Build.VERSION.SDK_INT)
                putExtra("boot_timestamp", timestamp)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            
            Log.d(TAG, "✅ AutoStartService started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка запуска AutoStartService", e)
            
            // Resetting the service started flag to allow retry
            prefs.edit()
                .putBoolean(PREF_KEY_SERVICE_STARTED, false)
                .apply()
        }
    }
    
    private fun isUserUnlocked(context: Context): Boolean {
        return try {
            // Try to access SharedPreferences
            context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
            true
        } catch (e: IllegalStateException) {
            // User is not unlocked yet
            false
        } catch (e: Exception) {
            Log.d(TAG, "Unexpected error checking user unlock state: ${e.message}")
            false
        }
    }
 }
