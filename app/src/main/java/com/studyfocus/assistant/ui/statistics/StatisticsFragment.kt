package com.studyfocus.assistant.ui.statistics

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.studyfocus.assistant.databinding.FragmentStatisticsBinding
import com.studyfocus.assistant.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    private val subjectColorMap = mapOf(
        "数学" to Color.rgb(255, 107, 138),
        "英语" to Color.rgb(129, 199, 132),
        "背单词" to Color.rgb(100, 181, 246),
        "专业课一" to Color.rgb(255, 183, 77),
        "专业课二" to Color.rgb(186, 104, 200),
        "物理" to Color.rgb(255, 143, 112),
        "化学" to Color.rgb(149, 117, 205),
        "其他" to Color.rgb(144, 164, 174)
    )

    private var pieYear = 0; private var pieMonth = 0; private var pieDay = 0
    private var todYear = 0; private var todMonth = 0; private var todDay = 0
    private var barYear = 0; private var barMonth = 0

    private val dateFmt = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    private val monFmt = SimpleDateFormat("yyyy年M月", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val now = Calendar.getInstance()
        pieYear = now.get(Calendar.YEAR); pieMonth = now.get(Calendar.MONTH) + 1; pieDay = now.get(Calendar.DAY_OF_MONTH)
        todYear = pieYear; todMonth = pieMonth; todDay = pieDay
        barYear = pieYear; barMonth = pieMonth

        setupPieNav(); setupRadioGroup(); setupTodNav(); setupBarNav(); setupCheckIn()
        observeData()
        viewModel.refreshAll()
        refreshCurrentPeriod()
        refreshTodView()
        refreshBarView()
    }

    private fun setupPieNav() {
        binding.btnPiePrev.setOnClickListener { pieNavigate(-1) }
        binding.btnPieNext.setOnClickListener { pieNavigate(1) }
    }

    private fun setupTodNav() {
        binding.btnTodPrev.setOnClickListener { todNavigate(-1) }
        binding.btnTodNext.setOnClickListener { todNavigate(1) }
    }

    private fun setupBarNav() {
        binding.btnBarPrev.setOnClickListener { barNavigate(-1) }
        binding.btnBarNext.setOnClickListener { barNavigate(1) }
    }

    private fun setupRadioGroup() {
        binding.rgPeriod.setOnCheckedChangeListener { _, _ ->
            val now = Calendar.getInstance()
            pieYear = now.get(Calendar.YEAR); pieMonth = now.get(Calendar.MONTH) + 1; pieDay = now.get(Calendar.DAY_OF_MONTH)
            refreshCurrentPeriod()
        }
    }

    private fun pieNavigate(delta: Int) {
        when {
            binding.rbDay.isChecked -> { val cal = Calendar.getInstance().apply { set(pieYear, pieMonth-1, pieDay) }; cal.add(Calendar.DAY_OF_MONTH, delta); pieYear = cal.get(Calendar.YEAR); pieMonth = cal.get(Calendar.MONTH)+1; pieDay = cal.get(Calendar.DAY_OF_MONTH) }
            binding.rbWeek.isChecked -> { val cal = Calendar.getInstance().apply { set(pieYear, pieMonth-1, pieDay) }; cal.add(Calendar.DAY_OF_YEAR, delta * 7); pieYear = cal.get(Calendar.YEAR); pieMonth = cal.get(Calendar.MONTH)+1; pieDay = cal.get(Calendar.DAY_OF_MONTH) }
            binding.rbMonth.isChecked -> { pieMonth += delta; if (pieMonth > 12) { pieMonth = 1; pieYear++ }; if (pieMonth < 1) { pieMonth = 12; pieYear-- }; pieDay = 1 }
        }
        refreshCurrentPeriod()
    }

    private fun refreshCurrentPeriod() {
        val (start, end) = when {
            binding.rbDay.isChecked -> {
                val cal = Calendar.getInstance().apply { set(pieYear, pieMonth-1, pieDay); set(Calendar.HOUR_OF_DAY,0); set(Calendar.MINUTE,0); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0) }
                val s = cal.timeInMillis; cal.set(Calendar.HOUR_OF_DAY,23); cal.set(Calendar.MINUTE,59); cal.set(Calendar.SECOND,59); cal.set(Calendar.MILLISECOND,999)
                binding.tvPieRange.text = "${pieYear}/%02d/%02d".format(pieMonth, pieDay); s to cal.timeInMillis
            }
            binding.rbWeek.isChecked -> {
                val cal = Calendar.getInstance().apply { set(pieYear, pieMonth-1, pieDay); set(Calendar.HOUR_OF_DAY,0); set(Calendar.MINUTE,0); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0) }
                val toMon = if (cal.get(Calendar.DAY_OF_WEEK)==Calendar.SUNDAY) -6 else -(cal.get(Calendar.DAY_OF_WEEK)-Calendar.MONDAY)
                cal.add(Calendar.DAY_OF_YEAR, toMon); val s = cal.timeInMillis; val ws = dateFmt.format(cal.time)
                cal.add(Calendar.DAY_OF_YEAR, 6); cal.set(Calendar.HOUR_OF_DAY,23); cal.set(Calendar.MINUTE,59); cal.set(Calendar.SECOND,59); cal.set(Calendar.MILLISECOND,999)
                binding.tvPieRange.text = "$ws - ${dateFmt.format(cal.time)}"; s to cal.timeInMillis
            }
            else -> {
                val cal = Calendar.getInstance().apply { set(pieYear, pieMonth-1, 1); set(Calendar.HOUR_OF_DAY,0); set(Calendar.MINUTE,0); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0) }
                val s = cal.timeInMillis; cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH)); cal.set(Calendar.HOUR_OF_DAY,23); cal.set(Calendar.MINUTE,59); cal.set(Calendar.SECOND,59); cal.set(Calendar.MILLISECOND,999)
                binding.tvPieRange.text = monFmt.format(cal.time); s to cal.timeInMillis
            }
        }
        viewModel.refreshSubjectStatsRange(start, end)
    }

    private fun todNavigate(delta: Int) {
        val cal = Calendar.getInstance().apply { set(todYear, todMonth-1, todDay) }
        cal.add(Calendar.DAY_OF_MONTH, delta)
        todYear = cal.get(Calendar.YEAR); todMonth = cal.get(Calendar.MONTH)+1; todDay = cal.get(Calendar.DAY_OF_MONTH)
        refreshTodView()
    }

    private fun refreshTodView() {
        val now = Calendar.getInstance()
        val today = now.get(Calendar.YEAR)==todYear && now.get(Calendar.MONTH)+1==todMonth && now.get(Calendar.DAY_OF_MONTH)==todDay
        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -1) }
        val yesterday = yesterdayCal.get(Calendar.YEAR)==todYear && yesterdayCal.get(Calendar.MONTH)+1==todMonth && yesterdayCal.get(Calendar.DAY_OF_MONTH)==todDay
        binding.tvTodLabel.text = when {
            today -> "今天"; yesterday -> "昨天"
            else -> "${todYear}/%02d/%02d".format(todMonth, todDay)
        }
        viewModel.refreshTimeOfDayForDay(todYear, todMonth, todDay)
    }

    private fun barNavigate(delta: Int) {
        barMonth += delta
        if (barMonth > 12) { barMonth = 1; barYear++ }
        if (barMonth < 1) { barMonth = 12; barYear-- }
        refreshBarView()
    }

    private fun refreshBarView() {
        binding.tvBarMonth.text = "${barYear}年${barMonth}月"
        viewModel.refreshDailyStatsForMonth(barYear, barMonth)
    }

    private fun setupCheckIn() {
        binding.btnCheckIn.setOnClickListener { viewModel.checkIn() }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.totalStats.observe(viewLifecycleOwner) { stats ->
                    binding.tvTotalTimes.text = "${stats.first}"
                    binding.tvTotalDuration.text = formatBig(stats.second)
                    binding.tvAvgDaily.text = formatBig(stats.third.toInt())
                }
                viewModel.todayStats.observe(viewLifecycleOwner) { stats ->
                    val cal = Calendar.getInstance()
                    binding.tvTodayDate.text = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(cal.time)
                    binding.tvTodayTimes.text = "${stats.first}"
                    binding.tvTodayDuration.text = formatBig(stats.second)
                }
                viewModel.subjectStats.observe(viewLifecycleOwner) { stats ->
                    setupPieChart(stats)
                    if (stats.isEmpty()) binding.tvPieTotal.text = "总计 0min"
                    else binding.tvPieTotal.text = "总计 ${formatBig(stats.sumOf { it.totalMinutes })}"
                }
                viewModel.dailyStats.observe(viewLifecycleOwner) { stats -> setupBarChart(stats) }
                viewModel.checkedInToday.observe(viewLifecycleOwner) { checked ->
                    binding.btnCheckIn.visibility = if (checked) View.GONE else View.VISIBLE
                    binding.tvCheckedIn.visibility = if (checked) View.VISIBLE else View.GONE
                }
                viewModel.checkInStreak.observe(viewLifecycleOwner) { binding.tvStreak.text = "$it" }
                viewModel.totalCheckIns.observe(viewLifecycleOwner) { binding.tvTotalCheckins.text = "$it" }
                viewModel.weeklyFocusMinutes.observe(viewLifecycleOwner) { binding.tvWeeklyFocus.text = formatBig(it) }
                viewModel.timeOfDaySlots.observe(viewLifecycleOwner) { buildHeatmap(it) }
                viewModel.barChartSummary.observe(viewLifecycleOwner) { (total, maxPair, avg) ->
                    binding.tvBarTotal.text = "总计 ${formatBig(total)}"
                    binding.tvBarMax.text = if (maxPair.second > 0) "最高 ${formatBig(maxPair.second)}" else "最高 -"
                    binding.tvBarAvg.text = "日均 ${formatBig(avg)}"
                }
            }
        }
    }

    private fun buildHeatmap(slots: Map<Int, Int>) {
        binding.layoutTodHeatmap.removeAllViews()
        binding.layoutTodLegend.removeAllViews()

        val labels = listOf("0-2", "2-4", "4-6", "6-8", "8-10", "10-12", "12-14", "14-16", "16-18", "18-20", "20-22", "22-24")
        val allMinutes = slots.values.toList()
        val maxMins = allMinutes.maxOrNull()?.coerceAtLeast(1) ?: 1

        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44))
        }
        val cellSize = dp(24)

        for (i in 0..11) {
            val inner = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT).apply { weight = 1f }
            }
            val cell = View(requireContext()).apply {
                val mins = slots[i * 2] ?: 0
                setBackgroundColor(cellColor(mins, maxMins))
                layoutParams = LinearLayout.LayoutParams(cellSize, cellSize)
            }
            inner.addView(cell)
            val lbl = TextView(requireContext()).apply {
                text = labels[i]; textSize = 9f; setTextColor(Color.parseColor("#AAAAAA"))
                gravity = Gravity.CENTER; layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            inner.addView(lbl)
            row.addView(inner)
        }
        binding.layoutTodHeatmap.addView(row)

        val legendItems = listOf("0min" to "#FFEEEEEE", "1-15" to "#FFC8E6C9", "16-30" to "#FF81C784", "31-60" to "#FF4CAF50", "60+" to "#FF2E7D32")
        for ((txt, clr) in legendItems) {
            val dot = View(requireContext()).apply {
                setBackgroundColor(Color.parseColor(clr))
                layoutParams = LinearLayout.LayoutParams(dp(10), dp(10)).apply { marginEnd = dp(3) }
            }
            binding.layoutTodLegend.addView(dot)
            val lt = TextView(requireContext()).apply {
                text = txt; textSize = 9f; setTextColor(Color.parseColor("#AAAAAA"))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginEnd = dp(8) }
            }
            binding.layoutTodLegend.addView(lt)
        }
    }

    private fun cellColor(mins: Int, max: Int): Int {
        return if (mins <= 0) Color.parseColor("#FFEEEEEE")
        else {
            val ratio = mins.toFloat() / max
            val r = (220 - 46 * ratio).toInt()
            val g = (240 - 139 * ratio).toInt()
            val b = (220 - 160 * ratio).toInt()
            Color.rgb(r, g, b)
        }
    }

    private fun setupPieChart(stats: List<com.studyfocus.assistant.data.entity.SubjectStat>) {
        val entries = stats.map { PieEntry(it.totalMinutes.toFloat(), it.subject) }
        if (entries.isEmpty()) { binding.pieChart.clear(); binding.pieChart.setNoDataText("暂无数据"); return }
        val dataSet = PieDataSet(entries, "").apply {
            sliceSpace = 3f; selectionShift = 5f; colors = stats.map { subjectColorMap[it.subject] ?: Color.GRAY }
            valueTextSize = 12f; valueTextColor = Color.WHITE; valueFormatter = PercentFormatter(binding.pieChart)
        }
        binding.pieChart.apply {
            data = PieData(dataSet); setUsePercentValues(true); description.isEnabled = false
            isDrawHoleEnabled = true; setHoleColor(Color.WHITE); holeRadius = 40f; transparentCircleRadius = 45f
            setDrawEntryLabels(false); legend.isEnabled = true; legend.textSize = 12f; legend.textColor = Color.parseColor("#8E8E93")
            legend.orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.VERTICAL
            legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.CENTER
            legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.RIGHT
            setExtraOffsets(5f, 10f, 5f, 5f); animateY(800); invalidate()
        }
    }

    private fun setupBarChart(stats: List<com.studyfocus.assistant.data.entity.DailyStat>) {
        if (stats.isEmpty()) { binding.barChart.clear(); binding.barChart.setNoDataText("暂无数据"); return }
        val entries = stats.mapIndexed { i, s -> BarEntry(i.toFloat(), s.totalMinutes.toFloat()) }
        val labels = stats.map { it.date }
        val dataSet = BarDataSet(entries, "").apply {
            color = Color.rgb(244, 143, 177); valueTextSize = 10f; valueTextColor = Color.parseColor("#8E8E93")
        }
        binding.barChart.apply {
            data = BarData(dataSet).apply { barWidth = 0.6f }; description.isEnabled = false; setFitBars(true)
            xAxis.apply { valueFormatter = IndexAxisValueFormatter(labels); position = XAxis.XAxisPosition.BOTTOM; setDrawGridLines(false); granularity = 1f; labelRotationAngle = -45f; textColor = Color.parseColor("#8E8E93"); textSize = 10f }
            axisLeft.apply { setDrawGridLines(true); gridColor = Color.parseColor("#E8E8EC"); textColor = Color.parseColor("#8E8E93"); textSize = 10f; axisMinimum = 0f }
            axisRight.isEnabled = false; legend.isEnabled = false
            setTouchEnabled(true); setPinchZoom(false); isDoubleTapToZoomEnabled = false; animateY(800); invalidate()
        }
    }

    private fun formatBig(minutes: Int): String {
        return if (minutes >= 60) { val h = minutes/60; val m = minutes%60; if (m > 0) "${h}h${m}min" else "${h}h" } else "${minutes}min"
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
