package dev.rx.app2proxy

import android.app.Application
import android.os.Build
import com.google.android.material.color.DynamicColors

class App2ProxyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)
        val useMaterialYou = prefs.getBoolean("material_you", false)
        if (useMaterialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            DynamicColors.applyToActivitiesIfAvailable(this)
        }
    }
}
