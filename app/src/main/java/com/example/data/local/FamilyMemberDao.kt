package com.example.data.local

import androidx.room.*
import com.example.data.model.FamilyMember
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyMemberDao {
    @Query("SELECT * FROM family_members ORDER BY generation ASC, id ASC")
    fun getAllMembers(): Flow<List<FamilyMember>>

    @Query("SELECT * FROM family_members WHERE id = :id")
    fun getMemberById(id: Int): Flow<FamilyMember?>

    @Query("SELECT * FROM family_members WHERE id = :id")
    suspend fun getMemberByIdSuspend(id: Int): FamilyMember?

    @Query("SELECT * FROM family_members WHERE id IN (:ids)")
    fun getMembersByIds(ids: List<Int>): Flow<List<FamilyMember>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: FamilyMember): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<FamilyMember>): List<Long>

    @Update
    suspend fun updateMember(member: FamilyMember)

    @Delete
    suspend fun deleteMember(member: FamilyMember)

    @Query("DELETE FROM family_members")
    suspend fun clearAllMembers()
}
