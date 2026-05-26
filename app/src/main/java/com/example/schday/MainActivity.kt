package com.example.schday

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.schday.data.AppDatabase
import com.example.schday.data.DefaultDataRepository
import com.example.schday.theme.GlowScheduleTheme
import com.example.schday.theme.GlowTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)

    val database = AppDatabase.getDatabase(applicationContext)
    val repository = DefaultDataRepository(database)
    com.example.schday.scheduler.AlarmScheduler.scheduleHomeworkReminderAlarm(applicationContext)

    enableEdgeToEdge()
    setContent {
      val sharedPreferences = remember { getSharedPreferences("schday_settings", Context.MODE_PRIVATE) }
      var currentThemeName by remember {
        mutableStateOf(sharedPreferences.getString("app_theme", GlowTheme.DEEP_CHARCOAL.name) ?: GlowTheme.DEEP_CHARCOAL.name)
      }
      val currentTheme = remember(currentThemeName) {
        try { GlowTheme.valueOf(currentThemeName) } catch (e: Exception) { GlowTheme.DEEP_CHARCOAL }
      }

      GlowScheduleTheme(appTheme = currentTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          MainNavigation(
            repository = repository,
            appTheme = currentTheme,
            onThemeChange = { newTheme ->
              currentThemeName = newTheme.name
              sharedPreferences.edit().putString("app_theme", newTheme.name).apply()
            }
          )
        }
      }
    }
  }
}
