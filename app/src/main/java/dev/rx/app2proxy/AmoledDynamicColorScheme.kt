package dev.rx.app2proxy

import android.app.Activity
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.DynamicColors

object AmoledDynamicColorScheme {
    private const val TAG = "AmoledDynamicColorScheme"
    
    /**
     * Применяет AMOLED динамические цвета с сохранением черного фона
     */
    fun applyAmoledDynamicColors(activity: Activity) {
        try {
            Log.d(TAG, "🎨 Применяем AMOLED + Material You динамические цвета")
            
            // Сначала применяем стандартные динамические цвета
            DynamicColors.applyToActivityIfAvailable(activity)
            
            // Затем принудительно переопределяем фон и системные элементы на черный
            activity.window.decorView.setBackgroundColor(android.graphics.Color.BLACK)
            
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
}
