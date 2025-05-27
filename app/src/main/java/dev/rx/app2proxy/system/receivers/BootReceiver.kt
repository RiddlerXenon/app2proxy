package dev.rx.app2proxy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.*
import java.io.File

class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "App2ProxyBootReceiver"
        private const val DEFAULT_PROXY_PORT = 12345
        private const val DEFAULT_DNS_PORT = 10853
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val timestamp = System.currentTimeMillis()
        val action = intent.action ?: "null"
        
        Log.d(TAG, "=== BootReceiver АКТИВИРОВАН ===")
        Log.d(TAG, "Action: $action")
        Log.d(TAG, "Android версия: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        Log.d(TAG, "Производитель: ${Build.MANUFACTURER}")
        Log.d(TAG, "Модель: ${Build.MODEL}")
        Log.d(TAG, "Время: $timestamp")
        
        // Проверяем что это действительно событие загрузки
        if (!isBootEvent(action)) {
            Log.d(TAG, "Событие $action не является событием загрузки, игнорируем")
            return
        }
        
        // Игнорируем LOCKED_BOOT_COMPLETED для Android 15, работаем только с BOOT_COMPLETED
        if (action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            Log.d(TAG, "⏭️ Пропускаем LOCKED_BOOT_COMPLETED, ждем BOOT_COMPLETED")
            return
        }
        
        try {
            // Проверяем доступность SharedPreferences
            if (!isUserUnlocked(context)) {
                Log.w(TAG, "⚠️ Пользователь еще не разблокирован, откладываем обработку")
                return
            }
            
            // Немедленно сохраняем факт активации
            val prefs = context.getSharedPreferences("proxy_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean("boot_receiver_activated", true)
                .putLong("last_boot_receiver_time", timestamp)
                .putString("last_boot_action", action)
                .putString("boot_android_version", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                .putString("boot_device_info", "${Build.MANUFACTURER} ${Build.MODEL}")
                .apply()
            
            writeToLogFile(context, "BOOT_EVENT_RECEIVED: action=$action, android=${Build.VERSION.SDK_INT}, time=$timestamp")
            
            // Специальная обработка для Android 15
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                Log.d(TAG, "🔥 Android 15+ обнаружен, используем специальную логику")
                handleAndroid15BootEvent(context, action, timestamp)
            } else {
                Log.d(TAG, "📱 Используем стандартную логику для Android ${Build.VERSION.SDK_INT}")
                handleStandardBootEvent(context, action, timestamp)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Критическая ошибка в onReceive", e)
            writeToLogFile(context, "BOOT_RECEIVER_ERROR: ${e.message}")
        }
    }
    
    private fun isUserUnlocked(context: Context): Boolean {
        return try {
            // Попытка получить доступ к SharedPreferences
            context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
            true
        } catch (e: IllegalStateException) {
            // Пользователь еще не разблокирован
            false
        } catch (e: Exception) {
            Log.e(TAG, "Неожиданная ошибка проверки разблокировки", e)
            false
        }
    }
    
    private fun isBootEvent(action: String): Boolean {
        return when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_USER_PRESENT,
            Intent.ACTION_USER_UNLOCKED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
            "miui.intent.action.MIUI_APPLICATION_START" -> true
            else -> false
        }
    }
    
    private fun handleAndroid15BootEvent(context: Context, action: String, timestamp: Long) {
        try {
            Log.d(TAG, "🚀 Обработка события для Android 15+")
            
            val prefs = context.getSharedPreferences("proxy_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean("android_15_boot_handled", true)
                .apply()
            
            writeToLogFile(context, "ANDROID_15_BOOT_EVENT: action=$action, time=$timestamp")
            
            // Проверяем разрешения
            if (!checkBootPermissions(context)) {
                Log.e(TAG, "❌ Недостаточно разрешений для Android 15")
                writeToLogFile(context, "ANDROID_15_PERMISSION_ERROR")
                return
            }
            
            // Запускаем восстановление с учетом ограничений Android 15
            startAndroid15Restoration(context, action)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка обработки Android 15", e)
            writeToLogFile(context, "ANDROID_15_ERROR: ${e.message}")
        }
    }
    
    private fun handleStandardBootEvent(context: Context, action: String, timestamp: Long) {
        try {
            Log.d(TAG, "📱 Стандартная обработка события")
            
            writeToLogFile(context, "STANDARD_BOOT_EVENT: action=$action, time=$timestamp")
            
            startStandardRestoration(context, action)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка стандартной обработки", e)
            writeToLogFile(context, "STANDARD_ERROR: ${e.message}")
        }
    }
    
    private fun checkBootPermissions(context: Context): Boolean {
        try {
            val packageManager = context.packageManager
            val packageName = context.packageName
            
            val hasBootPermission = packageManager.checkPermission(
                android.Manifest.permission.RECEIVE_BOOT_COMPLETED,
                packageName
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            val hasForegroundService = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                packageManager.checkPermission(
                    android.Manifest.permission.FOREGROUND_SERVICE_SPECIAL_USE,
                    packageName
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true
            
            Log.d(TAG, "Разрешение BOOT_COMPLETED: $hasBootPermission")
            Log.d(TAG, "Разрешение FOREGROUND_SERVICE_SPECIAL_USE: $hasForegroundService")
            
            return hasBootPermission && hasForegroundService
            
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка проверки разрешений", e)
            return false
        }
    }
    
    private fun startAndroid15Restoration(context: Context, action: String) {
        Log.d(TAG, "🔥 Запуск восстановления для Android 15")
        
        val prefs = context.getSharedPreferences("proxy_prefs", Context.MODE_PRIVATE)
        val selectedUids = prefs.getStringSet("selected_uids", emptySet()) ?: emptySet()
        
        if (selectedUids.isEmpty()) {
            Log.d(TAG, "❌ Нет правил для восстановления")
            writeToLogFile(context, "ANDROID_15_NO_RULES")
            return
        }
        
        Log.d(TAG, "📋 Найдено ${selectedUids.size} правил для Android 15")
        writeToLogFile(context, "ANDROID_15_RULES_FOUND: count=${selectedUids.size}, uids=${selectedUids.joinToString(",")}")
        
        // Метод 1: Попытка запуска foreground service
        var serviceStarted = false
        try {
            val serviceIntent = Intent(context, AutoStartService::class.java).apply {
                putExtra("event_type", "ANDROID_15_$action")
                putExtra("uid_count", selectedUids.size)
                putExtra("android_version", Build.VERSION.SDK_INT)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            
            serviceStarted = true
            Log.d(TAG, "✅ Service запущен для Android 15")
            writeToLogFile(context, "ANDROID_15_SERVICE_STARTED")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка запуска service для Android 15", e)
            writeToLogFile(context, "ANDROID_15_SERVICE_ERROR: ${e.message}")
        }
        
        // Метод 2: Агрессивный fallback для Android 15
        if (!serviceStarted || action == Intent.ACTION_USER_PRESENT) {
            startAndroid15Fallback(context, selectedUids)
        }
    }
    
    private fun startAndroid15Fallback(context: Context, selectedUids: Set<String>) {
        Log.d(TAG, "🔧 Android 15 fallback восстановление")
        writeToLogFile(context, "ANDROID_15_FALLBACK_STARTED")
        
        // Используем короткую задержку для Android 15
        Handler(Looper.getMainLooper()).postDelayed({
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d(TAG, "🎯 Прямое применение правил для Android 15")
                    
                    val uidsString = selectedUids.joinToString(" ")
                    val result = applyRulesDirectlyAndroid15(context, uidsString)
                    
                    Log.d(TAG, "📝 Результат Android 15: $result")
                    writeToLogFile(context, "ANDROID_15_RESULT: $result")
                    
                    // Сохраняем результат
                    val prefs = context.getSharedPreferences("proxy_prefs", Context.MODE_PRIVATE)
                    prefs.edit()
                        .putLong("last_android_15_restore", System.currentTimeMillis())
                        .putString("android_15_result", result)
                        .putBoolean("android_15_success", result.contains("Applied") || result.contains("применены"))
                        .apply()
                    
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Ошибка Android 15 fallback", e)
                    writeToLogFile(context, "ANDROID_15_FALLBACK_ERROR: ${e.message}")
                }
            }
        }, 3000) // Уменьшена задержка до 3 секунд
    }
    
    private fun startStandardRestoration(context: Context, action: String) {
        Log.d(TAG, "📱 Стандартное восстановление")
        
        val prefs = context.getSharedPreferences("proxy_prefs", Context.MODE_PRIVATE)
        val selectedUids = prefs.getStringSet("selected_uids", emptySet()) ?: emptySet()
        
        if (selectedUids.isEmpty()) {
            Log.d(TAG, "❌ Нет правил для восстановления")
            writeToLogFile(context, "STANDARD_NO_RULES")
            return
        }
        
        Log.d(TAG, "📋 Найдено ${selectedUids.size} правил для стандартного восстановления")
        writeToLogFile(context, "STANDARD_RULES_FOUND: count=${selectedUids.size}, uids=${selectedUids.joinToString(",")}")
        
        // Запуск через service
        try {
            val serviceIntent = Intent(context, AutoStartService::class.java).apply {
                putExtra("event_type", action)
                putExtra("uid_count", selectedUids.size)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            
            Log.d(TAG, "✅ Стандартный service запущен")
            writeToLogFile(context, "STANDARD_SERVICE_STARTED")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка запуска стандартного service", e)
            writeToLogFile(context, "STANDARD_SERVICE_ERROR: ${e.message}")
            
            // Fallback
            Handler(Looper.getMainLooper()).postDelayed({
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val uidsString = selectedUids.joinToString(" ")
                        val result = applyRulesDirectlyStandard(context, uidsString)
                        writeToLogFile(context, "STANDARD_FALLBACK_RESULT: $result")
                    } catch (fallbackError: Exception) {
                        Log.e(TAG, "❌ Ошибка стандартного fallback", fallbackError)
                        writeToLogFile(context, "STANDARD_FALLBACK_ERROR: ${fallbackError.message}")
                    }
                }
            }, 8000)
        }
    }
    
    private suspend fun applyRulesDirectlyAndroid15(context: Context, uids: String): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔥 Применяем правила напрямую для Android 15")
                
                val prefs = context.getSharedPreferences("proxy_prefs", Context.MODE_PRIVATE)
                val proxyPort = prefs.getInt("proxy_port", DEFAULT_PROXY_PORT)
                val dnsPort = prefs.getInt("dns_port", DEFAULT_DNS_PORT)
                
                val script = buildAndroid15Script(uids, proxyPort, dnsPort)
                executeRootCommandAndroid15(script)
                
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка прямого применения Android 15", e)
                "Android 15 Error: ${e.message}"
            }
        }
    }
    
    private suspend fun applyRulesDirectlyStandard(context: Context, uids: String): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "📱 Применяем правила напрямую стандартно")
                
                val prefs = context.getSharedPreferences("proxy_prefs", Context.MODE_PRIVATE)
                val proxyPort = prefs.getInt("proxy_port", DEFAULT_PROXY_PORT)
                val dnsPort = prefs.getInt("dns_port", DEFAULT_DNS_PORT)
                
                val script = buildStandardScript(uids, proxyPort, dnsPort)
                executeRootCommandStandard(script)
                
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка стандартного прямого применения", e)
                "Standard Error: ${e.message}"
            }
        }
    }
    
    private fun buildAndroid15Script(uids: String, proxyPort: Int, dnsPort: Int): String {
        return """
            #!/system/bin/sh
            
            echo "=== App2Proxy Android 15 Boot Script ==="
            echo "Время: $(date)"
            echo "Android версия: $(getprop ro.build.version.release)"
            echo "API уровень: $(getprop ro.build.version.sdk)"
            echo "UID для восстановления: $uids"
            echo "Порт прокси: $proxyPort"
            echo "Порт DNS: $dnsPort"
            
            # Константы
            UIDS="$uids"
            PORT=$proxyPort
            DNS_PORT=$dnsPort
            
            # Проверяем root доступ
            if [ "$(id -u)" != "0" ]; then
                echo "ОШИБКА: Нет root доступа"
                exit 1
            fi
            
            echo "Root доступ подтвержден"
            
            # Проверяем доступность iptables
            if ! command -v iptables >/dev/null 2>&1; then
                echo "КРИТИЧЕСКАЯ ОШИБКА: iptables не найден"
                exit 1
            fi
            
            echo "iptables найден: $(which iptables)"
            
            # Специальная обработка для Android 15
            echo "Применяем оптимизированную логику для Android 15"
            
            SUCCESS_COUNT=0
            ERROR_COUNT=0
            
            for UID in ${'$'}UIDS; do
                echo "--- Android 15: Обработка UID ${'$'}UID ---"
                
                # Очистка существующих правил (более агрессивная)
                echo "Очистка существующих правил для UID ${'$'}UID"
                
                # TCP правила - несколько попыток
                for attempt in 1 2 3; do
                    if iptables -t nat -C OUTPUT -p tcp -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}PORT 2>/dev/null; then
                        if iptables -t nat -D OUTPUT -p tcp -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}PORT; then
                            echo "✅ Удалено старое TCP правило (попытка ${'$'}attempt)"
                        else
                            echo "❌ Ошибка удаления TCP правила (попытка ${'$'}attempt)"
                        fi
                    else
                        break
                    fi
                done
                
                # DNS правила - несколько попыток
                for attempt in 1 2 3; do
                    if iptables -t nat -C OUTPUT -p udp --dport 53 -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}DNS_PORT 2>/dev/null; then
                        if iptables -t nat -D OUTPUT -p udp --dport 53 -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}DNS_PORT; then
                            echo "✅ Удалено старое DNS правило (попытка ${'$'}attempt)"
                        else
                            echo "❌ Ошибка удаления DNS правила (попытка ${'$'}attempt)"
                        fi
                    else
                        break
                    fi
                done
                
                # Добавление новых правил с проверкой
                echo "Добавляем новые правила для UID ${'$'}UID"
                
                # TCP правило
                if iptables -t nat -A OUTPUT -p tcp -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}PORT 2>/dev/null; then
                    echo "✅ TCP правило добавлено для UID ${'$'}UID (порт ${'$'}PORT)"
                    SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
                else
                    echo "❌ Ошибка добавления TCP правила для UID ${'$'}UID"
                    ERROR_COUNT=$((ERROR_COUNT + 1))
                fi
                
                # DNS правило
                if iptables -t nat -A OUTPUT -p udp --dport 53 -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}DNS_PORT 2>/dev/null; then
                    echo "✅ DNS правило добавлено для UID ${'$'}UID (порт ${'$'}DNS_PORT)"
                    SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
                else
                    echo "❌ Ошибка добавления DNS правила для UID ${'$'}UID"
                    ERROR_COUNT=$((ERROR_COUNT + 1))
                fi
                
                echo "--- Завершена обработка UID ${'$'}UID ---"
            done
            
            echo ""
            echo "=== ANDROID 15 ОТЧЕТ ==="
            echo "Успешных операций: ${'$'}SUCCESS_COUNT"
            echo "Ошибок: ${'$'}ERROR_COUNT"
            echo "Обработанных UID: $(echo ${'$'}UIDS | wc -w)"
            echo "Используемые порты: прокси=${'$'}PORT, DNS=${'$'}DNS_PORT"
            
            # Проверка результата
            echo "=== ПРОВЕРКА ПРАВИЛ ==="
            FOUND_RULES=$(iptables -t nat -L OUTPUT -n | grep -E "(${'$'}PORT|${'$'}DNS_PORT)" | wc -l)
            echo "Найдено активных правил: ${'$'}FOUND_RULES"
            
            if [ ${'$'}FOUND_RULES -gt 0 ]; then
                echo "✅ ANDROID 15: ПРАВИЛА УСПЕШНО ПРИМЕНЕНЫ"
                echo "Applied rules for UIDs: ${'$'}UIDS with proxy port ${'$'}PORT and DNS port ${'$'}DNS_PORT"
            else
                echo "⚠️ ANDROID 15: ПРАВИЛА НЕ ОБНАРУЖЕНЫ"
                echo "No rules found for UIDs: ${'$'}UIDS"
            fi
            
            echo "=== КОНЕЦ ANDROID 15 СКРИПТА ==="
        """.trimIndent()
    }
    
    private fun buildStandardScript(uids: String, proxyPort: Int, dnsPort: Int): String {
        return """
            #!/system/bin/sh
            
            echo "=== App2Proxy Standard Boot Script ==="
            echo "Время: $(date)"
            echo "UID для восстановления: $uids"
            echo "Порт прокси: $proxyPort"
            echo "Порт DNS: $dnsPort"
            
            UIDS="$uids"
            PORT=$proxyPort
            DNS_PORT=$dnsPort
            
            if ! command -v iptables >/dev/null 2>&1; then
                echo "ОШИБКА: iptables не найден"
                exit 1
            fi
            
            for UID in ${'$'}UIDS; do
                echo "Обработка UID: ${'$'}UID"
                
                # Очистка
                while iptables -t nat -C OUTPUT -p tcp -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}PORT 2>/dev/null; do
                    iptables -t nat -D OUTPUT -p tcp -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}PORT
                done
                
                while iptables -t nat -C OUTPUT -p udp --dport 53 -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}DNS_PORT 2>/dev/null; do
                    iptables -t nat -D OUTPUT -p udp --dport 53 -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}DNS_PORT
                done
                
                # Добавление
                iptables -t nat -A OUTPUT -p tcp -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}PORT
                iptables -t nat -A OUTPUT -p udp --dport 53 -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}DNS_PORT
                
                echo "Правила добавлены для UID ${'$'}UID (прокси: ${'$'}PORT, DNS: ${'$'}DNS_PORT)"
            done
            
            echo "Applied rules for UIDs: ${'$'}UIDS with proxy port ${'$'}PORT and DNS port ${'$'}DNS_PORT"
        """.trimIndent()
    }
    
    private fun executeRootCommandAndroid15(script: String): String {
        return try {
            Log.d(TAG, "🔥 Выполнение команды для Android 15")
            
            val process = Runtime.getRuntime().exec("su")
            val writer = process.outputStream.bufferedWriter()
            
            writer.write(script)
            writer.write("\nexit\n")
            writer.flush()
            writer.close()
            
            val exitCode = process.waitFor()
            val output = process.inputStream.bufferedReader().readText()
            val errorOutput = process.errorStream.bufferedReader().readText()
            
            Log.d(TAG, "Android 15 - Код выхода: $exitCode")
            Log.d(TAG, "Android 15 - Вывод: $output")
            
            if (errorOutput.isNotEmpty()) {
                Log.w(TAG, "Android 15 - Ошибки: $errorOutput")
            }
            
            if (exitCode == 0) output else "Android 15 Error (код $exitCode): $errorOutput"
            
        } catch (e: Exception) {
            val error = "Android 15 критическая ошибка: ${e.message}"
            Log.e(TAG, error, e)
            error
        }
    }
    
    private fun executeRootCommandStandard(script: String): String {
        return try {
            Log.d(TAG, "📱 Стандартное выполнение команды")
            
            val process = Runtime.getRuntime().exec("su")
            val writer = process.outputStream.bufferedWriter()
            
            writer.write(script)
            writer.write("\nexit\n")
            writer.flush()
            writer.close()
            
            val exitCode = process.waitFor()
            val output = process.inputStream.bufferedReader().readText()
            
            Log.d(TAG, "Стандартный код выхода: $exitCode")
            Log.d(TAG, "Стандартный вывод: $output")
            
            if (exitCode == 0) output else "Standard Error (код $exitCode)"
            
        } catch (e: Exception) {
            val error = "Стандартная ошибка: ${e.message}"
            Log.e(TAG, error, e)
            error
        }
    }
    
    private fun writeToLogFile(context: Context, message: String) {
        try {
            val logFile = File(context.filesDir, "boot_receiver_log.txt")
            
            // Создаем директорию если не существует
            if (!logFile.parentFile?.exists()!!) {
                logFile.parentFile?.mkdirs()
            }
            
            val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
            
            java.io.FileWriter(logFile, true).use { writer ->
                writer.appendLine("[$timestamp] $message")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка записи в лог файл", e)
        }
    }
}
