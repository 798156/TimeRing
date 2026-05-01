package com.studyfocus.assistant.data.entity

data class FocusRecord(
    val id: Long = 0,
    val subject: String,
    val durationMinutes: Int,
    val timestamp: Long
)
