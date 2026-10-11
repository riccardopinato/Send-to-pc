# Evidence Bundle — Invia al PC

Target: v1.26.0 / versionCode 32
Package: `com.riccardopinato.inviaalpc`

## Factory baseline

App-Factory-Core:
`fa3c0679de393f1cb13c2fed2be314d1a1269353`

## Source state

Feature branch PR #15 automated gate:
- PR: https://github.com/riccardopinato/Send-to-pc/pull/15
- head tested: `cc15d943433c59ebc26094ce7318c2b96661f3fc`
- Android checks run: `38184045834`
- result: PASS
- unit tests: PASS
- Android Lint: PASS
- Slim/R8 build: PASS
- unsigned release bundle gate: PASS
- internal APK signature check: PASS
- checksum step: PASS

Merged source:
- main commit: `10d6e44f5bdf6d4726709127c467d88388f1a1ab`
- post-merge Android checks run: `38184362992`
- result: PASS
- unit tests: PASS
- Android Lint: PASS
- Slim/R8 build: PASS
- unsigned release bundle gate: PASS
- internal APK signature verify: PASS
- checksum: PASS
- internal artifact: `Invia-al-PC-v1.26.0-internal`
- internal APK size: 6.3 MB
- internal APK SHA-256:
  `34144f189ce3906199084d68dc6d2af6452b9ed2848699285da5a338e0ae1f8b`
- uploaded artifact ID: `11718878010`
- uploaded artifact ZIP digest:
  `63a98110b541bd5b2fe26c34f5e217d70ee1df0a63b80381dc19cd92a9c0a964`

## Automated coverage

### v1.20 transport
- HTTPS-only session URL test;
- TLS identity implementation compiled/linted;
- localization parity 206/206 EN/IT/ES/FR/DE/PT.

### v1.21 reliability
- exact max-file boundary;
- file-over-limit rejection;
- final-chunk boundary;
- chunk-over-end rejection;
- arithmetic overflow rejection;
- 100-file batch accepted;
- 101-file batch rejected;
- upload ID shape;
- exact storage headroom;
- storage overflow rejection;
- pre-existing Range tests;
- pre-existing network recovery policy tests.

### v1.22 security
- Host header allow/reject tests;
- cross-site POST reject;
- same-origin POST allow;
- legacy Fetch Metadata compatibility;
- PIN lockout after ten failures;
- filename/header injection/token tests;
- duplicate HTTP headers rejected by parser;
- Transfer-Encoding rejected by parser.

## Production signing baseline

Last production-signing VERIFIED run:
- version: v1.16.0 / code 27
- workflow run: `38159474760`
- source SHA: `3240f107875372a01e7ba2e6577a73397ee72766`
- package: `com.riccardopinato.inviaalpc`
- certificate SHA-256:
  `248AE3BEDEC1CA6F80B2272F0ACDE5F010987864B4F5754FE2FA499353D77CE4`
- APK SHA-256:
  `639f471c29788ad80d5777fea9944633b603e69e792b05d8d175226341c8f552`
- AAB SHA-256:
  `5ae82d96d4a43a8aef6c5851a0d2c9613a6a445d4ef2c45d8207c265a4d846ab`

This proves the signing identity exists. It does not prove the v1.26 artifact until a v1.26 release workflow is executed.

## Store preparation evidence

Prepared:
- Privacy source;
- Data Safety engineering draft;
- AdMob readiness checklist;
- app-ads.txt template;
- Play Console pack;
- Store listing copy in six languages;
- Store asset specifications;
- Physical QA matrix;
- reliability/security audit.

## Required evidence still missing

NOT VERIFIED / BLOCKED:
- v1.26 signed release run;
- production AdMob IDs;
- app-ads.txt verified;
- AdMob readiness approved/known;
- public Privacy Policy URL;
- real Store screenshots/assets accepted;
- TLS/fingerprint physical browser validation;
- packet capture;
- large/batch/resume physical tests;
- AdMob/UMP physical tests;
- update-in-place;
- final Play Console forms;
- pre-launch report;
- production rollout.

## Current verdict

Engineering implementation through v1.26: IMPLEMENTED.
Automated CI: PASS on PR source.
Production signing identity: VERIFIED historically.
Current exact production artifact: NOT VERIFIED.
Physical certification: NOT VERIFIED.
Store Ready: BLOCKED.
Production Released: BLOCKED.
