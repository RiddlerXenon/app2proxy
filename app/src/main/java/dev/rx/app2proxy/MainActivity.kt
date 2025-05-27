package dev.rx.app2proxy

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.color.DynamicColors
import dev.rx.app2proxy.databinding.ActivityMainBinding
import kotlin.math.abs

class MainActivity : AppCompatActivity(), RulesUpdateListener {
    
    companion object {
        private const val TAG = "MainActivity"
        private const val SWIPE_THRESHOLD = 100
        private const val SWIPE_VELOCITY_THRESHOLD = 100
    }
    
    private lateinit var binding: ActivityMainBinding
    private var showSystemApps = false
    private var isSearchExpanded = false
    private lateinit var gestureDetector: GestureDetector
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Применяем тему до super.onCreate
        applySelectedTheme()
        
        super.onCreate(savedInstanceState)
        
        try {
            enableEdgeToEdge()
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            setupGestureDetector()
            setupToolbar()
            setupToolbarButtons()
            setupSearch()
            setupViewPager()
            setupBottomNavigation()
            
            // Применяем динамические цвета если включен Material You
            val prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)
            if (prefs.getBoolean("material_you", false) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                DynamicColors.applyToActivityIfAvailable(this)
            }
            
            Log.d(TAG, "✅ MainActivity создан")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка создания MainActivity", e)
        }
    }
    
    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (!isSearchExpanded) return false
                
                val diffX = e2.x - (e1?.x ?: 0f)
                val diffY = e2.y - (e1?.y ?: 0f)
                
                // Проверяем горизонтальный свайп (влево или вправо)
                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        // Закрываем поиск при свайпе влево или вправо
                        Log.d(TAG, "🔍 Поиск закрыт свайпом")
                        collapseSearch()
                        return true
                    }
                }
                // Проверяем вертикальный свайп вверх
                else if (diffY < -SWIPE_THRESHOLD && abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    // Закрываем поиск при свайпе вверх
                    Log.d(TAG, "🔍 Поиск закрыт свайпом вверх")
                    collapseSearch()
                    return true
                }
                
                return false
            }
        })
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }
    
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        // Обрабатываем жесты только если поиск развернут
        if (isSearchExpanded) {
            gestureDetector.onTouchEvent(ev)
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun applySelectedTheme() {
        try {
            val prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)
            val isDarkTheme = prefs.getBoolean("dark_theme", true)
            val isAmoledTheme = prefs.getBoolean("amoled_theme", false)
            
            if (isDarkTheme && isAmoledTheme) {
                setTheme(R.style.Theme_App2Proxy_Amoled)
            } else {
                // Используем основную тему, которая поддерживает DayNight
                setTheme(R.style.Theme_App2Proxy)
            }
            
            Log.d(TAG, "✅ Тема применена: dark=$isDarkTheme, amoled=$isAmoledTheme")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка применения темы", e)
        }
    }

    private fun applyAmoledStylesToToolbarButtons() {
        try {
            val whiteColor = ContextCompat.getColor(this, android.R.color.white)
            
            // Применяем белый цвет ко всем иконкам кнопок в toolbar
            binding.btnSearch.iconTint = android.content.res.ColorStateList.valueOf(whiteColor)
            binding.btnSettings.iconTint = android.content.res.ColorStateList.valueOf(whiteColor)
            binding.btnToggleSystemApps.iconTint = android.content.res.ColorStateList.valueOf(whiteColor)
            binding.btnCloseSearch.iconTint = android.content.res.ColorStateList.valueOf(whiteColor)
            
            // Применяем белый цвет к заголовку
            binding.toolbarTitle.setTextColor(whiteColor)
            
            Log.d(TAG, "✅ AMOLED стиль применен к кнопкам toolbar")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка применения AMOLED к кнопкам", e)
        }
    }

    private fun setupToolbar() {
        try {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.apply {
                // Скрываем стандартный заголовок, так как используем кастомный
                setDisplayShowTitleEnabled(false)
            }
            
            Log.d(TAG, "✅ Toolbar настроен")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка настройки toolbar", e)
        }
    }
    
    private fun setupToolbarButtons() {
        try {
            // Настройка кнопки поиска
            binding.btnSearch.setOnClickListener {
                expandSearch()
            }
            
            // Настройка кнопки закрытия поиска
            binding.btnCloseSearch.setOnClickListener {
                collapseSearch()
            }
            
            // Настройка кнопки переключения показа системных приложений
            updateSystemAppsButtonIcon()
            binding.btnToggleSystemApps.setOnClickListener {
                showSystemApps = !showSystemApps
                updateSystemAppsButtonIcon()
                getAppListFragment()?.setShowSystemApps(showSystemApps)
                
                val message = if (showSystemApps) "Показаны системные приложения" else "Скрыты системные приложения"
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
            
            // Настройка кнопки настроек
            binding.btnSettings.setOnClickListener {
                try {
                    startActivity(Intent(this, SettingsActivity::class.java))
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка открытия настроек", e)
                    Toast.makeText(this, "Ошибка открытия настроек", Toast.LENGTH_SHORT).show()
                }
            }
            
            Log.d(TAG, "✅ Кнопки toolbar настроены")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка настройки кнопок toolbar", e)
        }
    }

    private fun setupSearch() {
        try {
            // Настройка поля поиска
            binding.searchEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                
                override fun afterTextChanged(s: Editable?) {
                    val query = s?.toString() ?: ""
                    performSearch(query)
                }
            })
            
            // Обработка нажатия клавиши поиска на клавиатуре
            binding.searchEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                    hideKeyboard()
                    true
                } else {
                    false
                }
            }
            
            // Добавляем обработчик касаний для поля поиска
            binding.searchEditText.setOnTouchListener { _, event ->
                // Передаем касания детектору жестов, но не блокируем их
                gestureDetector.onTouchEvent(event)
                false // Возвращаем false, чтобы EditText продолжал обрабатывать касания
            }
            
            Log.d(TAG, "✅ Поиск настроен")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка настройки поиска", e)
        }
    }

    private fun expandSearch() {
        if (isSearchExpanded) return
        
        try {
            isSearchExpanded = true
            
            // Скрываем обычное состояние тулбара
            binding.toolbarNormalState.visibility = View.GONE
            
            // Показываем состояние поиска
            binding.toolbarSearchState.visibility = View.VISIBLE
            
            // Фокусируемся на поле поиска и показываем клавиатуру
            binding.searchEditText.requestFocus()
            showKeyboard()
            
            Log.d(TAG, "✅ Поиск развернут")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка разворачивания поиска", e)
        }
    }

    private fun collapseSearch() {
        if (!isSearchExpanded) return
        
        try {
            isSearchExpanded = false
            
            // Очищаем поле поиска
            binding.searchEditText.text?.clear()
            
            // Скрываем состояние поиска
            binding.toolbarSearchState.visibility = View.GONE
            
            // Показываем обычное состояние тулбара
            binding.toolbarNormalState.visibility = View.VISIBLE
            
            // Скрываем клавиатуру
            hideKeyboard()
            
            // Сбрасываем фильтр в списке приложений
            getAppListFragment()?.filterApps("")
            
            Log.d(TAG, "✅ Поиск свернут")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка сворачивания поиска", e)
        }
    }

    private fun performSearch(query: String) {
        try {
            // Выполняем поиск только на вкладке приложений
            if (binding.viewPager.currentItem == 0) {
                getAppListFragment()?.filterApps(query)
                
                if (query.isNotBlank()) {
                    val count = getAppListFragment()?.getFilteredAppsCount() ?: 0
                    Log.d(TAG, "🔍 Найдено $count приложений по запросу: $query")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка выполнения поиска", e)
        }
    }

    private fun showKeyboard() {
        try {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.searchEditText, InputMethodManager.SHOW_IMPLICIT)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка показа клавиатуры", e)
        }
    }

    private fun hideKeyboard() {
        try {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка скрытия клавиатуры", e)
        }
    }
    
    private fun updateSystemAppsButtonIcon() {
        try {
            val iconRes = if (showSystemApps) R.drawable.ic_visibility_24 else R.drawable.ic_visibility_off_24
            binding.btnToggleSystemApps.setIconResource(iconRes)
            
            val description = if (showSystemApps) "Скрыть системные приложения" else "Показать системные приложения"
            binding.btnToggleSystemApps.contentDescription = description
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка обновления иконки", e)
        }
    }

    private fun setupViewPager() {
        try {
            val adapter = ViewPagerAdapter(this)
            binding.viewPager.adapter = adapter
            Log.d(TAG, "✅ ViewPager настроен")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка настройки ViewPager", e)
        }
    }

    private fun setupBottomNavigation() {
        try {
            val bottomNavigation = binding.bottomNavigation
            
            bottomNavigation.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_apps -> {
                        binding.viewPager.currentItem = 0
                        // Показываем кнопку поиска только на вкладке приложений
                        binding.btnSearch.visibility = View.VISIBLE
                        true
                    }
                    R.id.nav_rules -> {
                        binding.viewPager.currentItem = 1
                        // Сворачиваем поиск и скрываем кнопку на других вкладках
                        if (isSearchExpanded) {
                            collapseSearch()
                        }
                        binding.btnSearch.visibility = View.GONE
                        true
                    }
                    else -> false
                }
            }

            // Синхронизируем ViewPager с BottomNavigation
            binding.viewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    when (position) {
                        0 -> {
                            bottomNavigation.selectedItemId = R.id.nav_apps
                            binding.btnSearch.visibility = View.VISIBLE
                        }
                        1 -> {
                            bottomNavigation.selectedItemId = R.id.nav_rules
                            if (isSearchExpanded) {
                                collapseSearch()
                            }
                            binding.btnSearch.visibility = View.GONE
                        }
                    }
                }
            })
            
            Log.d(TAG, "✅ BottomNavigation настроен")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка настройки BottomNavigation", e)
        }
    }

    override fun onResume() {
        super.onResume()
        
        // Применяем AMOLED стили при возврате в активность
        applyAmoledThemeIfNeeded()
        
        val prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)
        val useMaterialYou = prefs.getBoolean("material_you", false)
        val useAmoledTheme = prefs.getBoolean("amoled_theme", false)
        val isDarkTheme = prefs.getBoolean("dark_theme", true)
        
        if (useMaterialYou && useAmoledTheme && isDarkTheme && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Повторно применяем стили с задержкой
            binding.toolbar.postDelayed({
                AmoledDynamicColorScheme.applyAmoledToolbarStyle(binding.toolbar, this)
                applyAmoledStylesToToolbarButtons()
            }, 100)
        }    
    }

    override fun onBackPressed() {
        if (isSearchExpanded) {
            Log.d(TAG, "🔍 Поиск закрыт кнопкой Назад")
            collapseSearch()
        } else {
            // super.onBackPressed() 'onBackPressed(): Unit' is deprecated. Deprecated in Java
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun checkRulesConsistency() {
        try {
            val prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)
            val selectedUids = prefs.getStringSet("selected_uids", emptySet()) ?: emptySet()
            
            if (selectedUids.isNotEmpty()) {
                Log.d(TAG, "📋 Проверяем консистентность правил для ${selectedUids.size} приложений")
                
                val missingRules = mutableListOf<String>()
                
                for (uid in selectedUids) {
                    val hasOutputRule = checkOutputRuleExists(uid)
                    if (!hasOutputRule) {
                        missingRules.add(uid)
                    }
                }
                
                if (missingRules.isNotEmpty()) {
                    Log.w(TAG, "⚠️ Обнаружены отсутствующие правила для ${missingRules.size} приложений")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка проверки консистентности", e)
        }
    }

    private fun checkOutputRuleExists(uid: String): Boolean {
        try {
            val process = ProcessBuilder("su", "-c", "iptables -t nat -L OUTPUT -n")
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            
            return output.contains("owner UID match $uid")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка проверки правила для UID $uid", e)
            return false
        }
    }

    private fun getAppListFragment(): AppListFragment? {
        return try {
            supportFragmentManager.findFragmentByTag("f0") as? AppListFragment
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка получения AppListFragment", e)
            null
        }
    }

    private fun getRulesManagerFragment(): RulesManagerFragment? {
        return try {
            supportFragmentManager.findFragmentByTag("f1") as? RulesManagerFragment
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка получения RulesManagerFragment", e)
            null
        }
    }

    private fun applyAmoledThemeIfNeeded() {
        val prefs = getSharedPreferences("proxy_prefs", MODE_PRIVATE)
        val useAmoledTheme = prefs.getBoolean("amoled_theme", false)
        val isDarkTheme = prefs.getBoolean("dark_theme", true)
        val useMaterialYou = prefs.getBoolean("material_you", false)
        
        if (useAmoledTheme && isDarkTheme) {
            if (useMaterialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AmoledDynamicColorScheme.applyAmoledToolbarStyle(binding.toolbar, this)
            }
            applyAmoledStylesToToolbarButtons()
        }
    }

    override fun onRulesUpdated() {
        // Обновляем состояние чекбоксов в списке приложений
        getAppListFragment()?.refreshSelectedStates()
    }

    // Адаптер для ViewPager
    private inner class ViewPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> AppListFragment()
                1 -> {
                    val fragment = RulesManagerFragment()
                    fragment.setRulesUpdateListener(this@MainActivity)
                    fragment
                }
                else -> throw IllegalArgumentException("Invalid position: $position")
            }
        }
    }
}
