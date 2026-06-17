package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- ROOM ENTITIES ---

@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "study_logs")
data class StudyLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val level: String, // "Foundation", "Intermediate", "Final"
    val subject: String, // e.g. "Advanced Accounting"
    val durationMinutes: Int,
    val date: String, // "YYYY-MM-DD"
    val xpEarned: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "sprint_progress")
data class SprintProgress(
    @PrimaryKey val challengeType: String, // "20-Day Revision", "50-Day Intensive", "70-Day Mastery"
    val startDate: String,
    val currentDay: Int,
    val isActive: Boolean
)

@Entity(tableName = "milestone_progress")
data class MilestoneProgress(
    @PrimaryKey val id: String, // challengeType + "_" + dayNumber
    val challengeType: String,
    val dayNumber: Int,
    val title: String,
    val isCompleted: Boolean,
    val dateCompleted: String? = null
)

@Entity(tableName = "saved_notes")
data class SavedNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val inputText: String,
    val summaryNotes: String,
    val flashcards: String,
    val mnemonics: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_evaluations")
data class SavedEvaluation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val questionName: String,
    val myAnswer: String,
    val score: Int, // Out of 10
    val errors: String,
    val corrections: String,
    val timestamp: Long = System.currentTimeMillis()
)

// --- ROOM DAO ---

@Dao
interface AppDao {
    // Settings
    @Query("SELECT * FROM app_settings WHERE key = :key LIMIT 1")
    suspend fun getSetting(key: String): AppSetting?

    @Query("SELECT * FROM app_settings")
    fun getAllSettingsFlow(): Flow<List<AppSetting>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: AppSetting)

    // Study Logs
    @Query("SELECT * FROM study_logs ORDER BY timestamp DESC")
    fun getAllStudyLogs(): Flow<List<StudyLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyLog(log: StudyLog)

    @Query("DELETE FROM study_logs WHERE id = :id")
    suspend fun deleteStudyLog(id: Int)

    @Query("DELETE FROM study_logs")
    suspend fun clearAllStudyLogs()

    // Sprints
    @Query("SELECT * FROM sprint_progress")
    fun getAllSprints(): Flow<List<SprintProgress>>

    @Query("SELECT * FROM sprint_progress WHERE challengeType = :challengeType LIMIT 1")
    suspend fun getSprint(challengeType: String): SprintProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSprint(sprint: SprintProgress)

    @Update
    suspend fun updateSprint(sprint: SprintProgress)

    // Milestones
    @Query("SELECT * FROM milestone_progress WHERE challengeType = :challengeType ORDER BY dayNumber ASC")
    fun getMilestonesForChallenge(challengeType: String): Flow<List<MilestoneProgress>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilestones(milestones: List<MilestoneProgress>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilestone(milestone: MilestoneProgress)

    // Notes Cache
    @Query("SELECT * FROM saved_notes ORDER BY timestamp DESC")
    fun getAllSavedNotes(): Flow<List<SavedNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedNote(note: SavedNote)

    // Evaluations Cache
    @Query("SELECT * FROM saved_evaluations ORDER BY timestamp DESC")
    fun getAllSavedEvaluations(): Flow<List<SavedEvaluation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedEvaluation(evaluation: SavedEvaluation)
}

// --- DATABASE CLASS ---

@Database(
    entities = [
        AppSetting::class,
        StudyLog::class,
        SprintProgress::class,
        MilestoneProgress::class,
        SavedNote::class,
        SavedEvaluation::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ca_epic_academy_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// --- REPOSITORY ---

class AppRepository(private val dao: AppDao) {
    val studyLogs: Flow<List<StudyLog>> = dao.getAllStudyLogs()
    val settings: Flow<List<AppSetting>> = dao.getAllSettingsFlow()
    val sprints: Flow<List<SprintProgress>> = dao.getAllSprints()
    val savedNotes: Flow<List<SavedNote>> = dao.getAllSavedNotes()
    val savedEvaluations: Flow<List<SavedEvaluation>> = dao.getAllSavedEvaluations()

    suspend fun getSettingValue(key: String, defaultValue: String): String {
        return dao.getSetting(key)?.value ?: defaultValue
    }

    suspend fun saveSetting(key: String, value: String) {
        dao.insertSetting(AppSetting(key, value))
    }

    suspend fun addStudyLog(level: String, subject: String, minutes: Int, xp: Int, date: String) {
        dao.insertStudyLog(StudyLog(level = level, subject = subject, durationMinutes = minutes, xpEarned = xp, date = date))
    }

    suspend fun deleteStudyLog(id: Int) {
        dao.deleteStudyLog(id)
    }

    suspend fun clearStudyLogs() {
        dao.clearAllStudyLogs()
    }

    suspend fun enrollInSprint(challengeType: String, totalDays: Int) {
        val sprint = SprintProgress(
            challengeType = challengeType,
            startDate = "2026-06-17",
            currentDay = 1,
            isActive = true
        )
        dao.insertSprint(sprint)

        // Generate milestones
        val milestones = (1..totalDays).map { day ->
            val milestoneTitle = when (challengeType) {
                "20-Day Revision" -> {
                    when (day) {
                        1 -> "General Accounting Standards Review"
                        2 -> "Partnership Accounts & Dissolution"
                        3 -> "Company Accounts & Shares/Debentures"
                        4 -> "Revision of Business Law Core Concepts"
                        5 -> "Indian Contract Act Sectionals"
                        6 -> "Companies Act & LLP Provisions"
                        7 -> "Quantitative Aptitude: Ratio, Prop, Indices"
                        8 -> "Logical Reasoning Practice Problems"
                        9 -> "Business Economics Introduction & Demand"
                        10 -> "Market Forms & Price Indexing"
                        11 -> "Advanced Accounting standards (AS)"
                        12 -> "Financial Statements of Companies"
                        13 -> "Taxation: Direct Tax & Heads of Income"
                        14 -> "Indirect Tax: GST Basics & Supply Place"
                        15 -> "Costing: Material & Labor Variance"
                        16 -> "Auditing Standards & Reporting Forms"
                        17 -> "Finals prep: Financial Reporting (FR) Review"
                        18 -> "Advanced Auditing & Ethics Scenarios"
                        19 -> "Direct & Indirect Tax Laws Synthesis"
                        20 -> "Grand Final Mock Exam Overdrive"
                        else -> "Revision Sprint Daily Prep Day $day"
                    }
                }
                "50-Day Intensive" -> {
                    val paperNum = ((day - 1) % 6) + 1
                    val stage = if (day <= 15) "Fundamentals & Concepts" else if (day <= 35) "Intermediate Level Hard Computations" else "Evaluations & Mock Drill"
                    "Paper $paperNum Focus: Study $stage ($day/50)"
                }
                else -> {
                    // 70-Day
                    val stage = if (day <= 25) "Phase 1 - Comprehensive Reads" else if (day <= 50) "Phase 2 - Section-Wise Mock Questions" else "Phase 3 - Intensive PYQs & Key Mnemonics"
                    "Complete Mastery Day $day: study $stage ($day/70)"
                }
            }
            MilestoneProgress(
                id = "${challengeType}_$day",
                challengeType = challengeType,
                dayNumber = day,
                title = milestoneTitle,
                isCompleted = false
            )
        }
        dao.insertMilestones(milestones)
    }

    suspend fun updateMilestone(challengeType: String, dayNumber: Int, isCompleted: Boolean) {
        val milestoneId = "${challengeType}_$dayNumber"
        val milestoneTitle = when (challengeType) {
            "20-Day Revision" -> "Revision Prep Day $dayNumber"
            "50-Day Intensive" -> "Intensive Prep Day $dayNumber"
            else -> "Mastery Prep Day $dayNumber"
        }
        dao.insertMilestone(
            MilestoneProgress(
                id = milestoneId,
                challengeType = challengeType,
                dayNumber = dayNumber,
                title = milestoneTitle,
                isCompleted = isCompleted,
                dateCompleted = if (isCompleted) "2026-06-17" else null
            )
        )
    }

    fun getMilestonesFlow(challengeType: String): Flow<List<MilestoneProgress>> {
        return dao.getMilestonesForChallenge(challengeType)
    }

    suspend fun updateSprintDay(challengeType: String, newDay: Int) {
        val sprint = dao.getSprint(challengeType)
        if (sprint != null) {
            dao.insertSprint(sprint.copy(currentDay = newDay))
        }
    }

    suspend fun addSavedNote(title: String, input: String, notes: String, flashcards: String, mnemonics: String) {
        dao.insertSavedNote(SavedNote(title = title, inputText = input, summaryNotes = notes, flashcards = flashcards, mnemonics = mnemonics))
    }

    suspend fun addSavedEvaluation(question: String, answer: String, score: Int, errors: String, corrections: String) {
        dao.insertSavedEvaluation(SavedEvaluation(questionName = question, myAnswer = answer, score = score, errors = errors, corrections = corrections))
    }
}
