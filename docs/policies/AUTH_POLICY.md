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
- Keep the exact Apple redirect URI in server environment configuration. Do not
  accept it from the API request.
- Treat an invalid, expired, or already-used authorization code as
  `401 SOCIAL_LOGIN_FAILED`.
- Treat an Apple timeout or service failure as
  `503 SOCIAL_LOGIN_UNAVAILABLE`.
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
- Expose the `X-Trace-Id` response header so browser clients can correlate an
  API response with server logs.
- If `CORS_ALLOWED_ORIGINS` exists in the EC2 runtime `.env`, it overrides the
  default origins in `application-dev.yml`.
