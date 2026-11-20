package com.ehealthwares.prognocare.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A patient cached on device so searches still work offline. Patients created
 * locally (not yet synced) are stored with [isLocal] = true and a synthetic
 * `LOCAL-…` patientId.
 */
@Entity(tableName = "cached_patients")
data class CachedPatientEntity(
    @PrimaryKey val id: String,
    val patientId: String,
    val firstName: String,
    val lastName: String,
    val otherNames: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val isActive: Boolean = true,
    val isLocal: Boolean = false,
    val updatedAt: Long = 0L,
)

/** Kinds of writes queued while offline. */
object PendingActionType {
    const val PATIENT = "PATIENT"
    const val APPOINTMENT = "APPOINTMENT"
    const val DOCUMENT = "DOCUMENT"
}

/** An outbox entry: a create request captured while offline. */
@Entity(tableName = "pending_actions")
data class PendingActionEntity(
    @PrimaryKey val id: String,
    /** One of [PendingActionType]. */
    val type: String,
    /** Serialized request DTO (kotlinx JSON). */
    val payload: String,
    /** Local id linking dependent records (e.g. a local patient id). */
    val localRef: String? = null,
    val createdAt: Long = 0L,
    val attempts: Int = 0,
    val lastError: String? = null,
)
