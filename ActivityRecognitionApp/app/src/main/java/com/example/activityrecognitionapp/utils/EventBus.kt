package com.example.activityrecognitionapp.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow


object EventBus {
        private val _events = MutableSharedFlow<Event>()
        val events = _events.asSharedFlow()

        suspend fun sendEvent(event: Event) {
            _events.emit(event)
        }
    }

    sealed class Event {
        object Logout : Event()

    }
