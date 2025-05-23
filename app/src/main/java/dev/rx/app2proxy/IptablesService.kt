package dev.rx.app2proxy

import android.content.Context
import android.util.Log
import java.io.DataOutputStream

object IptablesService {
    private const val XRAY_PORT = 12345
    private const val XRAY_DNS_PORT = 10853
    private const val TAG = "IptablesService"

    fun applyRulesFromPrefs(context: Context) {
        try {
            val prefs = context.getSharedPreferences("proxy_prefs", Context.MODE_PRIVATE)
            val uids = prefs.getStringSet("selected_uids", emptySet())?.joinToString(" ") ?: ""
            
            if (uids.isNotEmpty()) {
                Log.d(TAG, "Применяем правила для UID: $uids")
                applyRules(uids)
            } else {
                Log.d(TAG, "Нет сохранённых UID для применения правил")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при применении правил из настроек", e)
        }
    }

    fun applyRules(uids: String) {
        if (uids.trim().isEmpty()) {
            Log.w(TAG, "Пустой список UID для применения правил")
            return
        }
        
        try {
            Log.d(TAG, "Применяем правила iptables для UID: $uids")
            val script = buildScript(uids)
            val result = runAsRoot(script)
            Log.d(TAG, "Результат применения правил: $result")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при применении правил iptables", e)
        }
    }

    fun clearRules(uids: String) {
        if (uids.trim().isEmpty()) {
            Log.w(TAG, "Пустой список UID для очистки правил")
            return
        }

        try {
            Log.d(TAG, "Очищаем правила iptables для UID: $uids")
            val script = buildClearScript(uids)
            val result = runAsRoot(script)
            Log.d(TAG, "Результат очистки правил: $result")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при очистке правил iptables", e)
        }
    }

    private fun buildScript(uids: String): String {
        return """
            #!/system/bin/sh
            
            # Переменные
            UIDS="$uids"
            PORT=$XRAY_PORT
            DNS_PORT=$XRAY_DNS_PORT
            
            echo "Применение правил iptables для UID: ${'$'}UIDS"
            
            # Очистка существующих правил для этих UID
            for UID in ${'$'}UIDS; do
              echo "Очистка существующих правил для UID: ${'$'}UID"
              while iptables -t nat -C OUTPUT -p tcp -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}PORT 2>/dev/null; do
                iptables -t nat -D OUTPUT -p tcp -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}PORT
              done
              while iptables -t nat -C OUTPUT -p udp --dport 53 -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}DNS_PORT 2>/dev/null; do
                iptables -t nat -D OUTPUT -p udp --dport 53 -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}DNS_PORT
              done
            done

            # Добавление новых правил
            for UID in ${'$'}UIDS; do
              echo "Добавление правил для UID: ${'$'}UID"
              # TCP трафик перенаправляется на прокси
              iptables -t nat -A OUTPUT -p tcp -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}PORT
              # UDP DNS запросы перенаправляются на DNS сервер
              iptables -t nat -A OUTPUT -p udp --dport 53 -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}DNS_PORT
            done

            echo "Правила применены для UID: ${'$'}UIDS"
            
            # Показываем текущие правила
            echo "Текущие правила NAT OUTPUT:"
            iptables -t nat -L OUTPUT -n --line-numbers | grep -E "(REDIRECT|${'$'}PORT|${'$'}DNS_PORT)"
        """.trimIndent()
    }

    private fun buildClearScript(uids: String): String {
        return """
            #!/system/bin/sh
            
            # Переменные
            UIDS="$uids"
            PORT=$XRAY_PORT
            DNS_PORT=$XRAY_DNS_PORT
            
            echo "Очистка правил iptables для UID: ${'$'}UIDS"
            
            for UID in ${'$'}UIDS; do
              echo "Удаление правил для UID: ${'$'}UID"
              # Удаляем все TCP правила для данного UID
              while iptables -t nat -C OUTPUT -p tcp -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}PORT 2>/dev/null; do
                iptables -t nat -D OUTPUT -p tcp -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}PORT
              done
              # Удаляем все UDP DNS правила для данного UID
              while iptables -t nat -C OUTPUT -p udp --dport 53 -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}DNS_PORT 2>/dev/null; do
                iptables -t nat -D OUTPUT -p udp --dport 53 -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}DNS_PORT
              done
            done
            
            echo "Правила очищены для UID: ${'$'}UIDS"
            
            # Показываем оставшиеся правила
            echo "Оставшиеся правила NAT OUTPUT:"
            iptables -t nat -L OUTPUT -n --line-numbers | grep -E "(REDIRECT|${'$'}PORT|${'$'}DNS_PORT)" || echo "Правил не найдено"
        """.trimIndent()
    }

    private fun runAsRoot(script: String): String {
        return try {
            Log.d(TAG, "Выполнение скрипта с правами root")
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            
            // Записываем скрипт
            os.writeBytes(script)
            os.writeBytes("\nexit\n")
            os.flush()
            
            // Ждём завершения
            val exitCode = process.waitFor()
            
            // Читаем результат
            val output = process.inputStream.bufferedReader().readText()
            val errorOutput = process.errorStream.bufferedReader().readText()
            
            Log.d(TAG, "Код завершения: $exitCode")
            Log.d(TAG, "Вывод: $output")
            if (errorOutput.isNotEmpty()) {
                Log.w(TAG, "Ошибки: $errorOutput")
            }
            
            if (exitCode == 0) output else "Ошибка выполнения (код: $exitCode): $errorOutput"
        } catch (e: Exception) {
            val errorMessage = "Ошибка выполнения команды: ${e.localizedMessage}"
            Log.e(TAG, errorMessage, e)
            errorMessage
        }
    }
}
