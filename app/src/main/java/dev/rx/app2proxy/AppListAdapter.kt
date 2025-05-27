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
    private var filteredApps: List<AppInfo> = apps
    private var currentFilter: String = ""

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppViewHolder(binding)
    }

    override fun getItemCount(): Int = filteredApps.size

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = filteredApps[position]
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
                // Уведомляем об изменении без пересортировки
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
            val context = holder.binding.root.context
            val sharedPrefs = context.getSharedPreferences("proxy_prefs", android.content.Context.MODE_PRIVATE)
            val useAmoledTheme = sharedPrefs.getBoolean("amoled_theme", false)
            val isDarkTheme = sharedPrefs.getBoolean("dark_theme", true)
            
            if (useAmoledTheme && isDarkTheme) {
                AmoledDynamicColorScheme.applyAmoledCardStyle(holder.binding.root as MaterialCardView, context)
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
        selected.addAll(filteredApps.map { it.uid.toString() })
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
        applyFilter(currentFilter)
    }

    // Метод для обновления данных с сортировкой
    fun updateDataWithSort(newApps: List<AppInfo>, selectedUids: Set<String>) {
        this.apps = newApps
        selected.clear()
        selected.addAll(selectedUids)
        applyFilter(currentFilter)
        onSelectedChanged(selected)
    }

    // Метод для обновления только состояния выбранных элементов
    fun updateSelectedStates(selectedUids: Set<String>) {
        val oldSelected = selected.toSet()
        selected.clear()
        selected.addAll(selectedUids)
        
        // Обновляем только те элементы, состояние которых изменилось
        for (i in filteredApps.indices) {
            val uid = filteredApps[i].uid.toString()
            val wasSelected = oldSelected.contains(uid)
            val isSelected = selectedUids.contains(uid)
            
            if (wasSelected != isSelected) {
                notifyItemChanged(i)
            }
        }
        
        onSelectedChanged(selected)
    }

    // Новый метод для фильтрации приложений
    fun filter(query: String) {
        currentFilter = query
        applyFilter(query)
    }

    private fun applyFilter(query: String) {
        filteredApps = if (query.isBlank()) {
            apps
        } else {
            apps.filter { app ->
                app.appName.contains(query, ignoreCase = true) ||
                app.packageName.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }

    // Метод для получения количества отфильтрованных приложений
    fun getFilteredCount(): Int = filteredApps.size

    // Метод для проверки, активен ли фильтр
    fun isFiltered(): Boolean = currentFilter.isNotBlank()

    class AppViewHolder(val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root)
}
