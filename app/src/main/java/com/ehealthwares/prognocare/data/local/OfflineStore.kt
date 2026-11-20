package com.ehealthwares.prognocare.data.local

import com.ehealthwares.prognocare.data.remote.models.CreatePatientDto
import com.ehealthwares.prognocare.data.remote.models.Patient
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Device-local store: the patient cache (search fallback offline) and the
 * outbox of writes captured while offline.
 */
@Singleton
class OfflineStore @Inject constructor(
    private val patientDao: PatientDao,
    private val pendingActionDao: PendingActionDao,
) {

    // ── Patient cache ────────────────────────────────────────────

    suspend fun cachePatients(patients: List<Patient>) {
        if (patients.isEmpty()) return
        val now = System.currentTimeMillis()
        patientDao.upsertAll(patients.map { it.toEntity(now) })
    }

    suspend fun cachePatient(patient: Patient) {
        patientDao.upsert(patient.toEntity(System.currentTimeMillis()))
    }

    /** Persists a patient created on this device as "pending sync". */
    suspend fun saveLocalPatient(dto: CreatePatientDto): Patient {
        val localId = "local-${UUID.randomUUID()}"
        val localMrn = "LOCAL-${localId.takeLast(6).uppercase()}"
        val patient = Patient(
            id = localId,
            patientId = localMrn,
            firstName = dto.firstName,
            lastName = dto.lastName,
            otherNames = dto.otherNames,
            dateOfBirth = dto.dateOfBirth,
            gender = dto.gender,
            phone = dto.phone,
            email = dto.email,
            address = dto.address,
            isActive = true,
        )
        patientDao.upsert(
            patient.toEntity(System.currentTimeMillis()).copy(isLocal = true),
        )
        return patient
    }

    suspend fun searchPatients(query: String, limit: Int = 50): List<Patient> =
        patientDao.search(query.trim(), limit).map { it.toPatient() }

    suspend fun patient(id: String): Patient? = patientDao.find(id)?.toPatient()

    suspend fun cachedPatientCount(): Int = patientDao.count()

    fun observeCachedPatientCount(): Flow<Int> = patientDao.observeCount()

    suspend fun deleteLocalPatient(id: String) = patientDao.deleteLocal(id)

    // ── Outbox ───────────────────────────────────────────────────

    suspend fun enqueue(type: String, payload: String, localRef: String? = null): String {
        val id = UUID.randomUUID().toString()
        pendingActionDao.insert(
            PendingActionEntity(
                id = id,
                type = type,
                payload = payload,
                localRef = localRef,
                createdAt = System.currentTimeMillis(),
            ),
        )
        return id
    }

    suspend fun pendingActions(): List<PendingActionEntity> = pendingActionDao.all()

    fun observePendingActions(): Flow<List<PendingActionEntity>> = pendingActionDao.observeAll()

    fun observePendingCount(): Flow<Int> = pendingActionDao.observeCount()

    suspend fun completeAction(id: String) = pendingActionDao.delete(id)

    suspend fun failAction(id: String, error: String?) = pendingActionDao.markFailed(id, error)

    private fun Patient.toEntity(now: Long) = CachedPatientEntity(
        id = id,
        patientId = patientId,
        firstName = firstName,
        lastName = lastName,
        otherNames = otherNames,
        dateOfBirth = dateOfBirth,
        gender = gender,
        phone = phone,
        email = email,
        address = address,
        isActive = isActive,
        isLocal = false,
        updatedAt = now,
    )

    private fun CachedPatientEntity.toPatient() = Patient(
        id = id,
        patientId = patientId,
        firstName = firstName,
        lastName = lastName,
        otherNames = otherNames,
        dateOfBirth = dateOfBirth,
        gender = gender,
        phone = phone,
        email = email,
        address = address,
        isActive = isActive,
    )
}
