# Progress — android#6 EMR in-app notifications

## 2026-10-07
- Created GitHub issue: https://github.com/eHealthwares-Informatics/android/issues/6
- EMR: fixed notifications list 500 (TypeORM orderBy property paths); unit tests 229 green; committed `036c071` on `feat/emr-notifications-phase1`
- Android (PrognoCare `~/develop/rxsoft/prognocare` on `main`):
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
- [ ] Merge PR prognocare-android#2 (layout.size import fix)
- [ ] `./gradlew compileDebugKotlin` / `testDebugUnitTest` / `assembleDebug`
- [ ] Manual: doctor dashboard bell count + feed + request tap-through

## 2026-10-07 (follow-up)
- PR prognocare-android#1 merged into main (71cfc71) — compile still failed on missing \`layout.size\`
- Fix branch \`fix/notifications-size-import\` @ \`74e1b6f\` → PR prognocare-android#2
- EMR PR #5 updated with orderBy fix \`036c071\`


## 2026-10-11 — Phase 6.1 structured chat payload (clerking plan 6.1)
- New plan dir: .planning/2026-10-11-phase-6-1-structured-chat-payload
- Engine audit: WEB TestSender socket events carry {id, conversationId, senderId,
  receiverId, direction, text, createdAt, status}; Exchange.payload = rawPayload
  (REST GET /exchanges already returns it). Options flattened into text by
  askQuestion (title\nkey: label\u200B).
- Implemented:
  - ChatModels.kt: ExchangeMessage.payload (JsonElement?)
  - ChatRepository: socket payload passthrough (Json.parseToJsonElement, fail-safe)
  - ChatQuestionEnvelope.kt (new): structured-first envelope parser; falls back
    to ChatOptionParser text contract; supports MULTI selectionMode, allowNone,
    progress {index,total}; NONE_SENTINEL constant
  - ChatScreen: ChatBubble parses envelope; OptionCard rewritten — single-select
    sends key immediately; MULTI accumulates with ✓ ticks + Done (comma-joined
    batch answer) + 'None of these' when allowNone; progress subtitle
  - ChatQuestionEnvelopeTest.kt (new): 7 tests
- Verified: ./gradlew compileDebugKotlin EXIT 0; full testDebugUnitTest
  118 tests / 0 failures (needs JAVA_HOME=temurin-21 — gradle defaulted to
  VSCode-embedded JRE without jlink).
- Not committed yet (branch + PR pending user go-ahead).
