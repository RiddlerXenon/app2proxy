package dev.rx.app2proxy

import android.content.Context
import android.util.Log
import java.io.DataOutputStream

object IptablesService {
    private const val DEFAULT_XRAY_PORT = 12345
    private const val DEFAULT_XRAY_DNS_PORT = 10853
    private const val TAG = "IptablesService"
    
    // Adding an identifier for security validation
    private const val APP_SIGNATURE = "app2proxy_legitimate_traffic_redirector"
    
    fun applyRulesFromPrefs(context: Context) {
        try {
            // Checking legitimacy of the operation
            if (!validateSecurityContext(context)) {
                Log.w(TAG, "Operation rejected: security violation")
                return
            }
            
            val prefs = context.getSharedPreferences("proxy_prefs", Context.MODE_PRIVATE)
            val uids = prefs.getStringSet("selected_uids", emptySet())?.joinToString(" ") ?: ""
            
            if (uids.isNotEmpty()) {
                Log.d(TAG, "Applying saved rules for UID: $uids")
                applyRules(context, uids)
            } else {
                Log.d(TAG, "No saved UIDs to apply rules")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying rules from preferences", e)
        }
    }

    fun applyRules(context: Context, uids: String) {
        if (uids.trim().isEmpty()) {
            Log.w(TAG, "Empty UID list for applying rules")
            return
        }
        
        try {
            // Validate security
            if (!validateSecurityContext(context)) {
                Log.w(TAG, "Operation rejected: security violation")
                return
            }
            
            val prefs = context.getSharedPreferences("proxy_prefs", Context.MODE_PRIVATE)
            val proxyPort = prefs.getInt("proxy_port", DEFAULT_XRAY_PORT)
            val dnsPort = prefs.getInt("dns_port", DEFAULT_XRAY_DNS_PORT)
            
            // Validate ports
            if (!isValidPort(proxyPort) || !isValidPort(dnsPort)) {
                Log.w(TAG, "Invalid ports: proxy=$proxyPort, DNS=$dnsPort")
                return
            }
            
            Log.d(TAG, "Applying iptables rules for UID: $uids, proxy port: $proxyPort, DNS port: $dnsPort")
            val script = buildSecureScript(uids, proxyPort, dnsPort)
            val result = runAsRoot(script)
            Log.d(TAG, "Result of applying rules: $result")
        } catch (e: Exception) {
            Log.e(TAG, "Error applying iptables rules", e)
        }
    }

    fun clearRules(context: Context, uids: String) {
        if (uids.trim().isEmpty()) {
            Log.w(TAG, "Empty UID list for clearing rules")
            return
        }

        try {
            if (!validateSecurityContext(context)) {
                Log.w(TAG, "Operation rejected: security violation")
                return
            }
            
            val prefs = context.getSharedPreferences("proxy_prefs", Context.MODE_PRIVATE)
            val proxyPort = prefs.getInt("proxy_port", DEFAULT_XRAY_PORT)
            val dnsPort = prefs.getInt("dns_port", DEFAULT_XRAY_DNS_PORT)
            
            Log.d(TAG, "Clearing iptables rules for UID: $uids, proxy port: $proxyPort, DNS port: $dnsPort")
            val script = buildClearScript(uids, proxyPort, dnsPort)
            val result = runAsRoot(script)
            Log.d(TAG, "Result of clearing rules: $result")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing iptables rules", e)
        }
    }

    fun clearAllRulesForUids(uids: String) {
        if (uids.trim().isEmpty()) {
            Log.w(TAG, "Empty UID list for universal rule clearing")
            return
        }

        try {
            Log.d(TAG, "Universal clearing of all iptables rules for UID: $uids")
            val script = buildUniversalClearScript(uids)
            val result = runAsRoot(script)
            Log.d(TAG, "Result of universal rule clearing: $result")
        } catch (e: Exception) {
            Log.e(TAG, "Error during universal iptables rule clearing", e)
        }
    }

    fun clearRulesWithOldPorts(uids: String, oldProxyPort: Int, oldDnsPort: Int) {
        if (uids.trim().isEmpty()) {
            Log.w(TAG, "Empty UID list for clearing old rules")
            return
        }

        try {
            Log.d(TAG, "Clearing old iptables rules for UID: $uids with old ports: proxy=$oldProxyPort, DNS=$oldDnsPort")
            val script = buildClearScript(uids, oldProxyPort, oldDnsPort)
            val result = runAsRoot(script)
            Log.d(TAG, "Result of clearing old iptables rules: $result")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing old iptables rules", e)
        }
    }
    
    // Validation of security context to ensure the operation is legitimate
    private fun validateSecurityContext(context: Context): Boolean {
        return try {
            // Check if the app is running in a legitimate context
            val packageName = context.packageName
            if (packageName != "dev.rx.app2proxy") {
                Log.w(TAG, "[$APP_SIGNATURE] Неверный пакет: $packageName")
                return false
            }
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error validating security", e)
            false
        }
    }
    
    // Validate port numbers to ensure they are within a safe range
    private fun isValidPort(port: Int): Boolean {
        return port in 1024..65535 && port != 22 && port != 80 && port != 443
    }

    private fun buildSecureScript(uids: String, proxyPort: Int, dnsPort: Int): String {
        // Sanitize UID input to prevent injection attacks
        val sanitizedUids = uids.replace(Regex("[^0-9 ]"), "")
        
        return """
            #!/system/bin/sh
            
            # $APP_SIGNATURE

            UIDS="$sanitizedUids"
            PORT=$proxyPort
            DNS_PORT=$dnsPort
            
            echo "[$APP_SIGNATURE] Apllying iptables rules for UID: ${'$'}UIDS"
            echo "[$APP_SIGNATURE] Using ports: proxy=${'$'}PORT, DNS=${'$'}DNS_PORT"
            
            if ! command -v iptables > /dev/null 2>&1; then
                echo "[$APP_SIGNATURE] ERROR: iptables is not available"
                exit 1
            fi
            
            for UID in ${'$'}UIDS; do
              if ! echo "${'$'}UID" | grep -qE '^[0-9]+${'$'}'; then
                echo "[$APP_SIGNATURE] Warning: Skipping invalid UID: ${'$'}UID"
                continue
              fi
              
              echo "[$APP_SIGNATURE] Universal clearing all rules for UID: ${'$'}UID"

              while iptables -t nat -L OUTPUT -n | grep -q "owner UID match ${'$'}UID.*tcp.*REDIRECT"; do
                LINE_NUM=${'$'}(iptables -t nat -L OUTPUT -n --line-numbers | grep "owner UID match ${'$'}UID.*tcp.*REDIRECT" | head -1 | awk '{print ${'$'}1}')
                if [ ! -z "${'$'}LINE_NUM" ]; then
                  iptables -t nat -D OUTPUT ${'$'}LINE_NUM
                  echo "[$APP_SIGNATURE] Deleting TCP rule #${'$'}LINE_NUM for UID ${'$'}UID"
                else
                  break
                fi
              done
              
              while iptables -t nat -L OUTPUT -n | grep -q "owner UID match ${'$'}UID.*udp.*dpt:53.*REDIRECT"; do
                LINE_NUM=${'$'}(iptables -t nat -L OUTPUT -n --line-numbers | grep "owner UID match ${'$'}UID.*udp.*dpt:53.*REDIRECT" | head -1 | awk '{print ${'$'}1}')
                if [ ! -z "${'$'}LINE_NUM" ]; then
                  iptables -t nat -D OUTPUT ${'$'}LINE_NUM
                  echo "[$APP_SIGNATURE] Deleting DNS rule #${'$'}LINE_NUM for UID ${'$'}UID"
                else
                  break
                fi
              done
            done

            for UID in ${'$'}UIDS; do
              if ! echo "${'$'}UID" | grep -qE '^[0-9]+${'$'}'; then
                continue
              fi
              
              echo "[$APP_SIGNATURE] Addding rules for UID: ${'$'}UID"
              
              if ! iptables -t nat -C OUTPUT -p tcp -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}PORT 2>/dev/null; then
                iptables -t nat -A OUTPUT -p tcp -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}PORT
                echo "[$APP_SIGNATURE] ✅ TCP rule added for UID ${'$'}UID"
              else
                echo "[$APP_SIGNATURE] ℹ️ TCP rule already exists for UID ${'$'}UID"
              fi
              
              if ! iptables -t nat -C OUTPUT -p udp --dport 53 -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}DNS_PORT 2>/dev/null; then
                iptables -t nat -A OUTPUT -p udp --dport 53 -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}DNS_PORT
                echo "[$APP_SIGNATURE] ✅ DNS rule added for UID ${'$'}UID"
              else
                echo "[$APP_SIGNATURE] ℹ️ DNS rule already exists for UID ${'$'}UID"
              fi
            done

            echo "[$APP_SIGNATURE] Rules applied for UID: ${'$'}UIDS"
            echo "[$APP_SIGNATURE] Using ports: proxy=${'$'}PORT, DNS=${'$'}DNS_PORT"

            echo "[$APP_SIGNATURE] Remaining NAT OUTPUT rules:"
            iptables -t nat -L OUTPUT -n --line-numbers | grep -E "(REDIRECT|${'$'}PORT|${'$'}DNS_PORT)"
        """.trimIndent()
    }

    private fun buildClearScript(uids: String, proxyPort: Int, dnsPort: Int): String {
        val sanitizedUids = uids.replace(Regex("[^0-9 ]"), "")
        
        return """
            #!/system/bin/sh
            
            # $APP_SIGNATURE

            UIDS="$sanitizedUids"
            PORT=$proxyPort
            DNS_PORT=$dnsPort
                        
            echo "[$APP_SIGNATURE] Clearing iptables rules for UID: ${'$'}UIDS"
            echo "[$APP_SIGNATURE] Using ports: proxy=${'$'}PORT, DNS=${'$'}DNS_PORT"

            for UID in ${'$'}UIDS; do
              if ! echo "${'$'}UID" | grep -qE '^[0-9]+${'$'}'; then
                echo "[$APP_SIGNATURE] WARNING: Skipping invalid UID: ${'$'}UID"
                continue
              fi
              
              echo "[$APP_SIGNATURE] Clearing rules for UID: ${'$'}UID"
              while iptables -t nat -C OUTPUT -p tcp -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}PORT 2>/dev/null; do
                iptables -t nat -D OUTPUT -p tcp -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}PORT
              done
              while iptables -t nat -C OUTPUT -p udp --dport 53 -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}DNS_PORT 2>/dev/null; do
                iptables -t nat -D OUTPUT -p udp --dport 53 -m owner --uid-owner ${'$'}UID -j REDIRECT --to-ports ${'$'}DNS_PORT
              done
            done
            
            echo "[$APP_SIGNATURE] Rules cleared for UID: ${'$'}UIDS (ports: proxy=${'$'}PORT, DNS=${'$'}DNS_PORT)"
            
            echo "[$APP_SIGNATURE] Remaining NAT OUTPUT rules:"
            iptables -t nat -L OUTPUT -n --line-numbers | grep -E "(REDIRECT|${'$'}PORT|${'$'}DNS_PORT)" || echo "Rules not found"
        """.trimIndent()
    }

    private fun buildUniversalClearScript(uids: String): String {
        val sanitizedUids = uids.replace(Regex("[^0-9 ]"), "")
        
        return """
            #!/system/bin/sh
            
            # $APP_SIGNATURE

            UIDS="$sanitizedUids"
            
            echo "[$APP_SIGNATURE] Universal clearing of all iptables rules for UID: ${'$'}UIDS"

            for UID in ${'$'}UIDS; do
              if ! echo "${'$'}UID" | grep -qE '^[0-9]+${'$'}'; then
                echo "[$APP_SIGNATURE] WARNING: Skipping invalid UID: ${'$'}UID"
                continue
              fi
              
              echo "[$APP_SIGNATURE] Deleting ALL rules for UID: ${'$'}UID"

              while iptables -t nat -L OUTPUT -n | grep -q "owner UID match ${'$'}UID.*tcp.*REDIRECT"; do
                LINE_NUM=${'$'}(iptables -t nat -L OUTPUT -n --line-numbers | grep "owner UID match ${'$'}UID.*tcp.*REDIRECT" | head -1 | awk '{print ${'$'}1}')
                if [ ! -z "${'$'}LINE_NUM" ]; then
                  iptables -t nat -D OUTPUT ${'$'}LINE_NUM
                  echo "[$APP_SIGNATURE] Deleted TCP rule #${'$'}LINE_NUM for UID ${'$'}UID"
                else
                  break
                fi
              done
              
              while iptables -t nat -L OUTPUT -n | grep -q "owner UID match ${'$'}UID.*udp.*dpt:53.*REDIRECT"; do
                LINE_NUM=${'$'}(iptables -t nat -L OUTPUT -n --line-numbers | grep "owner UID match ${'$'}UID.*udp.*dpt:53.*REDIRECT" | head -1 | awk '{print ${'$'}1}')
                if [ ! -z "${'$'}LINE_NUM" ]; then
                  iptables -t nat -D OUTPUT ${'$'}LINE_NUM
                  echo "[$APP_SIGNATURE] Deleted DNS rule #${'$'}LINE_NUM for UID ${'$'}UID"
                else
                  break
                fi
              done
            done
            
            echo "[$APP_SIGNATURE] Universal clearing completed for UID: ${'$'}UIDS"
            
            echo "[$APP_SIGNATURE] Checking remaining rules:"
            for UID in ${'$'}UIDS; do
              REMAINING=${'$'}(iptables -t nat -L OUTPUT -n | grep "owner UID match ${'$'}UID" | wc -l)
              echo "[$APP_SIGNATURE] UID ${'$'}UID: remaining rules = ${'$'}REMAINING"
            done
        """.trimIndent()
    }

    private fun runAsRoot(script: String): String {
        return try {
            Log.d(TAG, "[$APP_SIGNATURE] Executing script with root privileges")
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            
            // Writing the script to the process output stream
            os.writeBytes(script)
            os.writeBytes("\nexit\n")
            os.flush()
            
            // Waiting for the process to finish
            val exitCode = process.waitFor()
            
            // Reading output and error streams
            val output = process.inputStream.bufferedReader().readText()
            val errorOutput = process.errorStream.bufferedReader().readText()
                        
            Log.d(TAG, "[$APP_SIGNATURE] Code exit: $exitCode")
            Log.d(TAG, "[$APP_SIGNATURE] Output: $output")
            if (errorOutput.isNotEmpty()) {
                Log.w(TAG, "[$APP_SIGNATURE] Errors: $errorOutput")
            }

            if (exitCode == 0) output else "Error executing script (code: $exitCode): $errorOutput"
        } catch (e: Exception) {
            val errorMessage = "[$APP_SIGNATURE] Error executing command: ${e.localizedMessage}"
            Log.e(TAG, errorMessage, e)
            errorMessage
        }
    }
}
