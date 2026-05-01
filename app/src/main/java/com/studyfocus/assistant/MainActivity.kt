package com.studyfocus.assistant

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.studyfocus.assistant.databinding.ActivityMainBinding
import com.studyfocus.assistant.ui.settings.SettingsFragment
import com.studyfocus.assistant.ui.statistics.StatisticsFragment
import com.studyfocus.assistant.ui.timer.TimerFragment
import com.studyfocus.assistant.ui.todo.TodoFragment
import com.studyfocus.assistant.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        val themeIdx = getSharedPreferences("settings", Context.MODE_PRIVATE).getInt("theme", 0)
        val themeRes = when (themeIdx) {
            1 -> R.style.Theme_StudyFocus_Mint
            2 -> R.style.Theme_StudyFocus_Ocean
            3 -> R.style.Theme_StudyFocus_Sunset
            4 -> R.style.Theme_StudyFocus_Midnight
            else -> R.style.Theme_StudyFocus
        }
        setTheme(themeRes)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        binding.viewPager.adapter = ViewPagerAdapter(this)
        binding.viewPager.offscreenPageLimit = 4

        val startPage = if (intent?.getBooleanExtra("nav_to_todo", false) == true) 0 else 1
        binding.viewPager.setCurrentItem(startPage, false)

        binding.viewPager.registerOnPageChangeCallback(object :
            androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val id = when (position) {
                    0 -> R.id.nav_todo
                    1 -> R.id.nav_timer
                    2 -> R.id.nav_statistics
                    3 -> R.id.nav_mine
                    else -> R.id.nav_timer
                }
                binding.bottomNav.selectedItemId = id
                if (position == 2) viewModel.refreshAll()
            }
        })

        binding.bottomNav.setOnItemSelectedListener { item ->
            val pos = when (item.itemId) {
                R.id.nav_todo -> 0
                R.id.nav_timer -> 1
                R.id.nav_statistics -> 2
                R.id.nav_mine -> 3
                else -> 1
            }
            binding.viewPager.setCurrentItem(pos, true)
            if (pos == 2) viewModel.refreshAll()
            true
        }

        showPermissionGuideIfNeeded()
    }

    private fun showPermissionGuideIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        if (prefs.getBoolean("permission_guide_shown", false)) return

        val alarmMgr = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (alarmMgr.canScheduleExactAlarms()) {
            prefs.edit().putBoolean("permission_guide_shown", true).apply()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("开启提醒权限")
            .setMessage("时环需要「闹钟和提醒」权限，才能在待办截止时间到达时通知你。\n\n点击「去设置」→ 找到「时环」→ 打开开关即可。")
            .setPositiveButton("去设置") { _, _ ->
                prefs.edit().putBoolean("permission_guide_shown", true).apply()
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = android.net.Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
            .setNegativeButton("稍后") { _, _ ->
                prefs.edit().putBoolean("permission_guide_shown", true).apply()
            }
            .setCancelable(false)
            .show()
    }

    private class ViewPagerAdapter(activity: MainActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 4
        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> TodoFragment()
            1 -> TimerFragment()
            2 -> StatisticsFragment()
            3 -> SettingsFragment()
            else -> TimerFragment()
        }
    }
}
