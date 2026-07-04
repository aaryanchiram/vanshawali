package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "family_folders")
data class FamilyFolder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val referenceYear: String = "", // Reference year (e.g., "१९९०" or "2024")
    val certificatePath: String = "", // File path for the handmade certificate
    val history: String = "", // Documented stories, origins, key historical milestones
    val signaturePath: String = "", // Uploaded signature image path of issuer
    val issuerName: String = "" // Name of the person issuing/declaring
)

@Entity(
    tableName = "family_members",
    foreignKeys = [
        ForeignKey(
            entity = FamilyFolder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FamilyMember(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: Long,
    val firstName: String,
    val lastName: String,
    val gender: String, // "Male" / "Female" / "Other"
    val birthDate: String = "", // YYYY-MM-DD
    val deathDate: String = "", // YYYY-MM-DD (Empty if alive)
    val birthPlace: String = "",
    val occupation: String = "",
    val notes: String = "", // To be encrypted
    val photoPath: String = "", // App-private sandbox path
    val documentPath: String = "", // App-private sandbox path
    val fatherId: Long? = null,
    val motherId: Long? = null,
    val spouseId: Long? = null,
    val mobile: String = "", // To be encrypted
    val email: String = "", // To be encrypted
    val isStarred: Boolean = false,
    val marriageDate: String = "", // Relationship marriage date
    val isTargetSubject: Boolean = false // Selected member for whom the tree is crafted
) {
    val fullName: String
        get() = if (lastName.isNotBlank()) "$firstName $lastName" else firstName

    val age: String
        get() {
            if (birthDate.isBlank()) return "???"
            try {
                val birthYear = birthDate.substring(0, 4).toInt()
                val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                
                if (deathDate.isNotBlank()) {
                    val deathYear = deathDate.substring(0, 4).toInt()
                    return "${deathYear - birthYear} (मृत्यु)"
                }
                
                return (currentYear - birthYear).toString()
            } catch (e: Exception) {
                return "???"
            }
        }
}

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = FamilyMember::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: Long,
    val memberId: Long,
    val memberName: String, // Denormalized for convenience
    val title: String, // e.g. "जन्मदिन (Birthday)", "पुण्यतिथि (Death Anniversary)"
    val date: String, // YYYY-MM-DD
    val notes: String = ""
)
