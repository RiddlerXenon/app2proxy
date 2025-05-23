package dev.rx.app2proxy

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.rx.app2proxy.databinding.ItemAppBinding

class AppListAdapter(
    private var apps: List<AppInfo>,
    selectedUids: Set<String>,
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
        holder.binding.checkBox.apply {
            text = "${app.appName} (${app.packageName})"
            setOnCheckedChangeListener(null)
            isChecked = selected.contains(app.uid.toString())
            setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selected.add(app.uid.toString()) else selected.remove(app.uid.toString())
                onSelectedChanged(selected)
            }
        }
    }

    fun getSelectedUids(): Set<String> = selected

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

    class AppViewHolder(val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root)
}
