package com.example.schday.data

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

class DefaultDataRepository(private val db: AppDatabase) : DataRepository {
    private val semesterDao = db.semesterDao()
    private val courseDao = db.courseDao()
    private val homeworkDao = db.homeworkDao()
    private val periodTimeDao = db.periodTimeDao()

    override fun getAllSemesters(): Flow<List<Semester>> = semesterDao.getAllSemesters()
    override fun getCurrentSemester(): Flow<Semester?> = semesterDao.getCurrentSemester()
    override suspend fun insertSemester(semester: Semester): Long = semesterDao.insertSemester(semester)
    override suspend fun updateSemester(semester: Semester) = semesterDao.updateSemester(semester)
    override suspend fun deleteSemester(semester: Semester) = semesterDao.deleteSemester(semester)
    override suspend fun setCurrentSemester(semesterId: Int) = semesterDao.setCurrentSemester(semesterId)

    override fun getCoursesBySemester(semesterId: Int): Flow<List<CourseWithSchedules>> = 
        courseDao.getCoursesBySemester(semesterId)
    override fun getCourseById(courseId: Int): Flow<CourseWithSchedules?> = 
        courseDao.getCourseById(courseId)
    override suspend fun saveCourseWithSlots(course: Course, slots: List<ScheduleSlot>): Long = 
        courseDao.saveCourseWithSlots(course, slots)
    override suspend fun deleteCourse(course: Course) = 
        courseDao.deleteCourse(course)

    override fun getAllHomework(): Flow<List<Homework>> = homeworkDao.getAllHomework()
    override fun getUncompletedHomework(): Flow<List<Homework>> = homeworkDao.getUncompletedHomework()
    override fun getHomeworkForCourse(courseId: Int): Flow<List<Homework>> = homeworkDao.getHomeworkForCourse(courseId)
    override suspend fun insertHomework(homework: Homework): Long = homeworkDao.insertHomework(homework)
    override suspend fun updateHomework(homework: Homework) = homeworkDao.updateHomework(homework)
    override suspend fun deleteHomework(homework: Homework) = homeworkDao.deleteHomework(homework)

    override fun getAllPeriodTimes(): Flow<List<PeriodTime>> = periodTimeDao.getAllPeriodTimes()
    override suspend fun insertPeriodTimes(periods: List<PeriodTime>) = periodTimeDao.insertPeriodTimes(periods)
}
