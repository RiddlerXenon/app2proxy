package dev.rx.app2proxy

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import dev.rx.app2proxy.databinding.FragmentRulesManagerBinding

class RulesManagerFragment : Fragment() {
    private var _binding: FragmentRulesManagerBinding? = null
    private val binding get() = _binding!!
    private lateinit var rulesAdapter: RulesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRulesManagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        loadCurrentRules()
    }

    private fun setupViews() {
        binding.swipeRefreshRules.setOnRefreshListener {
            loadCurrentRules()
            binding.swipeRefreshRules.isRefreshing = false
        }

        binding.btnShowCurrentRules.setOnClickListener {
            loadCurrentRules()
        }

        binding.btnClearAllRules.setOnClickListener {
            clearAllRules()
        }

        binding.btnRemoveSelected.setOnClickListener {
            removeSelectedRules()
        }

        // Инициализируем адаптер
        rulesAdapter = RulesAdapter { _ ->
            // Callback для обновления списка выбранных правил
            // Пустая реализация, так как обновление происходит автоматически
        }
        binding.recyclerViewRules.adapter = rulesAdapter
    }

    private fun loadCurrentRules() {
        val prefs = requireContext().getSharedPreferences("proxy_prefs", Context.MODE_PRIVATE)
        val selectedUids = prefs.getStringSet("selected_uids", emptySet()) ?: emptySet()
        
        if (selectedUids.isEmpty()) {
            rulesAdapter.updateRules(emptyList())
            Toast.makeText(requireContext(), "Нет активных правил", Toast.LENGTH_SHORT).show()
            return
        }

        val pm = requireContext().packageManager
        val rulesList = selectedUids.mapNotNull { uid ->
            try {
                // Находим приложение по UID
                val packages = pm.getPackagesForUid(uid.toInt())
                val packageName = packages?.firstOrNull()
                if (packageName != null) {
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    RuleInfo(
                        uid = uid,
                        appName = appInfo.loadLabel(pm).toString(),
                        packageName = packageName,
                        isActive = true
                    )
                } else {
                    RuleInfo(
                        uid = uid,
                        appName = "Неизвестное приложение",
                        packageName = "unknown",
                        isActive = true
                    )
                }
            } catch (e: Exception) {
                null
            }
        }

        rulesAdapter.updateRules(rulesList)
        Toast.makeText(requireContext(), "Загружено ${rulesList.size} активных правил", Toast.LENGTH_SHORT).show()
    }

    private fun clearAllRules() {
        val prefs = requireContext().getSharedPreferences("proxy_prefs", Context.MODE_PRIVATE)
        val selectedUids = prefs.getStringSet("selected_uids", emptySet()) ?: emptySet()
        
        if (selectedUids.isEmpty()) {
            Toast.makeText(requireContext(), "Нет правил для удаления", Toast.LENGTH_SHORT).show()
            return
        }

        val uidsString = selectedUids.joinToString(" ")
        IptablesService.clearRules(uidsString)
        
        // Очищаем сохраненные UID
        prefs.edit().putStringSet("selected_uids", emptySet()).apply()
        
        // Обновляем список
        rulesAdapter.updateRules(emptyList())
        
        Toast.makeText(requireContext(), "Все правила удалены", Toast.LENGTH_SHORT).show()
    }

    private fun removeSelectedRules() {
        val selectedRules = rulesAdapter.getSelectedRules()
        
        if (selectedRules.isEmpty()) {
            Toast.makeText(requireContext(), "Выберите правила для удаления", Toast.LENGTH_SHORT).show()
            return
        }

        val uidsToRemove = selectedRules.map { it.uid }
        val uidsString = uidsToRemove.joinToString(" ")
        
        // Удаляем правила iptables
        IptablesService.clearRules(uidsString)
        
        // Обновляем сохраненные UID
        val prefs = requireContext().getSharedPreferences("proxy_prefs", Context.MODE_PRIVATE)
        val currentUids = prefs.getStringSet("selected_uids", emptySet())?.toMutableSet() ?: mutableSetOf()
        currentUids.removeAll(uidsToRemove.toSet())
        prefs.edit().putStringSet("selected_uids", currentUids).apply()
        
        // Обновляем список
        loadCurrentRules()
        
        Toast.makeText(requireContext(), "Удалено ${selectedRules.size} правил", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class RuleInfo(
    val uid: String,
    val appName: String,
    val packageName: String,
    val isActive: Boolean
)
