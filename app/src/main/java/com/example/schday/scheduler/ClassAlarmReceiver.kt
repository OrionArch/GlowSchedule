package com.example.schday.scheduler

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.schday.MainActivity
import com.example.schday.R
import com.example.schday.data.AppDatabase
import com.example.schday.data.DataRepository
import com.example.schday.data.DefaultDataRepository
import com.example.schday.data.entity.*
import com.example.schday.utils.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

class ClassAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val courseName = intent.getStringExtra("course_name") ?: context.getString(R.string.course_default)
        val classroom = intent.getStringExtra("classroom") ?: ""
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val sharedPrefs = context.getSharedPreferences("schday_settings", Context.MODE_PRIVATE)

        val channelId = "class_reminders"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Notification Channel on Oreo+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, context.getString(R.string.alarm_class_reminder_channel), NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
            val homeworkChannel = NotificationChannel("homework_reminders", context.getString(R.string.alarm_homework_reminder_channel), NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(homeworkChannel)
        }

        when (action) {
            ACTION_CLASS_REMINDER -> {
                // 1. Send pre-class warning notification
                val notificationIntent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val contentText = if (classroom.isNotEmpty()) context.getString(R.string.alarm_coming_with_room, classroom) else context.getString(R.string.alarm_coming_no_room)
                val notification = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle(context.getString(R.string.alarm_class_reminder_title, courseName))
                    .setContentText(contentText)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()

                notificationManager.notify(1001, notification)
            }
            ACTION_CLASS_START -> {
                // 2. Automate Silent Profile at exact start time
                val autoMute = sharedPrefs.getBoolean("auto_mute_enabled", false)
                if (autoMute) {
                    try {
                        // Save previous ringer mode state
                        val prevRinger = audioManager.ringerMode
                        sharedPrefs.edit().putInt("previous_ringer_mode", prevRinger).apply()

                        val muteType = sharedPrefs.getInt("auto_mute_type", 0) // 0 = DND, 1 = Vibrate, 2 = Silent
                        when (muteType) {
                            0 -> {
                                // Do Not Disturb (DND)
                                if (notificationManager.isNotificationPolicyAccessGranted) {
                                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                                } else {
                                    // Fallback to silent if DND not allowed
                                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                                }
                            }
                            1 -> audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                            2 -> audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ClassAlarmReceiver", "Failed to set audio/DND profile (e.g. missing permission)", e)
                    }
                }
                // Trigger widget update on class start
                com.example.schday.widget.ScheduleWidgetReceiver.updateWidget(context)
            }
            ACTION_CLASS_END -> {
                // Restore previous audio profile
                val autoMute = sharedPrefs.getBoolean("auto_mute_enabled", false)
                if (autoMute) {
                    try {
                        val prevRinger = sharedPrefs.getInt("previous_ringer_mode", AudioManager.RINGER_MODE_NORMAL)
                        audioManager.ringerMode = prevRinger

                        // Restore DND interruption filter
                        if (notificationManager.isNotificationPolicyAccessGranted) {
                            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ClassAlarmReceiver", "Failed to restore audio/DND profile (e.g. missing permission)", e)
                    }
                }
                // Trigger widget update on class end
                com.example.schday.widget.ScheduleWidgetReceiver.updateWidget(context)
            }
            ACTION_HOMEWORK_REMINDER -> {
                // Reschedule next day's alarm
                AlarmScheduler.scheduleHomeworkReminderAlarm(context)

                val db = AppDatabase.getDatabase(context)
                CoroutineScope(Dispatchers.IO).launch {
                    val homeworkList = db.homeworkDao().getUncompletedHomework().first()
                    val now = System.currentTimeMillis()
                    val limit = now + 48 * 60 * 60 * 1000 // 48 hours
                    val urgentHomework = homeworkList.filter { it.deadline in now..limit }

                    if (urgentHomework.isNotEmpty()) {
                        val coursesList = db.courseDao().getAllCoursesDirect()
                        val courseMap = coursesList.associateBy { it.id }

                        val notificationIntent = Intent(context, MainActivity::class.java)
                        val pendingIntent = PendingIntent.getActivity(
                            context, 1002, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        val summaryText = if (urgentHomework.size == 1) {
                            val hw = urgentHomework[0]
                            val courseNameStr = courseMap[hw.courseId]?.name ?: context.getString(R.string.course_default)
                            context.getString(R.string.alarm_single_homework, courseNameStr, hw.title)
                        } else {
                            context.getString(R.string.alarm_multi_homework, urgentHomework.size)
                        }

                        val detailText = context.getString(R.string.alarm_homework_detail_prefix) + urgentHomework.joinToString("\n") { hw ->
                            val courseNameStr = courseMap[hw.courseId]?.name ?: context.getString(R.string.course_default)
                            "- [${courseNameStr}] ${hw.title}"
                        }

                        val notification = NotificationCompat.Builder(context, "homework_reminders")
                            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                            .setContentTitle(context.getString(R.string.alarm_homework_deadline_title))
                            .setContentText(summaryText)
                            .setStyle(NotificationCompat.BigTextStyle().bigText(detailText))
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true)
                            .build()

                        notificationManager.notify(1002, notification)
                    }
                }
            }
            ACTION_MIDNIGHT_RESCHEDULE -> {
                val db = AppDatabase.getDatabase(context)
                val repository = DefaultDataRepository(db, context)
                CoroutineScope(Dispatchers.IO).launch {
                    AlarmScheduler.scheduleTodayAlarms(context, repository)
                    com.example.schday.widget.ScheduleWidgetReceiver.updateWidget(context)
                }
            }
        }
    }

    companion object {
        const val ACTION_CLASS_REMINDER = "com.example.schday.ACTION_CLASS_REMINDER"
        const val ACTION_CLASS_START = "com.example.schday.ACTION_CLASS_START"
        const val ACTION_CLASS_END = "com.example.schday.ACTION_CLASS_END"
        const val ACTION_HOMEWORK_REMINDER = "com.example.schday.ACTION_HOMEWORK_REMINDER"
        const val ACTION_MIDNIGHT_RESCHEDULE = "com.example.schday.ACTION_MIDNIGHT_RESCHEDULE"
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_DATE_CHANGED) {
            val db = AppDatabase.getDatabase(context)
            val repository = DefaultDataRepository(db, context)
            CoroutineScope(Dispatchers.IO).launch {
                AlarmScheduler.scheduleTodayAlarms(context, repository)
                AlarmScheduler.scheduleHomeworkReminderAlarm(context)
                com.example.schday.widget.ScheduleWidgetReceiver.updateWidget(context)
            }
        }
    }
}

object AlarmScheduler {

    fun scheduleHomeworkReminderAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ClassAlarmReceiver::class.java).apply {
            action = ClassAlarmReceiver.ACTION_HOMEWORK_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 9999, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val triggerTime = calendar.timeInMillis
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    suspend fun scheduleTodayAlarms(context: Context, repository: DataRepository) {
        val semester = repository.getCurrentSemester().first() ?: return
        val currentWeek = DateUtils.getCurrentWeek(semester.startDate, semester.totalWeeks)
        val dayOfWeek = DateUtils.getDayOfWeek()

        val courses = repository.getCoursesBySemester(semester.id).first()
        val periods = repository.getAllPeriodTimes().first()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val sharedPrefs = context.getSharedPreferences("schday_settings", Context.MODE_PRIVATE)
        val offsetMinutes = sharedPrefs.getInt("pre_class_reminder_offset", 10)

        // Cancel previously scheduled slot alarms to avoid leaks/orphans
        val prevScheduledIdsStr = sharedPrefs.getString("scheduled_slot_ids", "") ?: ""
        if (prevScheduledIdsStr.isNotEmpty()) {
            val prevScheduledIds = prevScheduledIdsStr.split(",").mapNotNull { it.toIntOrNull() }
            for (slotId in prevScheduledIds) {
                val reminderIntent = Intent(context, ClassAlarmReceiver::class.java).apply {
                    action = ClassAlarmReceiver.ACTION_CLASS_REMINDER
                }
                val reminderPI = PendingIntent.getBroadcast(
                    context, slotId * 3 + 0, reminderIntent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (reminderPI != null) {
                    alarmManager.cancel(reminderPI)
                    reminderPI.cancel()
                }

                val startIntent = Intent(context, ClassAlarmReceiver::class.java).apply {
                    action = ClassAlarmReceiver.ACTION_CLASS_START
                }
                val startPI = PendingIntent.getBroadcast(
                    context, slotId * 3 + 1, startIntent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (startPI != null) {
                    alarmManager.cancel(startPI)
                    startPI.cancel()
                }

                val endIntent = Intent(context, ClassAlarmReceiver::class.java).apply {
                    action = ClassAlarmReceiver.ACTION_CLASS_END
                }
                val endPI = PendingIntent.getBroadcast(
                    context, slotId * 3 + 2, endIntent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (endPI != null) {
                    alarmManager.cancel(endPI)
                    endPI.cancel()
                }
            }
        }

        val newlyScheduledIds = mutableListOf<Int>()

        for (c in courses) {
            for (slot in c.slots) {
                // If it is active today
                if (slot.dayOfWeek == dayOfWeek && DateUtils.isWeekActive(slot.activeWeeks, currentWeek)) {
                    val startPeriodTime = periods.find { it.periodNumber == slot.startPeriod } ?: continue
                    val endPeriodTime = periods.find { it.periodNumber == slot.endPeriod } ?: continue

                    newlyScheduledIds.add(slot.id)

                    // 1. Reminder alarm (fires offsetMinutes before class start)
                    val startTrigger = getTriggerTime(startPeriodTime.startTime)
                    val reminderTrigger = startTrigger - offsetMinutes * 60 * 1000
                    val reminderIntent = Intent(context, ClassAlarmReceiver::class.java).apply {
                        action = ClassAlarmReceiver.ACTION_CLASS_REMINDER
                        putExtra("course_name", c.course.name)
                        putExtra("classroom", slot.classroom)
                    }
                    val reminderPI = PendingIntent.getBroadcast(
                        context, slot.id * 3 + 0, reminderIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    scheduleAlarm(alarmManager, reminderTrigger, reminderPI)

                    // 2. Start silent mode alarm (fires 5 seconds after class start to ensure widget updates past the boundary)
                    val startIntent = Intent(context, ClassAlarmReceiver::class.java).apply {
                        action = ClassAlarmReceiver.ACTION_CLASS_START
                        putExtra("course_name", c.course.name)
                        putExtra("classroom", slot.classroom)
                    }
                    val startPI = PendingIntent.getBroadcast(
                        context, slot.id * 3 + 1, startIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    scheduleAlarm(alarmManager, startTrigger + 5000, startPI)

                    // 3. End silent mode alarm (fires 5 seconds after class end to ensure widget updates past the boundary)
                    val endTrigger = getTriggerTime(endPeriodTime.endTime)
                    val endIntent = Intent(context, ClassAlarmReceiver::class.java).apply {
                        action = ClassAlarmReceiver.ACTION_CLASS_END
                        putExtra("course_name", c.course.name)
                    }
                    val endPI = PendingIntent.getBroadcast(
                        context, slot.id * 3 + 2, endIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    scheduleAlarm(alarmManager, endTrigger + 5000, endPI)
                }
            }
        }

        // Save scheduled slot IDs for next rescheduling cancel step
        sharedPrefs.edit().putString("scheduled_slot_ids", newlyScheduledIds.joinToString(",")).apply()

        // 4. Schedule next midnight reschedule alarm (at 00:01 of the next day)
        val midnightCalendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 1)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val rescheduleIntent = Intent(context, ClassAlarmReceiver::class.java).apply {
            action = ClassAlarmReceiver.ACTION_MIDNIGHT_RESCHEDULE
        }
        val reschedulePI = PendingIntent.getBroadcast(
            context, 8888, rescheduleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, midnightCalendar.timeInMillis, reschedulePI)
    }

    private fun scheduleAlarm(alarmManager: AlarmManager, triggerTime: Long, pendingIntent: PendingIntent) {
        if (triggerTime < System.currentTimeMillis()) return // Do not schedule alarms in the past
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    private fun getTriggerTime(timeStr: String): Long {
        val parts = timeStr.split(":")
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()

        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
