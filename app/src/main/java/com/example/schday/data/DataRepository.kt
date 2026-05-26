package com.example.schday.data

import android.content.Context
import android.util.Log
import com.example.schday.data.dao.*
import com.example.schday.data.entity.*
import kotlinx.coroutines.flow.Flow

interface DataRepository {
    // Semester
    fun getAllSemesters(): Flow<List<Semester>>
    fun getCurrentSemester(): Flow<Semester?>
    suspend fun insertSemester(semester: Semester): Long
    suspend fun updateSemester(semester: Semester)
    suspend fun deleteSemester(semester: Semester)
    suspend fun setCurrentSemester(semesterId: Int)

    // Course & Schedule
    fun getCoursesBySemester(semesterId: Int): Flow<List<CourseWithSchedules>>
    fun getCourseById(courseId: Int): Flow<CourseWithSchedules?>
    suspend fun saveCourseWithSlots(course: Course, slots: List<ScheduleSlot>): Long
    suspend fun deleteCourse(course: Course)

    // Homework
    fun getAllHomework(): Flow<List<Homework>>
    fun getUncompletedHomework(): Flow<List<Homework>>
    fun getHomeworkForCourse(courseId: Int): Flow<List<Homework>>
    suspend fun insertHomework(homework: Homework): Long
    suspend fun updateHomework(homework: Homework)
    suspend fun deleteHomework(homework: Homework)

    // Period Times
    fun getAllPeriodTimes(): Flow<List<PeriodTime>>
    suspend fun insertPeriodTimes(periods: List<PeriodTime>)
}

class DefaultDataRepository(
    private val db: AppDatabase,
    private val context: Context? = null
) : DataRepository {
    private val semesterDao = db.semesterDao()
    private val courseDao = db.courseDao()
    private val homeworkDao = db.homeworkDao()
    private val periodTimeDao = db.periodTimeDao()

    private fun triggerWidgetUpdate() {
        context?.let { ctx ->
            Log.d("DefaultDataRepository", "Database modified, triggering widget update")
            com.example.schday.widget.ScheduleWidgetReceiver.updateWidget(ctx)
        }
    }

    override fun getAllSemesters(): Flow<List<Semester>> = semesterDao.getAllSemesters()
    override fun getCurrentSemester(): Flow<Semester?> = semesterDao.getCurrentSemester()
    override suspend fun insertSemester(semester: Semester): Long {
        val id = semesterDao.insertSemester(semester)
        triggerWidgetUpdate()
        return id
    }
    override suspend fun updateSemester(semester: Semester) {
        semesterDao.updateSemester(semester)
        triggerWidgetUpdate()
    }
    override suspend fun deleteSemester(semester: Semester) {
        semesterDao.deleteSemester(semester)
        triggerWidgetUpdate()
    }
    override suspend fun setCurrentSemester(semesterId: Int) {
        semesterDao.setCurrentSemester(semesterId)
        triggerWidgetUpdate()
    }

    override fun getCoursesBySemester(semesterId: Int): Flow<List<CourseWithSchedules>> = 
        courseDao.getCoursesBySemester(semesterId)
    override fun getCourseById(courseId: Int): Flow<CourseWithSchedules?> = 
        courseDao.getCourseById(courseId)
    override suspend fun saveCourseWithSlots(course: Course, slots: List<ScheduleSlot>): Long {
        val id = courseDao.saveCourseWithSlots(course, slots)
        triggerWidgetUpdate()
        return id
    }
    override suspend fun deleteCourse(course: Course) {
        courseDao.deleteCourse(course)
        triggerWidgetUpdate()
    }

    override fun getAllHomework(): Flow<List<Homework>> = homeworkDao.getAllHomework()
    override fun getUncompletedHomework(): Flow<List<Homework>> = homeworkDao.getUncompletedHomework()
    override fun getHomeworkForCourse(courseId: Int): Flow<List<Homework>> = homeworkDao.getHomeworkForCourse(courseId)
    override suspend fun insertHomework(homework: Homework): Long {
        val id = homeworkDao.insertHomework(homework)
        triggerWidgetUpdate()
        return id
    }
    override suspend fun updateHomework(homework: Homework) {
        homeworkDao.updateHomework(homework)
        triggerWidgetUpdate()
    }
    override suspend fun deleteHomework(homework: Homework) {
        homeworkDao.deleteHomework(homework)
        triggerWidgetUpdate()
    }

    override fun getAllPeriodTimes(): Flow<List<PeriodTime>> = periodTimeDao.getAllPeriodTimes()
    override suspend fun insertPeriodTimes(periods: List<PeriodTime>) {
        periodTimeDao.insertPeriodTimes(periods)
        triggerWidgetUpdate()
    }
}
