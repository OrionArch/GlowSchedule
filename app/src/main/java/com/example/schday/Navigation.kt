package com.example.schday

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.schday.data.DataRepository
import com.example.schday.ui.main.MainScreen
import com.example.schday.ui.screens.edit.AddEditCourseScreen
import com.example.schday.ui.screens.import.ImportCoursesScreen

@Composable
fun MainNavigation(repository: DataRepository) {
  val backStack = rememberNavBackStack(Main)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          MainScreen(
            repository = repository,
            onItemClick = { navKey -> backStack.add(navKey) }
          )
        }
        entry<AddEditCourse> { key ->
          AddEditCourseScreen(
            repository = repository,
            courseId = key.courseId,
            onBack = { backStack.removeLastOrNull() }
          )
        }
        entry<ImportCourses> {
          ImportCoursesScreen(
            repository = repository,
            onBack = { backStack.removeLastOrNull() }
          )
        }
      },
  )
}
