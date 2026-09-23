# Task Plan: Connect PrognoCare Android to EMR Backend + Conversation Engine

## Goal
Wire the PrognoCare Android app to the real EMR backend (port 8093, /api) and Conversation Engine (chat), with runtime-configurable URLs (hidden 7-tap input + settings view), merged dashboard metrics, full chat with option controls, config maps (view→form, mobile role→backend role), chat notifications, new backend roles (Specialist/Finance), and tests per feature.

## Current Phase
Phase 8 (complete) — all 9 roles wired to the EMR backend + 10-tap server-config easter egg

## Key Decisions
| Decision | Rationale |
|----------|-----------|
| Login proxy in EMR → rxsoft-identity | EMR has no auth; identity has login; share one JWT secret (admin-access-secret) |
| Base URL LAN device, configurable at runtime | Real phone via host.sh; settings view + 7-tap hidden input override |
| Chat separate IP/port, configurable | Conversation engine runs on its own host/port |
| Envelope PaginatedResponse<T> | Backend list endpoints return {data, meta} |
| New backend roles Specialist + Finance | Mobile goes to backend with role mapping/validation |
| Keep mock for unsupported features, tag "Not available" | Chat wired to conversation engine; billing/referrals/therapy/tickets/med-admin stay mock |
| Full chat scope | Inbox/modes/filters, send via /webhooks/web, socket namespace /conversations, option controls from web chat-ui |

## Phases

### Phase 0: Networking & config foundation
- [x] AppConfigStore (DataStore) with emrBaseUrl, conversationBaseUrl, webChannelId
- [x] Rebuildable RetrofitClient + chat client on config change
- [x] Re-type list endpoints to PaginatedResponse<T>
- [x] Cleartext HTTP config + buildConfigField for defaults
- **Status:** done — compile passes (`:app:compileDebugKotlin`)

### Phase 1: Hidden input + settings view
- [x] 7-tap easter egg on LoginScreen reveals server URL input
- [x] SettingsScreen + SettingsViewModel for URLs (EMR + conversation)
- **Status:** done — unit tests for TapCounter + AppConfig normalizers pass; compile passes

### Phase 2: Runtime URL switch confirmation
- [x] On save, verify EMR via /api/health, chat via /api/health; ✔/✘ confirmation per endpoint
- [x] Clients rebuild live (RetrofitClient StateFlow rebuilds on config change)
- [x] Added `/api/health` to conversation engine AppController (was missing)
- **Status:** done — ServerConfigVerifier + SettingsViewModel save-flow; MockWebServer tests pass; compile passes

### Phase 3: Dashboard metrics merge
- [x] Define business rules for all KPIs (mobile mock + backend)
- [x] Backend: add totalPatients, activeVisits, pendingRequests to dashboard service
- [x] Android: rewrite DashboardSummary to merged shape
- **Status:** done — dashboard service spec added (KPI counts, provider load, upcoming list); repo-mock gained getCount/addOrderBy/limit; all EMR unit tests pass; Android compiles

**KPI business rules:**
- `totalAppointments` = today's appointments count
- `scheduled`/`checkedIn`/`inProgress`/`completed`/`cancelled`/`noShow` = today's status counts (checkedIn = CHECKED_IN + IN_PROGRESS)
- `providersOnDuty` = distinct providers with active visits
- `averageWaitMinutes` = mean of (visit start − scheduled time) for checked-in/in-progress today
- `totalPatients` = active (`is_active`) non-deleted patients
- `activeVisits` = ONGOING non-deleted visits
- `pendingRequests` = requests with status REQUESTED or IN_PROGRESS

### Phase 4: Full chat integration
- [x] ChatApi (inbox, exchanges, send /webhooks/web, mark read)
- [x] Socket.IO client /conversations with JWT (ChatSocket, singleton)
- [x] ChatOptionParser + option controls in message bubbles
- **Status:** done — ChatClient rebuilds on config change; ChatRepository (inbox/messages flows, socket-driven inbox refresh); ConversationListScreen + ChatScreen rewired to backend; sender phone stored in `prognocare_auth`; ChatApiContract + ChatOptionParser tests pass (26 total)

### Phase 5: Config maps + login validation
- [x] ViewFormMap (view → form code) + config screen
- [x] RoleMap (mobile role → backend role) + validation on login
- **Status:** done — EMR AuthProxyModule (login/refresh/logout/logout-all/me → identity) with tests; STAFF_ROLE_TYPES += Specialist/Finance; Android LoginViewModel performs proxy login → saves JWT → GET /api/auth/me → UserRoleMapper picks the mobile role (role codes + module fallback); ViewFormMap maps encounter types/views to preferred form codes (picked from GET /forms/available); UserRoleMapper + ViewFormMap + AuthApiContract tests pass (31 total)

### Phase 6: Chat notifications + docs
- [x] Websocket message → in-app notification popup → navigate to messages
- [ ] Document unsupported features + tag "Not available"
- **Status:** chat notifications done — ChatRepository emits `IncomingMessageNotification` for inbound msgs; host scaffold collects via ChatNotificationViewModel → snackbar popup → navigate. Unsupported-feature tagging skipped per user direction.

### Phase 7 (user direction): Dynamic form builder
- [x] FormSchemaParser (schemaJson → typed FormField descriptors, all field types + tab/col/section/table containers)
- [x] FormValidator (mirrors web validateFormData) + FormViewModel (load available/by-id, build initial data, validate, submit)
- [x] DynamicFormScreen (renders all field types, table add/remove rows) + FormPickerScreen + nav routes + "Documentation" FAB on DoctorPatientDetail
- **Status:** done — fetch form definitions from backend (GET /forms/available, GET /form-definitions/:id), render dynamically, validate, submit to POST /api/form-submissions. Tests: FormSchemaParser, FormValidator, FormApiContract.

### Backend changes
- [x] AuthProxyModule (POST /api/auth/login → identity, GET /api/auth/me)
- [x] Add Specialist/Finance to STAFF_ROLE_TYPES
- [x] Dashboard KPI additions
- **Status:** auth-proxy done (login/refresh/logout/logout-all/me forwarded to identity; 124 tests pass); STAFF_ROLE_TYPES extended; dashboard done earlier

### Tests
- [x] Unit: config store, tap counter, ChatOptionParser, KPI rules, maps
- [x] API/repo: per domain with MockWebServer
- [ ] UI: settings save→switch, hidden input reveal, notification→nav, role validation
- **Status:** unit + API contract tests pass (62 Android; 124 EMR)

## Errors Encountered
| Error | Attempt | Resolution |
|-------|---------|------------|
|       |         |            |

### Phase 8 (user direction): Login + all 9 role features + 10-tap easter egg
- [x] Easter egg standardized at 10 taps (TapCounter default), tap-progress hint after 3 taps, current-URL display above the editable config; also reachable from the profile version row (10 taps toggles the same ServerConfigContent; saves app-wide via AppConfigStore)
- [x] EMR: GET /api/staff?userId= filter (self-scoping) + spec
- [x] SessionStore: identity userId + staff link + role persisted at login; cleared on sign-out
- [x] EmrRepository (dashboard/requests/encounters/staff/locations/patients) + PatientSearchScreen, NotAvailableBadge/DemoDataChip
- [x] Doctor: real dashboard (self-scoped), patient list/detail (live encounters/requests/docs), encounter screen fixed to real encounterId, Requests list/detail/create with line items + transitions + notes
- [x] Nurse: real check-in queue/tasks, vitals via VITALS form submission, medication admin on PRESCRIPTION requests (transition+note)
- [x] Patient: dashboard/meds/records keyed to linked MRN via SessionStore
- [x] Specialist: real consultations/patients, consultation-notes opens FormPicker; referrals demo-tagged
- [x] Therapist: real sessions; plans/assessments demo-tagged
- [x] Technician: LAB requests as orders with real status transitions; results demo-tagged (LIS owns files)
- [x] Support: real today's schedule + check-in/out; tickets demo-tagged
- [x] Finance: real KPIs + payment providers + patient lookup; billing demo-tagged (rxsoft-backend owns billing)
- [x] Admin: real KPIs, check-in queue, staff CRUD (add-staff dialog), facilities from LocationApi, patient directory
- [x] Tests: TapCounter 10-tap suite, RequestsApiContractTest (7 tests); 79 Android unit tests green; EMR 176 tests green; assembleDebug produces APK
