<!-- Parent: ../AGENTS.md -->

# scheduler/

Alarm system for class reminders, auto-mute, and homework deadline notifications.

## Key Files

| File | Purpose |
|------|---------|
| `ClassAlarmReceiver.kt` | Contains three classes: `ClassAlarmReceiver` (BroadcastReceiver handling four alarm actions), `BootReceiver` (reschedules alarms after device reboot), and `AlarmScheduler` (utility object that computes and sets AlarmManager alarms). |

## Alarm Actions

| Action | Behavior |
|--------|----------|
| `ACTION_CLASS_REMINDER` | Posts a high-priority notification N minutes before class start (offset from SharedPreferences `pre_class_reminder_offset`, default 10). |
| `ACTION_CLASS_START` | Activates silent/DND/vibrate mode if `auto_mute_enabled` is true. Saves previous ringer mode to restore later. |
| `ACTION_CLASS_END` | Restores the previous audio ringer mode and DND filter. |
| `ACTION_HOMEWORK_REMINDER` | Fires nightly at 20:00. Queries uncompleted homework due within 48 hours and posts a summary notification. Reschedules itself for the next day. |

## Scheduling Logic

- `AlarmScheduler.scheduleTodayAlarms()` reads today's active course slots, looks up period times, and schedules three alarms per slot (reminder, start-mute, end-mute).
- `AlarmScheduler.scheduleHomeworkReminderAlarm()` sets a daily 20:00 recurring alarm.
- Uses `setExactAndAllowWhileIdle` on API 31+ when `canScheduleExactAlarms()` is granted; falls back to `setAndAllowWhileIdle` otherwise.
- `BootReceiver` reschedules all alarms on `ACTION_BOOT_COMPLETED`.

## SharedPreferences Keys

| Key | Type | Default | Purpose |
|-----|------|---------|---------|
| `auto_mute_enabled` | Boolean | `false` | Whether auto-mute activates at class start |
| `auto_mute_type` | Int | `0` | 0=DND, 1=Vibrate, 2=Silent |
| `pre_class_reminder_offset` | Int | `10` | Minutes before class to fire reminder |
| `previous_ringer_mode` | Int | - | Saved ringer mode for restore after class |
