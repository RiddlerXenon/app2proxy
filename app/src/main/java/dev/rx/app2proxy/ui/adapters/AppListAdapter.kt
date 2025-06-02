package dev.rx.app2proxy

import android.content.Context
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
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
    
    companion object {
        const val MAX_SELECTED_APPS = 25
    }

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
                    if (selected.size >= MAX_SELECTED_APPS) {
                        // Отменяем выбор и показываем сообщение
                        checkBox.isChecked = false
                        Toast.makeText(
                            holder.binding.root.context,
                            holder.binding.root.context.getString(R.string.max_apps_selected, MAX_SELECTED_APPS),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnCheckedChangeListener
                    }
                    selected.add(app.uid.toString())
                } else {
                    selected.remove(app.uid.toString())
                }
                // УДАЛЕНО: автоматическое сохранение при изменении выбора
                // onSelectedChanged(selected) - убираем этот вызов
            }
            
            // Добавляем клик по всей карточке
            root.setOnClickListener {
                if (!checkBox.isChecked && selected.size >= MAX_SELECTED_APPS) {
                    Toast.makeText(
                        holder.binding.root.context,
                        holder.binding.root.context.getString(R.string.max_apps_selected, MAX_SELECTED_APPS),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                checkBox.toggle()
            }

            // Добавляем долгое нажатие для показа информации о приложении
            root.setOnLongClickListener {
                showAppInfoDialog(holder.binding.root.context, app)
                true
            }

            // Применяем AMOLED стили к карточке, если включена AMOLED тема
            applyAmoledStyleIfNeeded(holder)
        }
    }

    private fun showAppInfoDialog(context: Context, app: AppInfo) {
        try {
            val prefs = context.getSharedPreferences("proxy_prefs", Context.MODE_PRIVATE)
            val useAmoledTheme = prefs.getBoolean("amoled_theme", false)
            val isDarkTheme = prefs.getBoolean("dark_theme", true)
            val isRuleActive = selected.contains(app.uid.toString())
            
            // Получаем иконку приложения
            val appIcon = try {
                packageManager.getApplicationIcon(app.packageName)
            } catch (e: Exception) {
                context.getDrawable(android.R.drawable.sym_def_app_icon)
            }
            
            // Создаем диалог с AMOLED поддержкой
            val dialogBuilder = AmoledDynamicColorScheme.createAmoledMaterialAlertDialogBuilder(
                context, useAmoledTheme, isDarkTheme
            )
            
            // Создаем кастомный layout для диалога
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_app_info, null)
            
            // Настраиваем элементы диалога
            val dialogAppIcon = dialogView.findViewById<android.widget.ImageView>(R.id.dialogAppIcon)
            val dialogAppName = dialogView.findViewById<android.widget.TextView>(R.id.dialogAppName)
            val dialogPackageName = dialogView.findViewById<android.widget.TextView>(R.id.dialogPackageName)
            val dialogUid = dialogView.findViewById<android.widget.TextView>(R.id.dialogUid)
            val dialogRuleStatus = dialogView.findViewById<android.widget.TextView>(R.id.dialogRuleStatus)
            
            dialogAppIcon.setImageDrawable(appIcon)
            dialogAppName.text = app.appName
            dialogPackageName.text = app.packageName
            dialogUid.text = context.getString(R.string.app_uid_format, app.uid)
            
            // Устанавливаем статус правила
            if (isRuleActive) {
                dialogRuleStatus.text = context.getString(R.string.rule_status_active)
                dialogRuleStatus.setTextColor(context.getColor(android.R.color.holo_green_light))
            } else {
                dialogRuleStatus.text = context.getString(R.string.rule_status_inactive)
                dialogRuleStatus.setTextColor(context.getColor(android.R.color.holo_red_light))
            }
            
            val dialog = dialogBuilder
                .setTitle(R.string.app_info_title)
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok, null)
                .apply {
                    // Добавляем кнопку удаления правила только если правило активно
                    if (isRuleActive) {
                        setNegativeButton(R.string.remove_rule) { _, _ ->
                            removeAppRule(context, app)
                        }
                    }
                }
                .create()
            
            dialog.show()
            
            // Применяем AMOLED стиль к показанному диалогу
            AmoledDynamicColorScheme.applyAmoledStyleToDialog(dialog, useAmoledTheme, isDarkTheme)
            
        } catch (e: Exception) {
            android.util.Log.e("AppListAdapter", "Ошибка показа диалога информации о приложении", e)
            Toast.makeText(context, R.string.error_showing_app_info, Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun removeAppRule(context: Context, app: AppInfo) {
        try {
            // Удаляем приложение из выбранных
            selected.remove(app.uid.toString())
            
            // Удаляем правило iptables для данного приложения
            IptablesService.clearAllRulesForUids(app.uid.toString())
            
            // Сохраняем обновленное состояние
            val prefs = context.getSharedPreferences("proxy_prefs", Context.MODE_PRIVATE)
            prefs.edit().putStringSet("selected_uids", selected).apply()
            
            // Обновляем отображение
            notifyDataSetChanged()
            
            // Уведомляем о изменении
            onSelectedChanged(selected)
            
            Toast.makeText(
                context, 
                context.getString(R.string.rule_removed_for_app, app.appName), 
                Toast.LENGTH_SHORT
            ).show()
            
        } catch (e: Exception) {
            android.util.Log.e("AppListAdapter", "Ошибка удаления правила для приложения", e)
            Toast.makeText(context, R.string.error_removing_rule, Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyAmoledStyleIfNeeded(holder: AppViewHolder) {
        try {
            val context = holder.binding.root.context
            val sharedPrefs = context.getSharedPreferences("proxy_prefs", android.content.Context.MODE_PRIVATE)
            val useAmoledTheme = sharedPrefs.getBoolean("amoled_theme", false)
            val isDarkTheme = sharedPrefs.getBoolean("dark_theme", true)
            
            if (useAmoledTheme && isDarkTheme) {
                AmoledDynamicColorScheme.applyAmoledCardStyle(holder.binding.root as MaterialCardView)
            }
        } catch (e: Exception) {
            // Игнорируем ошибки применения стилей
            android.util.Log.w("AppListAdapter", "Не удалось применить AMOLED стиль к карточке", e)
        }
    }

    fun getSelectedUids(): Set<String> = selected

    fun getCurrentApps(): List<AppInfo> = apps

    fun updateData(newApps: List<AppInfo>, selectedUids: Set<String>) {
        this.apps = newApps
        selected.clear()
        // Ограничиваем количество выбранных приложений при загрузке
        selected.addAll(selectedUids.take(MAX_SELECTED_APPS))
        applyFilter(currentFilter)
    }

    // Метод для обновления данных с сортировкой
    fun updateDataWithSort(newApps: List<AppInfo>, selectedUids: Set<String>) {
        this.apps = newApps
        selected.clear()
        // Ограничиваем количество выбранных приложений при загрузке
        selected.addAll(selectedUids.take(MAX_SELECTED_APPS))
        applyFilter(currentFilter)
        // НЕ вызываем onSelectedChanged здесь автоматически
    }

    // Метод для обновления только состояния выбранных элементов
    fun updateSelectedStates(selectedUids: Set<String>) {
        val oldSelected = selected.toSet()
        selected.clear()
        // Ограничиваем количество выбранных приложений
        selected.addAll(selectedUids.take(MAX_SELECTED_APPS))
        
        // Обновляем только те элементы, состояние которых изменилось
        for (i in filteredApps.indices) {
            val uid = filteredApps[i].uid.toString()
            val wasSelected = oldSelected.contains(uid)
            val isSelected = selected.contains(uid)
            
            if (wasSelected != isSelected) {
                notifyItemChanged(i)
            }
        }
        
        // Вызываем onSelectedChanged только при обновлении извне
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
