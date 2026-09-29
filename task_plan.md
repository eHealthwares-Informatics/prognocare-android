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

### Phase 9 (user direction): App icon, location-based queries, role switcher, appointments/visits/encounters
- [x] App icon = splash image: legacy webp square/round tiles (logo at 80% on white) for all 5 densities + adaptive 108dp foreground PNG (logo at ~66%, safe zone) in drawable/; white `ic_launcher_background` color; old vector foreground/background drawables removed; verified inside the built APK
- [x] Location-based queries: LocationScope (active location id/label in SessionStore, seeded from staff.identityLocationId at login, clears on sign-out); shared LocationScopeChip picker (All locations + facility list); one shared match rule (LocationScope.matches) applied client-side to appointments (locationId|scheduleLocation), visits (locationId) and encounters (via parent visit); ScheduleAppointmentDialog gained a location picker; created appointments/visits carry the active locationId
- [x] Change role from user menu: "Active role" chip grid in UserProfileScreen; switchRole persists via SplashViewModel.saveAuthState + SessionStore.saveRole and relaunches the graph on the new role's dashboard
- [x] Create + view Appointments/Visits/Encounters: new shared feature/clinical (ClinicalViewModels, ClinicalScreens) with list screens (filters, actions: check-in/complete/cancel, visit end/cancel, encounter→detail) and create bottom-sheets (patient search, provider/type/priority/visit-link, encounter auto-creates an OUTPATIENT visit when none chosen); ClinicalRoutes wired in nav graph; doctor dashboard FAB/quick actions now navigate (encounters, new request, clinical note form picker); admin "View Reports" opens the shared appointments list
- [x] Tests: VisitsEncountersContractTest (6 MockWebServer contract tests + 2 LocationScope match-rule tests); 85 Android unit tests green; compileDebugKotlin + assembleDebug green
- Notes: no backend changes needed (encounters scope via visitId→visit.locationId); gradle daemon quirk — JAVA_HOME from VS Code extension JRE lacked jlink, fixed by ~/.gradle/gradle.properties org.gradle.java.home=temurin-21 + daemon restart

### Phase 10 (user direction): crash fixes + doctor encounter workflow (mobile + backend + web)
- [x] PatientDetailViewModel crash fixed: replaced broken assisted-inject (missing @HiltViewModel + missing javax.inject.Inject) with @HiltViewModel + bind(patientId); screen uses plain hiltViewModel()
- [x] Create Request 400 fixed: RetrofitClient Json explicitNulls=false — EMR whitelist validation no longer sees items[].id/requestId nulls
- [x] EMR: ENCOUNTER_STATUSES (ACTIVE/COMPLETED/CANCELLED) + encounters.status column + endedAt; create defaults ACTIVE, list filters by status, PATCH accepts status/endedAt; GET /api/dashboard/attended-patients?providerId= (distinct patients with a visit or encounter; org-wide when no providerId); encounters spec extended (11 tests)
- [x] Doctor dashboard Total = attended patients via new endpoint (self-scoped by staffId, org-wide fallback)
- [x] DocumentEncounter modal: 3 explicit modes (From visits / From patient / From appointment), mandatory patient in each, dedup per patient; appointment mode auto-starts a visit (appointmentId) for the encounter; encounters list route /clinical/encounters/new opens the dialog immediately (doctor FAB) and submit lands on the encounter screen
- [x] Clinical encounter screen rebuilt: real patient name (visit→patient record→MRN fallback), status badge, created/start/end times, live timer (1s tick, auto-end at 8h), documentation + requests of the encounter AND its visit with refresh/add/edit hooks, working End Encounter (status COMPLETED + endedAt)
- [x] New Request form: type chips in FlowRow (wrap) with icons (Science/Biotech/Medication/MonitorHeart); Patient/Visit/Encounter scope selector — ongoing visits exclude patients with active encounters, patient search excludes visit/encounter-covered patients; requests carry visitId/encounterId
- [x] Patient detail Records tab refreshable (Refresh button + empty-state action)
- [x] Web frontend: encounter page shows status badge + start/end/created + live timer with 8h auto-end + End Encounter button; requests tab merges encounter + visit requests; Encounter type gained status/endedAt
- [x] Verify: EMR 177 tests + build clean; Android 86 unit tests + compileDebugKotlin + assembleDebug green; frontend tsc clean

### Phase 11 (user direction): vitals save failure
- [x] Root cause 1: VitalsRecordingScreen sent snake_case keys (bp_systolic, heart_rate, oxygen_saturation, ...) while the seeded VITALS schema uses camelCase (bloodPressureSystolic, heartRate, oxygenSaturation) — values never landed under the schema's keys
- [x] Root cause 2: submitVitals serialized Map<String,Any?> via a polymorphic Any serializer (runtime risk) and returned a bare Boolean, hiding the server's validation message behind "Could not save vitals — check the form keys or server"
- [x] Fix: new feature/forms/SchemaKeyMapper maps clinical concepts (temperature, bpSystolic, oxygenSaturation, ...) onto the form definition's real schema keys via normalized alias matching (exact + containment for 5+ char aliases, containers walked); VitalsRecordingViewModel builds the payload from form.schemaJson and returns VitalsSaveResult.Failure with the server's error text; screen shows the actionable message
- [x] Tests: SchemaKeyMapperTest (5 tests: camelCase seed schema, snake_case, suffixed keys, nesting + dropping, blank/invalid schema); 91 Android unit tests green; compile + assembleDebug green

### Phase 12 (user direction): VitalsCard on the clinical encounter screen
- [x] SchemaKeyMapper.conceptFor reverse lookup (schema key -> clinical concept, same alias rules as the forward map)
- [x] VitalsReadings extractor: latest non-DRAFT VITALS submission (form name contains "vital"), values read via concepts with units (temp °C, BP systolic/diastolic pairing with one-sided fallback, bpm, /min, %, kg, cm, mg/dL, /10), notes + recordedAt/recordedBy; rows() ordered; returns null when payload has no vital concepts
- [x] DoctorEncounterViewModel exposes vitals from the visit-scoped submissions it already loads; encounter screen renders a VitalsCard (MonitorHeart icon, timestamp + recorder header, label/value rows) between the patient header and reason/notes
- [x] Tests: VitalsReadingsTest (6 tests); 97 Android unit tests green; compile + assembleDebug green

### Phase 13 (user direction): SchemaKeyMapper for all dynamic form submissions
- [x] SchemaKeyMapper.remap: generic submit-payload builder — resolves each schema field's value from the form value bag (exact key → normalized equality → concept-alias match), each bag entry consumed once, unknown entries and nulls dropped; missing/malformed schema passes the bag through unchanged
- [x] map() now delegates to remap (single source of truth; consume-once is strictly more correct)
- [x] FormViewModel.submit (DynamicFormScreen renderer for every custom form) routes its payload through remap and surfaces the server's validation message instead of just the HTTP code
- [x] Tests: remap suite (concept-key resolution + junk dropping, exact-key precedence + consume-once, null dropping, schema passthrough); 101 Android unit tests green; compile + assembleDebug green

### Phase 14 (user direction): Referrals + Medications across EMR/Android/web; admin & UX polish
- EMR backend: new modules referrals/ + medications/. Referral = encounter-tied specialist referral (REF-#, statuses PENDING/ACCEPTED/DECLINED/COMPLETED, priorities ROUTINE/URGENT; create validates encounter-patient match + blocks self-referral + resolves referring provider from the signed-in staff; decide() PENDING-only with staff-party check; complete() ACCEPTED→COMPLETED; list filters direction=incoming|outgoing scoped by providerId + status/patientId/encounterId). Medication = created from a PRESCRIPTION request item (MED-#, requestId+itemIndex, convert-once per item, rejects CANCELLED requests, item defaults with dto overrides; administer() stamps administeredAt/ById/ByName + notes/outcome and best-effort transitions the source request to COMPLETED; status/patient/request/encounter/administered filters).
- EMR specs: referrals.service.spec.ts + medications.service.spec.ts (28 new tests) → 210 EMR tests / 19 suites green; nest build clean.
- Android: Referral/Medication models + ReferralApi/MedicationApi wired into ApiBundle/RetrofitClient; EmrRepository referral+medication helpers. SpecialistReferralScreen rebuilt on the real API (incoming/outgoing tabs, status filters, pull-to-refresh, accept/decline/complete, create-referral dialog with encounter+specialist pickers); SpecialistReferralDetailScreen live (decision reason, complete); SpecialistDashboardScreen shows real recent referrals; MedicationsViewModel + rebuilt MedicationAdministrationScreen (create-from-prescription picker keyed by requestId+itemIndex, administer with notes, pull-to-refresh).
- Android UX (this phase's asks): RegisterPatientScreen (admin, identity/contact/next-of-kin sections → POST /patients) wired to the admin quick action + AdminRoutes.REGISTER_PATIENT; all 9 role dashboards wrapped in PullToRefreshBox (retry/refresh hooks); doctor's Clinical Note quick action now routes through FormPickerScreen which asks for patient/ongoing-visit/active-encounter when opened without scope (same semantics as New Request; new FormScopeViewModel) and passes the resolved scope into the form route; FormPickerScreen list pull-to-refresh; DynamicFormScreen arranges consecutive simple fields two-per-row (sections/tabs/tables/textareas keep full width); doctor patient-detail edit button removed (doctors don't edit patients).
- Web frontend: /emr/referrals + /emr/medications pages (ModelConfig DataPageShell schemas, route files, sidebar entries with Share2/Pill icons); StatusBadge kinds referral+medication; CreateReferralModal on the encounter detail page (specialist picker via /staff?roleType=Specialist, reason, priority); CreateMedicationModal on PRESCRIPTION request detail (item select + overrides → POST /medications); routeTree regenerated.
- Verify: EMR 210 tests + build; Android compileDebugKotlin + testDebugUnitTest; frontend tsc --noEmit — all green.

### Phase 15: schema-version awareness for form submissions
- EMR: FormSubmissionsService enriches get/list/getChain with schemaOutdated + schemaCurrentVersion — a submission is flagged when its fill-time formVersion is older than the definition's current publishedVersion (draft version bumps don't stale existing submissions; definition-missing degrades to no warning). amend() now stamps the amendment with the CURRENT published version (a fresh fill), so amendments never inherit staleness from the original.
- Tests: 3 new spec cases (outdated flagging, unpublished definition passthrough, amendment version stamping) → 213 EMR tests green; nest build clean.
- Web: FormSubmission type gains schemaOutdated/schemaCurrentVersion; shared SchemaOutdatedBadge (orange "Older schema" chip with tooltip "Filled on schema vN — the published schema is now vM") rendered in DocumentsAccordion rows and the SubmissionViewModal header.
- Android: FormSubmission model gains the same fields; the doctor encounter screen's documentation list shows a warning row ("Older schema (v2 → v3) — values may not match current fields") under stale submissions.
- Verify: Android compileDebugKotlin + testDebugUnitTest; frontend tsc --noEmit; EMR tsc + 213 tests + build — all green.

### Phase 16: patient tap-through → tabbed patient detail
- Root cause: `specialist/patients/{id}`, `therapist/patients/{id}`, `admin` directory taps and the nurse bottom-nav "Patients" item navigated to routes with NO registered destination (silent no-op); the shared DoctorPatientDetailScreen lacked a Visits tab; child queries scoped by the raw route arg (record id) while records key on the MRN.
- Fix: PatientDetailViewModel now resolves the patient by id OR MRN (getById → getByMrn fallback), scopes visits/encounters/requests/submissions by the resolved MRN, and loads visits (VisitApi.list gained the backend-supported patientId filter). New VisitsTab (type, status badge, number/date range/provider) between Overview and Encounters.
- Routes: registered shared-detail destinations for specialist/patients/{id}, nurse/patients/{id}, admin/patients/{id}; nurse Patients bottom-nav now opens PatientSearchScreen with tap-through; admin directory taps open the detail. Doctor flow unchanged.
- Verify: Android compileDebugKotlin + testDebugUnitTest green. (Web already had Appointments/Visits/Encounters/Requests/Documentation tabs on /emr/patients/$patientId.)

### Phase 17: messaging conformance with the conversation engine (PROGNOCARE_MESSAGING)
- Root causes of the flaky chat: (1) Android sent a hardcoded Mongo channelId (69bd061c…) to /webhooks/web — environment-specific, broke after reseeding; (2) its socket never joined the engine's phone:<normalized> delivery rooms, so bot replies were never delivered to the device; (3) it didn't listen to conversation.message.orphan (the first bot reply while the conversation is still a pending placeholder); (4) sender identity was a token prefix that changed every re-login, forking participants.
- Seed: PROGNOCARE_MESSAGING WEB channel (uuid 69c2930ad541d741898d5224, pseudo participant 680000000000000000000008, phone +234000000008) added to seed/seeds/conversation/inline/channels.json + participants.json (shape-matched, idempotent).
- Android: SendWebhookDto now carries channelCode + newConversation (engine resolves the channel by stable code); AppConfig.webChannelId → webChannelCode (default PROGNOCARE_MESSAGING via BuildConfig) across store/keys/settings UI/login/profile labels; ChatSocket joins delivery rooms via auth.phone handshake + conversation.identify on connect and listens to orphan events; new ChatIdentity (staff phone seeded at login → session phone → deterministic device id) replaces the token-prefix sender; ChatScreen supports a `new` sentinel (New conversation FAB on the list) — sends with conversationId=null + newConversation=true and adopts the real id from the first reply; after conversation.ended, sends go out fresh (never reuse the dead id); transcripts refetch after send so the user's own message appears.
- Verified conformance reference points: ehealthwares ChatbotWidget already channelCode-based (EHEALTHWARES_WEBCHAT_BOT); conversation gateway/test-sender/webhook-controller unchanged (they already implement code-first resolution, per-recipient projection emission, orphan/created/updated events, and phone/user room delivery).
- Verify: Android compileDebugKotlin + testDebugUnitTest green; seed JSON valid; no webChannelId references remain.
