package com.ehealthinformatics.prognocare.data.remote.models

import kotlinx.serialization.Serializable

@Serializable
data class Appointment(
    val id: String = "",
    val appointmentNumber: String? = null,
    val patientId: String = "",
    val patientName: String = "",
    val appointmentType: String = "",
    val date: String = "",
    val startTime: String = "",
    val endTime: String? = null,
    val providerId: String? = null,
    val providerName: String? = null,
    val locationId: String? = null,
    val scheduleLocation: String? = null,
    val visitId: String? = null,
    val priority: String = "ROUTINE",
    val status: String = "SCHEDULED",
    val reason: String? = null,
    val notes: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
) {
    val isUrgent: Boolean get() = priority == "URGENT" || priority == "EMERGENCY"
    val isActive: Boolean get() = status in listOf("CHECKED_IN", "IN_PROGRESS")
    val isCompleted: Boolean get() = status == "COMPLETED"

    val typeDisplay: String
        get() = appointmentType.replace("_", " ").lowercase()
            .replaceFirstChar { it.uppercase() }

    val statusDisplay: String
        get() = status.replace("_", " ").lowercase()
            .replaceFirstChar { it.uppercase() }

    val priorityDisplay: String
        get() = priority.replace("_", " ").lowercase()
            .replaceFirstChar { it.uppercase() }
}

@Serializable
data class CreateAppointmentDto(
    val patientId: String,
    val patientName: String? = null,
    val appointmentType: String,
    val date: String,
    val startTime: String,
    val endTime: String? = null,
    val providerId: String? = null,
    val providerName: String? = null,
    val locationId: String? = null,
    val priority: String = "ROUTINE",
    val reason: String? = null,
    val notes: String? = null,
)

@Serializable
data class UpdateAppointmentDto(
    val patientId: String? = null,
    val patientName: String? = null,
    val appointmentType: String? = null,
    val date: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val providerId: String? = null,
    val providerName: String? = null,
    val locationId: String? = null,
    val priority: String? = null,
    val status: String? = null,
    val reason: String? = null,
    val notes: String? = null,
)

@Serializable
data class CheckInAppointmentDto(
    val locationId: String? = null,
    val providerId: String? = null,
    val providerName: String? = null,
)

@Serializable
data class CancelAppointmentDto(
    val reason: String = "",
)

/** Backend check-in returns the transitioned appointment plus the created visit. */
@Serializable
data class CheckInResponse(
    val appointment: Appointment = Appointment(),
    val visitId: String? = null,
    val visit: Visit? = null,
)

enum class AppointmentType {
    CHECKUP, FOLLOW_UP, CONSULTATION, PROCEDURE, EMERGENCY, SURGERY, OTHER,
}

enum class AppointmentPriority {
    ROUTINE, URGENT, EMERGENCY,
}

enum class AppointmentStatus {
    SCHEDULED, CHECKED_IN, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW, MISSED,
}
