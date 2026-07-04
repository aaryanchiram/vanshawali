package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyDao {

    // --- Folders ---
    @Query("SELECT * FROM family_folders ORDER BY createdAt DESC")
    fun getAllFolders(): Flow<List<FamilyFolder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FamilyFolder): Long

    @Query("DELETE FROM family_folders WHERE id = :folderId")
    suspend fun deleteFolderById(folderId: Long)

    @Query("SELECT * FROM family_folders WHERE id = :folderId LIMIT 1")
    suspend fun getFolderById(folderId: Long): FamilyFolder?

    @Update
    suspend fun updateFolder(folder: FamilyFolder)


    // --- Members ---
    @Query("SELECT * FROM family_members WHERE folderId = :folderId ORDER BY id ASC")
    fun getMembersInFolder(folderId: Long): Flow<List<FamilyMember>>

    @Query("SELECT * FROM family_members WHERE id = :memberId LIMIT 1")
    suspend fun getMemberById(memberId: Long): FamilyMember?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: FamilyMember): Long

    @Update
    suspend fun updateMember(member: FamilyMember)

    @Query("DELETE FROM family_members WHERE id = :memberId")
    suspend fun deleteMemberById(memberId: Long)

    @Query("SELECT COUNT(*) FROM family_members")
    fun getTotalMembersCount(): Flow<Int>


    // --- Reminders ---
    @Query("SELECT * FROM reminders ORDER BY date ASC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE folderId = :folderId ORDER BY date ASC")
    fun getRemindersForFolder(folderId: Long): Flow<List<Reminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Query("DELETE FROM reminders WHERE id = :reminderId")
    suspend fun deleteReminderById(reminderId: Long)

    @Query("DELETE FROM reminders WHERE memberId = :memberId")
    suspend fun deleteRemindersByMemberId(memberId: Long)
}

@Database(
    entities = [FamilyFolder::class, FamilyMember::class, Reminder::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun familyDao(): FamilyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vanshvriksh_db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
