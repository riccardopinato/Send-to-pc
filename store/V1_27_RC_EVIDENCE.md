# v1.27 Production RC evidence — Send to PC

Status: **RC GENERATED / NOT PHYSICALLY CERTIFIED**

Canonical Store Candidate:
- workflow: `Android release`
- workflow run: `38247324555`
- source SHA: `79efe53c229e919d220ddc070a93a254b500888f`
- package: `com.riccardopinato.inviaalpc`
- versionName: `1.27.0`
- versionCode: `33`
- store_candidate: `true`
- admob_production_configured: `true`

## Artifact identity

APK:
- artifact: `Send-to-PC-v1.27.0-apk`
- artifact id: `11746713719`
- SHA-256: `78dde1a90def7cfb594b377ae5906869d745c186363a9d5094a0e3d47972619f`

AAB:
- artifact: `Send-to-PC-v1.27.0-aab`
- artifact id: `11746343998`
- SHA-256: `e524c19033ce838f2cf4120f22f2ab7ae856510a3683503fd417decfb1265a09`

Evidence bundle:
- artifact: `Send-to-PC-v1.27.0-release-evidence`
- artifact id: `11746069004`

## Signing identity

Expected certificate SHA-256:
`248AE3BEDEC1CA6F80B2272F0ACDE5F010987864B4F5754FE2FA499353D77CE4`

Actual certificate SHA-256:
`248AE3BEDEC1CA6F80B2272F0ACDE5F010987864B4F5754FE2FA499353D77CE4`

Result: **MATCH / VERIFIED**

APK signature verification:
- v2: verified
- signers: 1
- key: RSA 4096

## AdMob identity evidence

Raw production IDs are not duplicated in this evidence file. The workflow records SHA-256 identities only.

- App ID SHA-256: `b2e54725ecf3e04707ed69b00a8234d2e6123a2b8bd74a705fddb16e144e2eb9`
- Banner ID SHA-256: `6e6ed238ee4b52771807e026562eda349c3945bd9741cdb271dec1cb93cdfd3e`
- Interstitial ID SHA-256: `aed35f7769b26367db3654e988ded60554e15d3e64cb6267677f52304a51d074`

These hashes match the three production AdMob IDs selected for Send to PC before the canonical release run.

## Same-artifact rule

Physical QA v1.28 must use exactly:

`Send-to-PC-v1.27.0.apk`

with SHA-256:

`78dde1a90def7cfb594b377ae5906869d745c186363a9d5094a0e3d47972619f`

A rebuild is a different candidate and requires new release evidence.

## Remaining gates

Not yet closed by this RC build:
- real-device/browser Physical QA;
- TLS trust/fingerprint browser flow on target browsers;
- UMP/banner/interstitial/no-fill physical validation;
- update-in-place from a previous signed release;
- developer website / privacy URL final reachability;
- app-ads.txt publisher line and AdMob verification;
- AdMob app readiness status;
- Play Console Data Safety / content rating / target audience;
- final Store screenshots/assets from the certified artifact;
- final certification verdict.

Current verdict: **BLOCKED for Store publication pending v1.28 and external Store gates.**
