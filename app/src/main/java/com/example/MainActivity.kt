package com.example

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

// Sealed screen structures
sealed class Screen {
    object Folders : Screen()
    data class TreeViewer(val folder: FamilyFolder) : Screen()
    data class TimelineViewer(val folder: FamilyFolder) : Screen()
    object Reminders : Screen()
    object Settings : Screen()
}

class FamilyViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    val dao = db.familyDao()

    // Undo / Redo Stacks for Tree modifications
    sealed class TreeAction {
        data class InsertMemberAction(val member: FamilyMember) : TreeAction()
        data class DeleteMemberAction(val member: FamilyMember) : TreeAction()
        data class EditMemberAction(val oldMember: FamilyMember, val newMember: FamilyMember) : TreeAction()
    }

    private val undoStack = mutableListOf<TreeAction>()
    private val redoStack = mutableListOf<TreeAction>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private fun pushAction(action: TreeAction) {
        undoStack.add(action)
        redoStack.clear()
        updateUndoRedoStates()
    }

    private fun updateUndoRedoStates() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun undo() {
        viewModelScope.launch {
            if (undoStack.isEmpty()) return@launch
            val action = undoStack.removeAt(undoStack.lastIndex)
            when (action) {
                is TreeAction.InsertMemberAction -> {
                    dao.deleteMemberById(action.member.id)
                }
                is TreeAction.DeleteMemberAction -> {
                    dao.insertMember(action.member)
                }
                is TreeAction.EditMemberAction -> {
                    dao.updateMember(action.oldMember)
                }
            }
            redoStack.add(action)
            updateUndoRedoStates()
        }
    }

    fun redo() {
        viewModelScope.launch {
            if (redoStack.isEmpty()) return@launch
            val action = redoStack.removeAt(redoStack.lastIndex)
            when (action) {
                is TreeAction.InsertMemberAction -> {
                    dao.insertMember(action.member)
                }
                is TreeAction.DeleteMemberAction -> {
                    dao.deleteMemberById(action.member.id)
                }
                is TreeAction.EditMemberAction -> {
                    dao.updateMember(action.newMember)
                }
            }
            undoStack.add(action)
            updateUndoRedoStates()
        }
    }

    // Preferences SharedPreferences
    private val prefs = application.getSharedPreferences("vanshvriksh_prefs", Context.MODE_PRIVATE)

    // Security Unlock States
    private val _savedPin = MutableStateFlow(prefs.getString("security_pin", "") ?: "")
    val savedPin: StateFlow<String> = _savedPin.asStateFlow()

    private val _isUnlocked = MutableStateFlow(prefs.getString("security_pin", "").isNullOrEmpty())
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _isCustomDarkPreferred = MutableStateFlow(prefs.getBoolean("pref_dark_mode", false))
    val isCustomDarkPreferred: StateFlow<Boolean> = _isCustomDarkPreferred.asStateFlow()

    // Database Reactive Flows
    val foldersFlow: StateFlow<List<FamilyFolder>> = dao.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val remindersFlow: StateFlow<List<Reminder>> = dao.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active folder members
    private val _activeFolderId = MutableStateFlow<Long?>(null)
    val activeFolderMembers: StateFlow<List<FamilyMember>> = _activeFolderId
        .flatMapLatest { id ->
            if (id != null) dao.getMembersInFolder(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtering & Searching flows inside Member Tree view
    val searchQuery = MutableStateFlow("")
    val filterGender = MutableStateFlow("All") // "All", "Male", "Female"
    val filterStarredOnly = MutableStateFlow(false)

    // Filtered list derived state helper
    val filteredMembers: StateFlow<List<FamilyMember>> = combine(
        activeFolderMembers,
        searchQuery,
        filterGender,
        filterStarredOnly
    ) { members, query, gender, starred ->
        members.filter { m ->
            val matchesQuery = m.fullName.contains(query, ignoreCase = true) || 
                                m.occupation.contains(query, ignoreCase = true) ||
                                m.birthPlace.contains(query, ignoreCase = true)
            val matchesGender = (gender == "All") || (m.gender == gender)
            val matchesStarred = !starred || m.isStarred
            matchesQuery && matchesGender && matchesStarred
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Boostrap mock starter data if database is clean & empty
        viewModelScope.launch {
            dao.getAllFolders().first().let { current ->
                if (current.isEmpty()) {
                    bootstrapDemoFamily()
                }
            }
        }
    }

    // PIN passcode updater
    fun updatePin(newPin: String) {
        prefs.edit().putString("security_pin", newPin).apply()
        _savedPin.value = newPin
        _isUnlocked.value = newPin.isEmpty()
    }

    fun setUnlocked(unlocked: Boolean) {
        _isUnlocked.value = unlocked
    }

    // Theme dynamic toggler
    fun toggleDarkMode(customDark: Boolean) {
        prefs.edit().putBoolean("pref_dark_mode", customDark).apply()
        _isCustomDarkPreferred.value = customDark
    }

    private val _activeAccentColor = MutableStateFlow(Color(prefs.getLong("pref_accent_color", 0xFFD4AF37L)))
    val activeAccentColor: StateFlow<Color> = _activeAccentColor.asStateFlow()

    fun updateAccentColor(color: Color) {
        prefs.edit().putLong("pref_accent_color", color.value.toLong()).apply()
        _activeAccentColor.value = color
    }

    private val _activeStyle = MutableStateFlow(prefs.getInt("pref_active_style", 1))
    val activeStyle: StateFlow<Int> = _activeStyle.asStateFlow()

    fun updateActiveStyle(style: Int) {
        prefs.edit().putInt("pref_active_style", style).apply()
        _activeStyle.value = style
    }

    private val _activeFontSize = MutableStateFlow(prefs.getFloat("pref_font_size", 12f))
    val activeFontSize: StateFlow<Float> = _activeFontSize.asStateFlow()

    fun updateFontSize(size: Float) {
        prefs.edit().putFloat("pref_font_size", size).apply()
        _activeFontSize.value = size
    }

    // Set Active folder id to drive details
    fun selectFolder(folderId: Long) {
        _activeFolderId.value = folderId
    }

    // CRUD - Folder
    fun addFolder(name: String, desc: String, referenceYear: String, certificatePath: String, issuerName: String = "", signaturePath: String = "") {
        viewModelScope.launch {
            dao.insertFolder(
                FamilyFolder(
                    name = name,
                    description = desc,
                    referenceYear = referenceYear,
                    certificatePath = certificatePath,
                    issuerName = issuerName,
                    signaturePath = signaturePath
                )
            )
        }
    }

    fun addScannedFamilyTree(
        name: String,
        desc: String,
        referenceYear: String,
        certificatePath: String,
        scannedMembers: List<GeminiMember>
    ) {
        viewModelScope.launch {
            // 1. Insert folder first
            val folderId = dao.insertFolder(
                FamilyFolder(
                    name = name,
                    description = desc,
                    referenceYear = referenceYear,
                    certificatePath = certificatePath
                )
            )
            
            // 2. Insert all members to SQLite, maintaining a map of tempId -> generated DB ID
            val idMapping = mutableMapOf<Long, Long>()
            val insertedMembers = scannedMembers.map { gemini ->
                val newMember = FamilyMember(
                    folderId = folderId,
                    firstName = gemini.firstName,
                    lastName = gemini.lastName,
                    gender = gemini.gender,
                    birthDate = "",
                    deathDate = "",
                    birthPlace = "",
                    occupation = "",
                    notes = "AI Autocompleted",
                    photoPath = "",
                    documentPath = "",
                    fatherId = null,
                    motherId = null,
                    spouseId = null
                )
                val generatedId = dao.insertMember(newMember)
                idMapping[gemini.tempId] = generatedId
                gemini to generatedId
            }
            
            // 3. Now update the father, mother and spouse relationships based on correct SQLite IDs
            insertedMembers.forEach { (gemini, dbId) ->
                val updatedFatherId = gemini.fatherTempId?.let { idMapping[it] }
                val updatedMotherId = gemini.motherTempId?.let { idMapping[it] }
                val updatedSpouseId = gemini.spouseTempId?.let { idMapping[it] }
                
                dao.getMemberById(dbId)?.let { member ->
                    val withRelations = member.copy(
                        fatherId = updatedFatherId,
                        motherId = updatedMotherId,
                        spouseId = updatedSpouseId
                    )
                    dao.updateMember(withRelations)
                }
            }
        }
    }

    fun deleteFolder(folderId: Long) {
        viewModelScope.launch {
            dao.deleteFolderById(folderId)
        }
    }

    fun updateFolder(folder: FamilyFolder) {
        viewModelScope.launch {
            dao.updateFolder(folder)
        }
    }

    // CRUD - Member
    fun saveMember(
        id: Long = 0,
        folderId: Long,
        firstName: String,
        lastName: String,
        gender: String,
        birthDate: String,
        deathDate: String,
        birthPlace: String,
        occupation: String,
        notes: String,
        photoPath: String,
        documentPath: String,
        fatherId: Long?,
        motherId: Long?,
        spouseId: Long?,
        marriageDate: String = "",
        isTargetSubject: Boolean = false,
        mobile: String,
        email: String,
        isStarred: Boolean,
        addReminderTitle: String? // e.g. "जन्मदिन (Birthday)" or "पुण्यतिथि"
    ) {
        viewModelScope.launch {
            // Standard values encrypted with security helper using active pin
            val encNotes = SecurityHelper.encrypt(notes, _savedPin.value.ifEmpty { "1234" })
            val encMobile = SecurityHelper.encrypt(mobile, _savedPin.value.ifEmpty { "1234" })
            val encEmail = SecurityHelper.encrypt(email, _savedPin.value.ifEmpty { "1234" })

            val member = FamilyMember(
                id = id,
                folderId = folderId,
                firstName = firstName,
                lastName = lastName,
                gender = gender,
                birthDate = birthDate,
                deathDate = deathDate,
                birthPlace = birthPlace,
                occupation = occupation,
                notes = encNotes,
                photoPath = photoPath,
                documentPath = documentPath,
                fatherId = fatherId,
                motherId = motherId,
                spouseId = spouseId,
                marriageDate = marriageDate,
                isTargetSubject = isTargetSubject,
                mobile = encMobile,
                email = encEmail,
                isStarred = isStarred
            )

            val currentOld = if (id != 0L) dao.getMemberById(id) else null

            val savedMemberId = if (id == 0L) {
                val newId = dao.insertMember(member)
                val inserted = dao.getMemberById(newId)
                if (inserted != null) {
                    pushAction(TreeAction.InsertMemberAction(inserted))
                }
                newId
            } else {
                if (currentOld != null) {
                    pushAction(TreeAction.EditMemberAction(currentOld, member))
                }
                dao.updateMember(member)
                id
            }

            // Sync spouse bidirectionally in database
            if (spouseId != null) {
                val newSpouse = dao.getMemberById(spouseId)
                if (newSpouse != null) {
                    // Check if new spouse was married to someone else, decouple their old partner
                    if (newSpouse.spouseId != null && newSpouse.spouseId != savedMemberId) {
                        val oldPartner = dao.getMemberById(newSpouse.spouseId!!)
                        if (oldPartner != null && oldPartner.spouseId == spouseId) {
                            dao.updateMember(oldPartner.copy(spouseId = null))
                        }
                    }
                    // Couple new spouse to current member
                    dao.updateMember(newSpouse.copy(spouseId = savedMemberId))
                }
            } else {
                // If spouseId is set to null, decouple any old spouse
                if (currentOld != null && currentOld.spouseId != null) {
                    val oldSpouse = dao.getMemberById(currentOld.spouseId)
                    if (oldSpouse != null && oldSpouse.spouseId == savedMemberId) {
                        dao.updateMember(oldSpouse.copy(spouseId = null))
                    }
                }
            }

            // Clean previous partner of current member if spouse selection changed
            if (currentOld != null && currentOld.spouseId != null && currentOld.spouseId != spouseId) {
                val oldSpouse = dao.getMemberById(currentOld.spouseId)
                if (oldSpouse != null && oldSpouse.spouseId == savedMemberId) {
                    dao.updateMember(oldSpouse.copy(spouseId = null))
                }
            }

            // Optional auto reminder builder
            if (addReminderTitle != null) {
                val remDate = birthDate.ifBlank { "2026-06-09" }
                val reminder = Reminder(
                    folderId = folderId,
                    memberId = savedMemberId,
                    memberName = "$firstName $lastName",
                    title = addReminderTitle,
                    date = remDate,
                    notes = "$firstName $lastName का पारिवारिक उत्सव"
                )
                dao.insertReminder(reminder)
            }
        }
    }

    fun deleteMember(memberId: Long) {
        viewModelScope.launch {
            val member = dao.getMemberById(memberId)
            if (member != null) {
                pushAction(TreeAction.DeleteMemberAction(member))
                dao.deleteMemberById(memberId)
            }
        }
    }

    fun toggleMemberStar(member: FamilyMember) {
        viewModelScope.launch {
            dao.updateMember(member.copy(isStarred = !member.isStarred))
        }
    }

    fun deleteReminder(reminderId: Long) {
        viewModelScope.launch {
            dao.deleteReminderById(reminderId)
        }
    }

    fun importBackupData(json: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val isSuccess = TreeExporter.importTreeFromJson(json, dao)
            onComplete(isSuccess)
        }
    }

    private suspend fun bootstrapDemoFamily() {
        // Create an engaging Starter royal Indian Clan to show off on first app load!
        val folderId = dao.insertFolder(
            FamilyFolder(
                name = "चिराम जी का मुख्य परिवार (Chiram Family)",
                description = "यह वंशावली चिराम कुटुंब के पौराणिक इतिहास एवं आधुनिक पीढ़ियों को दर्शाती है।"
            )
        )

        // Seed Root Grandparents
        val fNotes = SecurityHelper.encrypt("चिराम परिवार के सबसे बुजुर्ग वरिष्ठ सलाहकार सदस्य, समाजसेवक।", "1234")
        val gFatherId = dao.insertMember(
            FamilyMember(
                folderId = folderId,
                firstName = "रामचंद्र",
                lastName = "चिराम",
                gender = "Male",
                birthDate = "1940-08-15",
                deathDate = "2021-02-14",
                birthPlace = "वाराणसी, उत्तर प्रदेश",
                occupation = "वरिष्ठ गुरुजी (Teacher)",
                notes = fNotes,
                isStarred = true
            )
        )

        val mNotes = SecurityHelper.encrypt("गृहलक्ष्मी, चिराम कुल की पूजनीय संरक्षिका, पौराणिक लोक गीतों की ज्ञाता।", "1234")
        val gMotherId = dao.insertMember(
            FamilyMember(
                folderId = folderId,
                firstName = "सावित्री",
                lastName = "चिराम",
                gender = "Female",
                birthDate = "1946-04-10",
                deathDate = "",
                birthPlace = "प्रयागराज",
                occupation = "गृहणी (Home Maker)",
                notes = mNotes,
                spouseId = gFatherId,
                isStarred = false
            )
        )

        // Link grandparent spouse bidirectionally
        dao.updateMember(
            dao.getMemberById(gFatherId)!!.copy(spouseId = gMotherId)
        )

        // Seed Father (Son of रामचंद्र & सावित्री)
        val dadNotes = SecurityHelper.encrypt("सॉफ्टवेयर आर्किटेक्ट, तकनीक प्रेमी, वंशावली के रचयिता।", "1234")
        val fatherId = dao.insertMember(
            FamilyMember(
                folderId = folderId,
                firstName = "अजय",
                lastName = "चिराम",
                gender = "Male",
                birthDate = "1975-11-20",
                birthPlace = "वाराणसी",
                occupation = "तकनीकी विशेषज्ञ",
                notes = dadNotes,
                fatherId = gFatherId,
                motherId = gMotherId,
                isStarred = true
            )
        )

        // Seed Mother (Spouse of अजय)
        val momNotes = SecurityHelper.encrypt("शिक्षिका, बागबानी करना और कवितायें लिखना पसंद है।", "1234")
        val motherId = dao.insertMember(
            FamilyMember(
                folderId = folderId,
                firstName = "प्रिया",
                lastName = "चिराम",
                gender = "Female",
                birthDate = "1980-05-18",
                birthPlace = "लखनऊ",
                occupation = "सरकारी स्कूल अध्यापिका",
                notes = momNotes,
                spouseId = fatherId,
                isStarred = false
            )
        )

        // Link AJay to Priya bidirectionally
        dao.updateMember(
            dao.getMemberById(fatherId)!!.copy(spouseId = motherId)
        )

        // Seed Child/Son
        val sonNotes = SecurityHelper.encrypt("विद्यार्थी, गिटार बजाना और कोडिंग करना पसंद है।", "1234")
        val sonId = dao.insertMember(
            FamilyMember(
                folderId = folderId,
                firstName = "आरव",
                lastName = "चिराम",
                gender = "Male",
                birthDate = "2010-09-09",
                birthPlace = "दिल्ली",
                occupation = "छात्र",
                notes = sonNotes,
                fatherId = fatherId,
                motherId = motherId,
                isStarred = false
            )
        )

        // Seed Sister/Daughter
        val sisNotes = SecurityHelper.encrypt("चित्रकारी और शास्त्रीय संगीत कला में निपुण।", "1234")
        dao.insertMember(
            FamilyMember(
                folderId = folderId,
                firstName = "दीक्षा",
                lastName = "चिराम",
                gender = "Female",
                birthDate = "2015-12-25",
                birthPlace = "दिल्ली",
                occupation = "छात्रा",
                notes = sisNotes,
                fatherId = fatherId,
                motherId = motherId,
                isStarred = false
            )
        )

        // Add 1 Birthday Reminder
        dao.insertReminder(
            Reminder(
                folderId = folderId,
                memberId = sonId,
                memberName = "आरव चिराम",
                title = "जन्मदिन (Birthday Celebration)",
                date = "2010-09-09",
                notes = "आरव का सोलहवाँ जन्मदिन धूमधाम से मनाना है!"
            )
        )
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: FamilyViewModel = viewModel()
            val savedPin by viewModel.savedPin.collectAsStateWithLifecycle()
            val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()
            val isCustomDarkPreferred by viewModel.isCustomDarkPreferred.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = isCustomDarkPreferred) {
                if (!isUnlocked) {
                    SecurityLockScreen(
                        savedPin = savedPin,
                        onPinCreated = { newPin ->
                            viewModel.updatePin(newPin)
                            viewModel.setUnlocked(true)
                        },
                        onUnlockSuccess = {
                            viewModel.setUnlocked(true)
                        }
                    )
                } else {
                    AppMainWorkflow(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMainWorkflow(viewModel: FamilyViewModel) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Folders) }

    // Observe Data States
    val folders by viewModel.foldersFlow.collectAsStateWithLifecycle()
    val filteredMembers by viewModel.filteredMembers.collectAsStateWithLifecycle()
    val rawMembersInView by viewModel.activeFolderMembers.collectAsStateWithLifecycle()
    val allReminders by viewModel.remindersFlow.collectAsStateWithLifecycle()
    val isCustomDarkPreferred by viewModel.isCustomDarkPreferred.collectAsStateWithLifecycle()
    val activeStyleCode by viewModel.activeStyle.collectAsStateWithLifecycle()
    val activeFontSize by viewModel.activeFontSize.collectAsStateWithLifecycle()

    // Add/Edit Dialog Controls
    var activeFolderForEdit by remember { mutableStateOf<FamilyFolder?>(null) }
    var selectedMemberForEdit by remember { mutableStateOf<FamilyMember?>(null) }
    var showMemberEditDialog by remember { mutableStateOf(false) }

    // Filter controls UI
    val searchTxt by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedGenderFilter by viewModel.filterGender.collectAsStateWithLifecycle()
    val starredOnlyFilter by viewModel.filterStarredOnly.collectAsStateWithLifecycle()
    var showFilterBar by remember { mutableStateOf(false) }

    // Toggle Custom Dark Theme
    val customDarkPreferred by viewModel.isCustomDarkPreferred.collectAsStateWithLifecycle()
    val activeAccentColor by viewModel.activeAccentColor.collectAsStateWithLifecycle()

    val treeTheme = remember(activeStyleCode, activeFontSize, activeAccentColor) {
        com.example.ui.theme.TreeTheme(styleId = activeStyleCode, fontSize = activeFontSize.sp, primaryColor = activeAccentColor)
    }

    androidx.compose.runtime.CompositionLocalProvider(com.example.ui.theme.LocalTreeTheme provides treeTheme) {
        Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
        when (screen) {
            is Screen.Folders -> {
                FoldersScreen(
                    folders = folders,
                    onFolderSelected = { folder ->
                        viewModel.selectFolder(folder.id)
                        activeFolderForEdit = folder
                        currentScreen = Screen.TreeViewer(folder)
                    },
                    onAddFolder = { name, desc, referenceYear, certificatePath, issuerName, signaturePath ->
                        viewModel.addFolder(name, desc, referenceYear, certificatePath, issuerName, signaturePath)
                        Toast.makeText(context, "नया फोल्डर बनाया गया!", Toast.LENGTH_SHORT).show()
                    },
                    onAddScannedFolder = { name, desc, referenceYear, certificatePath, scannedMembers ->
                        viewModel.addScannedFamilyTree(name, desc, referenceYear, certificatePath, scannedMembers)
                        Toast.makeText(context, "AI स्वतः-निर्मित वंशवृक्ष सहेजा गया!", Toast.LENGTH_SHORT).show()
                    },
                    onUpdateFolder = { folder ->
                        viewModel.updateFolder(folder)
                        Toast.makeText(context, "पारिवारिक जानकारी अपडेट की गई!", Toast.LENGTH_SHORT).show()
                    },
                    onDeleteFolder = { folder ->
                        viewModel.deleteFolder(folder.id)
                        Toast.makeText(context, "फोल्डर हटा दिया गया!", Toast.LENGTH_SHORT).show()
                    },
                    onOpenGlobalReminders = { currentScreen = Screen.Reminders },
                    onOpenGlobalBackup = { currentScreen = Screen.Settings },
                    isDarkMode = isCustomDarkPreferred,
                    onThemeChanged = { viewModel.toggleDarkMode(it) }
                )
            }

            is Screen.TreeViewer -> {
                val folder = folders.find { it.id == screen.folder.id } ?: screen.folder
                val savedPin by viewModel.savedPin.collectAsStateWithLifecycle()
                var lastAutoSaveTime by remember { mutableStateOf<String?>(null) }
                var showCertificateDialog by remember { mutableStateOf(false) }
                var showHistoryDialog by remember { mutableStateOf(false) }
                var selectedMemberIdForSync by remember { mutableStateOf<Long?>(null) }

                if (showHistoryDialog) {
                    var historyText by remember { mutableStateOf(folder.history) }
                    AlertDialog(
                        onDismissRequest = { showHistoryDialog = false },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "कुलवृत्तांत एवं पारिवारिक इतिहास",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                        },
                        text = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "अपने वंशवृक्ष (VanshVriksh) से संबंधित कहानियाँ, कुल की उत्पत्ति (Origins), और ऐतिहासिक मील के पत्थरों को यहाँ प्रलेखित करें। यह जानकारी पीडीएफ (PDF) रिपोर्ट में शामिल की जाएगी।",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                OutlinedTextField(
                                    value = historyText,
                                    onValueChange = { historyText = it },
                                    placeholder = { 
                                        Text(
                                            "यहाँ अपने परिवार के पूर्वजों का स्थान, गोत्र, कुलदेवी/कुलदेवता, ऐतिहासिक कहानियाँ एवं मुख्य मील के पत्थर दर्ज करें...", 
                                            fontSize = 13.sp
                                        ) 
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    maxLines = 15,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val updatedFolder = folder.copy(history = historyText)
                                    viewModel.updateFolder(updatedFolder)
                                    showHistoryDialog = false
                                    Toast.makeText(context, "पारिवारिक इतिहास सहेजा गया!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("सुरक्षित करें (Save)")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showHistoryDialog = false }) {
                                Text("रद्द करें")
                            }
                        }
                    )
                }

                if (showCertificateDialog && folder.certificatePath.isNotBlank()) {
                    AlertDialog(
                        onDismissRequest = { showCertificateDialog = false },
                        title = { Text("हस्तलिखित वंशावली दस्तावेज", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val certFile = File(folder.certificatePath)
                                if (certFile.exists()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(300.dp)
                                            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                             .clip(RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        coil.compose.AsyncImage(
                                            model = certFile,
                                            contentDescription = "Attached Handwritten Certificate",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                } else {
                                    Text("संलग्न वंशावली दस्तावेज फ़ाइल प्राप्त नहीं हुई।", color = Color.Red, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("फ़ाइल नाम: ${folder.certificatePath.substringAfterLast("/")}", fontSize = 11.sp, color = Color.Gray)
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showCertificateDialog = false }) {
                                Text("बंद करें (Close)")
                            }
                        }
                    )
                }
                
                // Periodic Local Auto-Save: Debounces edits and writes the JSON offline state
                LaunchedEffect(rawMembersInView, folder) {
                    if (rawMembersInView.isNotEmpty()) {
                        // Debounce: Wait 5 seconds of tranquility after any modification to avoid high write cycles
                        kotlinx.coroutines.delay(5000)
                        val exported = TreeExporter.exportTreeToJson(
                            folder = folder,
                            members = rawMembersInView,
                            reminders = allReminders.filter { it.folderId == folder.id },
                            userPin = savedPin.ifEmpty { "1234" }
                        )
                        val file = TreeExporter.saveLocalAutoSave(context, folder.id, folder.name, exported)
                        if (file != null) {
                            val sdf = java.text.SimpleDateFormat("hh:mm:ss a", java.util.Locale.getDefault())
                            lastAutoSaveTime = sdf.format(java.util.Date(file.lastModified()))
                        }
                    }
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = folder.name + (if (folder.referenceYear.isNotBlank()) " (सन: ${folder.referenceYear})" else ""),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    val subtitleText = "सदस्य संख्या: ${filteredMembers.size}" + 
                                             (if (lastAutoSaveTime != null) " | ✓ ऑटो-सेव्ड: $lastAutoSaveTime" else " | प्रतीक्षारत...")
                                    Text(
                                        text = subtitleText,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = { currentScreen = Screen.Folders }) {
                                    Icon(Icons.Filled.Home, "Folders Home")
                                }
                            },
                            actions = {
                                // Family History (कुलवृत्तांत) Editor Button
                                IconButton(onClick = { showHistoryDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.MenuBook,
                                        contentDescription = "Family History (कुलवृत्तांत)",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                // Timeline Page Button
                                IconButton(onClick = { currentScreen = Screen.TimelineViewer(folder) }) {
                                    Icon(
                                        imageVector = Icons.Filled.Route,
                                        contentDescription = "ऐतिहासिक घटनाक्रम (Timeline)",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (folder.certificatePath.isNotBlank()) {
                                    IconButton(onClick = { showCertificateDialog = true }) {
                                        Icon(
                                            imageVector = Icons.Filled.Verified,
                                            contentDescription = "View Family Certificate",
                                            tint = Color(0xFFFFD700)
                                        )
                                    }
                                }

                                // Search toggle
                                IconButton(onClick = { showFilterBar = !showFilterBar }) {
                                    Icon(
                                        imageVector = if (showFilterBar) Icons.Filled.FilterListOff else Icons.Filled.Search,
                                        contentDescription = "Search & Filter"
                                    )
                                }
                                // Export JSON Backup Offline
                                val savedPin by viewModel.savedPin.collectAsStateWithLifecycle()
                                IconButton(onClick = {
                                    val exported = TreeExporter.exportTreeToJson(
                                        folder = folder,
                                        members = rawMembersInView,
                                        reminders = allReminders.filter { it.folderId == folder.id },
                                        userPin = savedPin.ifEmpty { "1234" }
                                    )
                                    TreeExporter.shareBackupFile(context, exported, folder.name)
                                    Toast.makeText(context, "डेटा सुरक्षित JSON फ़ाइल में एक्सपोर्ट किया गया!", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.Backup,
                                        contentDescription = "Export Offline JSON Backup"
                                    )
                                }
                                // Global Theme Toggle
                                IconButton(onClick = {
                                    viewModel.toggleDarkMode(!isCustomDarkPreferred)
                                    val msg = if (!isCustomDarkPreferred) "डार्क थीम सक्रिय की गई" else "सुंदर पारंपरिक लाइट थीम सक्रिय की गई"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(
                                        imageVector = if (isCustomDarkPreferred) Icons.Filled.WbSunny else Icons.Filled.NightsStay,
                                        contentDescription = "Toggle Theme"
                                    )
                                }
                                // Settings
                                IconButton(onClick = { currentScreen = Screen.Settings }) {
                                    Icon(Icons.Filled.Settings, "Settings")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Expandable search & filter panel
                        AnimatedVisibility(
                            visible = showFilterBar,
                            enter = slideInVertically() + fadeIn(),
                            exit = slideOutVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // SEARCH BAR
                                OutlinedTextField(
                                    value = searchTxt,
                                    onValueChange = { viewModel.searchQuery.value = it },
                                    placeholder = { Text("सदस्य का नाम खोजें (e.g. अजय श.)") },
                                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )

                                // ADVANCED GENDER & STARRED FILTERS
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Row of Genders
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("लिंग:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        listOf("All", "Male", "Female").forEach { g ->
                                            val label = if (g == "All") "सभी" else if (g == "Male") "पुरुष" else "महिला"
                                            FilterChip(
                                                selected = selectedGenderFilter == g,
                                                onClick = { viewModel.filterGender.value = g },
                                                label = { Text(label, fontSize = 10.sp) }
                                            )
                                        }
                                    }

                                    // Starred Toggle Filter
                                    FilterChip(
                                        selected = starredOnlyFilter,
                                        onClick = { viewModel.filterStarredOnly.value = !starredOnlyFilter },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.Favorite,
                                                null,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        },
                                        label = { Text("प्रमुख", fontSize = 10.sp) }
                                    )
                                }
                            }
                        }

                        // THE MAIN INTERACTIVE TREE CANVAS!
                        Box(modifier = Modifier.weight(1f)) {
                            if (rawMembersInView.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                                        Icon(Icons.Filled.AccountTree, null, tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(80.dp))
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("यह वंशावली रिक्त है!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("ऊपरी कोने में (+) सदस्य जोड़ें बटन दबाकर अपने परिवार का पहला सदस्य जोड़ें और पीढ़ियों की सुंदर रेखाएं बनाएं।", fontSize = 12.sp, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    }
                                }
                            } else {
                                FamilyTreeCanvas(
                                    members = filteredMembers,
                                    treeName = folder.name,
                                    referenceYear = folder.referenceYear,
                                    onMemberSelected = { member ->
                                        selectedMemberIdForSync = member.id
                                    },
                                    onAddRelation = { _, _ -> },
                                    onEditMember = { member ->
                                        selectedMemberForEdit = member
                                        showMemberEditDialog = true
                                    },
                                    onToggleStar = { member ->
                                        viewModel.toggleMemberStar(member)
                                    },
                                    onAddMember = {
                                        selectedMemberForEdit = null
                                        showMemberEditDialog = true
                                    },
                                    onPrintReport = {
                                        TreeExporter.sharePdfReport(context, folder, rawMembersInView)
                                    },
                                    onSavePdfReport = {
                                        TreeExporter.sharePdfReport(context, folder, rawMembersInView)
                                    },
                                    selectedMemberId = selectedMemberIdForSync
                                )
                            }
                        }
                    }
                }
            }

            is Screen.TimelineViewer -> {
                val folder = folders.find { it.id == screen.folder.id } ?: screen.folder
                val savedPin by viewModel.savedPin.collectAsStateWithLifecycle()
                var selectedMemberIdForSync by remember { mutableStateOf<Long?>(null) }

                LaunchedEffect(folder) {
                    activeFolderForEdit = folder
                }

                androidx.activity.compose.BackHandler {
                    currentScreen = Screen.TreeViewer(folder)
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "ऐतिहासिक घटनाक्रम (Timeline) - ${folder.name}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { currentScreen = Screen.TreeViewer(folder) }) {
                                    Icon(Icons.Filled.ArrowBack, "Back to Tree View")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        FamilyTimelinePane(
                            members = rawMembersInView,
                            userPin = savedPin.ifEmpty { "1234" },
                            selectedMemberId = selectedMemberIdForSync,
                            onChooseMember = { member ->
                                selectedMemberIdForSync = member.id
                                selectedMemberForEdit = member
                                showMemberEditDialog = true
                            }
                        )
                    }
                }
            }

            is Screen.Reminders -> {
                RemindersScreen(
                    reminders = allReminders,
                    onBack = { currentScreen = Screen.Folders },
                    onDeleteReminder = { id ->
                        viewModel.deleteReminder(id)
                        Toast.makeText(context, "रिमाइंडर हटाया गया!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            is Screen.Settings -> {
                val savedPin by viewModel.savedPin.collectAsStateWithLifecycle()

                BackupSettingsScreen(
                    currentPin = savedPin,
                    onPinUpdated = { newPin ->
                        viewModel.updatePin(newPin)
                        Toast.makeText(context, "सुरक्षा पिन सफलतापूर्वक बदला गया!", Toast.LENGTH_SHORT).show()
                    },
                    onBack = { currentScreen = Screen.Folders },
                    onExportTrigger = {
                        val activeF = activeFolderForEdit
                        if (activeF != null) {
                            val exported = TreeExporter.exportTreeToJson(
                                folder = activeF,
                                members = rawMembersInView,
                                reminders = allReminders.filter { it.folderId == activeF.id },
                                userPin = savedPin.ifEmpty { "1234" }
                            )
                            TreeExporter.shareBackupFile(context, exported, activeF.name)
                        } else {
                            Toast.makeText(context, "कृपया पहले किसी फैमली ट्री को खोलें!", Toast.LENGTH_LONG).show()
                        }
                    },
                    onImportTrigger = { jsonCode ->
                        viewModel.importBackupData(jsonCode) { isSuccess ->
                            if (isSuccess) {
                                Toast.makeText(context, "पारिवारिक डेटा ऑफलाइन बैकअप से सफ़लतापूर्वक आयात किया गया!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "आयात विफल! कृपया कोड की पुनः जांच कर सही JSON पेस्ट करें।", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onPdfReportTrigger = {
                        val activeF = activeFolderForEdit
                        if (activeF != null) {
                            TreeExporter.sharePdfReport(context, activeF, rawMembersInView)
                        } else {
                            Toast.makeText(context, "कृपया पहले किसी वंशावली को खोलें!", Toast.LENGTH_LONG).show()
                        }
                    },
                    isDarkMode = isCustomDarkPreferred,
                    onThemeChanged = { isDark ->
                        viewModel.toggleDarkMode(isDark)
                        val msg = if (isDark) "डार्क थीम सक्रिय की गई" else "सुंदर पारंपरिक लाइट थीम सक्रिय की गई"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    activeStylePreference = activeStyleCode,
                    onStyleChanged = { styleId ->
                        viewModel.updateActiveStyle(styleId)
                        Toast.makeText(context, "वंशावली शैली अपडेट की गई!", Toast.LENGTH_SHORT).show()
                    },
                    fontSize = activeFontSize,
                    onFontSizeChanged = { fontSize ->
                        viewModel.updateFontSize(fontSize)
                    }
                )
            }
        }
    }

    // Dynamic Edit / Add Member dialog block
    if (showMemberEditDialog) {
        val activeF = activeFolderForEdit
        if (activeF != null) {
            val savedPin by viewModel.savedPin.collectAsStateWithLifecycle()

            MemberDetailDialog(
                member = selectedMemberForEdit,
                allMembers = rawMembersInView,
                userPin = savedPin.ifEmpty { "1234" },
                onDismiss = { showMemberEditDialog = false },
                onSave = { firstName, lastName, gender, birthDate, deathDate, birthPlace, occupation, decryptedNotes, photoPath, documentPath, fatherId, motherId, spouseId, marriageDate, isTargetSubject, decryptedMobile, decryptedEmail, isStarred, addReminderTitle ->
                    viewModel.saveMember(
                        id = selectedMemberForEdit?.id ?: 0L,
                        folderId = activeF.id,
                        firstName = firstName,
                        lastName = lastName,
                        gender = gender,
                        birthDate = birthDate,
                        deathDate = deathDate,
                        birthPlace = birthPlace,
                        occupation = occupation,
                        notes = decryptedNotes,
                        photoPath = photoPath,
                        documentPath = documentPath,
                        fatherId = fatherId,
                        motherId = motherId,
                        spouseId = spouseId,
                        marriageDate = marriageDate,
                        isTargetSubject = isTargetSubject,
                        mobile = decryptedMobile,
                        email = decryptedEmail,
                        isStarred = isStarred,
                        addReminderTitle = addReminderTitle
                    )
                    showMemberEditDialog = false
                    Toast.makeText(context, "पारिवारिक सदस्य विवरण सुरक्षित किया गया!", Toast.LENGTH_SHORT).show()
                },
                onDelete = if (selectedMemberForEdit != null) {
                    {
                        viewModel.deleteMember(selectedMemberForEdit!!.id)
                        showMemberEditDialog = false
                        Toast.makeText(context, "सदस्य प्रोफाइल हटा दी गयी!", Toast.LENGTH_SHORT).show()
                    }
                } else null
            )
        }
    }
}
}
