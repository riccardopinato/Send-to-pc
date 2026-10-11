# External / Physical Release Gates — Invia al PC

Current engineering target: v1.26.0 / versionCode 32.
Package: `com.riccardopinato.inviaalpc`

The code roadmap is implemented through Store preparation. The remaining gates cannot be truthfully marked PASS from repository CI.

## Gate A — AdMob production identity

Required external values:
- `ADMOB_APP_ID`
- `ADMOB_BANNER_ID`
- `ADMOB_INTERSTITIAL_ID`

They must be real production IDs. The release workflow rejects:
- missing values;
- malformed values;
- Google demo IDs.

After configuring them in GitHub repository Actions secrets, the Store Candidate workflow can be executed with `store_candidate=true`.

## Gate B — Public developer website / Privacy Policy

Prepared source:
- `docs/index.html`
- `docs/privacy.html`

Required:
1. publish the docs directory on a stable HTTPS URL;
2. verify the Privacy Policy opens without authentication;
3. put the public developer website/privacy URL into Google Play as required;
4. only then record the final URL in release evidence.

GitHub Pages is one possible host, but the repository does not claim it is enabled until the URL is actually reachable.

## Gate C — app-ads.txt / AdMob readiness

Prepared:
- `store/app-ads.txt.example`
- `store/ADMOB_READINESS.md`

Required:
1. copy the exact publisher line from AdMob;
2. publish it as `/app-ads.txt` on the developer domain;
3. link the developer website from the Store listing;
4. wait for AdMob verification;
5. record app readiness/review status.

Do not publish a placeholder publisher ID.

## Gate D — Signed current-version release

Signing secrets are already established from the verified v1.16 signing identity.

Required GitHub action:
`Actions -> Android release -> Run workflow`

For QA before production ads:
- `store_candidate=false`

For final Store candidate after Gate A:
- `store_candidate=true`

Expected evidence:
- APK + AAB;
- source SHA;
- workflow run;
- versionName/versionCode;
- expected/actual signer SHA-256 match;
- APK/AAB SHA-256;
- `admob_production_configured=true` for Store candidate.

Running the workflow does not publish the app to Google Play.

## Gate E — Physical QA

Use `PHYSICAL-QA.md`.

Must include the exact signed candidate:
- TLS handshake and browser warning;
- fingerprint equality;
- phone -> PC;
- PC -> phone;
- text/link;
- >1 GiB;
- 100-file batch;
- resume;
- background/screen-off;
- network interruption;
- trusted PCs;
- EN/IT/ES/FR/DE/PT;
- UMP/AdMob production behavior;
- packet capture/no plaintext payload.

## Gate F — Update-in-place

Install previous release signed with the permanent identity, then install the current candidate without uninstalling.

Record:
- previous version/code;
- new version/code;
- signer fingerprint;
- result PASS/FAIL;
- data/history continuity.

## Gate G — Play Console

Prepared:
- `store/PLAY_CONSOLE.md`
- `store/PLAY_LISTING.md`
- `store/ASSET_SPECS.md`
- `store/DATA_SAFETY.md`
- `store/STORE_RELEASE_CHECKLIST.md`

Still external:
- create/configure Play app if not already done;
- content rating;
- target audience;
- ads declaration;
- Data Safety submission;
- support contact;
- upload final AAB;
- testing track;
- pre-launch report;
- production rollout.

## Completion semantics

Only after all applicable gates:
- CERTIFIED
- STORE READY
- PRODUCTION RELEASED

Until then use:
- NOT VERIFIED for physical/external checks not performed;
- BLOCKED when required external configuration is missing.
