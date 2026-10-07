# Progress — android#6 EMR in-app notifications

## 2026-10-07
- Created GitHub issue: https://github.com/eHealthwares-Informatics/android/issues/6
- EMR: fixed notifications list 500 (TypeORM orderBy property paths); unit tests 229 green; committed `036c071` on `feat/emr-notifications-phase1`
- Android (PrognoCare `~/develop/rxsoft/android` on `main`):
  - `data/remote/models/Notification.kt` — models
  - `data/remote/api/NotificationApi.kt` — Retrofit API
  - `RetrofitClient`/`ApiBundle` — `notificationApi` wired
  - `feature/notifications/NotificationsRepository.kt`
  - `feature/notifications/NotificationsViewModel.kt` — 30s poll, merge by id, badge, mark read/all
  - `feature/notifications/NotificationsScreen.kt` — feed UI
  - `designsystem/components/NotificationBell.kt` — reusable bell + badge
  - `navigation/NavRoutes.kt` — `NotificationRoutes.NOTIFICATIONS`
  - `navigation/PrognoCareNavGraph.kt` — shared route + doctor deep-link
  - `DoctorDashboardScreen.kt` — stub replaced; subscription on open
  - `test/.../NotificationApiContractTest.kt` — 6 MockWebServer contract tests
- Planning files updated (task_plan/findings/progress)
- **Not yet run by agent:** Gradle unit tests + assembleDebug (user will run externally)

## Pending (user)
- [ ] `./gradlew testDebugUnitTest`
- [ ] `./gradlew assembleDebug`
- [ ] Manual: doctor dashboard bell count + feed + request tap-through
