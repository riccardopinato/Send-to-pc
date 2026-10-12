# Release engineering

## Lane INTERNAL

Build type: `slim`

Scopo:
- sviluppo;
- QA;
- installazione manuale interna.

Caratteristiche:
- R8;
- resource shrinking;
- APK leggero;
- firma debug.

Una build INTERNAL non è una release production con identità di firma permanente.

## Lane RELEASE

Workflow:
`.github/workflows/android-release.yml`

Richiede i GitHub Secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

La keystore non deve essere committata.

AdMob production può essere configurato con tre secret aggiuntivi:

- `ADMOB_APP_ID`
- `ADMOB_BANNER_ID`
- `ADMOB_INTERSTITIAL_ID`

Questi identificatori non sono credenziali private, ma vengono tenuti fuori dal codice per separare chiaramente test traffic e production traffic.

Se i tre valori non sono presenti, la release resta buildabile usando gli ID demo ufficiali Google e `ADMOB_PRODUCTION_CONFIGURED=false`. Tale artifact NON è la release monetizzata destinata allo Store.

Output:
- APK release firmato;
- AAB;
- fingerprint certificato;
- SHA256SUMS;
- RELEASE_IDENTITY con source SHA e workflow run.

## Identità di firma permanente

È stata generata una keystore persistente dedicata al progetto.

Fingerprint SHA-256 pubblico del certificato previsto:

`248AE3BEDEC1CA6F80B2272F0ACDE5F010987864B4F5754FE2FA499353D77CE4`

La pipeline release rifiuta automaticamente un APK firmato con un certificato diverso da questo fingerprint.

La chiave privata non deve essere committata nel repository. Va conservata in backup sicuri e caricata in CI esclusivamente tramite GitHub Secrets.

Per Google Play questa chiave viene usata come **upload key**. Se Play App Signing è attivo, Google conserva e usa separatamente la Play app signing key per firmare gli APK distribuiti dallo Store. Per distribuzione APK diretta, invece, questa stessa chiave deve rimanere stabile per mantenere la continuità degli aggiornamenti.

## Stato attuale

**STORE CANDIDATE GENERATED / PRODUCTION SIGNING VERIFIED — v1.27.0**

Canonical v1.27 Store Candidate:

- workflow: `Android release`
- run: `38247324555`
- source SHA: `79efe53c229e919d220ddc070a93a254b500888f`
- package: `com.riccardopinato.inviaalpc`
- versionName: `1.27.0`
- versionCode: `33`
- store_candidate: `true`
- admob_production_configured: `true`
- expected certificate SHA-256: `248AE3BEDEC1CA6F80B2272F0ACDE5F010987864B4F5754FE2FA499353D77CE4`
- actual certificate SHA-256: `248AE3BEDEC1CA6F80B2272F0ACDE5F010987864B4F5754FE2FA499353D77CE4`
- APK SHA-256: `78dde1a90def7cfb594b377ae5906869d745c186363a9d5094a0e3d47972619f`
- AAB SHA-256: `e524c19033ce838f2cf4120f22f2ab7ae856510a3683503fd417decfb1265a09`

The workflow completed the production AdMob gate, persistent signing gate, test/lint, signed APK/AAB build, signing verification and release evidence upload.

Historical first production-signing run: v1.16.0 / run `38159474760`.

The signing gate is closed. Physical QA, update-in-place, app-ads.txt/readiness and final certification remain separate gates.

## v1.27.1 startup hotfix

The v1.27.0 Store Candidate failed Physical QA because the production app crashed immediately on launch.

New candidate:
- workflow: `Android release`
- run: `38254139820`
- source SHA: `46748a9a2acadb27d7dd2650ef7b1b40b22025ee`
- package: `com.riccardopinato.inviaalpc`
- versionName: `1.27.1`
- versionCode: `34`
- store_candidate: `true`
- admob_production_configured: `true`
- expected certificate SHA-256: `248AE3BEDEC1CA6F80B2272F0ACDE5F010987864B4F5754FE2FA499353D77CE4`
- actual certificate SHA-256: `248AE3BEDEC1CA6F80B2272F0ACDE5F010987864B4F5754FE2FA499353D77CE4`
- APK SHA-256: `406b671396ac329e2035d7cd3b5931b09725d9a467cb4050e562c5256d3a5512`
- AAB SHA-256: `9553344d98937881b5c1c6ae0fd8f9e9bffe7dcc374a23b7fdbbb333436992dd`

The package and signing identity are unchanged, so v1.27.1 is the update-in-place candidate for an installed v1.27.0 production APK.

## Same-artifact rule

L'APK fisicamente verificato deve essere lo stesso artifact, con lo stesso SHA-256, poi distribuito. Una rebuild produce un artifact diverso e richiede nuova evidence.

## Versione sviluppo corrente

- versionName: 1.27.1
- versionCode: 34
- package: com.riccardopinato.inviaalpc
- signing identity: invariata
- AdMob production IDs: CONFIGURED / STORE CANDIDATE VERIFIED

La v1.27.0 è il Production RC canonico. L'esatto APK candidato è definito in `store/V1_27_RC_EVIDENCE.md`.


## Store candidate gate

Il workflow `Android release` accetta l'input `store_candidate`.

- `store_candidate=false`: release firmata per QA/evidence; se gli ID AdMob production non sono presenti usa gli ID di test ufficiali.
- `store_candidate=true`: la pipeline fallisce se manca anche uno solo tra `ADMOB_APP_ID`, `ADMOB_BANNER_ID` e `ADMOB_INTERSTITIAL_ID`.

Il nome degli artifact e RELEASE_IDENTITY vengono derivati dinamicamente da `versionName` e `versionCode`, evitando mismatch tra Gradle e workflow.

Un run Store Candidate verde non sostituisce Physical QA, app-ads.txt/readiness o Final Certification.

La v1.27 Production RC è stata generata dal run canonico `38247324555`. Physical QA e final certification restano separati.
