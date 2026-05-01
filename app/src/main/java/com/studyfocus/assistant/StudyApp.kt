package com.studyfocus.assistant

import android.app.Application
import com.studyfocus.assistant.data.CheckInRepository
import com.studyfocus.assistant.data.DataRepository
import com.studyfocus.assistant.data.TodoRepository
import com.studyfocus.assistant.notification.NotificationHelper

class StudyApp : Application() {

    val repository: DataRepository by lazy { DataRepository(this) }
    val todoRepository: TodoRepository by lazy { TodoRepository(this) }
    val checkInRepository: CheckInRepository by lazy { CheckInRepository(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        NotificationHelper.createChannel(this)
    }

    companion object {
        lateinit var instance: StudyApp
            private set
    }
}
