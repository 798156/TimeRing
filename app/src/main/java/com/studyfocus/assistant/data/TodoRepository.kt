package com.studyfocus.assistant.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.studyfocus.assistant.data.entity.TodoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class TodoRepository(private val context: Context) {

    private val gson = Gson()
    private val todoFile get() = File(context.filesDir, "todo_items.json")

    suspend fun addTodo(title: String, deadline: Long? = null): TodoItem = withContext(Dispatchers.IO) {
        val todos = readTodos().toMutableList()
        val id = if (todos.isEmpty()) 1L else todos.maxOf { it.id } + 1
        val todo = TodoItem(id = id, title = title, deadline = deadline)
        todos.add(0, todo)
        todoFile.writeText(gson.toJson(todos))
        todo
    }

    suspend fun updateTodo(todo: TodoItem) = withContext(Dispatchers.IO) {
        val todos = readTodos().toMutableList()
        val idx = todos.indexOfFirst { it.id == todo.id }
        if (idx >= 0) {
            todos[idx] = todo
            todoFile.writeText(gson.toJson(todos))
        }
    }

    suspend fun deleteTodo(todoId: Long) = withContext(Dispatchers.IO) {
        val todos = readTodos().toMutableList()
        todos.removeAll { it.id == todoId }
        todoFile.writeText(gson.toJson(todos))
    }

    suspend fun getAllTodos(): List<TodoItem> = withContext(Dispatchers.IO) {
        readTodos()
    }

    private fun readTodos(): List<TodoItem> {
        if (!todoFile.exists()) return emptyList()
        return try {
            gson.fromJson(todoFile.readText(), object : TypeToken<List<TodoItem>>() {}.type) ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }
}
