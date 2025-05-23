package dev.rx.app2proxy

import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import dev.rx.app2proxy.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)

        binding.switchAutostart.isChecked = prefs.getBoolean("autostart", false)
        binding.switchAutostart.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("autostart", isChecked).apply()
        }

        binding.switchTheme.isChecked = prefs.getBoolean("dark_theme", true)
        binding.switchTheme.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_theme", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        // Переключатель Material You
        binding.switchMaterialYou.isEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        binding.switchMaterialYou.isChecked = prefs.getBoolean("material_you", false)
        binding.switchMaterialYou.setOnCheckedChangeListener { _, isChecked ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                prefs.edit().putBoolean("material_you", isChecked).apply()
                Toast.makeText(this, "Material You будет применён после перезапуска приложения", Toast.LENGTH_SHORT).show()
            } else {
                binding.switchMaterialYou.isChecked = false
                Toast.makeText(this, "Material You доступен только на Android 12 и выше", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
