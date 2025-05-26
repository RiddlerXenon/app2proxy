package dev.rx.app2proxy

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar

/**
 * Утилиты для создания AMOLED темы с динамическими цветами Material You
 * Сохраняет черный фон AMOLED темы, но применяет динамические цвета к другим элементам
 */
object AmoledDynamicColorScheme {
    
    private const val TAG = "AmoledDynamicColors"
    
    /**
     * Применяет динамические цвета с сохранением AMOLED фона
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun applyAmoledDynamicColors(activity: android.app.Activity) {
        try {
            Log.d(TAG, "🎨 Применяем AMOLED динамические цвета")
            
            // Сначала применяем стандартные динамические цвета Material You
            com.google.android.material.color.DynamicColors.applyToActivityIfAvailable(activity)
            
            // Теперь принудительно переопределяем только фоновые цвета на черные
            val window = activity.window
            
            // Делаем статус бар и навигацию прозрачными
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            
            // Устанавливаем черный фон только для корневого элемента
            // Остальные цвета (кнопки, карточки, акценты) останутся динамическими
            window.decorView.setBackgroundColor(android.graphics.Color.BLACK)
            
            Log.d(TAG, "✅ AMOLED динамические цвета применены успешно")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка применения AMOLED динамических цветов", e)
        }
    }
    
    /**
     * Применяет черный фон к конкретному view, сохраняя динамические цвета для дочерних элементов
     */
    fun applyAmoledBackgroundToView(view: android.view.View) {
        try {
            // Применяем черный фон только к корневому контейнеру
            view.setBackgroundColor(android.graphics.Color.BLACK)
            Log.d(TAG, "✅ Черный AMOLED фон применен к view")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка применения AMOLED фона к view", e)
        }
    }
    
    /**
     * Применяет черный фон к AppBar/Toolbar для AMOLED темы
     */
    fun applyAmoledToolbarStyle(toolbar: MaterialToolbar, activity: android.app.Activity) {
        try {
            Log.d(TAG, "🎨 Применяем AMOLED стиль к Toolbar")
            
            // Устанавливаем черный фон для toolbar
            toolbar.setBackgroundColor(android.graphics.Color.BLACK)
            
            // Устанавливаем белый цвет для текста заголовка
            toolbar.setTitleTextColor(android.graphics.Color.WHITE)
            toolbar.setSubtitleTextColor(android.graphics.Color.WHITE)
            
            // Устанавливаем белый цвет для иконок в toolbar
            val whiteColor = androidx.core.content.ContextCompat.getColor(activity, android.R.color.white)
            toolbar.navigationIcon?.setTint(whiteColor)
            toolbar.overflowIcon?.setTint(whiteColor)
            
            // Устанавливаем белый цвет для меню
            val menu = toolbar.menu
            for (i in 0 until menu.size()) {
                val menuItem = menu.getItem(i)
                menuItem.icon?.setTint(whiteColor)
            }
            
            Log.d(TAG, "✅ AMOLED стиль применен к Toolbar")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка применения AMOLED стиля к Toolbar", e)
        }
    }
    
    /**
     * Проверяет, можно ли применить динамические цвета
     */
    fun canApplyDynamicColors(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }
    
    /**
     * Получает цвет для AMOLED поверхности с учетом динамических цветов
     */
    fun getAmoledSurfaceColor(): Int {
        return android.graphics.Color.BLACK // Всегда черный для AMOLED
    }
    
    /**
     * Получает цвет фона для AMOLED с учетом динамических цветов
     */
    fun getAmoledBackgroundColor(): Int {
        return android.graphics.Color.BLACK // Всегда черный для AMOLED
    }
    
    /**
     * Получает цвет карточек для AMOLED темы (светлее основного фона)
     */
    fun getAmoledCardColor(): Int {
        return 0xFF1A1A1A.toInt() // Темно-серый цвет для карточек
    }
}
