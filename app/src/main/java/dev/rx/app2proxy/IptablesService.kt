package dev.rx.app2proxy

import android.content.Context
import android.util.Log
import java.io.DataOutputStream

object IptablesService {
    private const val DEFAULT_XRAY_PORT = 12345
    private const val DEFAULT_XRAY_DNS_PORT = 10853
    private const val TAG = "IptablesService"

    fun applyRulesFromPrefs(context: Context) {
        try {
            val prefs = context.getSharedPreferences("proxy_prefs", Context.MODE_PRIVATE)
            val uids = prefs.getStringSet("selected_uids", emptySet())?.joinToString(" ") ?: ""
            
            if (uids.isNotEmpty()) {
                Log.d(TAG, "Применяем правила для UID: $uids")
                applyRules(context, uids)
            } else {
                Log.d(TAG, "Нет сохранённых UID для применения правил")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при применении правил из настроек", e)
        }
    }

    fun applyRules(context: Context, uids: String) {
        if (uids.trim().isEmpty()) {
            Log.w(TAG, "Пустой список UID для применения правил")
            return
        }
        
        try {
            val prefs = context.getSharedPreferences("proxy_prefs", Context.MODE_PRIVATE)
            val proxyPort = prefs.getInt("proxy_port", DEFAULT_XRAY_PORT)
            val dnsPort = prefs.getInt("dns_port", DEFAULT_XRAY_DNS_PORT)
            
            Log.d(TAG, "Применяем правила iptables для UID: $uids, прокси порт: $proxyPort, DNS порт: $dnsPort")
            val script = buildScript(uids, proxyPort, dnsPort)
            val result = runAsRoot(script)
            Log.d(TAG, "Результат применения правил: $result")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при применении правил iptables", e)
        }
    }

    fun clearRules(context: Context, uids: String) {
        if (uids.trim().isEmpty()) {
            Log.w(TAG, "Пустой список UID для очистки правил")
            return
        }

        try {
            val prefs = context.getSharedPreferences("proxy_prefs", Context.MODE_PRIVATE)
            val proxyPort = prefs.getInt("proxy_port", DEFAULT_XRAY_PORT)
            val dnsPort = prefs.getInt("dns_port", DEFAULT_XRAY_DNS_PORT)
            
            Log.d(TAG, "Очищаем правила iptables для UID: $uids, прокси порт: $proxyPort, DNS порт: $dnsPort")
            val script = buildClearScript(uids, proxyPort, dnsPort)
            val result = runAsRoot(script)
            Log.d(TAG, "Результат очистки правил: $result")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при очистке правил iptables", e)
        }
    }

    /**
     * Универсальная очистка всех правил для указанных UID независимо от портов
     * Используется при изменении портов в настройках
     */
    fun clearAllRulesForUids(context: Context, uids: String) {
        if (uids.trim().isEmpty()) {
            Log.w(TAG, "Пустой список UID для универсальной очистки правил")
            return
        }

        try {
            Log.d(TAG, "Универсальная очистка всех правил iptables для UID: $uids")
            val script = buildUniversalClearScript(uids)
            val result = runAsRoot(script)
            Log.d(TAG, "Результат универсальной очистки правил: $result")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при универсальной очистке правил iptables", e)
        }
    }

    /**
     * Очистка правил со старыми портами при изменении настроек
     */
    fun clearRulesWithOldPorts(context: Context, uids: String, oldProxyPort: Int, oldDnsPort: Int) {
        if (uids.trim().isEmpty()) {
            Log.w(TAG, "Пустой список UID для очистки старых правил")
            return
        }

        try {
            Log.d(TAG, "Очищаем старые правила iptables для UID: $uids со старыми портами: прокси=$oldProxyPort, DNS=$oldDnsPort")
            val script = buildClearScript(uids, oldProxyPort, oldDnsPort)
            val result = runAsRoot(script)
            Log.d(TAG, "Результат очистки старых правил: $result")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при очистке старых правил iptables", e)
        }
    }

    private fun buildScript(uids: String, proxyPort: Int, dnsPort: Int): String {
        return """
            #!/system/bin/sh
            
            # Переменные
            UIDS="$uids"
            PORT=$proxyPort
            DNS_PORT=$dnsPort
            
            echo "Применение правил iptables для UID: ${'$'}UIDS"
            echo "Порт прокси: ${'$'}PORT, Порт DNS: ${'$'}DNS_PORT"
            
            # Очистка существующих правил для этих UID (универсальная очистка)
            for UID in ${'$'}UIDS; do
              echo "Универсальная очистка всех правил для UID: ${'$'}UID"
              
              # Удаляем ВСЕ TCP правила для данного UID с любыми портами
              while iptables -t nat -L OUTPUT -n | grep -q "owner UID match ${'$'}UID.*tcp.*REDIRECT"; do
                # Находим и удаляем правило по номеру строки
                LINE_NUM=$(iptables -t nat -L OUTPUT -n --line-numbers | grep "owner UID match ${'$'}UID.*tcp.*REDIRECT" | head -1 | awk '{print $1}')
                if [ ! -z "${'$'}LINE_NUM" ]; then
                  iptables -t nat -D OUTPUT ${'$'}LINE_NUM
                  echo "Удалено TCP правило №${'$'}LINE_NUM для UID ${'$'}UID"
                else
                  break
                fi
              done
              
              # Удаляем ВСЕ UDP DNS правила для данного UID с любыми портами
              while iptables -t nat -L OUTPUT -n | grep -q "owner UID match ${'$'}UID.*udp.*dpt:53.*REDIRECT"; do
                # Находим и удаляем правило по номеру строки
                LINE_NUM=$(iptables -t nat -L OUTPUT -n --line-numbers | grep "owner UID match ${'$'}UID.*udp.*dpt:53.*REDIRECT" | head -1 | awk '{print $1}')
                if [ ! -z "${'$'}LINE_NUM" ]; then
                  iptables -t nat -D OUTPUT ${'$'}LINE_NUM
                  echo "Удалено DNS правило №${'$'}LINE_NUM для UID ${'$'}UID"
                else
                  break
                fi
              done
            done

            # Добавление новых правил (очистка уже произведена ранее)
            for UID in ${'$'}UIDS; do
              echo "Добавление правил для UID: ${'$'}UID"
              
              # Проверяем, что правило еще не существует перед добавлением
              if ! iptables -t nat -C OUTPUT -p tcp -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}PORT 2>/dev/null; then
                iptables -t nat -A OUTPUT -p tcp -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}PORT
                echo "✅ TCP правило добавлено для UID ${'$'}UID"
              else
                echo "ℹ️ TCP правило уже существует для UID ${'$'}UID"
              fi
              
              if ! iptables -t nat -C OUTPUT -p udp --dport 53 -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}DNS_PORT 2>/dev/null; then
                iptables -t nat -A OUTPUT -p udp --dport 53 -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}DNS_PORT
                echo "✅ DNS правило добавлено для UID ${'$'}UID"
              else
                echo "ℹ️ DNS правило уже существует для UID ${'$'}UID"
              fi
            done

            echo "Правила применены для UID: ${'$'}UIDS"
            echo "Используемые порты: прокси=${'$'}PORT, DNS=${'$'}DNS_PORT"
            
            # Показываем текущие правила
            echo "Текущие правила NAT OUTPUT:"
            iptables -t nat -L OUTPUT -n --line-numbers | grep -E "(REDIRECT|${'$'}PORT|${'$'}DNS_PORT)"
        """.trimIndent()
    }

    private fun buildClearScript(uids: String, proxyPort: Int, dnsPort: Int): String {
        return """
            #!/system/bin/sh
            
            # Переменные
            UIDS="$uids"
            PORT=$proxyPort
            DNS_PORT=$dnsPort
            
            echo "Очистка правил iptables для UID: ${'$'}UIDS"
            echo "Порт прокси: ${'$'}PORT, Порт DNS: ${'$'}DNS_PORT"
            
            for UID in ${'$'}UIDS; do
              echo "Удаление правил для UID: ${'$'}UID"
              # Удаляем все TCP правила для данного UID с указанным портом
              while iptables -t nat -C OUTPUT -p tcp -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}PORT 2>/dev/null; do
                iptables -t nat -D OUTPUT -p tcp -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}PORT
              done
              # Удаляем все UDP DNS правила для данного UID с указанным портом
              while iptables -t nat -C OUTPUT -p udp --dport 53 -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}DNS_PORT 2>/dev/null; do
                iptables -t nat -D OUTPUT -p udp --dport 53 -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}DNS_PORT
              done
            done
            
            echo "Правила очищены для UID: ${'$'}UIDS (порты: прокси=${'$'}PORT, DNS=${'$'}DNS_PORT)"
            
            # Показываем оставшиеся правила
            echo "Оставшиеся правила NAT OUTPUT:"
            iptables -t nat -L OUTPUT -n --line-numbers | grep -E "(REDIRECT|${'$'}PORT|${'$'}DNS_PORT)" || echo "Правил не найдено"
        """.trimIndent()
    }

    /**
     * Универсальный скрипт очистки всех правил для UID независимо от портов
     */
    private fun buildUniversalClearScript(uids: String): String {
        return """
            #!/system/bin/sh
            
            # Переменные
            UIDS="$uids"
            
            echo "Универсальная очистка всех правил iptables для UID: ${'$'}UIDS"
            
            for UID in ${'$'}UIDS; do
              echo "Удаление ВСЕХ правил для UID: ${'$'}UID"
              
              # Удаляем ВСЕ TCP правила для данного UID с любыми портами
              while iptables -t nat -L OUTPUT -n | grep -q "owner UID match ${'$'}UID.*tcp.*REDIRECT"; do
                # Находим и удаляем правило по номеру строки
                LINE_NUM=$(iptables -t nat -L OUTPUT -n --line-numbers | grep "owner UID match ${'$'}UID.*tcp.*REDIRECT" | head -1 | awk '{print $1}')
                if [ ! -z "${'$'}LINE_NUM" ]; then
                  iptables -t nat -D OUTPUT ${'$'}LINE_NUM
                  echo "Удалено TCP правило №${'$'}LINE_NUM для UID ${'$'}UID"
                else
                  break
                fi
              done
              
              # Удаляем ВСЕ UDP DNS правила для данного UID с любыми портами
              while iptables -t nat -L OUTPUT -n | grep -q "owner UID match ${'$'}UID.*udp.*dpt:53.*REDIRECT"; do
                # Находим и удаляем правило по номеру строки
                LINE_NUM=$(iptables -t nat -L OUTPUT -n --line-numbers | grep "owner UID match ${'$'}UID.*udp.*dpt:53.*REDIRECT" | head -1 | awk '{print $1}')
                if [ ! -z "${'$'}LINE_NUM" ]; then
                  iptables -t nat -D OUTPUT ${'$'}LINE_NUM
                  echo "Удалено DNS правило №${'$'}LINE_NUM для UID ${'$'}UID"
                else
                  break
                fi
              done
            done
            
            echo "Универсальная очистка завершена для UID: ${'$'}UIDS"
            
            # Показываем оставшиеся правила для этих UID
            echo "Проверка оставшихся правил:"
            for UID in ${'$'}UIDS; do
              REMAINING=$(iptables -t nat -L OUTPUT -n | grep "owner UID match ${'$'}UID" | wc -l)
              echo "UID ${'$'}UID: осталось правил = ${'$'}REMAINING"
            done
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
