# Reliability & Security Audit — v1.26 preparation

## Scope

Automated/code audit after v1.20 encrypted LAN transport.

Evidence semantics:
- PASS = automated evidence exists;
- NOT VERIFIED = needs physical/runtime evidence;
- BLOCKED = external dependency prevents closure.

## Reliability

### File limits
PASS
- single PC -> phone file limit: 8 GiB;
- selected/batch file count: 100;
- batch aggregate validation centralized.

### Chunk arithmetic
PASS
- offset/content-length arithmetic no longer uses an overflow-prone addition;
- final chunk exactly at EOF accepted;
- chunk beyond EOF rejected;
- Long overflow regression covered by unit tests.

### Storage headroom
PASS
- 100 MiB safety headroom;
- overflow-safe capacity computation;
- exact threshold covered by tests.

### Resume
IMPLEMENTED / PHYSICAL NOT VERIFIED
- upload metadata persisted;
- server reports received offset;
- offset mismatch returns current received offset;
- hash mismatch deletes invalid partial;
- stale partial metadata cleanup;
- orphan pending MediaStore cleanup.

### Download Range
PASS automated parser tests / PHYSICAL NOT VERIFIED
- closed ranges;
- open ranges;
- suffix ranges;
- end clamping;
- malformed/out-of-bounds reject.

### Network recovery
PASS automated policy / PHYSICAL NOT VERIFIED
- same IP stays active;
- temporary loss inside 12 s grace;
- prolonged loss terminates;
- changed IP classified after grace.

### Background
IMPLEMENTED / PHYSICAL NOT VERIFIED
- foreground service;
- partial wake lock;
- network callback;
- session health loop;
- Android timeout classification.

## Security

### Transport
IMPLEMENTED / PHYSICAL NOT VERIFIED
- TLS 1.2/1.3;
- persistent Android Keystore identity;
- HTTPS-only session URL;
- certificate SHA-256 fingerprint;
- Secure + HttpOnly + SameSite=Strict cookies.

### Request ambiguity
PASS by implementation
- duplicate header names rejected;
- Transfer-Encoding requests rejected;
- only HTTP/1.0 and HTTP/1.1 request lines accepted;
- only GET/POST accepted;
- header block capped.

### Host / cross-site
PASS automated policy tests
- Host must match session LAN IP:port;
- cross-site POST via Sec-Fetch-Site rejected;
- same-origin and legacy/no-Fetch-Metadata clients allowed.

### PIN
PASS automated lockout test
- high-entropy session token;
- 4-digit PIN;
- constant-time comparison;
- lockout after 10 failures;
- tracked client state bounded to prevent unbounded memory growth.

### Trusted PCs
IMPLEMENTED / PHYSICAL NOT VERIFIED
- raw trusted token not persisted on phone;
- SHA-256 token hash stored;
- token rotation on reconnect;
- 30-day expiry;
- individual/all revocation.

### Input/content
PASS by implementation / targeted unit tests
- filename sanitation;
- CR/LF header injection checks;
- URL scheme allowlist http/https for user-shared links;
- userinfo in links rejected;
- HTML escaping;
- CSP;
- X-Frame-Options DENY;
- no-referrer;
- nosniff;
- upload size/concurrency caps.

## Residual risks / mandatory physical gates

NOT VERIFIED:
- Android Keystore TLS handshake on real devices/OEMs;
- browser warning bypass UX and fingerprint comparison;
- packet capture proving no payload plaintext;
- >1 GiB transfer;
- 100-file transfer;
- resume after real network interruption;
- screen-off/background behavior;
- trusted-PC persistence/rotation in browser;
- AdMob/UMP real configuration;
- exact-artifact update-in-place.

## Verdict

Automated/code readiness: PASS for v1.21/v1.22 scope.
Physical certification: NOT VERIFIED.
Store Ready: BLOCKED by external + physical gates.
