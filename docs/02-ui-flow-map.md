# UI Flow Map

> Source: product-design images supplied in the Codex conversation on 2026-08-04.
> The source images are temporary attachments, so this document records the
> durable screen IDs, flow intent, and backend routing rather than linking to
> their temporary local paths.

## Purpose

Use this document when checking whether a product-design screen can be
implemented by the current Moyeo backend. It is a navigation aid, not a new
product-policy source. Canonical behavior remains in the policy documents
listed in [Routing](#routing).

## Flow overview

```text
Social entry / invite link
  -> social login when needed
  -> nickname onboarding when needed
  -> meeting creation or invite participation
  -> planning view
  -> host confirmation
  -> confirmed meeting / My Page
```

## Screen inventory and routing

| Design area | Screen IDs represented in supplied designs | Intent | Current backend route / response used |
| --- | --- | --- | --- |
| Onboarding and account | `ONB-01`, `ACC-01` | Kakao/Apple entry and initial default nickname | `POST /api/auth/kakao`, `POST /api/auth/apple`, `PUT /api/users/me/onboarding`, `GET /api/auth/me` |
| Meeting creation | `CRT-01` to `CRT-06` | Select planning type, basic data, date/time input, answer deadline, departure, optional cover, and success | Final-only `POST /api/meetings` (JSON) or multipart `POST /api/meetings` with optional cover |
| Sharing after creation | `CRT-07` | Share the returned invite code/link | Creation response `meetingId`, `inviteCode`; client-native sharing has no server route |
| Invite entry and authentication | `INV-01`, `ACC-01` | Read invitation, choose member or web-guest path, social-login return, meeting-scoped nickname/password entry | `GET /api/meetings/invitations/{inviteCode}`, social routes above, `POST /api/meetings/invitations/{inviteCode}/guests/entry` |
| Invite participation input | `INV-02`, `INV-02-A`, `INV-03`, `INV-04` | Submit date/date-time availability and/or departure snapshot, then see join completion | `POST /api/meetings/invitations/{inviteCode}/members` or `/guests` |
| Planning and confirmation | `HOME-01` meeting-planning variants | Read participant status, schedules and places; the host selects the final schedule/place | `GET /api/meetings/invitations/{inviteCode}/view`, `/view/schedules`, `/view/places`, then `POST /api/meetings/{meetingId}/schedule-confirmation` and `/place-confirmation` |
| My Page and member settings | `HOME-01-A`, `HOME-01-AA` variants | Default nickname, profile color, saved departure places, account withdrawal | `GET /api/users/me`, `PATCH /api/users/me/nickname`, `PATCH /api/users/me/profile-color`, `/api/me/places`, `DELETE /api/users/me` |

## Conditional routing rules

### Social login

After either social-login response, store the returned Access JWT and inspect
`user.onboardingCompleted`.

- `false`: route to `ONB-01` nickname registration and call
  `PUT /api/users/me/onboarding`.
- `true`: route to the requested protected destination or the My Meeting home.

There is no general ID/password signup or login route.

### Meeting creation

The client keeps values from `CRT-01` through `CRT-06` locally and submits one
final request. The current backend has no draft or per-step creation routes.

- `SCHEDULE_ONLY`: date candidates plus `DATE_ONLY` or `DATE_AND_TIME` input.
- `PLACE_ONLY`: departure and transportation mode; no schedule input.
- `SCHEDULE_AND_PLACE`: both conditional input sets.
- A cover is optional; use multipart only when a file is selected.

### Invite entry

Invite lookup is public. A valid optional Access JWT changes only the returned
participation status.

- Member path: an onboarded signed-in user submits a meeting-scoped nickname
  and required participation data to `/members`.
- Guest path: web only. Enter nickname plus a four-digit password, call the
  guest-entry branch route, then submit required participation data to
  `/guests` when the branch result is `NEW_GUEST`.
- Do not expose the web guest path in the native app entry flow.

### Planning and confirmation

Participants may inspect planning data through the invite view. The host alone
confirms the final schedule and/or place. A meeting becomes final only when all
needed dimensions for its planning type have been confirmed.

## Design-to-policy checkpoints

- Default member nicknames may duplicate outside a meeting. Guest nickname
  duplication is checked only among guests in the same meeting.
- A deadline may be absent. Otherwise it is submitted as 10-minute units from
  10 minutes to 7 days 23 hours; the server calculates the authoritative deadline time.
- Date-and-time selections and the meeting-wide time range use one-hour units.
- Place-coordination departure input requires address, latitude/longitude as a
  pair, and transportation mode. Current MVP address validation accepts Seoul
  or Gyeonggi only.
- The source designs show mobile layouts. `로그인 없이 참여하기` must be treated
  as a web-mobile screen, not an app entry screen, under the current policy.

## Routing

- Project lifecycle, scope, runtime, and deployment: `docs/00-project-setup.md`
- API/error envelope and client error handling: `docs/policies/API_POLICY.md`
- Social login and nickname onboarding: `docs/policies/AUTH_POLICY.md`
- Meeting creation, invite, participation, and confirmation: `docs/policies/MEETING_PARTICIPATION_POLICY.md`
- Current persistence contract: `docs/01-dbdiagram.md`
- Backend endpoint annotations and examples: `src/main/java/com/moyeo/controller/`

When a screen suggests behavior not covered by the routes and policies above,
record it as `POLICY_UNDEFINED` before implementing it.
