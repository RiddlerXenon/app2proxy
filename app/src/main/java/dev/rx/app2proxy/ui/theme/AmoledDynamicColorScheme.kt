package dev.rx.app2proxy

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object AmoledDynamicColorScheme {
    private const val TAG = "AmoledDynamicColors"
    
    /**
     * Применяет динамические цвета Material You с AMOLED модификациями
     */
    fun applyAmoledDynamicColors(activity: Activity) {
        try {
            Log.d(TAG, "🎨 Применяем AMOLED + Dynamic Colors")
            
            // Сначала применяем стандартные динамические цвета
            DynamicColors.applyToActivityIfAvailable(activity)
            
            // Затем переопределяем фон на черный
            activity.window.decorView.setBackgroundColor(Color.BLACK)
            
            Log.d(TAG, "✅ AMOLED Dynamic Colors применены")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка применения AMOLED Dynamic Colors", e)
        }
    }
    
    /**
     * Применяет черный фон к View для AMOLED темы
     */
    fun applyAmoledBackgroundToView(view: View) {
        try {
            Log.d(TAG, "🎨 Применяем AMOLED фон к View: ${view::class.simpleName}")
            view.setBackgroundColor(Color.BLACK)
            Log.d(TAG, "✅ AMOLED фон применен к View")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка применения AMOLED фона к View", e)
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
            
            Log.d(TAG, "✅ AMOLED стиль применен к Toolbar")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка применения AMOLED стиля к Toolbar", e)
        }
    }
    
    /**
     * Применяет AMOLED стиль к MaterialCardView для списка приложений
     */
    fun applyAmoledCardStyle(cardView: MaterialCardView) {
        try {
            Log.d(TAG, "🎨 Применяем AMOLED стиль к карточке")
            
            // Устанавливаем темный фон для карточки
            cardView.setCardBackgroundColor(Color.parseColor("#FF0A0909"))
            
            // Устанавливаем минимальную высоту границы для лучшего вида
            cardView.cardElevation = 2f
            cardView.radius = 12f
            
            Log.d(TAG, "✅ AMOLED стиль применен к карточке")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка применения AMOLED стиля к карточке", e)
        }
    }
    
    /**
     * Создает MaterialAlertDialogBuilder с поддержкой AMOLED фона
     */
    fun createAmoledMaterialAlertDialogBuilder(
        context: Context,
        useAmoledTheme: Boolean = false,
        isDarkTheme: Boolean = true
    ): MaterialAlertDialogBuilder {
        return try {
            Log.d(TAG, "🎨 Создаем MaterialAlertDialog с AMOLED поддержкой")
            
            val builder = MaterialAlertDialogBuilder(context)
            
            // Применяем AMOLED стиль только если включены темная тема и AMOLED
            if (useAmoledTheme && isDarkTheme) {
                // Используем кастомный стиль для AMOLED темы
                builder.background?.setTint(Color.BLACK)
                
                Log.d(TAG, "✅ AMOLED стиль применен к MaterialAlertDialog")
            } else {
                Log.d(TAG, "ℹ️ Используется стандартный стиль для MaterialAlertDialog")
            }
            
            builder
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка создания AMOLED MaterialAlertDialog", e)
            // Возвращаем стандартный builder в случае ошибки
            MaterialAlertDialogBuilder(context)
        }
    }
    
    /**
     * Применяет AMOLED стиль к уже созданному AlertDialog
     */
    fun applyAmoledStyleToDialog(
        dialog: AlertDialog,
        useAmoledTheme: Boolean = false,
        isDarkTheme: Boolean = true
    ) {
        try {
            if (useAmoledTheme && isDarkTheme) {
                Log.d(TAG, "🎨 Применяем AMOLED стиль к AlertDialog")
                
                // Применяем черный фон к диалогу
                dialog.window?.let { window ->
                    window.setBackgroundDrawableResource(android.R.color.black)
                    
                    // Находим корневой контейнер диалога и применяем черный фон
                    val decorView = window.decorView
                    decorView.setBackgroundColor(Color.BLACK)
                    
                    // Применяем стиль к содержимому диалога
                    decorView.findViewById<View>(android.R.id.content)?.let { contentView ->
                        contentView.setBackgroundColor(Color.BLACK)
                    }
                }
                
                Log.d(TAG, "✅ AMOLED стиль применен к AlertDialog")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка применения AMOLED стиля к AlertDialog", e)
        }
    }
}
