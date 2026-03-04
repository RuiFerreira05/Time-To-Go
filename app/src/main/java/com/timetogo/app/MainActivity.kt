package com.timetogo.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.timetogo.app.notification.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Create notification channel on app startup
        notificationHelper.createNotificationChannel()
    }
}
