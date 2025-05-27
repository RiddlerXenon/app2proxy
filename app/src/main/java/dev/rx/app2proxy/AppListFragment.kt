package dev.rx.app2proxy

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import dev.rx.app2proxy.databinding.FragmentAppListBinding

class AppListFragment : Fragment() {
    private var _binding: FragmentAppListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AppListAdapter
    private var showSystemApps = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Настраиваем SwipeRefreshLayout
        binding.swipeRefresh.setOnRefreshListener {
            updateAppList()
            binding.swipeRefresh.isRefreshing = false
        }

        binding.btnApply.setOnClickListener {
            applyProxyRules()
        }

        updateAppList()
    }

    fun setShowSystemApps(show: Boolean) {
        showSystemApps = show
        if (::adapter.isInitialized) {
            updateAppList()
        }
    }

    fun selectAll() {
        if (::adapter.isInitialized) {
            adapter.selectAll()
            saveSelectedUids(adapter.getSelectedUids())
            // Пересортировываем список после выбора всех
            resortAppList(adapter.getSelectedUids())
        }
    }

    fun deselectAll() {
        if (::adapter.isInitialized) {
            adapter.deselectAll()
            saveSelectedUids(adapter.getSelectedUids())
            // Пересортировываем список после снятия выбора
            resortAppList(adapter.getSelectedUids())
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

        // Сначала очищаем ВСЕ старые правила для всех ранее выбранных приложений
        if (prevSelectedUids.isNotEmpty()) {
            val prevUidsString = prevSelectedUids.joinToString(" ")
            IptablesService.clearAllRulesForUids(requireContext(), prevUidsString)
        }

        // Затем применяем правила только для текущих выбранных приложений
        if (selectedUids.isNotEmpty()) {
            val uidsString = selectedUids.joinToString(" ")
            IptablesService.applyRules(requireContext(), uidsString)
        }
        
        // Сохраняем текущее состояние
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
        getPrefs().edit().putStringSet("selected_uids", uids).apply()
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
            adapter = AppListAdapter(sortedApps, prevSelected, requireContext().packageManager) { updatedUids ->
                saveSelectedUids(updatedUids)
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

        apps.forEach { app ->
            if (selectedUids.contains(app.uid.toString())) {
                selectedApps.add(app)
            } else {
                unselectedApps.add(app)
            }
        }

        // Сортируем каждую группу по имени
        selectedApps.sortBy { it.appName }
        unselectedApps.sortBy { it.appName }

        // Возвращаем: сначала выбранные, потом невыбранные
        return selectedApps + unselectedApps
    }

    // Метод для пересортировки списка при изменении выбора
    // Теперь используется только для операций selectAll/deselectAll и refreshSelectedStates
    private fun resortAppList(selectedUids: Set<String>) {
        if (!::adapter.isInitialized) return

        // Сохраняем текущую позицию скролла
        val layoutManager = binding.recyclerView.layoutManager
        val scrollPosition = if (layoutManager is androidx.recyclerview.widget.LinearLayoutManager) {
            layoutManager.findFirstVisibleItemPosition()
        } else {
            0
        }

        val currentApps = adapter.getCurrentApps()
        val sortedApps = sortAppsBySelection(currentApps, selectedUids)
        adapter.updateDataWithSort(sortedApps, selectedUids)

        // Восстанавливаем позицию скролла
        binding.recyclerView.post {
            if (scrollPosition != RecyclerView.NO_POSITION) {
                binding.recyclerView.scrollToPosition(minOf(scrollPosition, sortedApps.size - 1))
            }
        }
    }

    private fun getPrefs() = requireContext().getSharedPreferences("proxy_prefs", Context.MODE_PRIVATE)

    private fun getInstalledApps(showSystem: Boolean): List<AppInfo> {
        return try {
            val pm = requireContext().packageManager
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter {
                    if (showSystem) true else (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0
                }
                .filter { 
                    // Исключаем само приложение App2Proxy из списка
                    it.packageName != "dev.rx.app2proxy"
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
