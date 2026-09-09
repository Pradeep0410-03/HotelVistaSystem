# Frontend account integration

## What this phase adds

Login and registration use the Spring session API. The homepage shows the signed-in name and a Sign out button. A valid selected sample stay survives the trip through registration and login. Real room search and booking confirmation remain later phases.

## Run and try it

1. Follow `backend/README.md` to prepare PostgreSQL and set DB_URL, DB_USERNAME and DB_PASSWORD.
2. From `backend/`, run `mvn spring-boot:run -Dspring-boot.run.profiles=local`.
3. Open `http://localhost:8080/index.html`. Use this Spring address for accounts; `npm run dev` serves only a static preview.
4. Choose future dates and open a sample stay. Reserve leads to Login; switch to Register to create a customer account.
5. After registration, follow Sign in. Successful login returns to the validated stay selection. No reservation or payment is created.
6. The homepage displays your account name. Sign out invalidates the server session. Refreshing the page retrieves the current account again.

The static Sites preview is not updated or connected by this PR. Without a reachable JSON account API, account forms remain disabled and show an unavailable message. Use the local profile only for local HTTP development; public hosting must use HTTPS and secure cookies.

## How the pieces connect

- `js/auth-client.js` retrieves a fresh CSRF token before each POST. Registration sends JSON; login sends form-encoded credentials; login success is confirmed through `/api/auth/me`.
- `js/account.js` validates form input, shows progress and errors, prevents duplicate submissions, clears successful password input, and restores only validated trip fields. There is no arbitrary redirect URL.
- `js/session-nav.js` retrieves the account, renders the name with textContent, and handles logout. Account state is held only in memory.
- The browser manages the HttpOnly session cookie. No password, token or account is put in localStorage. Existing saved stay IDs remain local favourites, unrelated to authentication.
- Maven packages only the three public HTML pages, CSS, JavaScript and optimized images into Spring static resources. Backend source, database configuration and documentation are excluded.
- Spring permits public GET requests for those assets and retains CSRF and authorization on the API. No cross-origin configuration is needed.

## Verification

Run `node --test tests/*.test.cjs` from the repository root. API client tests cover encoding, current-user confirmation, fresh CSRF on logout, safe failure on static HTML fallback, and no automatic registration retries.

Run `mvn -Pintegration verify` from `backend/` against a disposable PostgreSQL database. Existing account integration tests exercise real registration, login and logout. FrontendIT checks public asset delivery and backend file protection. GitHub Actions runs both suites. These are automated request and logic checks; this phase does not claim browser visual testing.

## Explain it in your own words

The form sends details to Spring. Spring verifies or creates the database account and maintains a session. On later requests the browser sends the session cookie; the frontend asks the server who is signed in. Signing in restores the selected stay but does not book it.
