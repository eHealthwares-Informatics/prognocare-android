package com.ehealthinformatics.prognocare.feature.dashboard.nurse

import androidx.compose.runtime.Composable
import com.ehealthinformatics.prognocare.feature.checkin.CheckInQueueScreen

/**
 * Nurse check-in — shares the front-desk check-in queue (today's appointments
 * with a check-in action) with the Support and Admin roles.
 */
@Composable
fun NurseCheckInScreen(
    onBack: () -> Unit,
) {
    CheckInQueueScreen(title = "Patient Check-In", onBack = onBack)
}
