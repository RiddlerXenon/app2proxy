package dev.rx.app2proxy

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import dev.rx.app2proxy.databinding.FragmentAppListBinding

class AppListFragment : Fragment(), RulesUpdateListener {

    private var _binding: FragmentAppListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AppListAdapter
    private var showSystemApps = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Настройка SwipeRefreshLayout для обновления списка
        binding.swipeRefresh.setOnRefreshListener {
            refreshAppList()
        }

        binding.btnApply.setOnClickListener {
            applyProxyRules()
        }

        updateAppList()
    }

    private fun refreshAppList() {
        try {
            // Обновляем список приложений
            updateAppList()
            
            // Останавливаем анимацию обновления
            binding.swipeRefresh.isRefreshing = false
            
            // Показываем сообщение об успешном обновлении
            Toast.makeText(requireContext(), "Список приложений обновлен", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            binding.swipeRefresh.isRefreshing = false
            Toast.makeText(requireContext(), "Ошибка обновления списка", Toast.LENGTH_SHORT).show()
        }
    }

    fun setShowSystemApps(show: Boolean) {
        showSystemApps = show
        if (::adapter.isInitialized) {
            updateAppList()
        }
    }

    // Новый метод для обновления состояния чекбоксов после изменения правил
    fun refreshSelectedStates() {
        if (::adapter.isInitialized) {
            val currentSelected = getPrefs().getStringSet("selected_uids", emptySet()) ?: emptySet()
            adapter.updateSelectedStates(currentSelected)
            // Пересортировываем список после обновления состояния
            resortAppList(currentSelected)
        }
    }

    // Новый метод для поиска приложений
    fun filterApps(query: String) {
        if (::adapter.isInitialized) {
            adapter.filter(query)
        }
    }

    // Метод для получения количества отфильтрованных приложений
    fun getFilteredAppsCount(): Int {
        return if (::adapter.isInitialized) {
            adapter.getFilteredCount()
        } else {
            0
        }
    }

    // Метод для проверки, активен ли фильтр
    fun isSearchActive(): Boolean {
        return if (::adapter.isInitialized) {
            adapter.isFiltered()
        } else {
            false
        }
    }

    private fun applyProxyRules() {
        if (!::adapter.isInitialized) {
            return
        }

        val selectedUids = adapter.getSelectedUids()
        val prevSelectedUids = getPrefs().getStringSet("selected_uids", emptySet()) ?: emptySet()

        // Проверяем лимит перед применением правил
        if (selectedUids.size > AppListAdapter.MAX_SELECTED_APPS) {
            Toast.makeText(
                requireContext(),
                "Слишком много выбранных приложений (${selectedUids.size}). Максимум: ${AppListAdapter.MAX_SELECTED_APPS}",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Сначала очищаем ВСЕ старые правила для всех ранее выбранных приложений
        if (prevSelectedUids.isNotEmpty()) {
            val prevUidsString = prevSelectedUids.joinToString(" ")
            IptablesService.clearAllRulesForUids(prevUidsString)
        }

        // Затем применяем правила только для текущих выбранных приложений
        if (selectedUids.isNotEmpty()) {
            val uidsString = selectedUids.joinToString(" ")
            IptablesService.applyRules(requireContext(), uidsString)
        }
        
        // Сохраняем текущее состояние ТОЛЬКО ПОСЛЕ нажатия кнопки "Применить"
        saveSelectedUids(selectedUids)
        
        // Показываем уведомление пользователю
        val message = when {
            selectedUids.isEmpty() -> "Все правила удалены"
            prevSelectedUids.isEmpty() -> "Правила применены для ${selectedUids.size} приложений"
            else -> "Правила обновлены для ${selectedUids.size} приложений"
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun saveSelectedUids(uids: Set<String>) {
        // Ограничиваем количество сохраняемых UID
        val limitedUids = uids.take(AppListAdapter.MAX_SELECTED_APPS).toSet()
        getPrefs().edit().putStringSet("selected_uids", limitedUids).apply()
    }

    private fun updateAppList() {
        val prevSelected = getPrefs().getStringSet("selected_uids", emptySet()) ?: emptySet()
        val apps = getInstalledApps(showSystemApps)
        
        if (apps.isEmpty()) {
            Toast.makeText(requireContext(), "Не удалось загрузить список приложений", Toast.LENGTH_LONG).show()
            return
        }

        // Сортируем приложения: сначала выбранные, потом остальные
        val sortedApps = sortAppsBySelection(apps, prevSelected)
        
        if (!::adapter.isInitialized) {
            adapter = AppListAdapter(sortedApps, prevSelected, requireContext().packageManager) { _ ->
                // УБИРАЕМ автоматическое сохранение - теперь сохранение происходит только при нажатии "Применить"
                // saveSelectedUids(updatedUids) - убираем
                // УБИРАЕМ автоматическую пересортировку при каждом изменении выбора
                // resortAppList(updatedUids) - закомментировано для предотвращения скачков
            }
            binding.recyclerView.adapter = adapter
        } else {
            adapter.updateData(sortedApps, prevSelected)
        }
    }

    // Метод для сортировки приложений по статусу выбора
    private fun sortAppsBySelection(apps: List<AppInfo>, selectedUids: Set<String>): List<AppInfo> {
        val selectedApps = mutableListOf<AppInfo>()
        val unselectedApps = mutableListOf<AppInfo>()

        for (app in apps) {
            if (selectedUids.contains(app.uid.toString())) {
                selectedApps.add(app)
            } else {
                unselectedApps.add(app)
            }
        }

        // Сортируем каждую группу по имени
        selectedApps.sortBy { it.appName.lowercase() }
        unselectedApps.sortBy { it.appName.lowercase() }

        // Возвращаем объединенный список: сначала выбранные, потом остальные
        return selectedApps + unselectedApps
    }

    // Метод для пересортировки списка без полной перезагрузки
    private fun resortAppList(selectedUids: Set<String>) {
        if (::adapter.isInitialized) {
            val currentApps = adapter.getCurrentApps()
            val sortedApps = sortAppsBySelection(currentApps, selectedUids)
            adapter.updateDataWithSort(sortedApps, selectedUids)
        }
    }

    override fun onRulesUpdated() {
        refreshSelectedStates()
    }

    private fun getPrefs(): SharedPreferences {
        return requireContext().getSharedPreferences("proxy_prefs", Context.MODE_PRIVATE)
    }

    private fun getInstalledApps(includeSystemApps: Boolean): List<AppInfo> {
        return try {
            val pm = requireContext().packageManager
            val packages = pm.getInstalledApplications(0)
            
            packages
                .filter { appInfo ->
                    // Фильтруем системные приложения, если нужно
                    val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val includeThisApp = includeSystemApps || !isSystemApp
                    
                    includeThisApp &&
                    // Исключаем само приложение App2Proxy из списка
                    appInfo.packageName != "dev.rx.app2proxy"
                }
                .mapNotNull { appInfo ->
                    try {
                        AppInfo(
                            appInfo.loadLabel(pm).toString(),
                            appInfo.packageName,
                            appInfo.uid
                        )
                    } catch (e: Exception) {
                        null // Пропускаем приложения, которые не удается обработать
                    }
                }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Ошибка загрузки приложений: ${e.message}", Toast.LENGTH_LONG).show()
            emptyList()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
