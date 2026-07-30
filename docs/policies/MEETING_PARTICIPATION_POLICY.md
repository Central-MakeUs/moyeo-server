# Meeting Participation Policy

> Purpose: Canonical domain policy for the current meeting creation, invite, guest
> join, and participation input flows.

DOMAIN-001: Undocumented domain behavior must not be implemented as product
policy.

If implementation or review requires a product/domain decision that is not backed
by human-defined policy, classify it as `POLICY_UNDEFINED`. Do not promote Codex
general best practice into domain policy.

## Meeting Creation

- For the first MVP, meeting creation is completed with one final API request after
  the creation steps are filled out.
- Draft or step-by-step meeting creation should be added later as a separate draft
  flow if product policy requires it.
- Meeting creation is selected by `planningType`: `SCHEDULE_ONLY`, `PLACE_ONLY`, or
  `SCHEDULE_AND_PLACE`.
- Schedule mode is currently stored as `VOTE`, `FIXED`, or `NONE`, but current
  meeting creation derives it from `planningType` and does not accept fixed schedule
  input.
- Place mode is currently stored as `FIXED`, `RECOMMEND`, or `NONE`, but current
  meeting creation derives it from `planningType` and does not accept fixed place
  input.
- Fixed schedule/place direct input is excluded from the current MVP creation
  flow and may be reconsidered in a later product discussion.
- Place recommendation strategy is separated from place mode. For the current
  MVP creation flow, the server always stores `MIDDLE_POINT` when place mode is
  `RECOMMEND`; clients do not send a strategy. The stored strategy and enum are
  retained so a later product decision can add another strategy without changing
  the meeting schema.
- Meeting creation creates the host `meeting_participants` row and saves every
  host input required by the selected planning flow in the same transaction.
- Meeting creation receives `scheduleCandidateDates` for schedule coordination,
  `scheduleResponse.availableTimeRanges` for `DATE_AND_TIME`, and `departure` for
  place coordination. `SCHEDULE_ONLY` and `SCHEDULE_AND_PLACE` also receive the explicit
  `scheduleInputType` selected in CRT rather than inferring it from nullable time
  fields.
- `PLACE_ONLY` omits `scheduleInputType` and the common time range.
  `SCHEDULE_ONLY` and `SCHEDULE_AND_PLACE` require either `DATE_ONLY` or
  `DATE_AND_TIME`. `DATE_ONLY` omits the common time range; `DATE_AND_TIME`
  requires both `availableStartTime` and `availableEndTime`.
- A participant whose coordinate pair is omitted counts as having submitted a departure snapshot, but is excluded from the straight-line middle-point preview. If no submitted departure has coordinates, the place view returns `COORDINATES_PENDING` with no recommendations.
- Meeting creation receives `noDeadline`. When it is false or omitted, the
  request also receives `deadlineMinutes` and the server calculates and stores
  `deadlineAt`. When `noDeadline=true`, `deadlineMinutes` is omitted or null
  and `deadlineAt` is stored as null.
- The meeting-creation success response returns `meetingId` and `inviteCode`.
- When `noDeadline` is false or omitted, `deadlineMinutes` is required and is
  accepted in 10-minute units from 10 minutes up to 7 days. A zero-minute
  deadline is not allowed. When `noDeadline=true`, `deadlineMinutes` is
  omitted or null.
- For a deadline-bound meeting, `deadlineAt` is calculated from the server
  processing time of the final meeting creation request. Any client-side
  expected end time is only a preview and may differ if the user stays on the
  screen before submitting.
- TODO: For CRT-06, keep the current duration-only selection UX until product
  confirmation. If an absolute deadline preview or second-accurate countdown is
  later required, decide whether the API should provide a server-time reference;
  the server-calculated `deadlineAt` remains authoritative after creation.
- A meeting cover image is optional and is used only as a meeting-home thumbnail.
  The server stores one resized cover derivative in durable object storage; the
  uploaded original is not retained. The resized derivative is part of the
  long-term meeting record.
- Temporary MVP policy: the client keeps a selected cover file locally until the
  final meeting-creation request. That request may use multipart form data with
  the existing meeting JSON and an optional cover file; the response returns the
  same `meetingId` and `inviteCode` as JSON creation. The server does
  not create temporary upload objects.
- Temporary MVP policy: the host may later replace or delete the cover through a
  dedicated authenticated API. The invite-link meeting view may read the cover.
  If the selected cover cannot be stored, the final meeting creation fails.
- The cover-image response URL includes a value derived from the current stored
  object key only to distinguish browser cache entries. It is not a new database
  field or object-storage path. When the host replaces the cover, clients use
  the returned new URL; the former S3 object is deleted only after the database
  transaction commits successfully. Store the former object's cleanup task in
  the same database transaction as the cover-key change, then attempt it after
  commit so a process restart cannot lose the object key. If a newly uploaded
  object cannot be deleted by the transaction-rollback callback, queue that
  failed deletion for the shared cover-cleanup scheduler.
- Because S3 upload and the database transaction cannot commit atomically, scan
  cover objects after a 24-hour grace period and delete only keys that are not
  referenced by any meeting. This recovers an upload left behind by a process
  or instance failure before the rollback callback can run.
- Temporary technical settings: JPEG/PNG input, 10 MB maximum upload size, a
  1280x720 output bounding box, an 8000x8000 source-dimension bound, a 13,000,000
  source-pixel bound, and JPEG quality 0.85. Validate source dimensions from the
  image header before full decode. These are configuration values, not final
  product policy; revisit format, size, crop, compression, visibility, source
  dimensions, and deletion retention after MVP feedback.
- TODO (frontend implementation): after the user selects a cover, preserve its
  orientation, offer a 16:9 crop, resize the long edge to at most 2,560 pixels,
  and encode JPEG around quality 0.82-0.85 before upload. Aim for 4 MB or less,
  while treating the server's 10 MB and 13,000,000-pixel limits as final safety
  bounds. Convert HEIC to JPEG because the current backend accepts only JPEG
  and PNG.
- TODO: Add the remaining negative-path cover-image tests for storage-unavailable
  API responses. Current automated tests cover invalid/oversized input,
  source-dimension rejection, non-host modification, failed-deletion queueing,
  scheduled retry, and cleanup after a real transaction rollback.
- Schedule voting candidate dates are stored as separate rows during meeting
  creation. The host may submit at most 21 candidate dates. Duplicate dates
  are normalized into one date before storage.
- `DATE_AND_TIME` applies the same available time range to every selected
  candidate date. Its common range and participant ranges are currently accepted
  in 1-hour units.
- `DATE_ONLY` stores participant selections in
  `meeting_participant_schedule_date_availabilities`; it does not create artificial
  start/end times.
- For schedule-coordination meetings, only the host selects the meeting's candidate
  dates. For `DATE_ONLY`, those candidate dates are also the host's available dates.
  For `DATE_AND_TIME`, the host selects their own available ranges within those
  dates and the common time range established during creation. Ordinary
  participants cannot add or replace candidate dates.

## Host Participation During Creation

- The authenticated creator submits the meeting settings and their own
  participation input in one final creation request.
- Schedule-coordination creation always receives
  `scheduleCandidateDates`. `DATE_ONLY` omits `scheduleResponse`, because the
  candidate dates are stored as the host's available dates.
  `DATE_AND_TIME` additionally receives
  `scheduleResponse.availableTimeRanges`. Place-coordination creation
  receives the same departure snapshot shape used by other participants.
- Meeting, host participant, candidate dates, host schedule availability, and host
  departure are saved in one transaction. Invalid host input must leave none of
  those rows behind.
- There is no separate post-creation host participation API. A successful creation
  returns `meetingId` and `inviteCode`.

## Meeting Participant Identity

- Meeting participant nickname duplication is checked only among guest
  participants inside the same meeting. Host/member nicknames may overlap with
  guest nicknames and with each other.
- A guest nickname consists of 2 to 10 Korean Hangul syllables or English
  letters, and a guest participation password consists of exactly four digits.
- A service user should not be linked to the same meeting more than once; enforce
  this with a meeting-scoped uniqueness rule such as `unique(meeting_id, user_id)`.
- Guest participants currently have nullable `user_id`, so multiple guest
  participants remain allowed.
- Current participant behavior is defined for `HOST`, `MEMBER`, and `GUEST`.
- When a service user withdraws, hard-delete every meeting hosted by that user,
  including participants, schedule candidates and availabilities, meeting-linked
  departure search history, and its stored cover image.
- In meetings hosted by another user, hard-delete the withdrawing user's `MEMBER`
  participant row, including its meeting-scoped nickname, schedule availability,
  and departure snapshot. The withdrawn user is excluded from participant lists,
  counts, schedule availability aggregation, and place recommendation
  calculations; the freed capacity may be reused.
- A host may hard-delete a meeting they created regardless of whether it is in
  `PLANNING` or `CONFIRMED`. Delete all participants, participant schedule
  responses, schedule candidates, meeting-linked departure search history, and
  the stored cover image with the meeting.
- A logged-in `MEMBER` may leave a meeting regardless of its status. Hard-delete
  only that member's participant row, meeting-scoped nickname, schedule
  responses, and departure snapshot; the freed capacity may be reused. A host
  cannot leave and must delete the hosted meeting instead. Guest leave remains
  deferred until guest re-entry authentication is defined.
- A `HOST` or `MEMBER` may change only their own meeting-scoped nickname using
  the same Korean/English 2-10-character rule as join. This does not change the
  user's default nickname. Nickname duplication remains prohibited only between
  guests in the same meeting.

## Invite and Guest Join

- TODO (invite-code collision): The current 10-character invite code uses a
  large random space and the database unique constraint remains the final
  safeguard. Before collision probability becomes operationally relevant,
  define a bounded retry policy for meeting creation when that constraint is
  violated, so a rare collision does not fail the user-visible creation flow.
- INV-01 invite entry is currently implemented through invite-code lookup. It
  returns the current participation availability status for the entry screen.
- If both the deadline and participant limit block joining, the deadline-passed
  status takes priority in the entry response.
- Web invite-link entry supports both logged-in member join and guest join.
- App invite-link entry supports logged-in member join only; guest join is not
  exposed in the app entry flow.
- The client decides which entry options to expose by platform. The server keeps
  separate member and guest join APIs.
- INV-01 member join creates a participant row for the current authenticated
  service user from `@CurrentMember` and saves the required participation
  details in the same request and transaction.
- Member join accepts a meeting-scoped nickname; the nickname may differ from the
  user's default nickname. The authenticated member is identified by the Bearer
  Access Token and does not submit a participant password.
- A service user can participate in the same meeting only once. The host
  participant row also counts as that user's meeting participation.
- INV-01 guest join creates the participant row and saves the required
  participation details in the same request and transaction.
- Member and guest join requests include a nested `scheduleResponse` for
  `SCHEDULE_ONLY` and `SCHEDULE_AND_PLACE`. It contains `availableDates` for
  `DATE_ONLY` or `availableTimeRanges` for `DATE_AND_TIME`, never both. They also
  include the departure snapshot and transportation mode for `PLACE_ONLY` and
  `SCHEDULE_AND_PLACE`.
- Guest join stores the participant password as a hash on the
  `meeting_participants` row. Guest password verification for later re-entry or
  modification remains deferred until its policy is confirmed.
- Guest join does not issue an Access JWT or a guest JWT. Authentication for a
  later guest re-entry or modification flow remains deferred until that flow's
  policy is confirmed.
- A repeated guest join attempt with the same nickname as an existing guest in
  the same meeting should continue to return a duplicate nickname conflict, even if
  the same password is provided.
- Member and guest participation is rejected after a non-null meeting
  `deadlineAt`. A null `deadlineAt` has no deadline-based participation block.
- Member and guest participation checks the current participant count before
  saving.
- To prevent concurrent joins from exceeding `maxParticipants`, participant
  participation may acquire a pessimistic write lock on the target meeting row
  during the join transaction.
- Keep this lock limited to the meeting join path; ordinary invite-code lookup
  should remain read-only.

## Participation Input

- The first meeting participation expansion covers the 2026-07-06 P0 INV-02 data:
  schedule availability for schedule-coordination meetings and participant
  departure snapshots for place-coordination meetings. These details are submitted
  together with INV-01 member or guest join. The host submits the corresponding
  inputs as part of meeting creation.
- Join requests save schedule availability for `SCHEDULE_ONLY` and
  `SCHEDULE_AND_PLACE` meetings.
- `DATE_ONLY` participation stores one selection per available host candidate date.
  `DATE_AND_TIME` participation stores selected availability ranges within the
  host's candidate dates and common available time range. Each range start and end
  time must be in 1-hour units, and multiple ranges may be saved.
- A join request creates the participant and their initial availability slots
  atomically.
- Join requests save departure and transportation mode for
  `PLACE_ONLY` and `SCHEDULE_AND_PLACE` meetings.
- Place participation stores the participant departure name, address, latitude, longitude, and transportation mode snapshot on `meeting_participants`. Departure `name` is optional; when omitted, the place-view response uses the saved departure address as its display name. A client using departure-place search sends the selected candidate's WGS84 coordinate pair. A legacy request may omit both coordinates; one without the other is invalid.
- The current MVP accepts place-coordination departure addresses only in Seoul or Gyeonggi. The server validates the normalized address prefix (`서울`, `서울특별시`, `경기`, or `경기도`) when creating or saving a host/member/guest participation snapshot.
- Join rejects mismatched input, such as departure input for schedule-only
  meetings or schedule availability input for place-only meetings.

## Pre-confirmation Meeting View

- VIEW-01 meeting status, schedule status, and place status are read-only
  pre-confirmation views.
- VIEW-01 APIs may be opened from an invite link without login. Authentication
  is not required for the current read-only status APIs.
- VIEW-01 status values are calculated at read time from the current meeting,
  participant, schedule availability, and departure snapshot rows.
- Meeting creation and participant join save every input required by the meeting
  mode in the same transaction. A participant row is not retained when required
  schedule or departure input is invalid, so status views do not expose separate
  response-progress counts, rates, or per-participant completion flags.
- For `DATE_ONLY`, VIEW-01-A aggregates availability by date and returns null
  `startTime`/`endTime`. For `DATE_AND_TIME`, it expands saved ranges into 1-hour
  units before calculating availability, then merges consecutive units with the
  same available participant set. It returns every merged availability block with
  its available-participant count so the client can calculate the response-rate
  color from that count and the meeting participant count.
- VIEW-01-A first finds the maximum simultaneous available-participant count. If
  that maximum is below two, it returns no best-schedule candidates. Otherwise it
  returns up to five candidates having that maximum: `LONGEST_MEETING` sorts by
  duration descending then date/start time ascending, and `EARLIEST_DATE` sorts by
  date/start time ascending. The default sort is `EARLIEST_DATE`.
- Each best-schedule candidate includes the meeting-scoped participant IDs and
  nicknames that are available for that candidate. VIEW-01 remains invite-link
  readable without login, so this candidate-specific availability list is also
  visible to an invite-link visitor.
- VIEW-01-A accepts only `LONGEST_MEETING` and `EARLIEST_DATE` as the schedule
  sorting value. Unsupported values return the common invalid-request error.
- VIEW-01-B place recommendations before final confirmation do not call
  external travel-time APIs.
- Before final confirmation, middle-point place recommendation is a preview
  based on saved participant departure snapshots. For each commercial area,
  multiply straight-line distance by the participant transportation weight
  (`CAR` 1.0, `PUBLIC_TRANSIT` 0.9), then sort by the lowest
  weighted-distance average plus weighted-distance maximum. Return up to three
  preliminary candidates.
- Pre-confirmation commercial-area candidates use the persistent
  `commercial_areas` table. The initial recommendation dataset is the 255 Seoul
  areas from the confirmed source selection: 249 development areas and six
  tourist-special areas. Each source record is imported once with its WGS84
  center coordinate; a later regional source uses the same table with its own
  source identifier and source-owned area code.
- The `RANDOM` place recommendation strategy shuffles the persistent
  commercial-area catalog for each view request before selecting up to five
  candidates.
- Actual travel-time based reranking and final place result storage should be
  handled through the host-only actual-time calculation endpoint, not on every
  pre-confirmation status view request. It calls Kakao only for the preliminary
  candidates (default three), requires every current participant departure and
  transportation mode, and returns the default top three by the lowest actual
  travel-time average plus maximum. A Kakao failure fails the entire calculation;
  it never mixes partial results. Calculation results are not persisted and are
  distinct from the later host final-confirmation and history flow.
- Confirmed follow-up policy: when the active participant count reaches the
  meeting capacity, automatically recalculate the actual-travel-time
  recommendations from the completed participant departures. The current MVP
  implementation remains the host's manual calculation endpoint until this
  automatic trigger is implemented.
- Confirmed policy: preliminary place recommendations remain visible and are
  recalculated from the current departure snapshots while the meeting is open.
  Actual-travel-time calculation may run only after every active participant
  has completed the required departure input; the current manual endpoint
  already enforces this condition.
- `POLICY_UNDEFINED`: when the deadline arrives before the meeting reaches its
  capacity, show a deadline-extension popup. Define the extension authority,
  selectable duration, decline/close behavior, and the resulting
  actual-travel-time calculation trigger before implementing that flow.

## Departure Place Search

- Departure input uses server-side integrated search APIs backed by Kakao Local.
  The Kakao REST API key remains server-side.
- Logged-in members and web guests use the same search endpoint. A valid Access
  JWT authorizes member access. Only when the `Authorization` header is absent,
  a valid invite code authorizes guest access; the server validates that the
  invite code belongs to an existing meeting before calling Kakao Local. A
  present invalid token never falls back to invite-code access, and a valid
  token takes precedence when both credentials are supplied.
- The integrated search API accepts a single keyword and returns unified
  `STATION`, `ADDRESS`, or `PLACE` candidates with a display name, representative
  address, road-name address, and lot-number address. It does not expose provider
  response shapes or provider-specific result IDs.
- The candidate `displayName` is only for the search list. The client may send
  it as the final departure snapshot `name`, subject to its 30-character limit,
  but `name` is optional. When it is omitted, the place-view response uses the
  saved departure address as the display name.
- A keyword ending exactly in `역` first uses Kakao keyword search with the
  `SW8` subway-station category. Keep only results whose place name is the
  requested station name or starts with it followed by whitespace or an opening
  parenthesis. If that filtered list is empty, use unfiltered Kakao keyword
  search as a fallback.
- A conservative road-name or lot-number address pattern first uses Kakao address
  search with similar matching. Strong lot-number input recognizes `동`, `리`, or
  numbered `가` followed by a lot number, with optional `산`, sub-number, and
  `번지`. Region-only terms, road names without a building number, underground
  road addresses, and incomplete `읍` or `면` input use Kakao keyword search.
  Address search keeps only complete `REGION_ADDR` or `ROAD_ADDR` documents;
  partial `REGION` or `ROAD` documents do not count as final candidates. If a
  successful address search returns no complete candidates, use Kakao keyword
  search as a fallback. All other keywords use Kakao keyword search directly.
- A fallback is allowed only after a successful search with no final candidates.
  Provider configuration, authorization, quota, network, or response failures
  return `DEPARTURE_PLACE_SEARCH_UNAVAILABLE` and do not trigger a fallback.
- Search candidates include the provider document's top-level WGS84 `y` and `x`
  values as `latitude` and `longitude`. The client passes the selected pair into
  the existing departure snapshot fields and must not geocode the address again
  when saving. The existing coordinate-pair validation remains unchanged.
- Persist a search only after the provider search completes successfully. Do not
  create search-history rows for request validation, authentication, invitation
  validation, provider configuration, authorization, quota, network, or response
  failures.
- A persisted search stores the normalized keyword sent to Kakao Local, the
  provider, the actual primary/fallback execution path, and only the final unified
  candidates returned to the client. Do not retain discarded primary documents,
  raw provider response JSON, provider credentials, request URIs, or
  provider-specific result IDs.
- A successful final search with no candidates stores the search execution with
  an empty candidate collection. Candidate positions preserve the client response
  order starting at 1.
- Search-history persistence is supplementary to the user-facing search. If the
  provider search succeeds but history persistence fails, return the successful
  search response and write an internal error log without exposing persistence
  details to the client.
- Member searches are linked to the authenticated service user. Invite-code guest
  searches are linked to the validated meeting because a guest participant may
  not exist yet; do not store the invite code as search history.
- TODO: Confirm the search-history retention/deletion period and Kakao Local data
  retention requirements before retaining real-user search history beyond MVP
  development.

## Saved Member Places

- An authenticated member may save a selected departure-place search candidate
  as a personal place. Guests cannot use member saved-place APIs.
- A saved place is a durable member-owned snapshot that is independent from
  supplementary search history. It stores the member-entered alias and the
  selected candidate's type, display name, addresses, and WGS84 coordinates.
- The alias is required, contains 1 to 30 characters after trimming, and is the
  only mutable place field. To replace the selected location snapshot, delete
  the saved place and create a new one from a search result.
- Saved places are returned newest first, with the ID as the stable tie-breaker.
- Duplicate places are allowed. Do not add a uniqueness constraint for member,
  address, coordinates, display name, or alias.
- The current saved-place count is unlimited. A later confirmed limit must be
  enforced in the application service and API error contract without requiring
  a saved-place schema redesign.
- Members may create, list, rename, and delete only their own saved places.
  Access to another member's saved-place ID is returned as not found.

## Deferred Policies

## Final Confirmation and Home (2026-07-28)

- A meeting begins in `PLANNING`. Only the host may independently confirm the
  schedule and place, and each confirmed selection is immutable. Confirmation
  requires at least two active participants.
- Confirmation can occur before or after `deadlineAt`. Preserve `deadlineAt` as
  the original input cutoff; `CONFIRMED` separately blocks new participant and
  participation-input writes.
- `SCHEDULE_AND_PLACE` becomes `CONFIRMED` only after both selections are
  confirmed. `SCHEDULE_ONLY` and `PLACE_ONLY` become `CONFIRMED` after their
  applicable selection. `DATE_ONLY` stores a candidate date only, while
  `DATE_AND_TIME` stores a candidate date plus start/end time. The host may
  choose a candidate that is unavailable to some participants.
- A place selection must be from the current preliminary recommendation or its
  actual-route reranking. Store commercial-area name and coordinates; address is
  nullable because the catalog has no road address.
- Home lists account-linked host/member meetings only. Planning order is closed
  deadline, nearest open deadline, then no deadline. Confirmed order is upcoming
  schedule then past schedules. Place-only hides schedule and schedule-only hides
  place.
- Each home card exposes the host nickname and the current user's `HOST` or
  `MEMBER` role. User profile images are deferred because the current User model
  has no profile-image policy or field.
- An authenticated host or member may retrieve a home-card meeting by
  `meetingId`. The detail response includes the meeting description, confirmed
  schedule/place fields, and its participant list; exactly the linked current
  user is marked with `isMe = true`. Non-participants receive
  `MEETING_NOT_FOUND` so the endpoint does not reveal another meeting.
- TODO: After the MVP creation flow is stable, decide whether to remove the
  remaining fixed schedule/place fields and enum values or reintroduce a fixed
  direct-input flow.
- TODO: Host departure modification after initial meeting creation remains
  deferred until the modification policy is confirmed.
- GPS/current-location lookup remains P1 or later client/domain work.
- Guest re-entry remains deferred until its policy is confirmed.
- Guest modification remains deferred until its policy is confirmed.
- Participant password verification for re-entry or modification remains
  deferred until its policy is confirmed.
- Member invitation beyond direct invite-link join remains deferred until its
  policy is confirmed.
- Group invitation remains deferred until its policy is confirmed.
- Final schedule decision APIs remain separate follow-up work.
- Actual travel-time based middle-point calculation remains deferred.
- Persistent commercial-area data import, external travel-time API integration,
  and final place confirmation policy remain deferred.
