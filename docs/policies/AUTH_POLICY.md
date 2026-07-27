# Authentication Policy

> Purpose: Canonical policy for Moyeo service identity, login identities, JWT,
> CORS, and authentication-related security boundaries.

## Identity Model

AUTH-001: Production authentication is social-login only. Service identity and
social provider identity remain separated through `User` and `SocialAccount`.

- Keep `User` as the service user identity.
- Do not provide a general login ID/password signup or login API.
- Keep social provider identities in `SocialAccount` using
  `provider + providerUserId`.
- `providerUserId` is the provider-issued user identifier, not CI/DI.
- Never merge users automatically by email. Different providers create separate
  users unless an already authenticated user explicitly links another provider
  through a future account-linking feature.
- Do not store CI/DI unless a separate human decision, consent policy, and
  security policy are documented.

## Social Login

AUTH-003: The frontend receives the provider callback and sends the one-time
authorization code to the backend. The backend exchanges and verifies the code,
then issues a Moyeo Access JWT.

- Apple login uses `POST /api/auth/apple` with `{ "code": "...", "nonce": "..." }`.
- The frontend Apple callback uses GET and does not request Apple name or email
  scopes.
- Identify Apple users only by Apple's verified `sub` claim.
- The backend must verify the Apple identity token signature, issuer, audience,
  expiration, subject, and nonce.
- Keep the exact Apple redirect URI set in server environment configuration.
  The API may accept only the fixed `redirectTarget` values `dev` or `prod` and
  the server maps that value to a registered URI. Do not accept a redirect URI
  string from the API request. Apple `local` is unsupported.
- Treat an invalid, expired, or already-used authorization code as
  `401 SOCIAL_LOGIN_FAILED`.
- Treat an Apple timeout, service failure, or provider response indicating
  invalid server credentials or configuration as
  `503 SOCIAL_LOGIN_UNAVAILABLE`.
- Require an Apple refresh token in every successful authorization-code
  exchange. Encrypt it with the server-only AES-256-GCM key before persistence,
  bind the ciphertext to the verified Apple `sub`, and replace the stored
  ciphertext on each successful Apple login.
- Never persist or log an Apple provider token in plaintext. Keep
  `APPLE_REFRESH_TOKEN_ENCRYPTION_KEY_BASE64` separate from the database and
  other Apple credentials.
- Kakao login uses `POST /api/auth/kakao` with `{ "code": "..." }`.
- The frontend must generate a unique `state` for each Kakao login request and
  verify that the callback returns the same value before sending the code to the
  backend.
- Keep Kakao OpenID Connect disabled for the current flow. The backend exchanges
  the code using the server-configured REST API key, client secret, and exact
  redirect URI, then identifies the user only by the Kakao user-information
  response `id`. The API may accept only the fixed `redirectTarget` values
  `local`, `dev`, or `prod`; the server maps the value to its registered URI and
  never accepts a redirect URI string from the API request.
- Do not request or store Kakao profile, email, CI, phone number, or other
  additional consent information for the current login flow.
- Treat an invalid, expired, or already-used Kakao authorization code, or an
  invalid Kakao access token response, as `401 SOCIAL_LOGIN_FAILED`.
- Treat a Kakao timeout, service failure, malformed provider response, or
  invalid server credentials/configuration as
  `503 SOCIAL_LOGIN_UNAVAILABLE`.
- Do not store Kakao access or refresh tokens after the login request completes.
- TODO(POLICY_UNDEFINED): Decide how provider-initiated Kakao unlink events are
  handled before public launch. Moyeo-initiated account withdrawal remains
  governed by AUTH-005.
- Do not expose provider error bodies, tokens, keys, or internal verification
  details to clients.

## Nickname Onboarding

AUTH-004: A verified social identity is registered immediately, before nickname
entry.

- On the first successful social login, create `User` and `SocialAccount`
  immediately with `users.nickname = null`, and issue an Access JWT.
- `nickname != null` is the single source of truth for onboarding completion.
  Do not add a separate `is_onboarded` column.
- Authentication responses return both nullable `nickname` and derived
  `onboardingCompleted`.
- A user whose onboarding is incomplete may call only `GET /api/auth/me` and
  the first-nickname registration API among member-authenticated APIs.
- Other member-authenticated APIs return `403 ONBOARDING_REQUIRED` until the
  nickname is registered. Guest and invite-code flows remain unchanged.
- `PUT /api/users/me/onboarding` registers the initial nickname.
- Repeating that request with the same nickname is idempotent and returns
  success. A different nickname after completion returns
  `409 ONBOARDING_ALREADY_COMPLETED`.
- Do not delete a user merely because nickname onboarding was abandoned.
  A later social login resumes the same user.
- Nickname editing is a separate future feature and is not provided by the
  onboarding API.

## Access JWT

- Use an Access JWT for successful social login and protected API
  authentication.
- Validate Access JWT format, signature, required headers and claims, expiration,
  and required JWT configuration at startup.
- Keep the current JWT implementation minimal: no refresh token, logout, or
  token rotation.
- Guest meeting participation does not issue an Access JWT or a guest JWT.
- Store real JWT secrets through environment variables in dev/prod.
- The unified departure place search API accepts either a valid Access JWT or,
  only when the `Authorization` header is absent, a valid meeting invite code.
- A present but invalid `Authorization` header returns
  `AUTHENTICATION_REQUIRED` and must not fall back to invite-code access.
- When a valid Access JWT and invite code are both present, member authentication
  takes precedence and the invite code is ignored.
- Invite-code access rejects an unknown invite code before calling the external
  search provider. It does not create or authenticate a guest participant.
- Member saved-place create, list, rename, and delete APIs require a valid Access
  JWT and do not accept meeting invite-code access.

## Account Withdrawal

AUTH-005: An authenticated service user may withdraw through
`DELETE /api/users/me`, including while nickname onboarding is incomplete.

- The withdrawal request requires only the current Moyeo Access JWT and has no
  request body. The backend derives the provider and provider identity from the
  stored `SocialAccount`; it does not trust client-supplied provider data.
- Apply and flush all rollbackable local withdrawal changes, disconnect the
  stored provider authorization immediately before committing, and roll back
  the local transaction if provider disconnection fails.
- For Apple, decrypt the stored refresh token using the server-only encryption
  key and verified stored `providerUserId` as authenticated context, then revoke
  that refresh token through Apple's revoke endpoint. Do not require a new Apple
  authorization code or nonce at withdrawal time.
- If an existing Apple account has no stored encrypted refresh token, return
  `503 SOCIAL_LOGIN_UNAVAILABLE` and leave the local account unchanged. Its next
  successful Apple login stores a fresh encrypted refresh token, after which
  immediate withdrawal is available.
- For Kakao, call the Unlink API with the server-owned Admin Key and the stored
  Kakao service user ID as `target_id`. Require the success response ID to match
  that stored ID. Do not require a new Kakao authorization code or persist a
  Kakao provider token.
- A provider timeout, service/configuration failure, or unsuccessful revoke or
  unlink returns `503 SOCIAL_LOGIN_UNAVAILABLE` and leaves all local account
  data unchanged. A provider response that confirms the authorization was
  already disconnected is treated as successful.
- Only the two fixed local/dev test users may withdraw without a
  `SocialAccount` and provider disconnection. An active user without exactly
  one `SocialAccount` in any other case is an internal data-integrity failure
  and returns `500 COMMON_INTERNAL_SERVER_ERROR` without completing withdrawal.
- Withdrawal returns `204 No Content` without a response body.
- Return `204 No Content` only after provider disconnection and the local
  withdrawal transaction both complete.
- Mark the service user as withdrawn with `users.deleted_at` and clear the
  service-level nickname. Keep the `User` row only because participation records
  in meetings hosted by other users must retain a stable withdrawn-user
  reference.
- Remove the user's social-account links, saved places, and member departure
  place search history. Removing the social-account link allows a later social
  login with the same provider identity to register a new service user.
- Serialize withdrawal with meeting creation, member meeting join, saved-place
  create/rename/delete, and member search-history writes by locking the active
  `User` row first. When one of these flows also requires another row lock, keep
  the order `User` first, followed by the affected resource, to avoid opposite
  lock ordering.
- Every authenticated request resolves the JWT subject against an active
  `User`. After withdrawal commits, previously issued Access JWTs for the old
  user return `401 AUTHENTICATION_REQUIRED`; a refresh token, token blacklist,
  or Redis is not required for the current implementation.
- Delete every meeting hosted by the withdrawing user, including its
  participants, schedule candidates and availabilities, meeting-linked departure
  search history, and stored cover image.
- Store a durable cover-cleanup task in the withdrawal transaction before
  deleting a hosted meeting. Attempt S3 deletion immediately after commit and
  retain failed tasks for scheduled retry so a transient failure or process
  restart does not lose the cleanup target.
- Keep the user's participant row and submitted participation snapshots in
  meetings hosted by other users. Participant responses expose whether the
  linked service user has withdrawn.

## Development Test Accounts

AUTH-002: The `local` and `dev` profiles may seed a fixed, idempotent pair of
direct `User` records to support frontend development. They are not general
login accounts, do not have passwords, and this initialization must not run in
the `prod` profile.

- The test-account token endpoint is available only when the `local` or `dev`
  profile is active.
- It may issue Access JWTs without a password only for the two fixed test
  accounts, and returns both accounts in one response.
- The endpoint is a temporary development convenience and must not be used as
  a production authentication mechanism.

## CORS

- Configure CORS with explicit frontend origins and update them when frontend
  deployment URLs are decided.
- The dev profile also allows Vercel PR Preview origins matching
  `https://moyeo-*-hyeonjirohs-projects.vercel.app`.
- Expose the `X-Trace-Id` response header so browser clients can correlate an
  API response with server logs.
- If `CORS_ALLOWED_ORIGINS` exists in the runtime environment, it overrides the
  profile default origins in `application-dev.yml` or `application-prod.yml`.
