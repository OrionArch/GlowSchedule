package com.example.schday.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.Relation

@Entity(tableName = "semesters")
data class Semester(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val startDate: Long, // Monday of week 1
    val totalWeeks: Int = 20,
    val isCurrent: Boolean = false
)

@Entity(
    tableName = "courses",
    foreignKeys = [
        ForeignKey(
            entity = Semester::class,
            parentColumns = ["id"],
            childColumns = ["semesterId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Course(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val semesterId: Int,
    val name: String,
    val teacher: String = "",
    val colorHex: String = "#E8F0FE"
)

@Entity(
    tableName = "schedule_slots",
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ScheduleSlot(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val courseId: Int,
    val dayOfWeek: Int, // 1 = Monday, 7 = Sunday
    val startPeriod: Int,
    val endPeriod: Int,
    val classroom: String = "",
    val activeWeeks: String // Serialized weeks, e.g. "1,2,3,4,5,6,7,8"
)

@Entity(
    tableName = "homework",
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Homework(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val courseId: Int,
    val title: String,
    val description: String = "",
    val deadline: Long,
    val isCompleted: Boolean = false
)

@Entity(tableName = "period_times")
data class PeriodTime(
    @PrimaryKey val periodNumber: Int, // 1, 2, 3...
    val startTime: String, // "08:00"
    val endTime: String // "08:45"
)

data class CourseWithSchedules(
    @Embedded val course: Course,
    @Relation(
        parentColumn = "id",
        entityColumn = "courseId"
    )
    val slots: List<ScheduleSlot>,
    @Relation(
        parentColumn = "id",
        entityColumn = "courseId"
    )
    val homework: List<Homework>
)
