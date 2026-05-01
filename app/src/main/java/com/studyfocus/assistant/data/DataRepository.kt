package com.studyfocus.assistant.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.studyfocus.assistant.data.entity.DailyStat
import com.studyfocus.assistant.data.entity.FocusRecord
import com.studyfocus.assistant.data.entity.SubjectStat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DataRepository(private val context: Context) {

    private val gson = Gson()
    private val dataFile: File
        get() = File(context.filesDir, "focus_records.json")

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dayMillis = 24 * 60 * 60 * 1000L

    private suspend fun readRecords(): List<FocusRecord> = withContext(Dispatchers.IO) {
        if (!dataFile.exists()) return@withContext emptyList()
        val json = dataFile.readText()
        if (json.isBlank()) return@withContext emptyList()
        try {
            gson.fromJson(json, object : TypeToken<List<FocusRecord>>() {}.type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun writeRecords(records: List<FocusRecord>) = withContext(Dispatchers.IO) {
        dataFile.writeText(gson.toJson(records))
    }

    suspend fun getAllRecords(): List<FocusRecord> = readRecords()

    suspend fun insertRecord(subject: String, durationMinutes: Int): FocusRecord {
        val records = readRecords().toMutableList()
        val id = if (records.isEmpty()) 1L else records.maxOf { it.id } + 1
        val record = FocusRecord(
            id = id,
            subject = subject,
            durationMinutes = durationMinutes,
            timestamp = System.currentTimeMillis()
        )
        records.add(0, record)
        writeRecords(records)
        return record
    }

    suspend fun deleteById(id: Long) {
        val records = readRecords().toMutableList()
        records.removeAll { it.id == id }
        writeRecords(records)
    }

    suspend fun deleteAll() {
        writeRecords(emptyList())
    }

    suspend fun getTotalCount(): Int = readRecords().size

    suspend fun getTotalDuration(): Int = readRecords().sumOf { it.durationMinutes }

    suspend fun getDurationByDay(startOfDay: Long, endOfDay: Long): Int {
        return readRecords()
            .filter { it.timestamp in startOfDay..endOfDay }
            .sumOf { it.durationMinutes }
    }

    suspend fun getCountByDay(startOfDay: Long, endOfDay: Long): Int {
        return readRecords().count { it.timestamp in startOfDay..endOfDay }
    }

    suspend fun getSubjectStats(start: Long, end: Long): List<SubjectStat> {
        return readRecords()
            .filter { it.timestamp in start..end }
            .groupBy { it.subject }
            .map { (subject, records) ->
                SubjectStat(subject, records.sumOf { it.durationMinutes })
            }
            .sortedByDescending { it.totalMinutes }
    }

    suspend fun getDailyStats(start: Long, end: Long): List<DailyStat> {
        return readRecords()
            .filter { it.timestamp in start..end }
            .groupBy { record ->
                val cal = Calendar.getInstance().apply { timeInMillis = record.timestamp }
                dateFormat.format(cal.time)
            }
            .map { (date, records) ->
                DailyStat(date, records.sumOf { it.durationMinutes })
            }
            .sortedBy { it.date }
    }

    suspend fun getAllDistinctDates(): Int {
        val cal = Calendar.getInstance()
        return readRecords()
            .map { record ->
                cal.timeInMillis = record.timestamp
                dateFormat.format(cal.time)
            }
            .distinct()
            .size
    }

    suspend fun getDurationByRange(start: Long, end: Long): Int {
        return readRecords()
            .filter { it.timestamp in start..end }
            .sumOf { it.durationMinutes }
    }
}
