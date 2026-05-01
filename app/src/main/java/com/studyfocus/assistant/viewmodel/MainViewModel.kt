package com.studyfocus.assistant.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.studyfocus.assistant.StudyApp
import com.studyfocus.assistant.data.entity.DailyStat
import com.studyfocus.assistant.data.entity.FocusRecord
import com.studyfocus.assistant.data.entity.SubjectStat
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as StudyApp
    private val repository = app.repository
    private val checkInRepo = app.checkInRepository

    private val _allRecords = MutableLiveData<List<FocusRecord>>()
    val allRecords: LiveData<List<FocusRecord>> = _allRecords

    private val _totalStats = MutableLiveData<Triple<Int, Int, Float>>()
    val totalStats: LiveData<Triple<Int, Int, Float>> = _totalStats

    private val _todayStats = MutableLiveData<Pair<Int, Int>>()
    val todayStats: LiveData<Pair<Int, Int>> = _todayStats

    private val _subjectStats = MutableLiveData<List<SubjectStat>>()
    val subjectStats: LiveData<List<SubjectStat>> = _subjectStats

    private val _dailyStats = MutableLiveData<List<DailyStat>>()
    val dailyStats: LiveData<List<DailyStat>> = _dailyStats

    private val _checkedInToday = MutableLiveData<Boolean>()
    val checkedInToday: LiveData<Boolean> = _checkedInToday

    private val _checkInStreak = MutableLiveData<Int>()
    val checkInStreak: LiveData<Int> = _checkInStreak

    private val _totalCheckIns = MutableLiveData<Int>()
    val totalCheckIns: LiveData<Int> = _totalCheckIns

    private val _weeklyFocusMinutes = MutableLiveData<Int>()
    val weeklyFocusMinutes: LiveData<Int> = _weeklyFocusMinutes

    private val _lastWeekFocusMinutes = MutableLiveData<Int>()
    val lastWeekFocusMinutes: LiveData<Int> = _lastWeekFocusMinutes

    private val _allCheckIns = MutableLiveData<Set<String>>()
    val allCheckIns: LiveData<Set<String>> = _allCheckIns

    private val _timeOfDaySlots = MutableLiveData<Map<Int, Int>>()
    val timeOfDaySlots: LiveData<Map<Int, Int>> = _timeOfDaySlots

    private val _barChartSummary = MutableLiveData<Triple<Int, Pair<String, Int>, Int>>()
    val barChartSummary: LiveData<Triple<Int, Pair<String, Int>, Int>> = _barChartSummary

    fun insertRecord(subject: String, durationMinutes: Int) {
        viewModelScope.launch { repository.insertRecord(subject, durationMinutes); refreshAll() }
    }

    fun deleteRecord(recordId: Long) {
        viewModelScope.launch { repository.deleteById(recordId); refreshAll() }
    }

    fun checkIn() {
        viewModelScope.launch { checkInRepo.checkInToday(); refreshCheckIn() }
    }

    fun clearAllData() {
        viewModelScope.launch { repository.deleteAll(); resetAllStats() }
    }

    fun refreshAll() {
        refreshTotalStats(); refreshTodayStats(); refreshAllRecords()
        refreshCheckIn(); refreshWeeklyFocus(); refreshWeeklyCompare(); refreshMonthCheckIns()
    }

    fun refreshAllRecords() {
        viewModelScope.launch { _allRecords.postValue(repository.getAllRecords()) }
    }

    fun refreshTotalStats() {
        viewModelScope.launch {
            val tc = repository.getTotalCount(); val td = repository.getTotalDuration()
            val days = repository.getAllDistinctDates()
            val avg = if (days > 0) td.toFloat() / days else 0f
            _totalStats.postValue(Triple(tc, td, avg))
        }
    }

    fun refreshTodayStats(targetDate: Calendar? = null) {
        viewModelScope.launch {
            val cal = targetDate ?: Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
            _todayStats.postValue(Pair(repository.getCountByDay(start, cal.timeInMillis), repository.getDurationByDay(start, cal.timeInMillis)))
        }
    }

    fun refreshSubjectStatsRange(start: Long, end: Long) {
        viewModelScope.launch { _subjectStats.postValue(repository.getSubjectStats(start, end)) }
    }

    fun refreshTimeOfDayForDay(year: Int, month: Int, day: Int) {
        viewModelScope.launch {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year); set(Calendar.MONTH, month - 1); set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val start = cal.timeInMillis
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
            val end = cal.timeInMillis
            val records = repository.getAllRecords().filter { it.timestamp in start..end }
            val map = mutableMapOf<Int, Int>()
            for (i in 0..11) map[i * 2] = 0
            for (r in records) {
                val h = Calendar.getInstance().apply { timeInMillis = r.timestamp }.get(Calendar.HOUR_OF_DAY)
                val slot = (h / 2) * 2
                map[slot] = (map[slot] ?: 0) + r.durationMinutes
            }
            _timeOfDaySlots.postValue(map)
        }
    }

    fun refreshDailyStatsForMonth(year: Int, month: Int) {
        viewModelScope.launch {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year); set(Calendar.MONTH, month - 1); set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val start = cal.timeInMillis
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
            val stats = repository.getDailyStats(start, cal.timeInMillis)
            _dailyStats.postValue(stats)

            val total = stats.sumOf { it.totalMinutes }
            val max = stats.maxByOrNull { it.totalMinutes }
            val maxLabel = if (max != null) "${max.date}(max)" else "—"
            val maxVal = max?.totalMinutes ?: 0
            val cnt = stats.size
            val avg = if (cnt > 0) total / cnt else 0
            _barChartSummary.postValue(Triple(total, Pair(maxLabel, maxVal), avg))
        }
    }

    fun refreshCheckIn() {
        viewModelScope.launch {
            _checkedInToday.postValue(checkInRepo.isCheckedInToday())
            _checkInStreak.postValue(checkInRepo.getStreak())
            _totalCheckIns.postValue(checkInRepo.getTotalCheckInDays())
        }
    }

    fun refreshWeeklyFocus() { viewModelScope.launch { _weeklyFocusMinutes.postValue(getWeekFocus(0)) } }
    fun refreshWeeklyCompare() { viewModelScope.launch { _lastWeekFocusMinutes.postValue(getWeekFocus(-7)) } }

    fun refreshMonthCheckIns() {
        viewModelScope.launch { _allCheckIns.postValue(checkInRepo.getAllCheckIns()) }
    }

    private suspend fun getWeekFocus(offset: Int): Int {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, offset)
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 6)
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
        return repository.getDurationByRange(start, cal.timeInMillis)
    }

    private fun resetAllStats() {
        _totalStats.postValue(Triple(0, 0, 0f)); _todayStats.postValue(Pair(0, 0))
        _subjectStats.postValue(emptyList()); _dailyStats.postValue(emptyList()); _allRecords.postValue(emptyList())
        _checkedInToday.postValue(false); _checkInStreak.postValue(0); _totalCheckIns.postValue(0)
        _weeklyFocusMinutes.postValue(0); _lastWeekFocusMinutes.postValue(0)
        _allCheckIns.postValue(emptySet()); _timeOfDaySlots.postValue(emptyMap())
        _barChartSummary.postValue(Triple(0, Pair("—", 0), 0))
    }
}
