package com.example.activityrecognitionapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ActivityRecognitionApp: Application(){

    override fun onCreate() {
        super.onCreate()
    }
}