# Origin Energy URL Shortener API — Test Report (v2)

**Date:** 2026-03-26
**Repo:** https://github.com/shaunandrewjackson1977/origin-energy
**Cloned to:** `/tmp/origin-energy-fresh`
**Java version:** OpenJDK 21.0.6 (Temurin)
**Reviewer:** Independent / public user (no repo write access)

---

## Repository Overview

The repository is publicly accessible with no authentication required. It contains a Kotlin + Spring Boot URL Shortener REST API.

**Git history (3 commits):**

| Commit | Message |
|--------|---------|
| `f5e2650` | Initial commit |
| `bae8ef1` | Fixes up the naming of the endpoints and removes some that are not necessary. Specifically changes the urls to use nouns as opposed to verbs. |
| `605b228` | Suppresses stack traces in 400, 404 api responses. |

---

## Step 1: Running Tests — SUCCESS

**Command:** `./gradlew test`
**Result:** `BUILD SUCCESSFUL in 6s`

| Test Suite | Tests | Failures | Errors | Skipped |
|---|---|---|---|---|
| `ShortenUrlServiceTest` | 5 | 0 | 0 | 0 |
| `Base62EncoderTest` | 2 | 0 | 0 | 0 |
| `ShortenUrlControllerTest$UrlShorteningTests` | 3 | 0 | 0 | 0 |
| `ShortenUrlControllerTest$RedirectShortUrlTests` | 3 | 0 | 0 | 0 |
| `ShortenUrlControllerTest$GetUrlInfoTests` | 3 | 0 | 0 | 0 |
| `ShortenUrlControllerComponentTest$UrlShorteningTests` | 1 | 0 | 0 | 0 |
| `ShortenUrlControllerComponentTest$RedirectShortUrlTests` | 1 | 0 | 0 | 0 |
| `ShortenUrlControllerComponentTest$GetUrlInfoTests` | 1 | 0 | 0 | 0 |
| `UrlShortnerApiApplicationTests` | 1 | 0 | 0 | 0 |
| **Total** | **20** | **0** | **0** | **0** |

---

## Step 2: Running App Locally — SUCCESS

**Command:** `./gradlew bootRun`

The application started successfully and responded on `http://localhost:8080`.

---

## Step 3: Shorten a URL + Idempotency — SUCCESS

**First call:**
```
POST /api/v1/urls
{"url": "https://www.originenergy.com.au/electricity-gas/plans.html"}

HTTP/1.1 200
{"slug":"1EZ4WAS","url":"http://short.ly/1EZ4WAS"}
```

**Second call (same URL):**
```
POST /api/v1/urls
{"url": "https://www.originenergy.com.au/electricity-gas/plans.html"}

HTTP/1.1 200
{"slug":"1EZ4WAS","url":"http://short.ly/1EZ4WAS"}
```

Idempotency confirmed — identical slug `1EZ4WAS` returned on both calls.

---

## Step 4: Redirect Endpoint — SUCCESS

```
GET /1EZ4WAS

HTTP/1.1 302
Location: https://www.originenergy.com.au/electricity-gas/plans.html
Content-Length: 0
```

Returned `302 Found` with the correct `Location` header.

---

## Step 5: Get URL Info Endpoint — SUCCESS

```
GET /api/v1/urls/1EZ4WAS

HTTP/1.1 200
{
  "shortenedUrl": "http://short.ly/1EZ4WAS",
  "originalUrl": "https://www.originenergy.com.au/electricity-gas/plans.html",
  "createdAt": "2026-03-25T21:54:40.874960Z"
}
```

---

## Step 6: Error Responses — ALL REPRODUCED SUCCESSFULLY

| Scenario | Request | Status | Response body |
|---|---|---|---|
| Invalid URL | `POST /api/v1/urls {"url":"not-a-valid-url"}` | 400 | `{"timestamp":"...","status":400,"error":"Bad Request","path":"/api/v1/urls"}` |
| Missing URL field | `POST /api/v1/urls {}` | 400 | `{"timestamp":"...","status":400,"error":"Bad Request","path":"/api/v1/urls"}` |
| Missing body | `POST /api/v1/urls` (empty) | 400 | `{"timestamp":"...","status":400,"error":"Bad Request","path":"/api/v1/urls"}` |
| Blank slug (redirect) | `GET /%20%20%20%20%20%20%20` | 400 | `{"timestamp":"...","status":400,"error":"Bad Request","path":"/..."}` |
| Blank slug (info) | `GET /api/v1/urls/%20%20%20%20%20%20%20` | 400 | `{"timestamp":"...","status":400,"error":"Bad Request","path":"/..."}` |
| Unknown slug (redirect) | `GET /unknownSlug999` | 404 | `{"timestamp":"...","status":404,"error":"Not Found","path":"/unknownSlug999"}` |
| Unknown slug (info) | `GET /api/v1/urls/unknownSlug999` | 404 | `{"timestamp":"...","status":404,"error":"Not Found","path":"/..."}` |

---

## Stack Trace Issue — RESOLVED

The v1 report (2026-03-25) identified that all `400` and `404` error responses included the full Java stack trace in a `trace` field, along with an internal `message` field. Example of the previous response:

```json
{
    "timestamp": "2026-03-25T21:18:51.660Z",
    "status": 400,
    "error": "Bad Request",
    "trace": "org.springframework.web.bind.MethodArgumentNotValidException: Validation failed for argument [0] ...\n\tat org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor.resolveArgument(RequestResponseBodyMethodProcessor.java:165)\n\t... (40+ more frames)",
    "message": "Validation failed for object='shortenUrlRequest'. Error count: 1",
    "errors": [ ... ],
    "path": "/api/v1/urls"
}
```

This has been fixed in commit `605b228` by adding the following to `application.yaml`:

```yaml
spring:
  devtools:
    add-properties: false

server:
  error:
    include-stacktrace: never
    include-message: never
```

**Note:** Both properties were required. `spring-boot-devtools` (declared in `build.gradle.kts`) injects `include-stacktrace: always` as a development default, overriding `application.yaml` unless `spring.devtools.add-properties: false` is also set.

All error responses now return only `timestamp`, `status`, `error`, and `path`:

```json
{
    "timestamp": "2026-03-25T21:55:53.783Z",
    "status": 400,
    "error": "Bad Request",
    "path": "/api/v1/urls"
}
```

---

## Summary

| Step | Result | Notes |
|---|---|---|
| 1. Clone repo (public) | SUCCESS | Publicly accessible, no auth required |
| 2. Run tests | SUCCESS | 20/20 passed, 0 failures |
| 3. Run locally | SUCCESS | App starts on port 8080 |
| 4. Shorten URL + Idempotency | SUCCESS | Consistent slug returned for same URL |
| 5. Redirect endpoint | SUCCESS | `GET /{slug}` → `302 Found` with correct `Location` |
| 6. Get URL info endpoint | SUCCESS | `GET /api/v1/urls/{slug}` → full metadata |
| 7. Error responses | SUCCESS | All expected 400/404 codes reproduced |
| 8. Stack trace exposure | **RESOLVED** | `trace` and `message` fields no longer present in error responses |
