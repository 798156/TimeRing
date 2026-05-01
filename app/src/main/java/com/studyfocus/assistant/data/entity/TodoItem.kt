package com.studyfocus.assistant.data.entity

data class TodoItem(
    val id: Long = 0,
    val title: String,
    val completed: Boolean = false,
    val deadline: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
