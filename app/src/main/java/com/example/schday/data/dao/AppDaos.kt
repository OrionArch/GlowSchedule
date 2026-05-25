package com.example.schday.data.dao

import androidx.room.*
import com.example.schday.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SemesterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSemester(semester: Semester): Long

    @Update
    suspend fun updateSemester(semester: Semester)

    @Delete
    suspend fun deleteSemester(semester: Semester)

    @Query("SELECT * FROM semesters WHERE id = :id LIMIT 1")
    suspend fun getSemesterById(id: Int): Semester?

    @Query("SELECT * FROM semesters ORDER BY id DESC")
    fun getAllSemesters(): Flow<List<Semester>>

    @Query("SELECT * FROM semesters WHERE isCurrent = 1 LIMIT 1")
    fun getCurrentSemester(): Flow<Semester?>

    @Transaction
    suspend fun setCurrentSemester(semesterId: Int) {
        clearCurrentSemesters()
        markSemesterAsCurrent(semesterId)
    }

    @Query("UPDATE semesters SET isCurrent = 0")
    suspend fun clearCurrentSemesters()

    @Query("UPDATE semesters SET isCurrent = 1 WHERE id = :semesterId")
    suspend fun markSemesterAsCurrent(semesterId: Int)
}

@Dao
interface CourseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course): Long

    @Query("SELECT * FROM courses WHERE id = :courseId LIMIT 1")
    suspend fun getCourseDirectById(courseId: Int): Course?

    @Query("SELECT * FROM courses")
    suspend fun getAllCoursesDirect(): List<Course>

    @Update
    suspend fun updateCourse(course: Course)

    @Delete
    suspend fun deleteCourse(course: Course)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleSlots(slots: List<ScheduleSlot>)

    @Query("DELETE FROM schedule_slots WHERE courseId = :courseId")
    suspend fun deleteScheduleSlotsForCourse(courseId: Int)

    @Transaction
    suspend fun saveCourseWithSlots(course: Course, slots: List<ScheduleSlot>) {
        val courseId = insertCourse(course).toInt()
        deleteScheduleSlotsForCourse(courseId)
        val slotsWithId = slots.map { it.copy(courseId = courseId) }
        insertScheduleSlots(slotsWithId)
    }

    @Transaction
    @Query("SELECT * FROM courses WHERE semesterId = :semesterId")
    fun getCoursesBySemester(semesterId: Int): Flow<List<CourseWithSchedules>>

    @Transaction
    @Query("SELECT * FROM courses WHERE id = :courseId LIMIT 1")
    fun getCourseById(courseId: Int): Flow<CourseWithSchedules?>
}

@Dao
interface HomeworkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomework(homework: Homework): Long

    @Update
    suspend fun updateHomework(homework: Homework)

    @Delete
    suspend fun deleteHomework(homework: Homework)

    @Query("SELECT * FROM homework ORDER BY deadline ASC")
    fun getAllHomework(): Flow<List<Homework>>

    @Query("SELECT * FROM homework WHERE isCompleted = 0 ORDER BY deadline ASC")
    fun getUncompletedHomework(): Flow<List<Homework>>

    @Query("SELECT * FROM homework WHERE courseId = :courseId ORDER BY deadline ASC")
    fun getHomeworkForCourse(courseId: Int): Flow<List<Homework>>
}

@Dao
interface PeriodTimeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeriodTimes(periods: List<PeriodTime>)

    @Query("SELECT * FROM period_times ORDER BY periodNumber ASC")
    fun getAllPeriodTimes(): Flow<List<PeriodTime>>
}
