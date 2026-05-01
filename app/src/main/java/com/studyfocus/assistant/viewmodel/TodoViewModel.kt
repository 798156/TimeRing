package com.studyfocus.assistant.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.studyfocus.assistant.StudyApp
import com.studyfocus.assistant.data.entity.TodoItem
import com.studyfocus.assistant.notification.NotificationHelper
import kotlinx.coroutines.launch

class TodoViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as StudyApp
    private val todoRepo = app.todoRepository

    private val _todos = MutableLiveData<List<TodoItem>>()
    val todos: LiveData<List<TodoItem>> = _todos

    fun loadTodos() {
        viewModelScope.launch {
            _todos.postValue(todoRepo.getAllTodos())
        }
    }

    fun addTodo(title: String, deadline: Long? = null) {
        viewModelScope.launch {
            val todo = todoRepo.addTodo(title, deadline)
            if (deadline != null && deadline > System.currentTimeMillis()) {
                NotificationHelper.scheduleDeadlineAlarm(app, todo.id, title, deadline)
            }
            loadTodos()
        }
    }

    fun toggleTodo(todo: TodoItem) {
        viewModelScope.launch {
            val updated = todo.copy(completed = !todo.completed)
            todoRepo.updateTodo(updated)
            if (updated.completed) {
                todo.deadline?.let { NotificationHelper.cancelAlarm(app, todo.id) }
            }
            loadTodos()
        }
    }

    fun setDeadline(todo: TodoItem, deadline: Long?) {
        viewModelScope.launch {
            todo.deadline?.let { NotificationHelper.cancelAlarm(app, todo.id) }
            val updated = todo.copy(deadline = deadline)
            todoRepo.updateTodo(updated)
            if (deadline != null && deadline > System.currentTimeMillis() && !todo.completed) {
                NotificationHelper.scheduleDeadlineAlarm(app, todo.id, todo.title, deadline)
            }
            loadTodos()
        }
    }

    fun deleteTodo(todoId: Long) {
        viewModelScope.launch {
            NotificationHelper.cancelAlarm(app, todoId)
            todoRepo.deleteTodo(todoId)
            loadTodos()
        }
    }
}
