package com.ehealthinformatics.prognocare.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.ehealthinformatics.prognocare.feature.dashboard.specialist.SpecialistDashboardScreen
import com.ehealthinformatics.prognocare.feature.dashboard.specialist.SpecialistReferralDetailScreen
import com.ehealthinformatics.prognocare.feature.dashboard.specialist.SpecialistReferralScreen
import com.ehealthinformatics.prognocare.feature.dashboard.specialist.SpecialistPatientListScreen
import com.ehealthinformatics.prognocare.feature.dashboard.therapist.TherapistDashboardScreen
import com.ehealthinformatics.prognocare.feature.dashboard.therapist.TherapistSessionDetailScreen
import com.ehealthinformatics.prognocare.feature.dashboard.therapist.TherapyPlanScreen
import com.ehealthinformatics.prognocare.feature.dashboard.therapist.TherapistSessionScreen
import com.ehealthinformatics.prognocare.feature.dashboard.therapist.TherapistPatientListScreen
import com.ehealthinformatics.prognocare.feature.dashboard.finance.FinanceDashboardScreen
import com.ehealthinformatics.prognocare.feature.dashboard.finance.FinanceBillListScreen
import com.ehealthinformatics.prognocare.feature.dashboard.support.SupportDashboardScreen
import com.ehealthinformatics.prognocare.feature.checkin.CheckInQueueScreen
import com.ehealthinformatics.prognocare.feature.dashboard.support.SupportCheckInScreen
import com.ehealthinformatics.prognocare.feature.dashboard.support.SupportRequestListScreen
import com.ehealthinformatics.prognocare.feature.dashboard.technician.TechnicianDashboardScreen
import com.ehealthinformatics.prognocare.feature.dashboard.technician.TechnicianOrdersScreen
import com.ehealthinformatics.prognocare.feature.dashboard.technician.TechnicianResultsScreen
import com.ehealthinformatics.prognocare.feature.dashboard.finance.FinanceBillDetailScreen
import com.ehealthinformatics.prognocare.feature.dashboard.admin.AdminDashboardScreen
import com.ehealthinformatics.prognocare.feature.dashboard.admin.AdminStaffManagementScreen
import com.ehealthinformatics.prognocare.feature.auth.LoginScreen
import com.ehealthinformatics.prognocare.feature.chat.ChatNotificationViewModel
import com.ehealthinformatics.prognocare.feature.chat.ChatScreen
import com.ehealthinformatics.prognocare.feature.chat.ConversationListScreen
import com.ehealthinformatics.prognocare.feature.clinical.ClinicalAppointmentsScreen
import com.ehealthinformatics.prognocare.feature.clinical.ClinicalEncountersScreen
import com.ehealthinformatics.prognocare.feature.clinical.ClinicalVisitsScreen
import com.ehealthinformatics.prognocare.feature.clinical.CreateAppointmentDialog
import com.ehealthinformatics.prognocare.feature.clinical.CreateEncounterDialog
import com.ehealthinformatics.prognocare.feature.clinical.CreateVisitDialog
import com.ehealthinformatics.prognocare.feature.forms.DynamicFormScreen
import com.ehealthinformatics.prognocare.feature.forms.FormPickerScreen
import com.ehealthinformatics.prognocare.feature.requests.CreateRequestScreen
import com.ehealthinformatics.prognocare.feature.requests.RequestDetailScreen
import com.ehealthinformatics.prognocare.feature.requests.RequestsListScreen
import com.ehealthinformatics.prognocare.feature.notifications.NotificationsScreen
import com.ehealthinformatics.prognocare.feature.dashboard.doctor.DoctorDashboardScreen
import com.ehealthinformatics.prognocare.feature.dashboard.doctor.DoctorEncounterScreen
import com.ehealthinformatics.prognocare.feature.dashboard.doctor.DoctorPrescriptionScreen
import com.ehealthinformatics.prognocare.feature.dashboard.doctor.DoctorAppointmentScreen
import com.ehealthinformatics.prognocare.feature.dashboard.doctor.DoctorPatientListScreen
import com.ehealthinformatics.prognocare.feature.dashboard.doctor.DoctorPatientDetailScreen
import com.ehealthinformatics.prognocare.feature.dashboard.nurse.NurseDashboardScreen
import com.ehealthinformatics.prognocare.feature.dashboard.nurse.VitalsListScreen
import com.ehealthinformatics.prognocare.feature.dashboard.nurse.VitalsRecordingScreen
import com.ehealthinformatics.prognocare.feature.dashboard.nurse.MedicationAdministrationScreen
import com.ehealthinformatics.prognocare.feature.dashboard.nurse.NurseCheckInScreen
import com.ehealthinformatics.prognocare.feature.dashboard.nurse.NurseTaskListScreen
import com.ehealthinformatics.prognocare.feature.dashboard.patient.PatientDashboardScreen
import com.ehealthinformatics.prognocare.feature.dashboard.patient.PatientAppointmentBookingScreen
import com.ehealthinformatics.prognocare.feature.dashboard.patient.PatientAppointmentScreen
import com.ehealthinformatics.prognocare.feature.dashboard.patient.PatientRecordsScreen
import com.ehealthinformatics.prognocare.feature.dashboard.patient.PatientMedicationsScreen
import com.ehealthinformatics.prognocare.feature.profile.UserProfileScreen
import com.ehealthinformatics.prognocare.feature.settings.SettingsScreen
import com.ehealthinformatics.prognocare.feature.splash.SplashDestination
import com.ehealthinformatics.prognocare.feature.splash.SplashScreen
import com.ehealthinformatics.prognocare.feature.splash.SplashViewModel

private const val ANIM_DURATION = 300

private val enterTransition: EnterTransition = fadeIn(animationSpec = tween(ANIM_DURATION))
private val exitTransition: ExitTransition = fadeOut(animationSpec = tween(ANIM_DURATION))
private val popEnterTransition: EnterTransition = fadeIn(animationSpec = tween(ANIM_DURATION))
private val popExitTransition: ExitTransition = fadeOut(animationSpec = tween(ANIM_DURATION))

// ── Bottom Nav Items ──────────────────────────────────────────

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

val DoctorBottomNav = listOf(
    BottomNavItem(DoctorRoutes.DASHBOARD, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(DoctorRoutes.PATIENT_LIST, "Patients", Icons.Filled.People, Icons.Outlined.People),
    BottomNavItem(DoctorRoutes.APPOINTMENTS, "Schedule", Icons.Filled.ListAlt, Icons.Outlined.ListAlt),
    BottomNavItem(ChatRoutes.CONVERSATIONS, "Chat", Icons.Filled.Chat, Icons.Outlined.Chat),
)

val NurseBottomNav = listOf(
    BottomNavItem(NurseRoutes.DASHBOARD, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(NurseRoutes.PATIENT_LIST, "Patients", Icons.Filled.People, Icons.Outlined.People),
    BottomNavItem(NurseRoutes.TASKS, "Tasks", Icons.Filled.ListAlt, Icons.Outlined.ListAlt),
    BottomNavItem(ChatRoutes.CONVERSATIONS, "Chat", Icons.Filled.Chat, Icons.Outlined.Chat),
)

val PatientBottomNav = listOf(
    BottomNavItem(PatientRoutes.DASHBOARD, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(PatientRoutes.RECORDS, "Records", Icons.Filled.Description, Icons.Outlined.Description),
    BottomNavItem(PatientRoutes.APPOINTMENTS, "Appointments", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    BottomNavItem(ChatRoutes.CONVERSATIONS, "Chat", Icons.Filled.Chat, Icons.Outlined.Chat),
    BottomNavItem("patient/profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person),
)

val SpecialistBottomNav = listOf(
    BottomNavItem(SpecialistRoutes.DASHBOARD, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(SpecialistRoutes.REFERRALS, "Referrals", Icons.Filled.ListAlt, Icons.Outlined.ListAlt),
    BottomNavItem(SpecialistRoutes.PATIENT_LIST, "Patients", Icons.Filled.People, Icons.Outlined.People),
    BottomNavItem(ChatRoutes.CONVERSATIONS, "Chat", Icons.Filled.Chat, Icons.Outlined.Chat),
)

val TherapistBottomNav = listOf(
    BottomNavItem(TherapistRoutes.DASHBOARD, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(TherapistRoutes.SESSIONS, "Sessions", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    BottomNavItem(TherapistRoutes.PATIENT_LIST, "Patients", Icons.Filled.People, Icons.Outlined.People),
    BottomNavItem(ChatRoutes.CONVERSATIONS, "Chat", Icons.Filled.Chat, Icons.Outlined.Chat),
)

val FinanceBottomNav = listOf(
    BottomNavItem(FinanceRoutes.DASHBOARD, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(FinanceRoutes.BILLS, "Bills", Icons.Filled.Description, Icons.Outlined.Description),
    BottomNavItem(FinanceRoutes.PAYMENTS, "Payments", Icons.Filled.Payment, Icons.Outlined.Payment),
    BottomNavItem(ChatRoutes.CONVERSATIONS, "Chat", Icons.Filled.Chat, Icons.Outlined.Chat),
)

val TechnicianBottomNav = listOf(
    BottomNavItem(TechnicianRoutes.DASHBOARD, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(TechnicianRoutes.ORDERS, "Orders", Icons.Filled.ListAlt, Icons.Outlined.ListAlt),
    BottomNavItem(TechnicianRoutes.RESULTS, "Results", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle),
    BottomNavItem(ChatRoutes.CONVERSATIONS, "Chat", Icons.Filled.Chat, Icons.Outlined.Chat),
)

val SupportBottomNav = listOf(
    BottomNavItem(SupportRoutes.DASHBOARD, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(SupportRoutes.CHECKIN, "Check-In", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle),
    BottomNavItem(SupportRoutes.REQUESTS, "Requests", Icons.Filled.ListAlt, Icons.Outlined.ListAlt),
    BottomNavItem(ChatRoutes.CONVERSATIONS, "Chat", Icons.Filled.Chat, Icons.Outlined.Chat),
)

val AdminBottomNav = listOf(
    BottomNavItem(AdminRoutes.DASHBOARD, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(AdminRoutes.CHECKIN, "Check-In", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle),
    BottomNavItem(AdminRoutes.STAFF, "Staff", Icons.Filled.People, Icons.Outlined.People),
    BottomNavItem(ChatRoutes.CONVERSATIONS, "Chat", Icons.Filled.Chat, Icons.Outlined.Chat),
)

@Composable
fun PrognoCareNavGraph(
    navController: NavHostController = rememberNavController(),
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val snackbarHostState = remember { SnackbarHostState() }

    // Determine which bottom bar items to show based on current route
    val isDoctorRoute = DoctorBottomNav.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    }
    val isNurseRoute = NurseBottomNav.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    }
    val isPatientRoute = PatientBottomNav.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    }
    val isSpecialistRoute = SpecialistBottomNav.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    }
    val isTherapistRoute = TherapistBottomNav.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    }
    val isFinanceRoute = FinanceBottomNav.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    }
    val isTechnicianRoute = TechnicianBottomNav.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    }
    val isSupportRoute = SupportBottomNav.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    }
    val isAdminRoute = AdminBottomNav.any { item ->
        currentDestination?.hierarchy?.any { it.route == item.route } == true
    }
    val showBottomBar = isDoctorRoute || isNurseRoute || isPatientRoute || isSpecialistRoute || isTherapistRoute || isFinanceRoute || isTechnicianRoute || isSupportRoute || isAdminRoute
    val currentBottomNavItems = when {
        isDoctorRoute -> DoctorBottomNav
        isNurseRoute -> NurseBottomNav
        isPatientRoute -> PatientBottomNav
        isSpecialistRoute -> SpecialistBottomNav
        isTherapistRoute -> TherapistBottomNav
        isFinanceRoute -> FinanceBottomNav
        isTechnicianRoute -> TechnicianBottomNav
        isSupportRoute -> SupportBottomNav
        isAdminRoute -> AdminBottomNav
        else -> emptyList()
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                ) {
                    currentBottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.route
                        } == true

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(24.dp),
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal,
                                )
                            },
                            selected = selected,
                            colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                                selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        val chatNotificationViewModel: ChatNotificationViewModel = hiltViewModel()
        LaunchedEffect(Unit) {
            chatNotificationViewModel.newMessage.collect { notification ->
                val result = snackbarHostState.showSnackbar(
                    message = "New message: ${notification.text}",
                    actionLabel = "Open",
                )
                if (result == SnackbarResult.ActionPerformed) {
                    navController.navigate(ChatRoutes.conversationDetail(notification.conversationId)) {
                        launchSingleTop = true
                    }
                }
            }
        }
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { enterTransition },
            exitTransition = { exitTransition },
            popEnterTransition = { popEnterTransition },
            popExitTransition = { popExitTransition },
        ) {
            // Splash
            composable(Routes.SPLASH) {
                val splashViewModel: SplashViewModel = hiltViewModel()
                SplashScreen()

                androidx.compose.runtime.LaunchedEffect(Unit) {
                    splashViewModel.destination.collect { dest ->
                        when (dest) {
                            is SplashDestination.Login -> {
                                navController.navigate(Routes.LOGIN) {
                                    popUpTo(Routes.SPLASH) { inclusive = true }
                                }
                            }
                            is SplashDestination.Dashboard -> {
                                navController.navigate(dest.role.route) {
                                    popUpTo(Routes.SPLASH) { inclusive = true }
                                }
                            }
                        }
                    }
                }
            }

            // Auth
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = { role ->
                        navController.navigate(role.route) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                )
            }

            // Doctor
            composable(DoctorRoutes.DASHBOARD) {
                DoctorDashboardScreen(
                    onNavigateToAppointments = { navController.navigate(DoctorRoutes.APPOINTMENTS) },
                    onNavigateToPatients = { navController.navigate(DoctorRoutes.PATIENT_LIST) },
                    onNavigateToPatientDetail = { id -> navController.navigate(DoctorRoutes.patientDetail(id)) },
                    onNavigateToChat = { navController.navigate(ChatRoutes.CONVERSATIONS) },
                    onNavigateToProfile = { navController.navigate(ProfileRoutes.PROFILE) },
                    onNavigateToEncounters = { navController.navigate(ClinicalRoutes.ENCOUNTERS + "/new") },
                    onNavigateToNewRequest = { navController.navigate(DoctorRoutes.CREATE_REQUEST) },
                    onNavigateToClinicalNote = {
                        navController.navigate(FormsRoutes.picker())
                    },
                    onNavigateToNotifications = {
                        navController.navigate(NotificationRoutes.NOTIFICATIONS)
                    },
                )
            }

            composable(DoctorRoutes.APPOINTMENTS) {
                DoctorAppointmentScreen(
                    onBack = { navController.popBackStack() },
                    onPatientClick = { id -> navController.navigate(DoctorRoutes.patientDetail(id)) },
                )
            }

            composable(DoctorRoutes.PATIENT_LIST) {
                DoctorPatientListScreen(
                    onBack = { navController.popBackStack() },
                    onPatientClick = { id -> navController.navigate(DoctorRoutes.patientDetail(id)) },
                )
            }

            composable(
                route = DoctorRoutes.PATIENT_DETAIL,
                arguments = listOf(navArgument("patientId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable
                DoctorPatientDetailScreen(
                    patientId = patientId,
                    onBack = { navController.popBackStack() },
                    onOpenDocumentation = { pid ->
                        navController.navigate(FormsRoutes.picker(patientId = pid))
                    },
                )
            }

            composable(
                route = DoctorRoutes.ENCOUNTER,
                arguments = listOf(navArgument("encounterId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val encounterId = backStackEntry.arguments?.getString("encounterId") ?: return@composable
                DoctorEncounterScreen(
                    encounterId = encounterId,
                    onBack = { navController.popBackStack() },
                    onOpenFormPicker = { patientId, visitId, encId ->
                        navController.navigate(FormsRoutes.picker(patientId, visitId, encId))
                    },
                    onEditSubmission = { patientId, visitId, encId, submissionId ->
                        navController.navigate(
                            FormsRoutes.form(
                                formId = "",
                                patientId = patientId,
                                visitId = visitId,
                                encounterId = encId,
                            ),
                        )
                    },
                )
            }

            composable(DoctorRoutes.REQUESTS) {
                RequestsListScreen(
                    title = "My Requests",
                    onBack = { navController.popBackStack() },
                    onRequestClick = { id -> navController.navigate(DoctorRoutes.requestDetail(id)) },
                    onCreateRequest = { navController.navigate(DoctorRoutes.CREATE_REQUEST) },
                )
            }

            composable(DoctorRoutes.CREATE_REQUEST) {
                CreateRequestScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }

            composable(
                route = DoctorRoutes.REQUEST_DETAIL,
                arguments = listOf(navArgument("requestId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val requestId = backStackEntry.arguments?.getString("requestId") ?: return@composable
                RequestDetailScreen(
                    requestId = requestId,
                    onBack = { navController.popBackStack() },
                )
            }

            // ── Notifications (shared) ────────────────────────
            composable(NotificationRoutes.NOTIFICATIONS) {
                NotificationsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenRequest = { requestId ->
                        navController.navigate(DoctorRoutes.requestDetail(requestId)) {
                            launchSingleTop = true
                        }
                    },
                )
            }

            // ── Chat ──────────────────────────────────────────
            composable(ChatRoutes.CONVERSATIONS) {
                ConversationListScreen(
                    onConversationClick = { id -> navController.navigate(ChatRoutes.conversationDetail(id)) },
                    onNewConversation = {
                        navController.navigate(ChatRoutes.conversationDetail(ChatRoutes.NEW_CONVERSATION_ID))
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = ChatRoutes.CONVERSATION_DETAIL,
                arguments = listOf(navArgument("conversationId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val conversationId = backStackEntry.arguments?.getString("conversationId") ?: return@composable
                ChatScreen(
                    conversationId = conversationId,
                    onBack = { navController.popBackStack() },
                )
            }

            // Dynamic forms
            composable(
                route = FormsRoutes.PICKER,
                arguments = listOf(
                    navArgument("patientId") { type = NavType.StringType; defaultValue = "" },
                    navArgument("visitId") { type = NavType.StringType; defaultValue = "" },
                    navArgument("encounterId") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { backStackEntry ->
                val patientId = backStackEntry.arguments?.getString("patientId").orEmpty()
                val visitId = backStackEntry.arguments?.getString("visitId").orEmpty().ifEmpty { null }
                val encounterId = backStackEntry.arguments?.getString("encounterId").orEmpty().ifEmpty { null }
                FormPickerScreen(
                    patientId = patientId,
                    visitId = visitId,
                    encounterId = encounterId,
                    onPick = { formId, pickedPatientId, pickedVisitId, pickedEncounterId ->
                        navController.navigate(
                            FormsRoutes.form(
                                formId,
                                pickedPatientId,
                                pickedVisitId,
                                pickedEncounterId,
                            ),
                        )
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = FormsRoutes.FORM,
                arguments = listOf(
                    navArgument("formId") { type = NavType.StringType },
                    navArgument("patientId") { type = NavType.StringType; defaultValue = "" },
                    navArgument("visitId") { type = NavType.StringType; defaultValue = "" },
                    navArgument("encounterId") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { backStackEntry ->
                val formId = backStackEntry.arguments?.getString("formId") ?: return@composable
                val patientId = backStackEntry.arguments?.getString("patientId").orEmpty()
                val visitId = backStackEntry.arguments?.getString("visitId").orEmpty().ifEmpty { null }
                val encounterId = backStackEntry.arguments?.getString("encounterId").orEmpty().ifEmpty { null }
                DynamicFormScreen(
                    formId = formId,
                    patientId = patientId,
                    visitId = visitId,
                    encounterId = encounterId,
                    onBack = { navController.popBackStack() },
                    onComplete = { navController.popBackStack() },
                )
            }

            // Nurse
            composable(NurseRoutes.DASHBOARD) {
                NurseDashboardScreen(
                    onNavigateToVitals = { navController.navigate(NurseRoutes.VITALS) },
                    onNavigateToRecordVitals = { navController.navigate(NurseRoutes.VITALS_RECORD) },
                    onNavigateToMedications = { navController.navigate(NurseRoutes.MEDICATIONS) },
                    onNavigateToCheckIn = { navController.navigate(NurseRoutes.CHECKIN) },
                    onNavigateToTasks = { navController.navigate(NurseRoutes.TASKS) },
                    onNavigateToPatients = { navController.navigate(NurseRoutes.PATIENT_LIST) },
                    onNavigateToChat = { navController.navigate(ChatRoutes.CONVERSATIONS) },
                    onNavigateToProfile = { navController.navigate(ProfileRoutes.PROFILE) },
                )
            }

            // Vitals list → FAB opens the recording form
            composable(NurseRoutes.VITALS) {
                VitalsListScreen(
                    onBack = { navController.popBackStack() },
                    onRecordVitals = { navController.navigate(NurseRoutes.VITALS_RECORD) },
                )
            }

            composable(NurseRoutes.VITALS_RECORD) {
                VitalsRecordingScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }

            composable(NurseRoutes.MEDICATIONS) {
                MedicationAdministrationScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            composable(NurseRoutes.CHECKIN) {
                NurseCheckInScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            // Nurse patient list + detail: shared search list and the shared
            // tabbed patient profile (the bottom-nav Patients item was dead).
            composable(NurseRoutes.PATIENT_LIST) {
                com.ehealthinformatics.prognocare.feature.shared.PatientSearchScreen(
                    title = "Patients",
                    onBack = { navController.popBackStack() },
                    onPatientClick = { patient ->
                        navController.navigate("nurse/patients/${patient.patientId.ifBlank { patient.id }}")
                    },
                )
            }

            composable(
                route = "nurse/patients/{patientId}",
                arguments = listOf(navArgument("patientId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable
                com.ehealthinformatics.prognocare.feature.dashboard.doctor.DoctorPatientDetailScreen(
                    patientId = patientId,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(NurseRoutes.TASKS) {
                NurseTaskListScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            // Patient
            composable(PatientRoutes.DASHBOARD) {
                PatientDashboardScreen(
                    onNavigateToAppointments = { navController.navigate(PatientRoutes.APPOINTMENTS) },
                    onNavigateToRecords = { navController.navigate(PatientRoutes.RECORDS) },
                    onNavigateToMedications = { navController.navigate(PatientRoutes.MEDICATIONS) },
                    onNavigateToChat = { navController.navigate(ChatRoutes.CONVERSATIONS) },
                    onNavigateToProfile = { navController.navigate(ProfileRoutes.PROFILE) },
                )
            }

            composable(PatientRoutes.APPOINTMENTS) {
                PatientAppointmentScreen(
                    onBack = { navController.popBackStack() },
                    onBook = { navController.navigate(PatientRoutes.BOOKING) },
                )
            }

            composable(PatientRoutes.BOOKING) {
                PatientAppointmentBookingScreen(
                    onBack = { navController.popBackStack() },
                    onBook = { navController.popBackStack() },
                )
            }

            composable(PatientRoutes.RECORDS) {
                PatientRecordsScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            composable(PatientRoutes.MEDICATIONS) {
                PatientMedicationsScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            // Specialist
            composable(SpecialistRoutes.DASHBOARD) {
                SpecialistDashboardScreen(
                    onNavigateToReferrals = { navController.navigate(SpecialistRoutes.REFERRALS) },
                    onNavigateToPatients = { navController.navigate(SpecialistRoutes.PATIENT_LIST) },
                    onNavigateToPatientDetail = { mrn -> navController.navigate("specialist/patients/$mrn") },
                    onNavigateToChat = { navController.navigate(ChatRoutes.CONVERSATIONS) },
                    onNavigateToProfile = { navController.navigate(ProfileRoutes.PROFILE) },
                )
            }

            composable(SpecialistRoutes.REFERRALS) {
                SpecialistReferralScreen(
                    onBack = { navController.popBackStack() },
                    onPatientClick = { mrn -> navController.navigate("specialist/patients/$mrn") },
                )
            }

            composable(
                route = SpecialistRoutes.REFERRAL_DETAIL,
                arguments = listOf(navArgument("referralId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val referralId = backStackEntry.arguments?.getString("referralId") ?: return@composable
                SpecialistReferralDetailScreen(
                    referralId = referralId,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(SpecialistRoutes.CONSULTATION_NOTES) {
                FormPickerScreen(
                    patientId = "",
                    visitId = null,
                    encounterId = null,
                    onPick = { formId, pickedPatientId, pickedVisitId, pickedEncounterId ->
                        navController.navigate(
                            FormsRoutes.form(formId, pickedPatientId, pickedVisitId, pickedEncounterId),
                        )
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(SpecialistRoutes.PATIENT_LIST) {
                SpecialistPatientListScreen(
                    onBack = { navController.popBackStack() },
                    onPatientClick = { mrn -> navController.navigate("specialist/patients/$mrn") },
                )
            }

            // Specialist patient detail: same tabbed profile as the doctor's
            // (Overview / Visits / Encounters / Requests / Records), reused via
            // the doctor route destination.
            composable(
                route = "specialist/patients/{patientId}",
                arguments = listOf(navArgument("patientId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable
                com.ehealthinformatics.prognocare.feature.dashboard.doctor.DoctorPatientDetailScreen(
                    patientId = patientId,
                    onBack = { navController.popBackStack() },
                )
            }

            // Therapist
            composable(TherapistRoutes.DASHBOARD) {
                TherapistDashboardScreen(
                    onNavigateToSessions = { navController.navigate(TherapistRoutes.SESSIONS) },
                    onNavigateToPatients = { navController.navigate(TherapistRoutes.PATIENT_LIST) },
                    onNavigateToPatientDetail = { mrn -> navController.navigate("therapist/patients/$mrn") },
                    onNavigateToChat = { navController.navigate(ChatRoutes.CONVERSATIONS) },
                    onNavigateToProfile = { navController.navigate(ProfileRoutes.PROFILE) },
                )
            }

            composable(TherapistRoutes.SESSIONS) {
                TherapistSessionScreen(
                    onBack = { navController.popBackStack() },
                    onPatientClick = { mrn -> navController.navigate("therapist/patients/$mrn") },
                )
            }

            composable(
                route = TherapistRoutes.SESSION_DETAIL,
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
                TherapistSessionDetailScreen(
                    sessionId = sessionId,
                    onBack = { navController.popBackStack() },
                    onComplete = { navController.popBackStack() },
                )
            }

            composable(TherapistRoutes.THERAPY_PLAN) {
                TherapyPlanScreen(
                    patientName = "Patient", // TODO: pass from arguments
                    onBack = { navController.popBackStack() },
                    onSave = { navController.popBackStack() },
                )
            }

            composable(TherapistRoutes.PATIENT_LIST) {
                TherapistPatientListScreen(
                    onBack = { navController.popBackStack() },
                    onPatientClick = { mrn -> navController.navigate("therapist/patients/$mrn") },
                )
            }

            // Finance
            composable(FinanceRoutes.DASHBOARD) {
                FinanceDashboardScreen(
                    onNavigateToBills = { navController.navigate(FinanceRoutes.BILLS) },
                    onNavigateToBillDetail = { billId -> navController.navigate(FinanceRoutes.billDetail(billId)) },
                    onNavigateToPayments = { navController.navigate(FinanceRoutes.PAYMENTS) },
                    onNavigateToPatientSearch = { navController.navigate(FinanceRoutes.PATIENT_SEARCH) },
                    onNavigateToChat = { navController.navigate(ChatRoutes.CONVERSATIONS) },
                    onNavigateToProfile = { navController.navigate(ProfileRoutes.PROFILE) },
                )
            }

            composable(FinanceRoutes.BILLS) {
                FinanceBillListScreen(
                    onBack = { navController.popBackStack() },
                    onBillClick = { billId -> navController.navigate(FinanceRoutes.billDetail(billId)) },
                    onCreateBill = { /* TODO: create bill screen */ },
                )
            }

            composable(
                route = FinanceRoutes.BILL_DETAIL,
                arguments = listOf(navArgument("billId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val billId = backStackEntry.arguments?.getString("billId") ?: return@composable
                FinanceBillDetailScreen(
                    billId = billId,
                    onBack = { navController.popBackStack() },
                    onRecordPayment = { /* record payment */ },
                )
            }

            composable(FinanceRoutes.PAYMENTS) {
                com.ehealthinformatics.prognocare.feature.dashboard.finance.FinancePaymentProvidersScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            composable(FinanceRoutes.PATIENT_SEARCH) {
                com.ehealthinformatics.prognocare.feature.shared.PatientSearchScreen(
                    title = "Patient Billing Lookup",
                    onBack = { navController.popBackStack() },
                )
            }

            // Admin
            composable(AdminRoutes.DASHBOARD) {
                AdminDashboardScreen(
                    onNavigateToPatientSearch = { navController.navigate(AdminRoutes.PATIENT_SEARCH) },
                    onNavigateToRegisterPatient = { navController.navigate(AdminRoutes.REGISTER_PATIENT) },
                    onNavigateToCheckIn = { navController.navigate(AdminRoutes.CHECKIN) },
                    onNavigateToStaff = { navController.navigate(AdminRoutes.STAFF) },
                    onNavigateToChat = { navController.navigate(ChatRoutes.CONVERSATIONS) },
                    onNavigateToProfile = { navController.navigate(ProfileRoutes.PROFILE) },
                    onNavigateToAppointments = { navController.navigate(ClinicalRoutes.APPOINTMENTS) },
                )
            }

            composable(AdminRoutes.PATIENT_SEARCH) {
                com.ehealthinformatics.prognocare.feature.shared.PatientSearchScreen(
                    title = "Patient Directory",
                    onBack = { navController.popBackStack() },
                    onPatientClick = { patient ->
                        navController.navigate("admin/patients/${patient.patientId.ifBlank { patient.id }}")
                    },
                )
            }

            composable(
                route = "admin/patients/{patientId}",
                arguments = listOf(navArgument("patientId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable
                com.ehealthinformatics.prognocare.feature.dashboard.doctor.DoctorPatientDetailScreen(
                    patientId = patientId,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(AdminRoutes.REGISTER_PATIENT) {
                com.ehealthinformatics.prognocare.feature.dashboard.admin.RegisterPatientScreen(
                    onBack = { navController.popBackStack() },
                    onRegistered = { mrn ->
                        // Back to the dashboard; the directory shows the new patient.
                        navController.popBackStack()
                    },
                )
            }

            composable(AdminRoutes.CHECKIN) {
                CheckInQueueScreen(
                    title = "Check-In",
                    onBack = { navController.popBackStack() },
                )
            }

            composable(AdminRoutes.STAFF) {
                AdminStaffManagementScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            composable(AdminRoutes.FACILITIES) {
                com.ehealthinformatics.prognocare.feature.dashboard.admin.AdminFacilitiesScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            // ── Placeholder routes for other roles ────────────
            composable(TechnicianRoutes.DASHBOARD) {
                TechnicianDashboardScreen(
                    onNavigateToOrders = {
                        navController.navigate(TechnicianRoutes.ORDERS) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToResults = {
                        navController.navigate(TechnicianRoutes.RESULTS) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToChat = {
                        navController.navigate(ChatRoutes.CONVERSATIONS) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToProfile = {
                        navController.navigate(ProfileRoutes.PROFILE)
                    },
                )
            }

            composable(TechnicianRoutes.ORDERS) {
                TechnicianOrdersScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            composable(TechnicianRoutes.RESULTS) {
                TechnicianResultsScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(SupportRoutes.DASHBOARD) {
                SupportDashboardScreen(
                    onNavigateToCheckIn = {
                        navController.navigate(SupportRoutes.CHECKIN) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToRequests = {
                        navController.navigate(SupportRoutes.REQUESTS) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToPatientLookup = {
                        // TODO: Add patient lookup route
                    },
                    onNavigateToChat = {
                        navController.navigate(ChatRoutes.CONVERSATIONS) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToProfile = {
                        navController.navigate(ProfileRoutes.PROFILE)
                    },
                )
            }

            composable(SupportRoutes.CHECKIN) {
                SupportCheckInScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            composable(SupportRoutes.REQUESTS) {
                SupportRequestListScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            // Clinical records: appointments / visits / encounters (shared)
            composable(ClinicalRoutes.APPOINTMENTS) {
                ClinicalAppointmentsScreen(
                    onBack = { navController.popBackStack() },
                    onCreateAppointment = {
                        // Dialogs stack on top of the list; popping back to it
                        // re-runs the list load with the new data.
                        navController.navigate(ClinicalRoutes.APPOINTMENTS + "/create")
                    },
                )
            }

            composable(ClinicalRoutes.APPOINTMENTS + "/create") {
                CreateAppointmentDialog(
                    onDismiss = { navController.popBackStack() },
                    onCreated = { navController.popBackStack() },
                )
            }

            composable(ClinicalRoutes.VISITS) {
                ClinicalVisitsScreen(
                    onBack = { navController.popBackStack() },
                    onStartVisit = { navController.navigate(ClinicalRoutes.VISITS + "/create") },
                )
            }

            composable(ClinicalRoutes.VISITS + "/create") {
                CreateVisitDialog(
                    onDismiss = { navController.popBackStack() },
                    onCreated = { navController.popBackStack() },
                )
            }

            composable(ClinicalRoutes.ENCOUNTERS) {
                ClinicalEncountersScreen(
                    onBack = { navController.popBackStack() },
                    onEncounterClick = { encounterId ->
                        navController.navigate(DoctorRoutes.encounter(encounterId))
                    },
                    onDocumentEncounter = { navController.navigate(ClinicalRoutes.ENCOUNTERS + "/create") },
                )
            }

            composable(ClinicalRoutes.ENCOUNTERS + "/create") {
                CreateEncounterDialog(
                    onDismiss = { navController.popBackStack() },
                    onCreated = { navController.popBackStack() },
                )
            }

            composable(ClinicalRoutes.ENCOUNTERS + "/new") {
                // Doctor "New Encounter": the list with the Document Encounter
                // dialog opened immediately.
                ClinicalEncountersScreen(
                    onBack = { navController.popBackStack() },
                    onEncounterClick = { encounterId ->
                        navController.navigate(DoctorRoutes.encounter(encounterId))
                    },
                    onDocumentEncounter = { navController.navigate(ClinicalRoutes.ENCOUNTERS + "/create") },
                )
                CreateEncounterDialog(
                    onDismiss = { navController.popBackStack() },
                    onCreated = { encounterId ->
                        // Swap this route for the new encounter's screen.
                        navController.navigate(DoctorRoutes.encounter(encounterId)) {
                            popUpTo(ClinicalRoutes.ENCOUNTERS + "/new") { inclusive = true }
                        }
                    },
                )
            }

            // Profile (shared across all roles)
            composable(ProfileRoutes.PROFILE) {
                UserProfileScreen(
                    onBack = { navController.popBackStack() },
                    onSignOut = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onOpenServerSettings = {
                        navController.navigate(Routes.SETTINGS)
                    },
                    onRoleChanged = { role ->
                        // Relaunch the whole graph on the new role's dashboard.
                        navController.navigate(role.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }

            // Server settings (shared across all roles)
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
