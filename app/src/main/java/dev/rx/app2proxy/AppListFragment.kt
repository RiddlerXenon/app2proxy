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
        }
    }

    fun deselectAll() {
        if (::adapter.isInitialized) {
            adapter.deselectAll()
            saveSelectedUids(adapter.getSelectedUids())
        }
    }

    // Новый метод для обновления состояния чекбоксов после изменения правил
    fun refreshSelectedStates() {
        if (::adapter.isInitialized) {
            val currentSelected = getPrefs().getStringSet("selected_uids", emptySet()) ?: emptySet()
            adapter.updateSelectedStates(currentSelected)
        }
    }

    private fun applyProxyRules() {
        if (!::adapter.isInitialized) {
            Toast.makeText(requireContext(), "Список приложений не загружен", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedUids = adapter.getSelectedUids()
        val prevSelectedUids = getPrefs().getStringSet("selected_uids", emptySet()) ?: emptySet()

        if (selectedUids.isNotEmpty()) {
            val uidsString = selectedUids.joinToString(" ")

            // Находим UID для удаления (которые были выбраны ранее, но больше не выбраны)
            val uidsToRemove = prevSelectedUids - selectedUids
            if (uidsToRemove.isNotEmpty()) {
                val uidsToRemoveString = uidsToRemove.joinToString(" ")
                IptablesService.clearRules(uidsToRemoveString)
            }
            
            // Применяем правила для текущих выбранных приложений
            IptablesService.applyRules(uidsString)
            
            Toast.makeText(requireContext(), 
                "Правила применены для ${selectedUids.size} приложений" + 
                if (uidsToRemove.isNotEmpty()) ", удалены для ${uidsToRemove.size}" else "", 
                Toast.LENGTH_SHORT).show()
        } else {
            // Если ничего не выбрано, показываем сообщение только если и раньше ничего не было выбрано
            if (prevSelectedUids.isEmpty()) {
                Toast.makeText(requireContext(), "Выберите хотя бы одно приложение", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Все правила удалены", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Сохраняем текущее состояние
        saveSelectedUids(selectedUids)
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
        
        if (!::adapter.isInitialized) {
            adapter = AppListAdapter(apps, prevSelected, requireContext().packageManager) { updatedUids ->
                saveSelectedUids(updatedUids)
            }
            binding.recyclerView.adapter = adapter
        } else {
            adapter.updateData(apps, prevSelected)
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
                .sortedBy { it.appName }
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
