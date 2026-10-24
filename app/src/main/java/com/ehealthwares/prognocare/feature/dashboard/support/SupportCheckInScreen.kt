package com.ehealthwares.prognocare.feature.dashboard.support

import androidx.compose.runtime.Composable
import com.ehealthwares.prognocare.feature.checkin.CheckInQueueScreen

/**
 * Support check-in — shares the front-desk check-in queue (today's appointments
 * with a check-in action) with the Nurse and Admin roles.
 */
@Composable
fun SupportCheckInScreen(
    onBack: () -> Unit,
) {
    CheckInQueueScreen(title = "Check-In Queue", onBack = onBack)
}
