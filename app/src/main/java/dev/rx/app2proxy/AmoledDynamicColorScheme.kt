package dev.rx.app2proxy

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.DynamicColors
import java.lang.reflect.Method

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
            activity.window.decorView.setBackgroundColor(Color.BLACK)
            
            Log.d(TAG, "✅ AMOLED динамические цвета применены успешно")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка применения AMOLED динамических цветов", e)
        }
    }
    
    /**
     * Применяет черный фон к конкретному view, сохраняя динамические цвета для дочерних элементов
     */
    fun applyAmoledBackgroundToView(view: View) {
        try {
            // Применяем черный фон только к корневому контейнеру
            view.setBackgroundColor(Color.BLACK)
            Log.d(TAG, "✅ Черный AMOLED фон применен к view")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка применения AMOLED фона к view", e)
        }
    }
    
    /**
     * Применяет черный фон к AppBar/Toolbar для AMOLED темы
     */
    fun applyAmoledToolbarStyle(toolbar: MaterialToolbar, activity: Activity) {
        try {
            Log.d(TAG, "🎨 Применяем AMOLED стиль к Toolbar")
            
            // Устанавливаем черный фон для toolbar
            toolbar.setBackgroundColor(Color.BLACK)
            
            // Устанавливаем белый цвет для текста заголовка
            toolbar.setTitleTextColor(Color.WHITE)
            toolbar.setSubtitleTextColor(Color.WHITE)
            
            // Устанавливаем белый цвет для иконок в toolbar
            val whiteColor = ContextCompat.getColor(activity, android.R.color.white)
            toolbar.navigationIcon?.setTint(whiteColor)
            toolbar.overflowIcon?.setTint(whiteColor)
            
            // Устанавливаем белый цвет для меню
            val menu = toolbar.menu
            for (i in 0 until menu.size()) {
                val menuItem = menu.getItem(i)
                menuItem.icon?.setTint(whiteColor)
            }
            
            // Применяем черный фон к overflow menu
            applyAmoledToOverflowMenu(toolbar, activity)
            
            Log.d(TAG, "✅ AMOLED стиль применен к Toolbar")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка применения AMOLED стиля к Toolbar", e)
        }
    }
    
    /**
     * Применяет AMOLED стиль к overflow menu
     */
    private fun applyAmoledToOverflowMenu(toolbar: MaterialToolbar, activity: Activity) {
        try {
            Log.d(TAG, "🎨 Применяем AMOLED стиль к overflow menu")
            
            // Метод 1: Через попытку установки popup theme
            try {
                toolbar.popupTheme = R.style.AmoledPopupMenuOverride
            } catch (e: Exception) {
                Log.w(TAG, "Не удалось установить popup theme", e)
            }
            
            // Метод 2: Через рефлексию к ActionMenuView
            try {
                val actionMenuView = findActionMenuView(toolbar)
                if (actionMenuView != null) {
                    // Устанавливаем popup theme для ActionMenuView
                    val setPopupThemeMethod = ActionMenuView::class.java.getMethod("setPopupTheme", Int::class.java)
                    setPopupThemeMethod.invoke(actionMenuView, R.style.AmoledPopupMenuOverride)
                    Log.d(TAG, "✅ Popup theme установлен через ActionMenuView")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Не удалось установить popup theme через ActionMenuView", e)
            }
            
            // Метод 3: Перехватываем показ overflow menu
            interceptOverflowMenuShow(toolbar, activity)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка применения AMOLED к overflow menu", e)
        }
    }
    
    /**
     * Находит ActionMenuView внутри Toolbar
     */
    private fun findActionMenuView(toolbar: MaterialToolbar): ActionMenuView? {
        try {
            for (i in 0 until toolbar.childCount) {
                val child = toolbar.getChildAt(i)
                if (child is ActionMenuView) {
                    return child
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Ошибка поиска ActionMenuView", e)
        }
        return null
    }
    
    /**
     * Перехватывает показ overflow menu
     */
    private fun interceptOverflowMenuShow(toolbar: MaterialToolbar, activity: Activity) {
        try {
            // Устанавливаем слушатель на нажатие overflow кнопки
            toolbar.setOnMenuItemClickListener { menuItem ->
                false // Не перехватываем клики по пунктам меню
            }
            
            // Получаем доступ к overflow кнопке
            val actionMenuView = findActionMenuView(toolbar)
            actionMenuView?.let { menuView ->
                // Устанавливаем кастомный popup theme
                try {
                    val field = ActionMenuView::class.java.getDeclaredField("mPopupTheme")
                    field.isAccessible = true
                    field.set(menuView, R.style.AmoledPopupMenuOverride)
                    Log.d(TAG, "✅ Popup theme установлен через поле mPopupTheme")
                } catch (e: Exception) {
                    Log.w(TAG, "Не удалось установить popup theme через поле", e)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка перехвата overflow menu", e)
        }
    }
    
    /**
     * Применяет AMOLED цвета ко всем элементам меню после его создания
     */
    fun applyAmoledToMenuItems(toolbar: MaterialToolbar, activity: Activity) {
        try {
            Log.d(TAG, "🎨 Применяем AMOLED к элементам меню")
            
            val menu = toolbar.menu
            if (menu is MenuBuilder) {
                // Устанавливаем кастомный фон для popup
                try {
                    val setCallback = MenuBuilder::class.java.getMethod("setCallback", MenuBuilder.Callback::class.java)
                    val originalCallback = menu.callback
                    
                    val customCallback = object : MenuBuilder.Callback {
                        override fun onMenuItemSelected(menu: MenuBuilder, item: MenuItem): Boolean {
                            return originalCallback?.onMenuItemSelected(menu, item) ?: false
                        }
                        
                        override fun onMenuModeChange(menu: MenuBuilder) {
                            originalCallback?.onMenuModeChange(menu)
                        }
                    }
                    
                    setCallback.invoke(menu, customCallback)
                } catch (e: Exception) {
                    Log.w(TAG, "Не удалось установить кастомный callback", e)
                }
            }
            
            // Устанавливаем белый цвет текста для всех пунктов меню
            val whiteColor = ContextCompat.getColor(activity, android.R.color.white)
            for (i in 0 until menu.size()) {
                val menuItem = menu.getItem(i)
                menuItem.icon?.setTint(whiteColor)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка применения AMOLED к элементам меню", e)
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
        return Color.BLACK // Всегда черный для AMOLED
    }
    
    /**
     * Получает цвет фона для AMOLED с учетом динамических цветов
     */
    fun getAmoledBackgroundColor(): Int {
        return Color.BLACK // Всегда черный для AMOLED
    }
}
