# Hotel Vista authentication: backend phase

The backend now supports customer registration, email/password login, current-user lookup and logout. The frontend forms now call these APIs when served by Spring; see [frontend integration](frontend-auth-integration.md). The separately hosted static preview has no deployed backend. This change does not reserve rooms or deploy the backend.

## Endpoints

| Method and path | Input / result | Access |
|---|---|---|
| GET /api/auth/csrf | JSON `{headerName, token}`; establishes a pre-login session | Public |
| POST /api/auth/register | JSON `{fullName, email, password}`; 201 account summary | Public, valid CSRF token required |
| POST /api/auth/login | Form-encoded `email` and `password`; 204 on success | Public, valid CSRF token required |
| GET /api/auth/me | JSON `{id, fullName, email, role}` | Signed-in session required |
| POST /api/auth/logout | 204 and invalidated session | Valid CSRF token required |

Never send credentials in URLs. Login is `application/x-www-form-urlencoded` because Spring Security's standard username/password filter owns that endpoint. Registration uses JSON. There is no JWT or localStorage authentication flag. Keep the session cookie and send the CSRF header name/value returned by the server.

## Browser sequence

1. Fetch `/api/auth/csrf` from the same origin, preserving cookies. Retain its token in memory.
2. To register, send the JSON fields and the returned header. Trim name/email; passwords are exact input. Email is stored lowercase. Registration creates a CUSTOMER and returns a summary; it does not log the user in automatically.
3. To log in, post URL-encoded email/password with the CSRF header and session cookie. On success the response is 204, not JSON and not a redirect. The session ID changes to prevent fixation.
4. Fetch `/api/auth/me` to confirm the signed-in account. Before the next write, fetch `/api/auth/csrf` again because login clears the previous token.
5. Continue to the retained sample stay in the frontend; never auto-confirm a reservation just because login succeeded.
6. Post logout with the current token, discard the displayed account, and fetch a new token before a later signup/login. Logout clears authentication, invalidates the session and deletes JSESSIONID.

CSRF is also required before signup/login, not only after authentication. A token is tied to its session. Do not disable CSRF to work around a missing cookie or stale token.

## Validation and storage

- Name: nonblank, at most 100 characters after trimming.
- Email: nonblank, valid email shape, at most 254 characters after trimming; lowercase canonical identity. Email ownership verification is not implemented.
- Password: nonblank, 8-128 characters. Do not trim it. Password reset is not implemented.
- PasswordEncoder uses Spring's PBKDF2 v5.8 defaults with random salt and a stored algorithm prefix. The users table stores only the hash. This choice supports the permitted password length without bcrypt's 72-byte truncation concern.
- A duplicate email returns 409; the unique index also rejects a racing duplicate insert. Invalid registration returns 400 without returning submitted values.
- Wrong-password and unknown-account login both return 401 with the same generic message. Passwords, hashes and session IDs are not returned by account summaries.
- Client-supplied role fields cannot grant privileges. The Account constructor sets CUSTOMER. There is no public endpoint to create admins or change roles.

## Sessions and permissions

Production-default cookies are HttpOnly, Secure and SameSite=Lax, with cookie-only tracking and a 30-minute inactivity timeout. Use `local` only for HTTP development; that profile disables the Secure flag. Never use it on a public host.

Sessions live in the application server's memory. Restarting it logs users out. Multiple backend instances would need shared sessions or a deliberate routing strategy, to be designed before scaling. Roles are captured at login; future role-management features must invalidate affected sessions.

Catalogue reads remain public. `/api/auth/me` requires login. `/api/admin/**` requires ADMIN; there are no admin feature endpoints yet. Everything else is denied by default. Future protected routes must be added deliberately, not by making all requests public. The test-only admin probe is not shipped in the main application.

There is no permissive CORS configuration. Connect the frontend and backend under the same origin, or explicitly design restricted CORS/cookie handling later. Registration/login abuse controls, email verification, recovery, external hosting and TLS must be addressed before a public account launch. These APIs are not a claim of production readiness.

## Files to read in order

1. `auth/RegisterRequest.java`: request validation and normalization.
2. `auth/Account.java`, `AccountRepository.java`: mapping to the existing users table.
3. `auth/AccountService.java`: hashing and registration; response has no secret fields.
4. `auth/AccountDetailsService.java`: loads the stored hash and role for Spring's authentication provider.
5. `config/SecurityConfig.java`: public/protected routes, login/logout handlers, CSRF and session behavior.
6. `auth/AuthController.java`: registration, CSRF token retrieval and current-user summary. Login/logout use framework filters.

Paths above are relative to `backend/src/main/java/com/hotelvista/`. No database migration is needed: V1 already contains the account fields and unique email index.

## Tests and local setup

Follow `backend/README.md` for Java/PostgreSQL setup. For plain HTTP local browser testing run:

```sh
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Run `mvn test` for controller, security-filter and account-service tests. Run `mvn -Pintegration verify` against a disposable PostgreSQL database for the real registration/login/me/logout path. AuthApiIT commits its signup as a real request would, then deletes only its unique test user during cleanup. Do not run integration tests against live data.

The security tests use real password verification and real Spring filters with fixture users, and retrieve actual CSRF tokens from the endpoint. They check token renewal after login, session ID rotation, logout invalidation, anonymous rejection, invalid registration and customer/admin boundaries. AccountServiceTest verifies that the saved password is hashed. AuthApiIT verifies hashing and account persistence against PostgreSQL, duplicate registration, attempted role injection and login using normalized email.

Official references: [form login](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/form.html), [CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html), [password storage](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html).
