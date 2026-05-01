package com.studyfocus.assistant.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CheckInRepository(private val context: Context) {

    private val gson = Gson()
    private val checkInFile get() = File(context.filesDir, "checkins.json")
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend fun checkInToday(): Boolean = withContext(Dispatchers.IO) {
        val today = todayStr()
        val dates = readCheckIns().toMutableList()
        if (dates.contains(today)) return@withContext false
        dates.add(today)
        checkInFile.writeText(gson.toJson(dates))
        true
    }

    suspend fun isCheckedInToday(): Boolean = withContext(Dispatchers.IO) {
        readCheckIns().contains(todayStr())
    }

    suspend fun getTotalCheckInDays(): Int = withContext(Dispatchers.IO) {
        readCheckIns().size
    }

    suspend fun getStreak(): Int = withContext(Dispatchers.IO) {
        val dates = readCheckIns().toSet()
        val cal = Calendar.getInstance()
        val today = todayStr()
        if (!dates.contains(today)) cal.add(Calendar.DAY_OF_YEAR, -1)
        var streak = 0
        while (true) {
            val d = dateFmt.format(cal.time)
            if (!dates.contains(d)) break
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        streak
    }

    suspend fun getMonthCheckIns(year: Int, month: Int): List<String> = withContext(Dispatchers.IO) {
        val prefix = "${year}-${String.format("%02d", month)}"
        readCheckIns().filter { it.startsWith(prefix) }
    }

    suspend fun getAllCheckIns(): Set<String> = withContext(Dispatchers.IO) {
        readCheckIns().toSet()
    }

    private fun todayStr(): String = dateFmt.format(Calendar.getInstance().time)
    private suspend fun readCheckIns(): List<String> = withContext(Dispatchers.IO) {
        if (!checkInFile.exists()) return@withContext emptyList()
        try {
            gson.fromJson(checkInFile.readText(), object : TypeToken<List<String>>() {}.type) ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }
}
