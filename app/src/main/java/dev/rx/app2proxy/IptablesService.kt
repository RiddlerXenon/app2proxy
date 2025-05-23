package dev.rx.app2proxy

import android.content.Context
import android.widget.Toast
import java.io.DataOutputStream

object IptablesService {
    private const val XRAY_PORT = 12345
    private const val XRAY_DNS_PORT = 10853

    fun applyRulesFromPrefs(context: Context) {
        val prefs = context.getSharedPreferences("proxy_prefs", Context.MODE_PRIVATE)
        val uids = prefs.getStringSet("selected_uids", emptySet())?.joinToString(" ") ?: ""
        applyRules(context, uids)
    }

    fun applyRules(context: Context, uids: String) {
        val script = buildScript(uids)
        val result = runAsRoot(script)
        Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
    }

    fun clearRules(context: Context, uids: String) {
        val script = buildClearScript(uids)
        val result = runAsRoot(script)
        Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
    }

    private fun buildScript(uids: String): String {
        return """
            UIDS="$uids"
            PORT=$XRAY_PORT

            for UID in ${'$'}UIDS; do
              while iptables -t nat -C OUTPUT -p tcp -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}PORT 2>/dev/null; do
                iptables -t nat -D OUTPUT -p tcp -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}PORT
              done
            done

            for UID in ${'$'}UIDS; do
              iptables -t nat -A OUTPUT -p tcp -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}PORT
              iptables -t nat -A OUTPUT -p udp --dport 53 -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports $XRAY_DNS_PORT
            done

            iptables -t nat -L OUTPUT -n --line-numbers
        """.trimIndent()
    }

    private fun buildClearScript(uids: String): String {
        return """
            UIDS="$uids"
            PORT=$XRAY_PORT
            for UID in ${'$'}UIDS; do
              while iptables -t nat -C OUTPUT -p tcp -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}PORT 2>/dev/null; do
                iptables -t nat -D OUTPUT -p tcp -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}PORT
              done
              while iptables -t nat -C OUTPUT -p udp --dport 53 -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports $XRAY_DNS_PORT 2>/dev/null; do
                iptables -t nat -D OUTPUT -p udp --dport 53 -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports $XRAY_DNS_PORT
              done
            done
            iptables -t nat -L OUTPUT -n --line-numbers
        """.trimIndent()
    }

    private fun runAsRoot(script: String): String {
        return try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            os.writeBytes(script)
            os.writeBytes("\nexit\n")
            os.flush()
            process.waitFor()
            process.inputStream.bufferedReader().readText()
        } catch (e: Exception) {
            "Ошибка: ${e.localizedMessage}"
        }
    }
}
