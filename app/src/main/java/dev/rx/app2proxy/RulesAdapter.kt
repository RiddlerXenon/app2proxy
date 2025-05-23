package dev.rx.app2proxy

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.rx.app2proxy.databinding.ItemRuleBinding

class RulesAdapter(
    private val onSelectedChanged: (Set<RuleInfo>) -> Unit
) : RecyclerView.Adapter<RulesAdapter.RuleViewHolder>() {
    
    private var rules: List<RuleInfo> = emptyList()
    private val selectedRules = mutableSetOf<RuleInfo>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RuleViewHolder {
        val binding = ItemRuleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RuleViewHolder(binding)
    }

    override fun getItemCount(): Int = rules.size

    override fun onBindViewHolder(holder: RuleViewHolder, position: Int) {
        val rule = rules[position]
        holder.binding.apply {
            textAppName.text = rule.appName
            textPackageName.text = rule.packageName
            textUid.text = "UID: ${rule.uid}"
            
            checkBox.setOnCheckedChangeListener(null)
            checkBox.isChecked = selectedRules.contains(rule)
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedRules.add(rule)
                } else {
                    selectedRules.remove(rule)
                }
                onSelectedChanged(selectedRules)
            }
            
            root.setOnClickListener {
                checkBox.toggle()
            }
        }
    }

    fun updateRules(newRules: List<RuleInfo>) {
        this.rules = newRules
        selectedRules.clear()
        notifyDataSetChanged()
    }

    fun getSelectedRules(): Set<RuleInfo> = selectedRules

    class RuleViewHolder(val binding: ItemRuleBinding) : RecyclerView.ViewHolder(binding.root)
}
