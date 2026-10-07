# Findings — EMR in-app notifications mobile (android#6)

## API contract (EMR :8093)
| Endpoint | Method | Notes |
|----------|--------|-------|
| `/api/notifications?since=&limit=` | GET | `{ data: NotificationItem[], meta }`; poll cursor = latest `createdAt` |
| `/api/notifications/unread-count` | GET | `{ count: number }` |
| `/api/notifications/{id}/read` | PUT | mark one read; id is **notification** id |
| `/api/notifications/read-all` | PATCH | mark all read |
| `/api/notification-subscriptions` | POST | body optional; org/location from JWT tenant |

NotificationItem: `id, title, body, type, sourceEntityType, sourceEntityId, sourceEntityRef, read, readAt, createdAt`

## EMR list endpoint crash (fixed)
- TypeORM `orderBy()` resolves **entity property paths**, not DB column names.
- `skip`/`take` + joins → `createOrderByCombinedWithSelectExpression` does `findColumnWithPropertyPath('created_at')` → `undefined.databaseName`.
- Fix: `.orderBy('notification.createdAt', 'DESC')` (and same latent pattern elsewhere).

## Android app layout (PrognoCare)
- Source root: `app/src/main/java/com/ehealthinformatics/prognocare/`
- Network: `data/remote/RetrofitClient.kt` → `ApiBundle` StateFlow rebuilt on config change
- Patterns to copy: `RequestApi.kt`, `AppointmentsRepository.kt`, `RequestsApiContractTest.kt`
- Doctor bell stub was `IconButton(onClick = { /* notifications */ })` showing `state.urgentCount`
- Design system badge colors: `AppThemeColors.notificationBadge` / `onNotificationBadge`
- EmptyState signature: `icon, title, message, actionText?, onActionClick?, modifier`
- Icons: `material-icons-extended` is on the classpath (`compose-material-icons` in libs.versions.toml)

## Navigation
- Shared route: `NotificationRoutes.NOTIFICATIONS = "notifications"`
- Doctor request deep-link: `DoctorRoutes.requestDetail(requestId)` → `doctor/requests/{requestId}`
- Feed screen starts/stops poll via `DisposableEffect` + `onFeedOpened`/`onFeedClosed`

## Verification ownership
- Agent cannot run `./gradlew` (permission denied for this session).
- User runs externally:
  - `./gradlew testDebugUnitTest`
  - `./gradlew assembleDebug`
