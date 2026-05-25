package com.example.schday

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object Main : NavKey

@Serializable
data class AddEditCourse(val courseId: Int? = null) : NavKey

@Serializable
data object ImportCourses : NavKey
