package dev.rx.app2proxy.ui.activities

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dev.rx.app2proxy.utils.LanguageManager

abstract class BaseActivity : AppCompatActivity() {
    
    private lateinit var languageManager: LanguageManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        languageManager = LanguageManager(this)
        languageManager.initializeLanguageOnFirstRun()
        super.onCreate(savedInstanceState)
    }
    
    override fun attachBaseContext(newBase: Context?) {
        if (newBase != null) {
            val languageManager = LanguageManager(newBase)
            languageManager.initializeLanguageOnFirstRun()
            val context = languageManager.applyLanguageToContext(newBase)
            super.attachBaseContext(context)
        } else {
            super.attachBaseContext(newBase)
        }
    }
    
    protected fun getLanguageManager(): LanguageManager {
        return languageManager
    }
}
