package dev.rx.app2proxy.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import java.util.*

class LanguageManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "language_prefs"
        private const val KEY_LANGUAGE = "selected_language"
        
        // Поддерживаемые языки
        const val LANGUAGE_SYSTEM = "system"
        const val LANGUAGE_RUSSIAN = "ru"
        const val LANGUAGE_ENGLISH = "en" 
        const val LANGUAGE_GERMAN = "de"
        
        // Список поддерживаемых языков
        val SUPPORTED_LANGUAGES = arrayOf(
            LANGUAGE_SYSTEM,
            LANGUAGE_RUSSIAN,
            LANGUAGE_ENGLISH,
            LANGUAGE_GERMAN
        )
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Получить текущий выбранный язык
     */
    fun getCurrentLanguage(): String {
        return prefs.getString(KEY_LANGUAGE, LANGUAGE_SYSTEM) ?: LANGUAGE_SYSTEM
    }
    
    /**
     * Установить язык
     */
    fun setLanguage(languageCode: String) {
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }
    
    /**
     * Получить системный язык
     */
    fun getSystemLanguage(): String {
        val systemLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
        
        return when (systemLocale.language) {
            "ru" -> LANGUAGE_RUSSIAN
            "en" -> LANGUAGE_ENGLISH  
            "de" -> LANGUAGE_GERMAN
            else -> LANGUAGE_ENGLISH // По умолчанию английский, если язык не поддерживается
        }
    }
    
    /**
     * Получить язык для применения (с учетом системного)
     */
    fun getLanguageToApply(): String {
        val selected = getCurrentLanguage()
        return if (selected == LANGUAGE_SYSTEM) {
            getSystemLanguage()
        } else {
            selected
        }
    }
    
    /**
     * Проверить, был ли язык инициализирован
     */
    fun isLanguageInitialized(): Boolean {
        return prefs.contains(KEY_LANGUAGE)
    }
    
    /**
     * Инициализировать язык при первом запуске
     */
    fun initializeLanguageOnFirstRun() {
        if (!isLanguageInitialized()) {
            // При первом запуске определяем системный язык
            val systemLang = getSystemLanguage()
            
            // Если системный язык поддерживается, ставим его, иначе системный выбор
            val initialLanguage = if (SUPPORTED_LANGUAGES.contains(systemLang)) {
                systemLang
            } else {
                LANGUAGE_SYSTEM
            }
            
            setLanguage(initialLanguage)
        }
    }
    
    /**
     * Применить контекст с выбранным языком
     */
    fun applyLanguageToContext(context: Context): Context {
        val languageCode = getLanguageToApply()
        
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }
    
    /**
     * Получить отображаемое название языка
     */
    fun getLanguageDisplayName(languageCode: String, context: Context): String {
        return when (languageCode) {
            LANGUAGE_SYSTEM -> context.getString(dev.rx.app2proxy.R.string.language_system)
            LANGUAGE_RUSSIAN -> context.getString(dev.rx.app2proxy.R.string.language_russian)
            LANGUAGE_ENGLISH -> context.getString(dev.rx.app2proxy.R.string.language_english)
            LANGUAGE_GERMAN -> context.getString(dev.rx.app2proxy.R.string.language_german)
            else -> languageCode
        }
    }
}
