package com.example.schday

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.schday.data.AppDatabase
import com.example.schday.data.DefaultDataRepository
import com.example.schday.theme.GlowScheduleTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)

    val database = AppDatabase.getDatabase(applicationContext)
    val repository = DefaultDataRepository(database)
    com.example.schday.scheduler.AlarmScheduler.scheduleHomeworkReminderAlarm(applicationContext)

    enableEdgeToEdge()
    setContent {
      GlowScheduleTheme { 
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { 
          MainNavigation(repository) 
        } 
      }
    }
  }
}
