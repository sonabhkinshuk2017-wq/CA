package com.example.ui.screens

import android.app.Application
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.EvaluationOutput
import com.example.ai.GeminiClient
import com.example.data.*
import com.example.ui.skins.Skin
import com.example.ui.skins.SkinRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// --- MAIN VIEWMODEL ---

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = AppRepository(database.dao())

    // UI Reactivity flows
    val studyLogs: StateFlow<List<StudyLog>> = repository.studyLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val savedNotes: StateFlow<List<SavedNote>> = repository.savedNotes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val savedEvaluations: StateFlow<List<SavedEvaluation>> = repository.savedEvaluations.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val sprints: StateFlow<List<SprintProgress>> = repository.sprints.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val currentThemeId = mutableStateOf("iron_man")
    val studentName = mutableStateOf("Chartered Hero")
    val selectedAcademicLevel = mutableStateOf("Intermediate") // "Foundation", "Intermediate", "Final"
    val userXp = mutableStateOf(1450)
    val userStreak = mutableStateOf(5)

    // Current Active Sprint
    var activeSprint = mutableStateOf<SprintProgress?>(null)
    var activeMilestones = mutableStateOf<List<MilestoneProgress>>(emptyList())

    // AI States
    var noteInputText = mutableStateOf("")
    var generatedNotes = mutableStateOf("")
    var generatedFlashcards = mutableStateOf("")
    var generatedMnemonics = mutableStateOf("")
    var isGeneratingNotes = mutableStateOf(false)

    var evaluatorQuestion = mutableStateOf("Explain when interest is deductible under Section 36(1)(iii) of the Income Tax Act, 1961.")
    var evaluatorAnswerInput = mutableStateOf("")
    var evaluationResult = mutableStateOf<EvaluationOutput?>(null)
    var isEvaluatingAnswer = mutableStateOf(false)

    // Log Study Session sheet state
    var showLogSessionDialog = mutableStateOf(false)
    var logSessionSubject = mutableStateOf("")
    var logSessionMinutes = mutableStateOf("45")

    // Onboarding setup or initialize data
    init {
        viewModelScope.launch {
            // Load theme
            val storedTheme = repository.getSettingValue("selected_theme", "iron_man")
            currentThemeId.value = storedTheme

            // Load name
            val storedName = repository.getSettingValue("student_name", "Chartered Legend")
            studentName.value = storedName

            // Load Level
            selectedAcademicLevel.value = repository.getSettingValue("academic_level", "Intermediate")

            // Load XP
            val xpStr = repository.getSettingValue("user_xp", "1450")
            userXp.value = xpStr.toIntOrNull() ?: 1450

            // Load Streak
            val streakStr = repository.getSettingValue("user_streak", "5")
            userStreak.value = streakStr.toIntOrNull() ?: 5

            // Track active sprints
            repository.sprints.collect { sprintList ->
                val active = sprintList.find { it.isActive }
                activeSprint.value = active
                if (active != null) {
                    repository.getMilestonesFlow(active.challengeType).collect { milestones ->
                        activeMilestones.value = milestones
                    }
                } else {
                    activeMilestones.value = emptyList()
                }
            }
        }
    }

    fun updateTheme(themeId: String) {
        currentThemeId.value = themeId
        viewModelScope.launch {
            repository.saveSetting("selected_theme", themeId)
        }
    }

    fun updateStudentName(name: String) {
        studentName.value = name
        viewModelScope.launch {
            repository.saveSetting("student_name", name)
        }
    }

    fun updateAcademicLevel(level: String) {
        selectedAcademicLevel.value = level
        viewModelScope.launch {
            repository.saveSetting("academic_level", level)
        }
    }

    fun enrollSprint(type: String) {
        val totalDays = when (type) {
            "20-Day Revision" -> 20
            "50-Day Intensive" -> 50
            else -> 70
        }
        viewModelScope.launch {
            repository.enrollInSprint(type, totalDays)
            userXp.value += 300 // Enroll bonus
            repository.saveSetting("user_xp", userXp.value.toString())
        }
    }

    fun toggleMilestone(challengeType: String, dayNumber: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.updateMilestone(challengeType, dayNumber, isCompleted)
            if (isCompleted) {
                userXp.value += 100 // milestone complete award
            } else {
                userXp.value = (userXp.value - 100).coerceAtLeast(0)
            }
            repository.saveSetting("user_xp", userXp.value.toString())

            // Advance current day if we completed previous day
            val sprint = activeSprint.value
            if (sprint != null && isCompleted && dayNumber == sprint.currentDay) {
                repository.updateSprintDay(challengeType, (dayNumber + 1).coerceAtMost(if (challengeType == "20-Day Revision") 20 else if (challengeType == "50-Day Intensive") 50 else 70))
            }
        }
    }

    fun logStudySession(subject: String, minutes: Int) {
        val xpAwarded = minutes * 2 // 2 XP per studying minute
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            repository.addStudyLog(selectedAcademicLevel.value, subject, minutes, xpAwarded, dateStr)
            
            userXp.value += xpAwarded
            // Increment streak if study happens
            userStreak.value += 1
            repository.saveSetting("user_xp", userXp.value.toString())
            repository.saveSetting("user_streak", userStreak.value.toString())
        }
    }

    fun deleteLog(id: Int) {
        viewModelScope.launch {
            repository.deleteStudyLog(id)
        }
    }

    // AI triggers
    fun triggerNoteGeneration() {
        if (noteInputText.value.isBlank()) return
        isGeneratingNotes.value = true
        viewModelScope.launch {
            val result = GeminiClient.generateNotes(noteInputText.value)
            
            // Extract mock partitions for displaying beautifully
            generatedNotes.value = extractBlock(result, "## High-Retention Notes")
            generatedFlashcards.value = extractBlock(result, "## Interactive Flashcards")
            generatedMnemonics.value = extractBlock(result, "## Clever Mnemonics")

            if (generatedNotes.value.isEmpty()) {
                generatedNotes.value = result // Fallback full markdown
            }

            // Save to Room cache
            repository.addSavedNote(
                title = noteInputText.value.take(40) + "...",
                input = noteInputText.value,
                notes = generatedNotes.value,
                flashcards = generatedFlashcards.value,
                mnemonics = generatedMnemonics.value
            )

            // Award AI synthesis XP
            userXp.value += 50
            repository.saveSetting("user_xp", userXp.value.toString())
            isGeneratingNotes.value = false
        }
    }

    fun triggerAnswerEvaluation() {
        if (evaluatorAnswerInput.value.isBlank()) return
        isEvaluatingAnswer.value = true
        viewModelScope.launch {
            val eval = GeminiClient.evaluateAnswer(evaluatorQuestion.value, evaluatorAnswerInput.value)
            evaluationResult.value = eval

            // Save to DB
            repository.addSavedEvaluation(
                question = evaluatorQuestion.value,
                answer = evaluatorAnswerInput.value,
                score = eval.score,
                errors = eval.technicalErrors.joinToString("\n"),
                corrections = eval.modelCorrections
            )

            // Dynamic XP based on quality score
            val bonusXp = eval.score * 20
            userXp.value += bonusXp
            repository.saveSetting("user_xp", userXp.value.toString())
            isEvaluatingAnswer.value = false
        }
    }

    private fun extractBlock(source: String, header: String): String {
        if (!source.contains(header)) return ""
        val index = source.indexOf(header) + header.length
        val nextHeaderIndex = source.indexOf("## ", index)
        return if (nextHeaderIndex != -1) {
            source.substring(index, nextHeaderIndex).trim()
        } else {
            source.substring(index).trim()
        }
    }
}

// --- DYNAMIC THEMING WRAPPER ---

@Composable
fun CAEpicAcademyThemeWrapper(
    skin: Skin,
    content: @Composable () -> Unit
) {
    val localM3ColorScheme = if (skin.isDark) {
        darkColorScheme(
            primary = skin.primary,
            secondary = skin.secondary,
            background = skin.background,
            surface = skin.surface,
            onPrimary = skin.onPrimary,
            onSurface = skin.onSurface,
            onBackground = skin.onSurface,
            outline = skin.border
        )
    } else {
        lightColorScheme(
            primary = skin.primary,
            secondary = skin.secondary,
            background = skin.background,
            surface = skin.surface,
            onPrimary = skin.onPrimary,
            onSurface = skin.onSurface,
            onBackground = skin.onSurface,
            outline = skin.border
        )
    }

    val styleFont = when (skin.fontFamilyLabel) {
        "Blocky" -> FontFamily.SansSerif
        "Hero" -> FontFamily.Serif
        "Comic" -> FontFamily.Cursive
        "Mono" -> FontFamily.Monospace
        else -> FontFamily.Default
    }

    val customizedTypography = Typography(
        displayLarge = MaterialTheme.typography.displayLarge.copy(fontFamily = styleFont),
        headlineMedium = MaterialTheme.typography.headlineMedium.copy(fontFamily = styleFont, fontWeight = FontWeight.Bold),
        titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = styleFont, fontWeight = FontWeight.SemiBold),
        bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontFamily = styleFont),
        labelMedium = MaterialTheme.typography.labelMedium.copy(fontFamily = styleFont)
    )

    MaterialTheme(
        colorScheme = localM3ColorScheme,
        typography = customizedTypography,
        content = content
    )
}

// --- CORE MAIN CONTENT COMPOSABLE ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val skinId by viewModel.currentThemeId
    val currentSkin = SkinRepository.getSkinOrDefault(skinId)

    val studyLogsList by viewModel.studyLogs.collectAsState()
    val savedNotesList by viewModel.savedNotes.collectAsState()
    val savedEvaluationsList by viewModel.savedEvaluations.collectAsState()

    var activeScreenTab by remember { mutableStateOf("Home HUD") } // Tabs: Home HUD, Syllabus, Sprints, Note-Gen AI, ICAI Evaluator, Analytics, Themes

    CAEpicAcademyThemeWrapper(skin = currentSkin) {
        Scaffold(
            topBar = {
                AppHeader(
                    skin = currentSkin,
                    activeTab = activeScreenTab,
                    onTabSelected = { activeScreenTab = it }
                )
            },
            bottomBar = {
                AppBottomBar(
                    skin = currentSkin,
                    activeTab = activeScreenTab,
                    onTabSelected = { activeScreenTab = it }
                )
            },
            containerColor = currentSkin.background,
            contentColor = currentSkin.onSurface
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                currentSkin.background,
                                currentSkin.surface
                            )
                        )
                    )
            ) {
                // Render screen content dynamically in a nice fade transition state
                AnimatedContent(
                    targetState = activeScreenTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                    },
                    label = "tabTransition"
                ) { targetTab ->
                    when (targetTab) {
                        "Home HUD" -> HomeHUDView(viewModel, studyLogsList, currentSkin, onLogRequest = { subject ->
                            if (subject == "NAV_NOTE_GEN") {
                                activeScreenTab = "Note-Gen AI"
                            } else if (subject == "NAV_ICAI_EVAL") {
                                activeScreenTab = "ICAI Evaluator"
                            } else {
                                viewModel.logSessionSubject.value = subject
                                viewModel.showLogSessionDialog.value = true
                            }
                        })
                        "Syllabus" -> SyllabusHubView(viewModel, currentSkin, onLogRequest = { subject ->
                            viewModel.logSessionSubject.value = subject
                            viewModel.showLogSessionDialog.value = true
                        })
                        "Sprints" -> GamifiedSprintPlannerView(viewModel, currentSkin)
                        "Note-Gen AI" -> NoteGenAiWorkspaceView(viewModel, savedNotesList, currentSkin)
                        "ICAI Evaluator" -> IcaiEvaluatorWorkspaceView(viewModel, savedEvaluationsList, currentSkin)
                        "Analytics" -> AnalyticsView(viewModel, studyLogsList, savedEvaluationsList, currentSkin)
                        "Themes/Skins" -> SkinSelectorView(viewModel, currentSkin)
                    }
                }

                // Add Study Session Modal Dialog
                if (viewModel.showLogSessionDialog.value) {
                    AlertDialog(
                        onDismissRequest = { viewModel.showLogSessionDialog.value = false },
                        shape = RoundedCornerShape(20.dp),
                        containerColor = currentSkin.surface,
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Timer, "Timer", tint = currentSkin.accent, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Log CA Study Hours",
                                    color = currentSkin.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }
                        },
                        text = {
                            Column {
                                Text(
                                    "Subject: ${viewModel.logSessionSubject.value}",
                                    color = currentSkin.onSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                Text(
                                    "Daily persistence is the secret to passing ICAI levels. Enter how many minutes you studied:",
                                    color = currentSkin.onSurface.copy(alpha = 0.7f),
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                OutlinedTextField(
                                    value = viewModel.logSessionMinutes.value,
                                    onValueChange = { viewModel.logSessionMinutes.value = it },
                                    label = { Text("Duration (minutes)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = currentSkin.accent,
                                        unfocusedBorderColor = currentSkin.border,
                                        focusedTextColor = currentSkin.onSurface,
                                        unfocusedTextColor = currentSkin.onSurface
                                    ),
                                    modifier = Modifier.fillMaxWidth().testTag("duration_input")
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "🔥 Study wins you ${((viewModel.logSessionMinutes.value.toIntOrNull() ?: 1) * 2)} XP points!",
                                    color = currentSkin.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val mins = viewModel.logSessionMinutes.value.toIntOrNull() ?: 30
                                    viewModel.logStudySession(viewModel.logSessionSubject.value, mins)
                                    viewModel.showLogSessionDialog.value = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = currentSkin.accent),
                                modifier = Modifier.testTag("submit_log_button")
                            ) {
                                Text("Log Session", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { viewModel.showLogSessionDialog.value = false }) {
                                Text("Cancel", color = currentSkin.onSurface.copy(alpha = 0.6f))
                            }
                        }
                    )
                }
            }
        }
    }
}

// --- TOP APPLICATION HEADER ---

@Composable
fun AppHeader(
    skin: Skin,
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    Surface(
        color = skin.surface,
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth(),
        border = SpacerBorder(skin.border)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(skin.primary)
                            .border(1.5.dp, skin.secondary.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "CA",
                            fontWeight = FontWeight.Black,
                            color = skin.secondary,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "CA Epic Academy",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = skin.onSurface,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            "${skin.name.uppercase()} EDITION (Skin 01/50)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = skin.secondary,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Header interactive skin button
                IconButton(
                    onClick = { onTabSelected("Themes/Skins") },
                    modifier = Modifier.testTag("skin_dropdown_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Palette,
                        contentDescription = "Themes",
                        tint = skin.accent
                    )
                }
            }
        }
    }
}

private fun SpacerBorder(color: Color) = androidx.compose.foundation.BorderStroke(0.5.dp, color)

// --- SYSTEM HUD CARD VIEWPORT (GAMIFIED LEVELLING ENGINE) ---

@Composable
fun HomeHUDView(
    viewModel: MainViewModel,
    studyLogs: List<StudyLog>,
    skin: Skin,
    onLogRequest: (String) -> Unit
) {
    val xp = viewModel.userXp.value
    val level = (xp / 1000) + 1
    val reqXpForNext = ((level) * 1000)
    val prevXpLevel = ((level - 1) * 1000)
    val currentLevelProgress = (xp - prevXpLevel).toFloat() / 1000f

    val streak = viewModel.userStreak.value
    var dynamicRank = when {
        level >= 7 -> "Double Entry Grandmaster"
        level >= 5 -> "ICAI Board Advisor"
        level >= 3 -> "Chartered Legend"
        else -> "Article Assistant"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // GAMIFIED RPG-STYLE LEVEL INTERFACE
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = skin.surface.copy(alpha = 0.9f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, skin.primary, RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "STUDENT CADET PROFILE",
                                fontSize = 11.sp,
                                color = skin.accent,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                            Text(
                                viewModel.studentName.value,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = skin.onSurface
                            )
                            Text(
                                "Rank: $dynamicRank",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = skin.primary
                            )
                        }

                        // Streak Ring
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(skin.primary.copy(alpha = 0.2f))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🔥", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "$streak Days",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = skin.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // XP Linear progress
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Level $level",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = skin.onSurface
                        )
                        Text(
                            "$xp / $reqXpForNext XP",
                            fontSize = 12.sp,
                            color = skin.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = currentLevelProgress,
                        color = skin.primary,
                        trackColor = skin.primary.copy(alpha = 0.15f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Gain +100 XP per Daily Milestone, +2 XP per studied study minute log. Reach Level ${level + 1} to unlock premium CA evaluation simulations!",
                        fontSize = 11.sp,
                        color = skin.onSurface.copy(alpha = 0.6f),
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // ACTIVE CORE SPRINT QUICK SNAPSHOT
        item {
            val sprint = viewModel.activeSprint.value
            if (sprint != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = skin.surface.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, skin.border.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left border indicator bar
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(80.dp)
                                .background(skin.primary)
                        )
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "CURRENT ACTIVE SPRINT",
                                    fontSize = 10.sp,
                                    color = skin.accent,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    sprint.challengeType,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = skin.onSurface
                                )
                                Text(
                                    "Current Target: Day ${sprint.currentDay}",
                                    fontSize = 13.sp,
                                    color = skin.onSurface.copy(alpha = 0.8f)
                                )
                            }
                            IconButton(
                                onClick = { /* Navigate or direct tick */ },
                                modifier = Modifier
                                    .background(skin.accent, CircleShape)
                                    .size(40.dp)
                            ) {
                                Icon(Icons.Filled.ChevronRight, "View Sprints", tint = Color.Black)
                            }
                        }
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = skin.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "No Enrolled Exam Revision Challenge Yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = skin.onSurface
                        )
                        Text(
                            "Enroll in mock preparation sprints to gamify your exam countdown!",
                            fontSize = 13.sp,
                            color = skin.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // AI MODULE QUICK ACTIONS (HIGH DENSITY HUD ADDITION)
        item {
            Text(
                "AI MODULE QUICK ACTIONS",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = skin.accent,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick Action 1: Note-Gen AI
                Card(
                    colors = CardDefaults.cardColors(containerColor = skin.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onLogRequest("NAV_NOTE_GEN") }
                        .border(1.dp, skin.secondary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(skin.primary)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🤖", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Note-Gen AI",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = skin.secondary
                            )
                            Text(
                                "PDF Summaries",
                                fontSize = 10.sp,
                                color = skin.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // Quick Action 2: ICAI Evaluator
                Card(
                    colors = CardDefaults.cardColors(containerColor = skin.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onLogRequest("NAV_ICAI_EVAL") }
                        .border(1.dp, skin.border.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(skin.border.copy(alpha = 0.15f))
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✍️", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "ICAI Evaluator",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = skin.onSurface
                            )
                            Text(
                                "Auto Grading",
                                fontSize = 10.sp,
                                color = skin.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        // QUICK LOG / STUDY ACTION TRIGGER
        item {
            Text(
                "QUICK TIMERS",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = skin.accent,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            val quickSubjects = when (viewModel.selectedAcademicLevel.value) {
                "Foundation" -> listOf("Accounting", "Business Laws", "Quant Aptitude", "Economics")
                "Final" -> listOf("Financial Reporting", "Advanced Audit", "Direct Tax", "Indirect Tax")
                else -> listOf("Advanced Accounting", "Taxation", "Corp Laws", "Costing", "Auditing")
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(quickSubjects) { subject ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = skin.surface),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .width(150.dp)
                            .clickable { onLogRequest(subject) }
                            .border(1.dp, skin.border.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(skin.primary.copy(alpha = 0.1f))
                                    .padding(6.dp)
                            ) {
                                Icon(Icons.Filled.MenuBook, "Book", tint = skin.primary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                subject,
                                color = skin.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "Tap to log prep",
                                color = skin.onSurface.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // STUDY LOGS LOGGED LIST HISTORY
        item {
            Text(
                "RECENT STUDY SESSIONS",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = skin.accent,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        if (studyLogs.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = skin.surface.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "No study sessions logged item yet. Use the Syllabus tab or quick timers above to log your hard work!",
                            fontSize = 12.sp,
                            color = skin.onSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(studyLogs.take(5)) { log ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = skin.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(skin.accent.copy(alpha = 0.1f))
                                    .padding(8.dp)
                            ) {
                                Icon(Icons.Filled.AccessTime, "Log", tint = skin.accent, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    log.subject,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = skin.onSurface
                                )
                                Text(
                                    "Log id: #${log.id} • ${log.level} Level",
                                    fontSize = 11.sp,
                                    color = skin.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${log.durationMinutes} mins",
                                    fontWeight = FontWeight.Bold,
                                    color = skin.primary,
                                    fontSize = 14.sp
                                )
                                Text(
                                    "+${log.xpEarned} XP",
                                    fontWeight = FontWeight.SemiBold,
                                    color = skin.accent,
                                    fontSize = 11.sp
                                )
                            }
                            IconButton(onClick = { viewModel.deleteLog(log.id) }, modifier = Modifier.testTag("delete_log_${log.id}")) {
                                Icon(Icons.Filled.DeleteOutline, "Remove", tint = Color.Red.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- CORE BRACKET TABS: ACADEMIC LEVEL & SYLLABUS HUB ---

@Composable
fun SyllabusHubView(
    viewModel: MainViewModel,
    skin: Skin,
    onLogRequest: (String) -> Unit
) {
    val selectedLevel = viewModel.selectedAcademicLevel.value

    val subcategories = listOf("Study Material", "Revision Test Papers (RTPs)", "Mock Test Papers (MTPs)", "Past Year Questions (PYQs)")

    // Define mock papers matching Indian ICAI Syllabus perfectly
    val foundationPapers = listOf(
        "Paper 1: Accounting",
        "Paper 2: Business Laws",
        "Paper 3: Quantitative Aptitude",
        "Paper 4: Business Economics"
    )

    val intermediatePapers = listOf(
        "[G1] Paper 1: Advanced Accounting",
        "[G1] Paper 2: Corporate and Other Laws",
        "[G1] Paper 3: Taxation (Direct & Indirect)",
        "[G2] Paper 4: Cost & Management Accounting",
        "[G2] Paper 5: Auditing and Ethics",
        "[G2] Paper 6: Financial & Strategic Mgmt"
    )

    val finalPapers = listOf(
        "[G1] Paper 1: Financial Reporting (FR)",
        "[G1] Paper 2: Advanced Financial Mgmt (AFM)",
        "[G1] Paper 3: Advanced Auditing & Professional Ethics",
        "[G2] Paper 4: Direct Tax & International Taxation",
        "[G2] Paper 5: Indirect Tax Laws",
        "[G2] Paper 6: Integrated Business Solutions"
    )

    val currentPapers = when (selectedLevel) {
        "Foundation" -> foundationPapers
        "Final" -> finalPapers
        else -> intermediatePapers
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // LEVEL SWITCHER BAR CAPSULES
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(skin.surface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Foundation", "Intermediate", "Final").forEach { lvl ->
                    val isLvlSel = selectedLevel == lvl
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isLvlSel) skin.primary else Color.Transparent)
                            .clickable { viewModel.updateAcademicLevel(lvl) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            lvl,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isLvlSel) skin.onPrimary else skin.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        item {
            Text(
                "$selectedLevel Course Papers Syllabus Hub".uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = skin.accent,
                letterSpacing = 2.sp
            )
        }

        // PAPERS LISTING
        items(currentPapers) { paper ->
            Card(
                colors = CardDefaults.cardColors(containerColor = skin.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, skin.border.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                paper,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = skin.onSurface
                            )
                            Text(
                                "Syllabus Group Segment • ICAI Prescribed",
                                fontSize = 11.sp,
                                color = skin.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        Button(
                            onClick = { onLogRequest(paper) },
                            colors = ButtonDefaults.buttonColors(containerColor = skin.primary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("log_btn_${paper.replace(" ", "_")}")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Add, "Log", modifier = Modifier.size(14.dp), tint = skin.onPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Log Prep", fontSize = 11.sp, color = skin.onPrimary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = skin.border.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // Study Category Checkboxes Placeholder items
                    subcategories.forEach { subCat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = "Target",
                                    tint = skin.accent.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    subCat,
                                    fontSize = 13.sp,
                                    color = skin.onSurface.copy(alpha = 0.85f)
                                )
                            }
                            // Visual badge
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = skin.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    "Ready to study",
                                    fontSize = 9.sp,
                                    color = skin.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- GAMIFIED COGNITIVE CHALLENGE SPRINT PLANNER ---

@Composable
fun GamifiedSprintPlannerView(viewModel: MainViewModel, skin: Skin) {
    val activeSprint = viewModel.activeSprint.value
    val milestones = viewModel.activeMilestones.value

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (activeSprint == null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = skin.surface.copy(alpha = 0.82f)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().border(2.dp, skin.primary, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "🎯 SELECT EXAM CountDOWN SPRINT",
                            color = skin.accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Enroll in a Gamified Prep Journey",
                            color = skin.onSurface,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Select one target exam preparation challenge depending on your proximity to the final ICAI Exams. Sprints automatically generate daily diagnostic study tasks, track completion milestones, and reward XP progression!",
                            color = skin.onSurface.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Challenge Option 1
                        SprintEnrollmentCard(
                            title = "20-Day Revision Sprint",
                            descriptor = "Intense rapid-fire syllabus brush up prior to exam week.",
                            xpBonus = "Enrolling awards +300 Initial XP bonus!",
                            buttonTag = "enroll_20",
                            onEnroll = { viewModel.enrollSprint("20-Day Revision") },
                            skin = skin
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Challenge Option 2
                        SprintEnrollmentCard(
                            title = "50-Day Intensive Syllabus Cover",
                            descriptor = "Strategic deeply layered coverage of Group 1 and Group 2 papers.",
                            xpBonus = "Best for double group standard aspirants.",
                            buttonTag = "enroll_50",
                            onEnroll = { viewModel.enrollSprint("50-Day Intensive") },
                            skin = skin
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Challenge Option 3
                        SprintEnrollmentCard(
                            title = "70-Day Complete Mastery Challenge",
                            descriptor = "Ultimate marathon for securing elite rankings with multiple mocks and PYQs.",
                            xpBonus = "Comprehensive academic shield configuration.",
                            buttonTag = "enroll_70",
                            onEnroll = { viewModel.enrollSprint("70-Day Complete Mastery") },
                            skin = skin
                        )
                    }
                }
            }
        } else {
            // RENDERS ACTIVE ENROLLED CHRONICLER SPRINT VIEW
            val completedCount = milestones.count { it.isCompleted }
            val totalMilestones = milestones.size
            val progressPercentScale = if (totalMilestones > 0) completedCount.toFloat() / totalMilestones.toFloat() else 0f

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = skin.surface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, skin.border.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .fillMaxHeight()
                                .background(skin.primary)
                        )
                        Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "ENROLLED ACTIVE CHALLENGE",
                                    color = skin.accent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    activeSprint.challengeType,
                                    color = skin.onSurface,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(skin.primary.copy(alpha = 0.2f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "Day ${activeSprint.currentDay} / $totalMilestones",
                                    color = skin.primary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Progress Indicator info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Milestones Compliance",
                                fontSize = 13.sp,
                                color = skin.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                "$completedCount Completed (${(progressPercentScale * 100).toInt()}%)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = skin.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = progressPercentScale,
                            color = skin.accent,
                            trackColor = skin.accent.copy(alpha = 0.15f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Checking off a daily revision milestone increases compliance ratings and directly credits your career profile sheet +100 XP points!",
                            fontSize = 11.sp,
                            color = skin.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

            item {
                Text(
                    "DAILY MILESTONE SPRINT CHECKLIST",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = skin.accent,
                    letterSpacing = 1.5.sp
                )
            }

            // LIST MILESTONES DAY BY DAY
            items(milestones) { milestone ->
                val isUnlocked = milestone.dayNumber <= activeSprint.currentDay
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (milestone.isCompleted) skin.primary.copy(alpha = 0.1f) else skin.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isUnlocked) {
                            viewModel.toggleMilestone(activeSprint.challengeType, milestone.dayNumber, !milestone.isCompleted)
                        },
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (milestone.isCompleted) skin.primary else skin.border.copy(alpha = 0.15f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (milestone.isCompleted) skin.primary else if (isUnlocked) skin.border.copy(alpha = 0.3f) else skin.border.copy(alpha = 0.1f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (milestone.isCompleted) {
                                    Icon(Icons.Filled.Check, "Done", tint = skin.onPrimary, modifier = Modifier.size(18.dp))
                                } else {
                                    Text(
                                        "D${milestone.dayNumber}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUnlocked) skin.onSurface else skin.onSurface.copy(alpha = 0.4f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    milestone.title,
                                    fontSize = 14.sp,
                                    color = if (isUnlocked) skin.onSurface else skin.onSurface.copy(alpha = 0.4f),
                                    fontWeight = if (isUnlocked) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    if (milestone.isCompleted) "Completed +100 XP awarded" else if (isUnlocked) "Available to clear today" else "Locked (clear prior days)",
                                    fontSize = 11.sp,
                                    color = if (milestone.isCompleted) skin.primary else if (isUnlocked) skin.accent else skin.onSurface.copy(alpha = 0.3f)
                                )
                            }
                        }

                        if (isUnlocked) {
                            Checkbox(
                                checked = milestone.isCompleted,
                                onCheckedChange = { checked ->
                                    viewModel.toggleMilestone(activeSprint.challengeType, milestone.dayNumber, checked)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = skin.primary,
                                    uncheckedColor = skin.border
                                ),
                                modifier = Modifier.testTag("checkbox_${milestone.dayNumber}")
                            )
                        } else {
                            Icon(Icons.Filled.Lock, "Locked", tint = skin.onSurface.copy(alpha = 0.2f), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SprintEnrollmentCard(
    title: String,
    descriptor: String,
    xpBonus: String,
    buttonTag: String,
    onEnroll: () -> Unit,
    skin: Skin
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = skin.surface.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().border(0.5.dp, skin.border.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                title,
                color = skin.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                descriptor,
                color = skin.onSurface.copy(alpha = 0.8f),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    xpBonus,
                    fontSize = 11.sp,
                    color = skin.accent,
                    fontWeight = FontWeight.SemiBold
                )
                Button(
                    onClick = onEnroll,
                    colors = ButtonDefaults.buttonColors(containerColor = skin.accent),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag(buttonTag)
                ) {
                    Text("Enroll Challenge", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- COGNITIVE AI MODULE: NOTE-GEN AI AGENT ---

@Composable
fun NoteGenAiWorkspaceView(
    viewModel: MainViewModel,
    savedNotes: List<SavedNote>,
    skin: Skin
) {
    var workspaceTab by remember { mutableStateOf("Create Notes") } // "Create Notes" or "Cached Summaries"

    val sampleTopics = listOf(
        "Companies Act Part IV - Corporate Social Responsibility Section 135",
        "SA 240: Auditor's Duties & Responsibilities Concerning Fraud",
        "Standards on Auditing SA 500: Audit Evidence Collection Criteria",
        "Direct Tax heads: PGBP Business Deductions & Allowances Section 37(1)"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Note-Gen subtab selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(skin.surface)
                    .padding(2.dp)
            ) {
                listOf("Create Notes", "Cached Summaries").forEach { tab ->
                    val isTabSel = workspaceTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isTabSel) skin.accent else Color.Transparent)
                            .clickable { workspaceTab = tab }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            tab,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isTabSel) Color.Black else skin.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        if (workspaceTab == "Create Notes") {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = skin.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "NOTE-GEN AI RESEARCH AGENT",
                            color = skin.accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Generate High-Retention Synthesis Sheets",
                            color = skin.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Input ICAI textbooks paragraphs, complex clauses, audit standards, or select one sample below. The AI synthesizes optimized memory sheets, interactive revision cards, and retention acronyms.",
                            color = skin.onSurface.copy(alpha = 0.65f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )

                        // Sample fast buttons
                        Text("TRY PRE-CONFIGURED SAMPLE CHAPTERS:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = skin.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        sampleTopics.forEach { sampleText ->
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .fillMaxWidth()
                                    .border(0.5.dp, skin.border.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .clickable { viewModel.noteInputText.value = sampleText }
                                    .padding(8.dp)
                            ) {
                                Text(
                                    sampleText,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = skin.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = viewModel.noteInputText.value,
                            onValueChange = { viewModel.noteInputText.value = it },
                            placeholder = { Text("Paste ICAI chapter content or legal text here...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("note_input_field"),
                            maxLines = 5,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = skin.primary,
                                unfocusedBorderColor = skin.border.copy(alpha = 0.5f),
                                focusedTextColor = skin.onSurface,
                                unfocusedTextColor = skin.onSurface
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.triggerNoteGeneration() },
                            colors = ButtonDefaults.buttonColors(containerColor = skin.accent),
                            modifier = Modifier.fillMaxWidth().testTag("generate_notes_btn"),
                            enabled = !viewModel.isGeneratingNotes.value && viewModel.noteInputText.value.isNotBlank()
                        ) {
                            if (viewModel.isGeneratingNotes.value) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Synthesizing Notes...", color = Color.Black)
                            } else {
                                Icon(Icons.Filled.Bolt, "Generate", tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Compile AI Memory Sheet (+50 XP)", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // SHOW RESULT
            if (viewModel.generatedNotes.value.isNotBlank()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = skin.surface.copy(alpha = 0.9f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().border(2.dp, skin.primary, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "HIGH-RETENTION SUMMARY NOTES",
                                color = skin.primary,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                viewModel.generatedNotes.value,
                                color = skin.onSurface,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )

                            HorizontalDivider(color = skin.border.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                "3 RETENTION FLASHCARDS",
                                color = skin.accent,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                viewModel.generatedFlashcards.value,
                                color = skin.onSurface,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )

                            HorizontalDivider(color = skin.border.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                "CLEVER ACADEMIC MNEMONICS",
                                color = skin.primary,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                viewModel.generatedMnemonics.value,
                                color = skin.onSurface,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // CACHED NOTES LISTING
            if (savedNotes.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "No summary notes compiles cached yet. Generate some dynamic sheets using the creation panel!",
                            fontSize = 12.sp,
                            color = skin.onSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(savedNotes) { note_item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = skin.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.generatedNotes.value = note_item.summaryNotes
                                viewModel.generatedFlashcards.value = note_item.flashcards
                                viewModel.generatedMnemonics.value = note_item.mnemonics
                                workspaceTab = "Create Notes"
                            }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.History, "Cache", tint = skin.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    note_item.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = skin.onSurface
                                )
                            }
                            Text(
                                "Compiled: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(note_item.timestamp))}",
                                fontSize = 11.sp,
                                color = skin.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- COGNITIVE AI MODULE: ICAI EVALUATOR AGENT ---

@Composable
fun IcaiEvaluatorWorkspaceView(
    viewModel: MainViewModel,
    savedEvaluations: List<SavedEvaluation>,
    skin: Skin
) {
    var evaluatorSubTab by remember { mutableStateOf("Assess Mock Sheet") } // "Assess Mock Sheet" or "Grading Sheet Index"

    val pastMockQuestions = listOf(
        "Explain the duties of an auditor regarding corporate social responsibility under the Companies Act, 2013.",
        "Define 'Professional Skepticism' according to SA 200 and discuss its absolute importance in designing audit procedures.",
        "How is residential status determined for an individual under Section 6(1) of the Income Tax Act?",
        "Discuss Standard on Auditing SA 240 guidelines concerning identifying fraud misstatements."
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Evaluator subtab
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(skin.surface)
                    .padding(2.dp)
            ) {
                listOf("Assess Mock Sheet", "Grading Sheet Index").forEach { tab ->
                    val isTabSel = evaluatorSubTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isTabSel) skin.primary else Color.Transparent)
                            .clickable { evaluatorSubTab = tab }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            tab,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isTabSel) skin.onPrimary else skin.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        if (evaluatorSubTab == "Assess Mock Sheet") {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = skin.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "ICAI CANONICAL COGNITIVE EVALUATOR",
                            color = skin.accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Strict Standard Evaluator Engine",
                            color = skin.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Type your answer against specific ICAI questions. The AI evaluates against the marking guides, flags missing statutory sections, checks technical keyword weights, and gives feedback with Suggested marks (out of 10).",
                            color = skin.onSurface.copy(alpha = 0.65f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )

                        Text("SELECT COMMONLY TESTED EXAM TOPICS:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = skin.accent)
                        Spacer(modifier = Modifier.height(4.dp))
                        pastMockQuestions.forEach { mockQ ->
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .fillMaxWidth()
                                    .border(0.5.dp, skin.border.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .clickable { viewModel.evaluatorQuestion.value = mockQ }
                                    .padding(8.dp)
                            ) {
                                Text(
                                    mockQ,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = skin.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text("Current Target Question:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = skin.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = viewModel.evaluatorQuestion.value,
                            onValueChange = { viewModel.evaluatorQuestion.value = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = skin.primary,
                                unfocusedBorderColor = skin.border.copy(alpha = 0.4f),
                                focusedTextColor = skin.onSurface,
                                unfocusedTextColor = skin.onSurface
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("Your Mock Answer Draft:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = skin.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = viewModel.evaluatorAnswerInput.value,
                            onValueChange = { viewModel.evaluatorAnswerInput.value = it },
                            placeholder = { Text("As per Auditing Standards SA 200, professional skepticism is...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("eval_answer_input"),
                            maxLines = 6,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = skin.accent,
                                unfocusedBorderColor = skin.border.copy(alpha = 0.4f),
                                focusedTextColor = skin.onSurface,
                                unfocusedTextColor = skin.onSurface
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.triggerAnswerEvaluation() },
                            colors = ButtonDefaults.buttonColors(containerColor = skin.accent),
                            modifier = Modifier.fillMaxWidth().testTag("evaluate_answer_btn"),
                            enabled = !viewModel.isEvaluatingAnswer.value && viewModel.evaluatorAnswerInput.value.isNotBlank()
                        ) {
                            if (viewModel.isEvaluatingAnswer.value) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Evaluating Legal Precision...", color = Color.Black)
                            } else {
                                Icon(Icons.Filled.HowToReg, "Assess", tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Assess against ICAI Standard Marking Guide", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // RENDERS SCORE feedback sheet
            val result = viewModel.evaluationResult.value
            if (result != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = skin.surface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().border(2.dp, skin.accent, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "EVALUATION CRITERIA SHEET",
                                        fontSize = 11.sp,
                                        color = skin.accent,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Scorecard & Suggestions",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = skin.onSurface
                                    )
                                }

                                // Score Ring
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(if (result.score >= 7) Color(0xFF2E7D32) else if (result.score >= 5) Color(0xFFEF6C00) else Color(0xFFC62828)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            "${result.score}/10",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 18.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            "Marks",
                                            fontSize = 9.sp,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Verdict: ${result.verdict}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = skin.accent
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = skin.border.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Text("TECHNICAL GAPS & INCORRECT SECTIONS:", fontSize = 12.sp, color = skin.primary, fontWeight = FontWeight.Bold)
                            result.technicalErrors.forEach { err ->
                                Row(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("🚨", fontSize = 11.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        err,
                                        fontSize = 12.sp,
                                        color = skin.onSurface.copy(alpha = 0.85f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = skin.border.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Text("MODEL CORRECT ANSWER PHRASING:", fontSize = 12.sp, color = skin.primary, fontWeight = FontWeight.Bold)
                            Text(
                                result.modelCorrections,
                                fontSize = 13.sp,
                                color = skin.onSurface,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = skin.border.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Text("DIRECT MENTORSHIP TIP:", fontSize = 12.sp, color = skin.accent, fontWeight = FontWeight.Bold)
                            Text(
                                result.mentorshipTip,
                                fontSize = 13.sp,
                                color = skin.onSurface.copy(alpha = 0.9f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // INDEX LOGS
            if (savedEvaluations.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "No mock answer scores index logged yet. Fire evaluates from the active grading panel!",
                            fontSize = 12.sp,
                            color = skin.onSurface.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(savedEvaluations) { eval_item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = skin.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    eval_item.questionName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = skin.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "Evaluated at: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(eval_item.timestamp))}",
                                    fontSize = 11.sp,
                                    color = skin.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(skin.accent.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "Score ${eval_item.score}/10",
                                    color = skin.accent,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- ANALYTICS AND WEEKEND/MONTH-END TILES ---

@Composable
fun AnalyticsView(
    viewModel: MainViewModel,
    studyLogs: List<StudyLog>,
    savedEvaluations: List<SavedEvaluation>,
    skin: Skin
) {
    var analyticSubTab by remember { mutableStateOf("Weekend Analysis") } // "Weekend Analysis" or "Month-End Forecast"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // tab selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(skin.surface)
                    .padding(2.dp)
            ) {
                listOf("Weekend Analysis", "Month-End Forecast").forEach { tab ->
                    val isTabSel = analyticSubTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isTabSel) skin.accent else Color.Transparent)
                            .clickable { analyticSubTab = tab }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            tab,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isTabSel) Color.Black else skin.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        if (analyticSubTab == "Weekend Analysis") {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = skin.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "WEEKEND REPORT & METRIC TILES",
                            color = skin.accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Weekly Study Hours Matrix",
                            color = skin.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Calculate totals per subject
                        val subjectMinutes = mutableMapOf<String, Float>()
                        // Set standard subjects based on selected academic level to prevent empty graph
                        val currentLevelSubjects = when (viewModel.selectedAcademicLevel.value) {
                            "Foundation" -> listOf("Accounting", "Business Laws", "Quant Aptitude", "Economics")
                            "Final" -> listOf("Financial Reporting", "Advanced Audit", "Direct Tax", "Indirect Tax")
                            else -> listOf("Advanced Accounting", "Taxation", "Corp Laws", "Costing")
                        }
                        currentLevelSubjects.forEach { subjectMinutes[it] = 0f }

                        studyLogs.forEach { log ->
                            val currentMinutes = subjectMinutes[log.subject] ?: 0f
                            subjectMinutes[log.subject] = currentMinutes + log.durationMinutes.toFloat()
                        }

                        // Drawing our beautiful Jetpack Compose Bar chart
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(skin.background.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                subjectMinutes.forEach { (sub, mins) ->
                                    val barHeightRatio = if (mins > 0f) (mins / 300f).coerceAtMost(1f) else 0.05f
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            "${mins.toInt()}m",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = skin.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight(barHeightRatio)
                                                .width(18.dp)
                                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                .background(skin.primary)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            sub.take(4),
                                            fontSize = 9.sp,
                                            color = skin.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Score metrics
                        val avgScore = if (savedEvaluations.isNotEmpty()) savedEvaluations.map { it.score }.average() else 7.2
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = skin.background)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Avg Scoring", fontSize = 11.sp, color = skin.accent)
                                    Text(
                                        String.format("%.1f/10", avgScore),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black,
                                        color = skin.onSurface
                                    )
                                }
                            }
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = skin.background)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Sprints Checked", fontSize = 11.sp, color = skin.accent)
                                    val progressScale = if (viewModel.activeMilestones.value.isNotEmpty()) {
                                        viewModel.activeMilestones.value.count { it.isCompleted }.toString()
                                    } else "0"
                                    Text(
                                        "$progressScale Done",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black,
                                        color = skin.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // MONTH-END FORECASTER PANELS
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = skin.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "MONTH-END SYLLABUS DISCOVERY FORECASTER",
                            color = skin.accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Macro Coverage Forecasts",
                            color = skin.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Text(
                            "Based on active revision logs and clear benchmarks, our forecast model calculates completion trajectories prior to the next ICAI Exam Cycle:",
                            color = skin.onSurface.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Forecast Row 1
                        ForecastProgressBar(label = "Group 1 General Papers", targetCoveragePercent = 0.68f, color = skin.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        ForecastProgressBar(label = "Group 2 Analytical Papers", targetCoveragePercent = 0.45f, color = skin.accent)
                        Spacer(modifier = Modifier.height(12.dp))
                        ForecastProgressBar(label = "Auditing Standards & Codes", targetCoveragePercent = 0.55f, color = skin.primary)

                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(color = skin.border.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // AI PINPOINT REPORTING FOR WEAK CHAPTERS
                        Text(
                            "PINPOINT RE-STUDY RECOMMENDATIONS",
                            fontWeight = FontWeight.Bold,
                            color = skin.accent,
                            fontSize = 11.sp
                        )
                        Text(
                            "• Standards on Auditing (SA 240 / 500) score benchmarks are slightly below aggregate requirements. Practice writing technical keywords in the ICAI Evaluator Agent.\n\n" +
                            "• residential Status calculations in Taxation Group 1 requires revision. Read section 6(1) provisions utilizing Note-Gen Agent.\n\n" +
                            "• Cost & Management Accounting variance analysis formulas require acronyms indexing.",
                            fontSize = 12.sp,
                            color = skin.onSurface,
                            lineHeight = 17.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ForecastProgressBar(label: String, targetCoveragePercent: Float, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("${(targetCoveragePercent * 100).toInt()}% Coverage", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = targetCoveragePercent,
            color = color,
            trackColor = color.copy(alpha = 0.15f),
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
        )
    }
}

// --- DYNAMIC THEMING SELECTION INTERFACE (50 POP skins) ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkinSelectorView(viewModel: MainViewModel, activeSkin: Skin) {
    var selectedCategory by remember { mutableStateOf("Marvel") } // "Marvel", "DC", "Cartoons"

    val currentCategorySkins = SkinRepository.skins.filter { it.category == selectedCategory }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = activeSkin.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "POP-CULTURE COSMETIC THEMING ENGINE",
                        color = activeSkin.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Exactly 50 Unique Visual Skins",
                        color = activeSkin.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "Shift entire UI variables (primary accents, gradients, borders, buttons, text styling) to match your favorite cartoon, superhero, or pop-culture universe. Choose below:",
                        color = activeSkin.onSurface.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Categories selector capsules
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Marvel", "DC", "Cartoons").forEach { cat ->
                            val isCatSel = selectedCategory == cat
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCatSel) activeSkin.primary else activeSkin.surface.copy(alpha = 0.4f))
                                    .clickable { selectedCategory = cat }
                                    .padding(vertical = 10.dp)
                                    .border(1.dp, if (isCatSel) activeSkin.primary else activeSkin.border.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    cat.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isCatSel) activeSkin.onPrimary else activeSkin.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "AVAILABLE UNIVERSE CLONES (${currentCategorySkins.size} Skins)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = activeSkin.accent,
                    letterSpacing = 1.sp
                )
                Text(
                    "Tab skin to activate",
                    fontSize = 10.sp,
                    color = activeSkin.onSurface.copy(alpha = 0.4f)
                )
            }
        }

        // Render skins as Cards inside Grid
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = 2
            ) {
                currentCategorySkins.forEach { skinOption ->
                    val isActivated = skinOption.id == viewModel.currentThemeId.value
                    Card(
                        colors = CardDefaults.cardColors(containerColor = skinOption.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 10.dp)
                            .clickable { viewModel.updateTheme(skinOption.id) }
                            .testTag("skin_card_${skinOption.id}")
                            .border(
                                width = if (isActivated) 2.dp else 0.5.dp,
                                color = if (isActivated) activeSkin.accent else skinOption.primary.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    skinOption.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = skinOption.onSurface
                                )
                                if (isActivated) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(activeSkin.accent)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Color bubble anchors
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(skinOption.primary)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(skinOption.secondary)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(skinOption.accent)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- APP BOTTOM NAVIGATION COMPONENT ---

@Composable
fun AppBottomBar(
    skin: Skin,
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = skin.surface,
        tonalElevation = 8.dp
    ) {
        val menuItems = listOf(
            Triple("Home HUD", Icons.Filled.Dashboard, "home_hud_tab"),
            Triple("Syllabus", Icons.Filled.MenuBook, "syllabus_tab"),
            Triple("Sprints", Icons.Filled.CalendarMonth, "sprints_tab"),
            Triple("Note-Gen AI", Icons.Filled.Bolt, "note_gen_tab"),
            Triple("ICAI Evaluator", Icons.Filled.Rule, "evaluator_tab"),
            Triple("Analytics", Icons.Filled.QueryStats, "analytics_tab")
        )

        menuItems.forEach { (title, icon, testTagId) ->
            NavigationBarItem(
                selected = activeTab == title,
                onClick = { onTabSelected(title) },
                icon = { Icon(icon, contentDescription = title, modifier = Modifier.size(20.dp)) },
                label = { Text(title, fontSize = 9.sp, maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = skin.secondary,
                    selectedTextColor = skin.secondary,
                    unselectedIconColor = skin.onSurface.copy(alpha = 0.6f),
                    unselectedTextColor = skin.onSurface.copy(alpha = 0.6f),
                    indicatorColor = skin.primary
                ),
                modifier = Modifier.testTag(testTagId)
            )
        }
    }
}
