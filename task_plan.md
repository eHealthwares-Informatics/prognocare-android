# Task Plan: PrognoCare Android ↔ EMR in-app notifications (issue #6)

## Goal
Phase 1 mobile integration of EMR in-app notifications: bell + feed + subscription, doctor dashboard wired first.

## Current Phase
Phase 18 — implementation complete; **awaiting external Gradle verification**

## Key Decisions
| Decision | Rationale |
|----------|-----------|
| Shared `NotificationRoutes.NOTIFICATIONS` | Issue asks for a shared route (not doctor-prefixed); doctor request detail is Phase 1 target for deep-links |
| `NotificationsRepository` reads `ApiBundle` each call | Same pattern as AppointmentsRepository — runtime base-URL changes apply immediately |
| Best-effort `subscribe()` on dashboard open | Issue: no throw on 404; Phase 1 in-app only |
| Poll only while feed visible (`startPolling`/`onFeedClosed`) | Acceptance: "Poll every 30s while screen visible" |
| Merge by id, newest `createdAt` wins + cursor | Issue contract: poll cursor = latest createdAt |
| Doctor badge = notifications unread (not urgentCount) | Acceptance: bell shows unread count |
| Request tap → doctor request detail only | Phase 1 doctor-first; other roles out of scope |

## Phases

### Phase 18: EMR in-app notifications mobile (android#6)
- [x] EMR bugfix (separate): TypeORM `orderBy` property paths — notifications list 500 fixed on `feat/emr-notifications-phase1` @ `036c071`
- [x] Models: `NotificationItem`, `SubscribeNotificationsDto`, `NotificationSubscription`, unread/mark-read responses
- [x] `NotificationApi` + wire into `ApiBundle`/`RetrofitClient`
- [x] `NotificationsRepository` (list/unread/markRead/markAllRead/subscribe)
- [x] `NotificationsViewModel` (30s poll, merge by id, badge, mark read/all, best-effort subscribe)
- [x] `NotificationBell` (designsystem, badge 99+ cap)
- [x] `NotificationsScreen` (feed, mark all read, request tap-through)
- [x] Shared route `NotificationRoutes.NOTIFICATIONS` + nav graph wiring
- [x] Doctor dashboard: replace `/* notifications */` stub with `NotificationBell` + `onNavigateToNotifications`; `LaunchedEffect` → `onDashboardOpen()`
- [x] `NotificationApiContractTest` (MockWebServer, RequestsApiContractTest pattern)
- [ ] **External:** `./gradlew testDebugUnitTest` (NotificationApiContractTest + full suite)
- [ ] **External:** `./gradlew assembleDebug`

## Errors Encountered
| Error | Attempt | Resolution |
|-------|---------|------------|
| gh issue create tool_call_id 400 | 1 | Retried later; issue android#6 created |
| EMR GET /notifications 500 databaseName | 1 | orderBy property paths (`notification.createdAt`) |
| EmptyState used wrong param name | 1 | `message` not `subtitle` (caught by reading SharedComponents) |
| Gradle test permission denied (agent) | 1 | User will run Gradle externally |

## Out of scope (follow-up)
- Native push (FCM) — Phase 2
- Badge on nurse/admin/other dashboards
- Offline queue for mark-read
