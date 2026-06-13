package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.FamilyMember
import com.example.data.repository.FamilyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FamilyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FamilyRepository

    // Raw, complete list of members
    val allMembers: StateFlow<List<FamilyMember>>

    // Language state: true = Hindi, false = English
    private val _isHindi = MutableStateFlow(true) // Defaults to Hindi as requested
    val isHindi: StateFlow<Boolean> = _isHindi.asStateFlow()

    // Selected member for detail views or focusing in the tree
    private val _selectedMember = MutableStateFlow<FamilyMember?>(null)
    val selectedMember: StateFlow<FamilyMember?> = _selectedMember.asStateFlow()

    // Filters for list/directory search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGeneration = MutableStateFlow<Int?>(null)
    val selectedGeneration: StateFlow<Int?> = _selectedGeneration.asStateFlow()

    private val _selectedGender = MutableStateFlow<String?>(null)
    val selectedGender: StateFlow<String?> = _selectedGender.asStateFlow()

    private val _livingFilter = MutableStateFlow<Boolean?>(null) // true = living, false = passed, null = all
    val livingFilter: StateFlow<Boolean?> = _livingFilter.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = FamilyRepository(database.familyMemberDao())
        
        allMembers = repository.allMembers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Sync selected member if it is modified
        viewModelScope.launch {
            allMembers.collect { list ->
                val currentSelected = _selectedMember.value
                if (currentSelected != null) {
                    val updated = list.find { it.id == currentSelected.id }
                    _selectedMember.value = updated ?: list.firstOrNull()
                } else if (list.isNotEmpty()) {
                    // Pre-select a default person (Aaryan Chiram id = 8 first, if not found then the first member)
                    val candidate = list.find { it.id == 8 } ?: list.firstOrNull()
                    _selectedMember.value = candidate
                }
            }
        }
    }

    // Toggle language
    fun toggleLanguage() {
        _isHindi.value = !_isHindi.value
    }

    // Select focal member for the Tree View
    fun selectMember(member: FamilyMember?) {
        _selectedMember.value = member
    }

    fun selectMemberById(id: Int) {
        val found = allMembers.value.find { it.id == id }
        if (found != null) {
            _selectedMember.value = found
        }
    }

    // Update filters
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun filterByGeneration(gen: Int?) {
        _selectedGeneration.value = gen
    }

    fun filterByGender(gender: String?) {
        _selectedGender.value = gender
    }

    fun filterByLivingStatus(living: Boolean?) {
        _livingFilter.value = living
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedGeneration.value = null
        _selectedGender.value = null
        _livingFilter.value = null
    }

    // Filtered list of members for directory UI
    val filteredMembers: StateFlow<List<FamilyMember>> = combine(
        allMembers,
        _searchQuery,
        _selectedGeneration,
        _selectedGender,
        _livingFilter
    ) { list, query, gen, gender, living ->
        list.filter { member ->
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                member.nameEnglish.contains(query, ignoreCase = true) ||
                        member.nameHindi.contains(query, ignoreCase = true) ||
                        (member.occupation?.contains(query, ignoreCase = true) == true) ||
                        (member.phone?.contains(query) == true)
            }
            val matchesGen = gen == null || member.generation == gen
            val matchesGender = gender == null || member.gender.equals(gender, ignoreCase = true)
            val matchesLiving = when (living) {
                true -> !member.isDeceased
                false -> member.isDeceased
                null -> true
            }
            matchesQuery && matchesGen && matchesGender && matchesLiving
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Tree relatives calculation for current highlighted node
    data class Relatives(
        val target: FamilyMember,
        val father: FamilyMember? = null,
        val mother: FamilyMember? = null,
        val spouse: FamilyMember? = null,
        val children: List<FamilyMember> = emptyList(),
        val siblings: List<FamilyMember> = emptyList()
    )

    val currentRelatives: StateFlow<Relatives?> = combine(
        _selectedMember,
        allMembers
    ) { selected, list ->
        if (selected == null) return@combine null

        val father = list.find { it.id == selected.fatherId }
        val mother = list.find { it.id == selected.motherId }
        val spouse = list.find { it.id == selected.spouseId }
        val children = list.filter { it.fatherId == selected.id || it.motherId == selected.id }
        
        // Siblings (share same father or mother, excluding selected self)
        val siblings = list.filter {
            it.id != selected.id && (
                (selected.fatherId != null && it.fatherId == selected.fatherId) ||
                (selected.motherId != null && it.motherId == selected.motherId)
            )
        }

        Relatives(
            target = selected,
            father = father,
            mother = mother,
            spouse = spouse,
            children = children,
            siblings = siblings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Operations
    fun saveMember(member: FamilyMember) {
        viewModelScope.launch {
            val generatedId: Int
            if (member.id == 0) {
                generatedId = repository.insertMember(member).toInt()
            } else {
                repository.updateMember(member)
                generatedId = member.id
            }

            // Sync reciprocal relationships automatically!
            val updatedMember = member.copy(id = generatedId)
            autoSyncRelations(updatedMember)
        }
    }

    private suspend fun autoSyncRelations(member: FamilyMember) {
        val all = allMembers.value

        // 1. If spouse sets spouseId, ensure that spouse has this member set as spouseId
        if (member.spouseId != null) {
            val spouse = all.find { it.id == member.spouseId }
            if (spouse != null && spouse.spouseId != member.id) {
                repository.updateMember(spouse.copy(spouseId = member.id))
            }
        } else {
            // If spouse is removed, clear the inverse spouses who had member.id as their spouse Id
            all.forEach { other ->
                if (other.spouseId == member.id) {
                    repository.updateMember(other.copy(spouseId = null))
                }
            }
        }

        // 2. If fatherId is set, ensure that child points correctly to father
        // This is handled by saving the child; but what if we dynamically bind?
        // We can check if any family members should be linked. The main database handles unidirectional IDs,
        // which matches the entity fields.
    }

    fun deleteMember(member: FamilyMember) {
        viewModelScope.launch {
            // Nullify references in spouses or children to prevent broken links
            val all = allMembers.value
            all.forEach { other ->
                var changed = false
                var mod = other
                if (other.spouseId == member.id) {
                    mod = mod.copy(spouseId = null)
                    changed = true
                }
                if (other.fatherId == member.id) {
                    mod = mod.copy(fatherId = null)
                    changed = true
                }
                if (other.motherId == member.id) {
                    mod = mod.copy(motherId = null)
                    changed = true
                }
                if (changed) {
                    repository.updateMember(mod)
                }
            }

            repository.deleteMember(member)
            if (_selectedMember.value?.id == member.id) {
                _selectedMember.value = allMembers.value.firstOrNull { it.id != member.id }
            }
        }
    }

    fun loadSampleData() {
        viewModelScope.launch {
            repository.clearAllMembers()
            val samples = repository.getPresetSampleMembers()
            repository.insertMembers(samples)
            // Focus on Aaryan Chiram (ID 8) after sample data loaded.
            _selectedMember.value = samples.find { it.id == 8 } ?: samples.firstOrNull()
        }
    }

    fun resetData() {
        viewModelScope.launch {
            repository.clearAllMembers()
            _selectedMember.value = null
        }
    }
}
