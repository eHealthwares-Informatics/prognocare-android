package com.ehealthwares.prognocare.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PatientDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(patients: List<CachedPatientEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(patient: CachedPatientEntity)

    @Query(
        """
        SELECT * FROM cached_patients
        WHERE (:query = '' OR
               firstName LIKE '%' || :query || '%' COLLATE NOCASE OR
               lastName LIKE '%' || :query || '%' COLLATE NOCASE OR
               otherNames LIKE '%' || :query || '%' COLLATE NOCASE OR
               patientId LIKE '%' || :query || '%' COLLATE NOCASE OR
               phone LIKE '%' || :query || '%' COLLATE NOCASE)
        ORDER BY (isLocal = 1) DESC, lastName ASC, firstName ASC
        LIMIT :limit
        """,
    )
    suspend fun search(query: String, limit: Int = 50): List<CachedPatientEntity>

    @Query("SELECT * FROM cached_patients WHERE id = :id OR patientId = :id LIMIT 1")
    suspend fun find(id: String): CachedPatientEntity?

    @Query("SELECT COUNT(*) FROM cached_patients")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM cached_patients")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM cached_patients WHERE isLocal = 1 AND id = :id")
    suspend fun deleteLocal(id: String)
}

@Dao
interface PendingActionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(action: PendingActionEntity)

    @Query("SELECT * FROM pending_actions ORDER BY createdAt ASC")
    suspend fun all(): List<PendingActionEntity>

    @Query("SELECT * FROM pending_actions ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<PendingActionEntity>>

    @Query("SELECT COUNT(*) FROM pending_actions")
    fun observeCount(): Flow<Int>

    @Query("UPDATE pending_actions SET attempts = attempts + 1, lastError = :error WHERE id = :id")
    suspend fun markFailed(id: String, error: String?)

    @Query("DELETE FROM pending_actions WHERE id = :id")
    suspend fun delete(id: String)
}
