# Typicode-Backend — API Documentation

**Project:** `herbert.task.app:Typicode-Backend:1.0` (WAR)
**Stack:** Jakarta EE 10 (JAX-RS, JPA, EJB, Bean Validation), MicroProfile Config 3.1, `com.auth0:java-jwt:4.4.0`, Jakarta Mail (SMTP)
**Live base URL:** `https://observe-country-enhanced-conceptual.trycloudflare.com/Typicode-Backend/api/v1`
> `JakartaRestConfiguration.java:11` sets `@ApplicationPath("api/v1")`, so append `/api/v1` to the deployed WAR context `.../Typicode-Backend/`.

**Content negotiation (all resources):**
- `Produces: application/json, application/xml`
- `Consumes: application/json, application/xml`
- CORS: `*` origin, `GET, POST, PUT, DELETE, OPTIONS, HEAD`, headers `Content-Type, Authorization, X-Requested-With` — see `filter/CORSFilter.java`.

**Auth model:** passwordless OTP via email + short-lived JWT access token + long-lived opaque refresh token (UUID stored in `REFRESH_TOKEN` table). Protected endpoints use `@Secured` + `Authorization: Bearer <jwt>` header enforced by `filter/JWTFilter.java`.

**Conventions used below:**
- `{{BASE}}` = `https://observe-country-enhanced-conceptual.trycloudflare.com/Typicode-Backend/api/v1` (live Cloudflare tunnel; local equivalent is `http://localhost:8080/Typicode-Backend-1.0/api/v1`).
- Success/error entities are either a DTO (`MessageDTO` / `JWTDto` / `TimerDto`) serialized as JSON (or XML), or a bare JSON string (`"..."`) when the resource returns `entity("...")`. Samples are shown as JSON.
- `password` (OTP) is a JSON **number**, not a string.

**Live endpoints (expanded):**
- `POST https://observe-country-enhanced-conceptual.trycloudflare.com/Typicode-Backend/api/v1/user/signin`
- `POST https://observe-country-enhanced-conceptual.trycloudflare.com/Typicode-Backend/api/v1/user/confirmOTP`
- `POST https://observe-country-enhanced-conceptual.trycloudflare.com/Typicode-Backend/api/v1/user/logout` (Secured)
- `GET https://observe-country-enhanced-conceptual.trycloudflare.com/Typicode-Backend/api/v1/user/test-secure` (Secured)
- `GET https://observe-country-enhanced-conceptual.trycloudflare.com/Typicode-Backend/api/v1/user/me` (Secured)
- `PUT https://observe-country-enhanced-conceptual.trycloudflare.com/Typicode-Backend/api/v1/user/profile` (Secured)
- `POST https://observe-country-enhanced-conceptual.trycloudflare.com/Typicode-Backend/api/v1/user/refresh`
- `POST https://observe-country-enhanced-conceptual.trycloudflare.com/Typicode-Backend/api/v1/user/refresh-timer`
- `POST https://observe-country-enhanced-conceptual.trycloudflare.com/Typicode-Backend/api/v1/blog/create` (Secured)
- `GET https://observe-country-enhanced-conceptual.trycloudflare.com/Typicode-Backend/api/v1/blog/all?type=&search=&page=&size=`
- `GET https://observe-country-enhanced-conceptual.trycloudflare.com/Typicode-Backend/api/v1/blog/my-blogs` (Secured)
- `GET https://observe-country-enhanced-conceptual.trycloudflare.com/Typicode-Backend/api/v1/blog/{id}`
- `PUT https://observe-country-enhanced-conceptual.trycloudflare.com/Typicode-Backend/api/v1/blog/{id}` (Secured, author only)
- `DELETE https://observe-country-enhanced-conceptual.trycloudflare.com/Typicode-Backend/api/v1/blog/{id}` (Secured, author or ADMIN)

---

## 1. Authentication Flow

```
1. POST /user/signin {email}          -> creates/rotates 6-digit OTP, emails it (5 min TTL)
2. POST /user/confirmOTP {email, password} -> verifies OTP, marks emailVerified=true,
                                              returns {token: "Bearer <jwt>", refreshToken: "<uuid>"}
3. Client calls secured APIs with header: Authorization: Bearer <jwt>
4. POST /user/refresh {refreshToken}  -> rotates refresh token, returns new access+refresh tokens
5. POST /user/logout (Secured)         -> revokes refresh token
6. POST /user/refresh-timer {refreshToken} -> returns refresh token expiry for UI countdown
```

OTP rules (`models/OTPModel.java`, `utils/OTPGenerator.java`):
- 6-digit: `100000 + SecureRandom.nextInt(1, 900000)` (100000–999999).
- Unique across table (checked via `PersistenceService.OTPExists()` loop).
- `expiryTime = now + 5 minutes`, `retries = 0`, `maxRetries = 3` (default).
- Wrong OTP increments `retries`; at `retries >= maxRetries` the OTP row is deleted (orphan removal via `user.setOtp(null)`) and client must re-call `/signin`.

JWT rules (`utils/JWTUtil.java`):
- `Algorithm.HMAC256(jwt.secret.key)`, `issuer = jwt.secret.issuer`.
- Access TTL: `jwt.secret.access.ttl` (minutes) → `Duration.ofMinutes(ttl)`.
- Claims: `sub=user.id`, `email`, `reference`, `avatar`, `role`, `iss`, `iat`, `exp`.
- Refresh TTL: `jwt.secret.refresh.ttl` (days) → `OffsetDateTime.now().plusDays(TTL)`. Stored as UUID in `RefreshToken.tokenHash`.

---

## 2. Endpoints

### 2.1 `POST /user/signin` — Request OTP (signup + signin unified)

**Resource:** `resources/SignResource.java:60-124`
**Auth:** public. **Validation:** `@Valid EmailDto`.

Handles 3 cases by email lookup (`PersistenceService.CheckUser`):

| Case | Condition | Action | HTTP |
|------|-----------|--------|------|
| New user | no row | create `UserModel(role=ENDUSER)`, create `OTPModel`, `persist`, send email `Test Signup` | `201 Created` |
| Unverified existing | `emailVerified==false` | delete old OTP, create new OTP, `merge`, send email `Test Signup` | `202 Accepted` |
| Verified existing | `emailVerified==true` | delete old OTP, create new OTP, `merge`, send email `Test Sign In` | `200 OK` |

Additional:
- `409 Conflict` if `email` lacks `@`. Note: only a `contains("@")` check.
- Email is normalized: `trim().toLowerCase()` on create.
- OTP is emailed as plain text/HTML body: `Welcome ... <otp> This link is valid for 5 mins` via `EmailUtil.sendSmtpEmail()` (SMTP SSL).

#### Sample request

```http
POST {{BASE}}/user/signin HTTP/1.1
Content-Type: application/json

{ "email": "user@example.com" }
```

```bash
curl -X POST "{{BASE}}/user/signin" \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com"}'
```

#### Sample responses

New user — `201 Created`:

```json
{ "message": "User Created", "status": null }
```

Existing but unverified — `202 Accepted`:

```json
{ "message": "User trying to verify. Success", "status": null }
```

Existing verified sign-in — `200 OK`:

```json
{ "message": "User signing in", "status": null }
```

Invalid email — `409 Conflict`:

```json
{ "message": "This is not a valid email address! Try again", "status": null }
```

> All responses are `MessageDTO`. `status` is always `null` on this endpoint (only `logout` sets it).

---

### 2.2 `POST /user/confirmOTP` — Verify OTP, issue tokens

**Resource:** `SignResource.java:128-215`
**Auth:** public. **Body:** `@Valid OTPDto`.

Logic:
1. Lookup user by email → `404` if missing.
2. If `user.otp == null` → `404`.
3. If `password != stored OTP` → increment `retries`, `merge`; if `retries >= maxRetries` delete OTP, else keep.
4. If `otp.expiryTime < now` → `404` (OTP row kept; client must re-signin to rotate).
5. Success: `user.otp=null`, `emailVerified=true`, `merge`. Build `UserDTO` claims, `generateLoginToken()`, prefix with `"Bearer "`. Create or rotate `RefreshToken` (`@PrePersist` generates UUID + expiry + `revoked=false` on first create; explicit rotation on subsequent logins).

#### Sample request

```http
POST {{BASE}}/user/confirmOTP HTTP/1.1
Content-Type: application/json

{ "email": "user@example.com", "password": 482913 }
```

```bash
curl -X POST "{{BASE}}/user/confirmOTP" \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":482913}'
```

#### Sample responses

Success — `200 OK` (`JWTDto`):

```json
{
  "token": "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJ0eXBpY29kZSIsInN1YiI6ImFiYzEyMyIsImVtYWlsIjoidXNlckBleGFtcGxlLmNvbSIsInJlZmVyZW5jZSI6IlVTRVIxMjM0IiwiYXZhdGFyIjpudWxsLCJyb2xlIjoiRU5EVVNFUiIsImlhdCI6MTc1OTAwMDAwMCwiZXhwIjoxNzU5MDAwNjAwfQ.AbCdEfGhIjKlMnOpQrStUvWxYz",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "message": "JWT Token for login"
}
```

> `token` already includes the `Bearer ` prefix. When calling `@Secured` endpoints, send it verbatim as `Authorization: Bearer <jwt>` (filter strips one `Bearer ` prefix before verify — do not double-prefix).

Unknown email — `404 Not Found`:

```json
{ "message": "Invalid User", "status": null }
```

No active OTP (never called `/signin`, or OTP wiped after max retries) — `404 Not Found`:

```json
{ "message": "Retry signing in/ signing up !", "status": null }
```

Wrong OTP, retries left — `404 Not Found`:

```json
{ "message": "Invalid OTP. Try again!", "status": null }
```

Wrong OTP, retries exhausted — `404 Not Found`:

```json
{ "message": "You have exceeded the max number of retries. Type in the correct email and try again! ", "status": null }
```

OTP expired (older than 5 min) — `404 Not Found`:

```json
{ "message": "OTP has Expired. Please retry!", "status": null }
```

---

### 2.3 `POST /user/logout` — Revoke refresh token

**Resource:** `SignResource.java:218-235`
**Auth:** `@Secured` — requires valid access JWT. `userId` taken from `ContainerRequestContext.getProperty("userId")` (= JWT `sub`). No request body.

- Looks up `UserModel` by JWT subject → `400` if missing.
- Sets `tokenModel.revoked=true`, merges.

#### Sample request

```http
POST {{BASE}}/user/logout HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Content-Type: application/json
```

```bash
curl -X POST "{{BASE}}/user/logout" \
  -H "Authorization: Bearer <jwt>" \
  -H "Content-Type: application/json"
```

#### Sample responses

Success — `200 OK` (`MessageDTO`):

```json
{ "message": "User logged out", "status": "200" }
```

JWT subject not in DB — `400 Bad Request`:

```json
{ "message": "User not found.", "status": null }
```

Missing/invalid/expired JWT (from `JWTFilter`, before resource runs) — `401 Unauthorized`:

```json
"Invalid Token"
```

```json
"Token is invalid or expired"
```

> Access JWT itself is stateless and remains valid until `exp`; only the refresh token is revoked. Client must discard both tokens.

---

### 2.4 `GET /user/test-secure` — Auth probe

**Resource:** `SignResource.java:238-243`
**Auth:** `@Secured`. No request body.

Use for frontend auth-guard / token health checks.

#### Sample request

```http
GET {{BASE}}/user/test-secure HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Accept: application/json
```

```bash
curl "{{BASE}}/user/test-secure" -H "Authorization: Bearer <jwt>"
```

#### Sample responses

Valid token — `200 OK` (bare string, not a DTO):

```json
"Secured!!!"
```

Missing/malformed header — `401 Unauthorized`:

```json
"Invalid Token"
```

Expired/bad signature/wrong issuer — `401 Unauthorized`:

```json
"Token is invalid or expired"
```

---

### 2.5 `POST /user/refresh` — Rotate refresh token, new access token

**Resource:** `SignResource.java:247-282`
**Auth:** public (possession of refresh token is the credential).

- Body: `JWTDto` with only `refreshToken` populated (other fields ignored on input).
- Lookup `RefreshToken` by `tokenHash`. If `null || revoked || expiresAt < now` → `401` plain-text.
- Else rotate: `dateUpdated=now`, `expiresAt=now+TTL(days)`, `tokenHash=new UUID`, `revoked=false`, `merge`.
- Rebuild `UserDTO` (`id, email, reference, avatar, role=ENDUSER`) → `generateLoginToken()`.
- **Important asymmetry:** unlike `/confirmOTP`, the returned `token` here does **NOT** include the `"Bearer "` prefix — client must add it when building the `Authorization` header.

#### Sample request

```http
POST {{BASE}}/user/refresh HTTP/1.1
Content-Type: application/json

{ "refreshToken": "550e8400-e29b-41d4-a716-446655440000" }
```

```bash
curl -X POST "{{BASE}}/user/refresh" \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"550e8400-e29b-41d4-a716-446655440000"}'
```

> Sending the full DTO shape also works; only `refreshToken` is read: `{"token": null, "refreshToken": "...", "message": null}`.

#### Sample responses

Success — `200 OK` (`JWTDto`):

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJ0eXBpY29kZSIsInN1YiI6ImFiYzEyMyIsImV4cCI6MTc1OTAwMDYwMH0.XyZ123",
  "refreshToken": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
  "message": "Login in after rotation"
}
```

Client must replace **both** stored tokens (rotation invalidates the old refresh token hash). Prefix the new access token yourself: `Authorization: Bearer <token>`.

Revoked / expired / unknown — `401 Unauthorized` (bare string, note upstream typo `canot`):

```json
"Token canot be found or it has expired or it has been revoked. Please login!"
```

---

### 2.6 `POST /user/refresh-timer` — Get refresh-token expiry

**Resource:** `SignResource.java:285-299`
**Auth:** public.

- Body: `JWTDto` with `refreshToken`.
- Returns `TimerDto { timer: OffsetDateTime }` = `RefreshToken.expiresAt`.
- `404 "RefreshToken not found"` (plain string) if lookup returns null.
- Null-check is done before dereferencing `getExpiresAt()` (fixed; previously NPE'd on unknown tokens).

#### Sample request

```http
POST {{BASE}}/user/refresh-timer HTTP/1.1
Content-Type: application/json

{ "refreshToken": "6ba7b810-9dad-11d1-80b4-00c04fd430c8" }
```

```bash
curl -X POST "{{BASE}}/user/refresh-timer" \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"6ba7b810-9dad-11d1-80b4-00c04fd430c8"}'
```

#### Sample responses

Success — `200 OK` (`TimerDto`, ISO-8601 offset date-time):

```json
{ "timer": "2026-10-04T12:00:00.123456+00:00" }
```

Use to drive silent-refresh countdowns.

Unknown token (intended) — `404 Not Found`:

```json
"RefreshToken not found"
```

---

### 2.7 `POST /blog/create` — Create blog post

**Resource:** `resources/BlogResource.java:39-77`
**Auth:** `@Secured` (access JWT required; author = JWT subject).

Steps:
1. Resolve author via `ps.findUser(userId)`; title uniqueness via `ps.findBlogTitle(title)` → `409` (plain string, global uniqueness).
2. Map `BlogDTO` → `BlogModel`: `title`, `paragraph`, `headerImage`, `description`, `blogtype=BlogType.valueOf(blogtype.toUpperCase())` (accepts `ai|mobile|web` case-insensitive; anything else throws 500).
3. For each string in `blogImages`, create `BlogImagesModel{image, blog}`; attach list; set `blog.user=author`, `author.blog.add(blog)`, `ps.updateUser(author)` (cascade persist).
4. `201` on success. `404` if author row missing.

`blogtype` enum: `AI | MOBILE | WEB` (`models/BlogModel.java:54-58`).

#### Sample request

```http
POST {{BASE}}/blog/create HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Content-Type: application/json

{
  "title": "My first post",
  "description": "Short summary shown in listings",
  "paragraph": "Full body text of the post...",
  "headerImage": "https://cdn.example.com/header.png",
  "blogImages": ["https://cdn.example.com/1.png", "https://cdn.example.com/2.png"],
  "blogtype": "WEB"
}
```

```bash
curl -X POST "{{BASE}}/blog/create" \
  -H "Authorization: Bearer <jwt>" \
  -H "Content-Type: application/json" \
  -d '{"title":"My first post","description":"Short summary shown in listings","paragraph":"Full body text of the post...","headerImage":"https://cdn.example.com/header.png","blogImages":["https://cdn.example.com/1.png","https://cdn.example.com/2.png"],"blogtype":"WEB"}'
```

Minimal valid request (no inline images):

```json
{
  "title": "AI trends 2026",
  "description": "Summary",
  "paragraph": "Body...",
  "headerImage": "https://cdn.example.com/ai.png",
  "blogImages": [],
  "blogtype": "ai"
}
```

#### Sample responses

Success — `201 Created` (bare string):

```json
"Blog Created"
```

Duplicate title — `409 Conflict`:

```json
"Title already exists !!!. Try a new title."
```

Author row missing — `404 Not Found`:

```json
"User Not found"
```

Missing/invalid JWT — `401 Unauthorized` (from filter):

```json
"Invalid Token"
```

```json
"Token is invalid or expired"
```

Invalid `blogtype` (e.g. `"SPORTS"`) — unhandled `IllegalArgumentException` → `500` (no JSON contract; fix by validating against `AI|MOBILE|WEB` first).

---

### 2.8 `GET /blog/all` — List blogs (filter, search, paginate)

**Resource:** `resources/BlogResource.java` (`getAllBlogs`)
**Auth:** public.

Query params (all optional): `type=AI|MOBILE|WEB` (case-insensitive), `search=<keyword>` (matches `title` or `description`, case-insensitive), `page=<0-based, default 0>`, `size=<default 20>`. Precedence: `search` > `type` > paged list. Ordered `dateCreated DESC`. Returns `List<BlogResponseDTO>`.

#### Sample requests

```http
GET {{BASE}}/blog/all HTTP/1.1
Accept: application/json
```

```http
GET {{BASE}}/blog/all?type=web&page=0&size=10 HTTP/1.1
Accept: application/json
```

```http
GET {{BASE}}/blog/all?search=ai+trends HTTP/1.1
Accept: application/json
```

#### Sample responses

Success — `200 OK`:

```json
[
  {
    "id": "abc123...",
    "title": "My first post",
    "description": "Short summary shown in listings",
    "paragraph": "Full body text of the post...",
    "headerImage": "https://cdn.example.com/header.png",
    "blogImages": ["https://cdn.example.com/1.png"],
    "blogtype": "WEB",
    "authorId": "def456...",
    "authorEmail": "user@example.com",
    "authorReference": "USER1234",
    "dateCreated": "2026-09-27T10:00:00",
    "dateUpdated": "2026-09-27T10:00:00"
  }
]
```

Invalid type — `400 Bad Request`:

```json
"Invalid blogtype. Use AI, MOBILE or WEB."
```

---

### 2.9 `GET /blog/{id}` — Get single blog

**Auth:** public.

#### Sample request

```http
GET {{BASE}}/blog/abc123... HTTP/1.1
Accept: application/json
```

#### Sample responses

Success — `200 OK` (`BlogResponseDTO`, same shape as list item above).

Not found — `404 Not Found`:

```json
"Blog Not found"
```

---

### 2.10 `GET /blog/my-blogs` — List my blogs

**Auth:** `@Secured` (author = JWT subject).

#### Sample request

```http
GET {{BASE}}/blog/my-blogs HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Accept: application/json
```

#### Sample responses

Success — `200 OK` (`List<BlogResponseDTO>`, same item shape).

Author row missing — `404 Not Found`:

```json
"User Not found"
```

---

### 2.11 `PUT /blog/{id}` — Update blog (author only)

**Auth:** `@Secured`. Owner check: `blog.user.id == JWT sub`, else `403`. Title change re-checks global uniqueness excluding self (`findBlogTitleExcludingId`). All `BlogDTO` fields optional; `blogImages` when present **replaces** the whole image list (orphan removal deletes old rows); when `null`, images untouched. `blogtype` parsed via `valueOf(...toUpperCase())`.

#### Sample request (partial update)

```http
PUT {{BASE}}/blog/abc123... HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Content-Type: application/json

{
  "title": "My updated post",
  "description": "New summary",
  "blogtype": "AI",
  "blogImages": ["https://cdn.example.com/new1.png"]
}
```

#### Sample responses

Success — `200 OK` (`BlogResponseDTO` with updated fields).

Not author — `403 Forbidden`:

```json
"You are not the author of this blog."
```

Duplicate title — `409 Conflict`:

```json
"Title already exists !!!. Try a new title."
```

Invalid type — `400 Bad Request`:

```json
"Invalid blogtype. Use AI, MOBILE or WEB."
```

Missing blog — `404`:

```json
"Blog Not found"
```

---

### 2.12 `DELETE /blog/{id}` — Delete blog (author or ADMIN)

**Auth:** `@Secured`. Allowed if caller is author **or** `userRole == ADMIN`.

#### Sample request

```http
DELETE {{BASE}}/blog/abc123... HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
```

#### Sample responses

Success — `200 OK`:

```json
"Blog Deleted"
```

Forbidden — `403`:

```json
"You are not allowed to delete this blog."
```

Missing — `404`:

```json
"Blog Not found"
```

---

### 2.13 `GET /user/me` — Get profile

**Resource:** `resources/SignResource.java` (`getProfile`)
**Auth:** `@Secured`.

#### Sample request

```http
GET {{BASE}}/user/me HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Accept: application/json
```

#### Sample response — `200 OK` (`UserDTO`):

```json
{
  "id": "def456...",
  "reference": "USER1234",
  "email": "user@example.com",
  "emailVerified": true,
  "role": "ENDUSER",
  "avatar": null
}
```

Not found — `404`:

```json
{ "message": "User not found.", "status": null }
```

---

### 2.14 `PUT /user/profile` — Update profile (avatar)

**Auth:** `@Secured`. Currently only `avatar` is writable; other fields ignored.

#### Sample request

```http
PUT {{BASE}}/user/profile HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Content-Type: application/json

{ "avatar": "https://cdn.example.com/avatar.png" }
```

#### Sample response — `200 OK` (`UserDTO`, same shape as `/me` with new avatar).

---

## 3. DTO Reference

| DTO | File | Fields (type) | Used by |
|-----|------|---------------|---------|
| `EmailDto` | `DTO/EmailDto.java` | `email: String` `@NotNull` | `POST /user/signin` request |
| `OTPDto` | `DTO/OTPDto.java` | `email: String` `@NotNull`, `password: Integer` `@NotNull` | `POST /user/confirmOTP` request |
| `JWTDto` | `DTO/JWTDto.java` | `token: String`, `refreshToken: String`, `message: String` | `confirmOTP` response, `refresh` req+resp, `refresh-timer` request |
| `MessageDTO` | `DTO/MessageDTO.java` | `message: String`, `status: String` (only set on logout) | `signin`, `confirmOTP` errors, `logout` |
| `TimerDto` | `DTO/TimerDto.java` | `timer: OffsetDateTime` (ISO-8601) | `refresh-timer` response |
| `UserDTO` | `DTO/UserDTO.java` | `id: String`, `reference: String`, `email: String`, `emailVerified: boolean`, `role: UserRole`, `avatar: String \| null` | JWT claims carrier (never sent directly over HTTP; mapped from `UserModel` in `confirmOTP`/`refresh`) |
| `BlogDTO` | `DTO/BlogDTO.java` | `title: String`, `description: String`, `paragraph: String`, `headerImage: String`, `blogImages: List<String>` (default `[]`), `blogtype: String` | `POST /blog/create` request, `PUT /blog/{id}` request (all fields optional on update) |
| `BlogResponseDTO` | `DTO/BlogResponseDTO.java` | `id, title, description, paragraph, headerImage: String`, `blogImages: List<String>`, `blogtype: String`, `authorId, authorEmail, authorReference: String`, `dateCreated/dateUpdated: LocalDateTime` | `GET /blog/all`, `GET /blog/{id}`, `GET /blog/my-blogs` responses, `PUT /blog/{id}` response |
| `BlogImageDTO` | `DTO/BlogImageDTO.java` | `image: String` | Defined but **currently unused** by any resource (blog images are passed as `List<String>` inside `BlogDTO`) |

Data-point notes:
- `OTPDto.password` is `Integer`, not `String` — send JSON number (`123456`, no quotes). Leading-zero OTPs are impossible by construction (range floor 100000).
- `BlogDTO.blogImages` may be `[]`; loop handles empty list. `null` would NPE — always send at least `[]`.
- `UserDTO.avatar` is currently never set at signup (no avatar input), so JWT `avatar` claim is `null` until populated elsewhere.
- `UserDTO.role` is hardcoded to `ENDUSER` in both token-issue paths even if `UserModel.userRole` is `ADMIN`.

---

## 4. Persistence Models (data points)

| Model / Table | Key fields | Relations |
|---------------|------------|-----------|
| `BaseModel` (mapped superclass) | `id: String` (32-char UUID no dashes, `@PrePersist`), `dateCreated/dateUpdated: LocalDateTime` | inherited by all |
| `UserModel` / `USER_LIST` | `referenceId: String` (`USER...`), `email: String` unique+not-null, `emailVerified: boolean`, `avatar: String`, `userRole: enum ADMIN, ENDUSER` | `1-1 OTPModel (orphanRemoval)`, `1-1 RefreshToken`, `1-N BlogModel` |
| `OTPModel` / `OTP` | `referenceId (OTP...)`, `password: Integer`, `expiryTime: OffsetDateTime (+5m)`, `retries: Integer (0)`, `maxRetries: Integer (3)` | `N-1 UserModel (USER_ID)` |
| `RefreshToken` / `REFRESH_TOKEN` | `tokenHash: String (UUID)`, `expiresAt: OffsetDateTime (+TTL days)`, `revoked: boolean` | `1-1 UserModel (USER_ID)` |
| `BlogModel` / `BLOGS` | `title: String`, `description: @Lob String`, `paragraph: @Lob String`, `headerImage: @Lob String`, `blogtype: enum AI, MOBILE, WEB` | `N-1 UserModel`, `1-N BlogImagesModel` |
| `BlogImagesModel` / `BLOG_IMAGES` | `image: String` (URL/base64) | `N-1 BlogModel (BLOG_ID)` |

`services/PersistenceService.java` (app-scoped, transactional) exposes: `createUser, OTPExists, OTPAndEmailExistsAndExpired` (unused), `CheckUserAndOtp` (unused), `CheckUser(email)`, `updateUser, findUser(id), deleteUser, updateOTP, deleteOTP, findRefreshToken(hash)`, `createBlog, findBlog, updateBlog, findBlogsByUserId, findBlogTitle, findBlogTitleExcludingId, findAllBlogs, findAllBlogsPaged(offset,limit), countBlogs, findBlogsByType, searchBlogs, deleteBlog`.

---

## 5. Security & Config

- `@Secured` (`annotation/Secured.java`) is a `@NameBinding` marker; `JWTFilter` intercepts only annotated methods/classes.
- `JWTFilter`: requires `Authorization: Bearer <token>`; verifies issuer+signature+expiry via `JWTUtil.verify()`; sets `userId = JWT subject`; aborts `401` otherwise.
- MicroProfile Config keys (read via `ConfigProvider`): `jwt.secret.issuer`, `jwt.secret.key`, `jwt.secret.access.ttl` (minutes, `JWTUtil`), `jwt.secret.refresh.ttl` (days, `SignResource` + `RefreshToken.@PrePersist`), `mail.smtp.host/port/user/password` (`EmailUtil`), DB keys in `.env` (`DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASS`) + `PORT`.
- SMTP: SSL (`mail.smtp.ssl.enable=true`), sender `TYPICODE <mail.smtp.user>`, HTML content type. Send failures are swallowed/logged (`printStackTrace`), so `/signin` can return success even if mail failed.

## 6. Status-code quick map

| Code | Where |
|------|-------|
| 200 | signin (existing verified), confirmOTP success, logout, refresh success, refresh-timer success, blog list/get/update/delete success, profile get/update |
| 201 | signin (new user), blog create |
| 202 | signin (unverified existing) |
| 400 | logout (JWT subject not in DB), blog list/update with invalid `blogtype` |
| 401 | refresh (bad/expired/revoked token), any `@Secured` with bad JWT |
| 403 | blog update/delete by non-author (delete allows ADMIN override) |
| 404 | confirmOTP (bad user/OTP/expired/retries-exhausted — all share 404), refresh-timer (unknown token), blog create/update/delete (author/blog missing), profile (user missing) |
| 409 | signin (malformed email), blog create/update (duplicate title) |

All success/error entity shapes are either `MessageDTO`/`JWTDto`/`TimerDto` (JSON/XML) or bare `String` (notably `test-secure`, `refresh` 401, `refresh-timer` 404, all `blog/create` non-validation responses) — clients must handle both.
