package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "family_members")
data class FamilyMember(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nameEnglish: String,
    val nameHindi: String,
    val gender: String, // "Male", "Female", "Other"
    val generation: Int, // e.g., 1 for oldest ancestors, 2 for their children, etc.
    val fatherId: Int? = null,
    val motherId: Int? = null,
    val spouseId: Int? = null,
    val birthDate: String? = null,
    val deathDate: String? = null,
    val isDeceased: Boolean = false,
    val phone: String? = null,
    val address: String? = null,
    val occupation: String? = null,
    val notes: String? = null,
    val profileColorHex: String = "#4F46E5" // Hex color for avatar background
) {
    fun getName(hindi: Boolean): String {
        return if (hindi && nameHindi.isNotBlank()) nameHindi else nameEnglish
    }
}
