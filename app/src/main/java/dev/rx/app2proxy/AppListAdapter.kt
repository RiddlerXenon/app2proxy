package dev.rx.app2proxy

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import dev.rx.app2proxy.databinding.ItemAppBinding

class AppListAdapter(
    private var apps: List<AppInfo>,
    selectedUids: Set<String>,
    private val packageManager: PackageManager,
    private val onSelectedChanged: (Set<String>) -> Unit
) : RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {
    private val selected = selectedUids.toMutableSet()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppViewHolder(binding)
    }

    override fun getItemCount(): Int = apps.size

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.binding.apply {
            // Загружаем иконку приложения
            try {
                val appIconDrawable = packageManager.getApplicationIcon(app.packageName)
                appIcon.setImageDrawable(appIconDrawable)
            } catch (e: Exception) {
                // Используем стандартную иконку если не удалось загрузить
                appIcon.setImageResource(android.R.drawable.sym_def_app_icon)
            }
            
            // Устанавливаем тексты
            appName.text = app.appName
            packageName.text = app.packageName
            
            // Настраиваем чекбокс
            checkBox.setOnCheckedChangeListener(null)
            checkBox.isChecked = selected.contains(app.uid.toString())
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selected.add(app.uid.toString())
                } else {
                    selected.remove(app.uid.toString())
                }
                onSelectedChanged(selected)
            }
            
            // Добавляем клик по всей карточке
            root.setOnClickListener {
                checkBox.toggle()
            }

            // Применяем AMOLED стили к карточке, если включена AMOLED тема
            applyAmoledStyleIfNeeded(holder)
        }
    }

    private fun applyAmoledStyleIfNeeded(holder: AppViewHolder) {
        try {
            val context = holder.itemView.context
            val prefs = context.getSharedPreferences("proxy_prefs", android.content.Context.MODE_PRIVATE)
            val useAmoledTheme = prefs.getBoolean("amoled_theme", false)
            val isDarkTheme = prefs.getBoolean("dark_theme", true)
            
            if (useAmoledTheme && isDarkTheme) {
                // Проверяем, что root действительно MaterialCardView
                val cardView = holder.binding.root
                if (cardView is MaterialCardView) {
                    // Получаем цвет для карточки из ресурсов
                    val cardColor = androidx.core.content.ContextCompat.getColor(
                        context, 
                        R.color.amoled_card_surface
                    )
                    cardView.setCardBackgroundColor(cardColor)
                    
                    // Убираем тень и elevation для AMOLED
                    cardView.cardElevation = 0f
                    cardView.strokeWidth = 0
                }
            }
        } catch (e: Exception) {
            // Игнорируем ошибки применения стилей
            android.util.Log.w("AppListAdapter", "Не удалось применить AMOLED стиль к карточке", e)
        }
    }

    fun getSelectedUids(): Set<String> = selected

    fun getCurrentApps(): List<AppInfo> = apps

    fun selectAll() {
        selected.clear()
        selected.addAll(apps.map { it.uid.toString() })
        notifyDataSetChanged()
        onSelectedChanged(selected)
    }

    fun deselectAll() {
        selected.clear()
        notifyDataSetChanged()
        onSelectedChanged(selected)
    }

    fun updateData(newApps: List<AppInfo>, selectedUids: Set<String>) {
        this.apps = newApps
        selected.clear()
        selected.addAll(selectedUids)
        notifyDataSetChanged()
    }

    // Метод для обновления данных с сортировкой
    fun updateDataWithSort(newApps: List<AppInfo>, selectedUids: Set<String>) {
        this.apps = newApps
        selected.clear()
        selected.addAll(selectedUids)
        notifyDataSetChanged()
    }

    // Метод для обновления только состояния выбранных элементов
    fun updateSelectedStates(selectedUids: Set<String>) {
        selected.clear()
        selected.addAll(selectedUids)
        notifyDataSetChanged()
        onSelectedChanged(selected)
    }

    class AppViewHolder(val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root)
}
