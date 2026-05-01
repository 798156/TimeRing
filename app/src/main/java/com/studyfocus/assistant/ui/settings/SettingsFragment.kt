package com.studyfocus.assistant.ui.settings

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.studyfocus.assistant.R
import com.studyfocus.assistant.databinding.FragmentSettingsBinding
import com.studyfocus.assistant.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    private val dayNames = listOf("一", "二", "三", "四", "五", "六", "日")
    private val allCheckedDates = mutableSetOf<String>()

    private var calYear: Int = 0
    private var calMonth: Int = 0

    private val themes = listOf(
        Triple("🌸", "粉色", intArrayOf(-3800609, -394758)),  // #FFC5E1, #FFF48FB1
        Triple("🍃", "薄荷", intArrayOf(-8088268, -16743746)), // #ff81c784, #ff43a047
        Triple("🌊", "天蓝", intArrayOf(-10170888, -15254104)),  // #ff64b5f6, #ff1e88e5
        Triple("🍊", "暖橘", intArrayOf(-36532, -3547648)),      // #ffffb74d, #fff57c00
        Triple("🌙", "暗夜", intArrayOf(-7969140, -9545425))     // #ff78909c, #ff455a64
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val now = Calendar.getInstance()
        calYear = now.get(Calendar.YEAR)
        calMonth = now.get(Calendar.MONTH) + 1

        buildThemeButtons()
        binding.btnCalPrev.setOnClickListener { changeMonth(-1) }
        binding.btnCalNext.setOnClickListener { changeMonth(1) }
        binding.tvClearData.setOnClickListener { showClearDataDialog() }

        observeData()
        viewModel.refreshAll()
    }

    private fun buildThemeButtons() {
        binding.layoutThemeColors.removeAllViews()
        val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        val current = prefs.getInt("theme", 0)

        for ((i, theme) in themes.withIndex()) {
            val ball = TextView(requireContext()).apply {
                val bg = GradientDrawable()
                bg.shape = GradientDrawable.OVAL
                bg.setSize(dp(40), dp(40))
                val colors = theme.third
                bg.colors = intArrayOf(colors[0], colors[1])
                bg.gradientType = GradientDrawable.LINEAR_GRADIENT
                bg.orientation = GradientDrawable.Orientation.TL_BR
                background = bg
                text = theme.first
                textSize = 18f
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)).apply {
                    setMargins(dp(8), 0, dp(8), 0)
                }
                if (i == current) {
                    val border = GradientDrawable()
                    border.shape = GradientDrawable.OVAL
                    border.setStroke(dp(3), colors[1])
                    border.setColor(Color.TRANSPARENT)
                    background = bg
                    // overlay border
                }

                setOnClickListener {
                    prefs.edit().putInt("theme", i).apply()
                    activity?.recreate()
                }
            }
            binding.layoutThemeColors.addView(ball)
        }
    }

    private fun changeMonth(delta: Int) {
        calMonth += delta
        if (calMonth > 12) { calMonth = 1; calYear++ }
        if (calMonth < 1) { calMonth = 12; calYear-- }
        updateCalendar()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.weeklyFocusMinutes.observe(viewLifecycleOwner) { mins ->
                    binding.tvWeekFocus.text = formatBig(mins)
                }
                viewModel.lastWeekFocusMinutes.observe(viewLifecycleOwner) { mins ->
                    binding.tvLastWeekFocus.text = formatBig(mins)
                    val thisWeek = viewModel.weeklyFocusMinutes.value ?: 0
                    if (thisWeek > mins) {
                        binding.tvWeekCompare.text = "🔥${formatBig(thisWeek - mins)}"
                    } else if (mins > thisWeek && mins > 0) {
                        binding.tvWeekCompare.text = "💪+${formatBig(mins - thisWeek)}"
                    } else {
                        binding.tvWeekCompare.text = "—"
                    }
                }
                viewModel.allCheckIns.observe(viewLifecycleOwner) { dates ->
                    allCheckedDates.clear()
                    allCheckedDates.addAll(dates)
                    updateCalendar()
                }
            }
        }
    }

    private fun updateCalendar() {
        binding.tvCalMonth.text = "${calYear}年${calMonth}月"
        buildCalendar(allCheckedDates, calYear, calMonth)
    }

    private fun buildCalendar(checkedDates: Set<String>, year: Int, month: Int) {
        binding.layoutWeekHeader.removeAllViews()
        binding.layoutCalendarGrid.removeAllViews()

        for (day in dayNames) {
            val tv = TextView(requireContext()).apply {
                text = day
                textSize = 11f
                setTextColor(Color.DKGRAY)
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, dp(30), 1f)
            }
            binding.layoutWeekHeader.addView(tv)
        }

        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1)
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val offset = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2

        val todayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = todayFmt.format(Calendar.getInstance().time)
        val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val totalCells = offset + maxDay
        val totalRows = (totalCells + 6) / 7
        val fullCells = totalRows * 7

        var dayCounter = 1
        var row: LinearLayout? = null

        for (i in 0 until fullCells) {
            if (i % 7 == 0) {
                row = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(34))
                }
                binding.layoutCalendarGrid.addView(row)
            }

            val tv = TextView(requireContext()).apply {
                gravity = Gravity.CENTER
                textSize = 12f
                layoutParams = LinearLayout.LayoutParams(0, dp(30), 1f)
            }

            if (i >= offset && dayCounter <= maxDay) {
                val tempCal = Calendar.getInstance().apply { set(year, month - 1, dayCounter) }
                val dateStr = dayFmt.format(tempCal.time)
                tv.text = "$dayCounter"
                dayCounter++

                if (dateStr == todayStr) {
                    tv.setTextColor(Color.WHITE)
                    tv.background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setSize(dp(30), dp(30))
                        setColor(Color.parseColor("#EC407A"))
                    }
                } else if (checkedDates.contains(dateStr)) {
                    tv.background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL; setSize(dp(30), dp(30))
                        setColor(Color.parseColor("#2AF8BBD0"))
                        setStroke(dp(1), Color.parseColor("#FFF48FB1"))
                    }
                    tv.setTextColor(Color.parseColor("#D81B60"))
                } else tv.setTextColor(Color.parseColor("#8E8E93"))
            } else { tv.text = ""; tv.setTextColor(Color.TRANSPARENT) }

            row!!.addView(tv)
        }
    }

    private fun showClearDataDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("清空数据")
            .setMessage("确认清空所有专注数据吗？此操作不可恢复。")
            .setPositiveButton("确认") { _, _ ->
                viewModel.clearAllData()
                Toast.makeText(requireContext(), "数据已清空", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun formatBig(minutes: Int): String {
        return if (minutes >= 60) {
            val h = minutes / 60; val m = minutes % 60
            if (m > 0) "${h}h${m}min" else "${h}h"
        } else "${minutes}min"
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
