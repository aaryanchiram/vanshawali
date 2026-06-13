package com.example.data.repository

import com.example.data.local.FamilyMemberDao
import com.example.data.model.FamilyMember
import kotlinx.coroutines.flow.Flow

class FamilyRepository(private val familyMemberDao: FamilyMemberDao) {
    val allMembers: Flow<List<FamilyMember>> = familyMemberDao.getAllMembers()

    fun getMemberById(id: Int): Flow<FamilyMember?> = familyMemberDao.getMemberById(id)

    suspend fun getMemberByIdSuspend(id: Int): FamilyMember? = familyMemberDao.getMemberByIdSuspend(id)

    fun getMembersByIds(ids: List<Int>): Flow<List<FamilyMember>> = familyMemberDao.getMembersByIds(ids)

    suspend fun insertMember(member: FamilyMember): Long = familyMemberDao.insertMember(member)

    suspend fun insertMembers(members: List<FamilyMember>): List<Long> = familyMemberDao.insertMembers(members)

    suspend fun updateMember(member: FamilyMember) = familyMemberDao.updateMember(member)

    suspend fun deleteMember(member: FamilyMember) = familyMemberDao.deleteMember(member)

    suspend fun clearAllMembers() = familyMemberDao.clearAllMembers()

    fun getPresetSampleMembers(): List<FamilyMember> {
        // We set ID structures manually for sample relations
        return listOf(
            // Generation 1 (Grandparents)
            FamilyMember(
                id = 1,
                nameEnglish = "Ram Prasad Chiram",
                nameHindi = "राम प्रसाद चिराम",
                gender = "Male",
                generation = 1,
                spouseId = 2,
                birthDate = "1940-08-15",
                deathDate = "2021-02-12",
                isDeceased = true,
                occupation = "Farmer",
                notes = "The patriarch of the Chiram family. Traditional farmer who founded our villages' community system.",
                profileColorHex = "#B45309" // Amber
            ),
            FamilyMember(
                id = 2,
                nameEnglish = "Savitri Devi",
                nameHindi = "सावित्री देवी",
                gender = "Female",
                generation = 1,
                spouseId = 1,
                birthDate = "1945-03-10",
                deathDate = null,
                isDeceased = false,
                occupation = "Home Maker",
                notes = "Loved by everyone. Has amazing recipes and rules our kitchen.",
                profileColorHex = "#BE185D" // Pink
            ),

            // Generation 2 (Parents / Uncles)
            FamilyMember(
                id = 3,
                nameEnglish = "Ramesh Chiram",
                nameHindi = "रमेश चिराम",
                gender = "Male",
                generation = 2,
                fatherId = 1,
                motherId = 2,
                spouseId = 4,
                birthDate = "1970-07-22",
                deathDate = null,
                isDeceased = false,
                phone = "+919876543210",
                address = "Halba Colony, Raipur",
                occupation = "Teacher",
                notes = "High School Science teacher. Enjoys gardening and reading history books.",
                profileColorHex = "#1D4ED8" // Blue
            ),
            FamilyMember(
                id = 4,
                nameEnglish = "Rekha Chiram",
                nameHindi = "रेखा चिराम",
                gender = "Female",
                generation = 2,
                spouseId = 3,
                birthDate = "1975-11-05",
                deathDate = null,
                isDeceased = false,
                phone = "+919876543211",
                address = "Halba Colony, Raipur",
                occupation = "Government Service",
                notes = "Works in the Municipal Department. Highly supportive and disciplined.",
                profileColorHex = "#047857" // Green
            ),
            FamilyMember(
                id = 5,
                nameEnglish = "Suresh Chiram",
                nameHindi = "सुरेश चिराम",
                gender = "Male",
                generation = 2,
                fatherId = 1,
                motherId = 2,
                spouseId = null,
                birthDate = "1973-05-18",
                deathDate = null,
                isDeceased = false,
                phone = "+919876543212",
                address = "Bhilai Sector 4",
                occupation = "Engineer",
                notes = "Mechanical engineer at BSP. Lives in Bhilai.",
                profileColorHex = "#4338CA" // Indigo
            ),
            FamilyMember(
                id = 6,
                nameEnglish = "Sunita Chiram (Netam)",
                nameHindi = "सुनीता चिराम (नेताम)",
                gender = "Female",
                generation = 2,
                fatherId = 1,
                motherId = 2,
                spouseId = 7,
                birthDate = "1978-01-30",
                deathDate = null,
                isDeceased = false,
                phone = "+919876543213",
                address = "Jagdalpur",
                occupation = "Bank Officer",
                notes = "The youngest daughter of Ram Prasadji. Lives in Jagdalpur.",
                profileColorHex = "#7C3AED" // Purple
            ),
            FamilyMember(
                id = 7,
                nameEnglish = "Mohan Netam",
                nameHindi = "मोहन नेताम",
                gender = "Male",
                generation = 2,
                spouseId = 6,
                birthDate = "1974-09-12",
                deathDate = null,
                isDeceased = false,
                occupation = "Business",
                notes = "Sunita's spouse. Runs a garments retail showroom.",
                profileColorHex = "#0E7490" // Cyan
            ),

            // Generation 3 (Children)
            FamilyMember(
                id = 8,
                nameEnglish = "Aaryan Chiram",
                nameHindi = "आर्यन चिराम",
                gender = "Male",
                generation = 3,
                fatherId = 3,
                motherId = 4,
                spouseId = null,
                birthDate = "2001-09-21",
                deathDate = null,
                isDeceased = false,
                phone = "+918888877777",
                address = "Raipur, Chhattisgarh",
                occupation = "Software Engineer",
                notes = "Tech lover and coder. Developing native family trees to connect the Halba Kranti lineage.",
                profileColorHex = "#0F172A" // Slate
            ),
            FamilyMember(
                id = 9,
                nameEnglish = "Pooja Chiram",
                nameHindi = "पूजा चिराम",
                gender = "Female",
                generation = 3,
                fatherId = 3,
                motherId = 4,
                spouseId = null,
                birthDate = "2004-04-14",
                deathDate = null,
                isDeceased = false,
                phone = "+918888866666",
                occupation = "College Student",
                notes = "Studying Medical Sciences. Talented sitar player.",
                profileColorHex = "#DB2777" // Red/Pink
            ),
            FamilyMember(
                id = 10,
                nameEnglish = "Neha Netam",
                nameHindi = "नेहा नेताम",
                gender = "Female",
                generation = 3,
                fatherId = 7,
                motherId = 6,
                spouseId = null,
                birthDate = "2006-12-05",
                deathDate = null,
                occupation = "High School Student",
                notes = "Sunita's daughter. Love reading novels and cycling.",
                profileColorHex = "#65A30D" // Lime
            )
        )
    }
}
