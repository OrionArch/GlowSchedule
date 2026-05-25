<!-- Parent: ../AGENTS.md -->

# data/

Data layer: Room database, repository pattern, and entity definitions.

## Key Files

| File | Purpose |
|------|---------|
| `AppDatabase.kt` | Room database singleton. Registers all entities and DAOs. Database name: `schday_database`. |
| `DataRepository.kt` | `DataRepository` interface and `DefaultDataRepository` implementation. Single entry point for all data operations. All read queries return `Flow<T>` for reactive updates. |
| `dao/AppDaos.kt` | Four `@Dao` interfaces: `SemesterDao`, `CourseDao`, `HomeworkDao`, `PeriodTimeDao`. `CourseDao.saveCourseWithSlots()` is a `@Transaction` that inserts a course and its schedule slots atomically. |
| `entity/Entities.kt` | Room `@Entity` classes: `Semester`, `Course`, `ScheduleSlot`, `Homework`, `PeriodTime`. Also defines `CourseWithSchedules` (an `@Embedded` + `@Relation` POJO joining a course with its slots and homework). |

## Subdirectories

- `dao/` -- DAO interfaces for Room queries
- `entity/` -- Room entity classes and relation POJOs

## Architecture Notes

- All foreign keys use `onDelete = ForeignKey.CASCADE`.
- `activeWeeks` on `ScheduleSlot` is stored as a comma-separated string (e.g. `"1,2,3,4,5,6,7,8"`).
- `Semester.startDate` is epoch millis for the Monday of week 1.
- `PeriodTime` uses a non-autoincrement `@PrimaryKey` of `periodNumber` (1-based).
- Database version is 1 with `fallbackToDestructiveMigration()` and `exportSchema = false`.
