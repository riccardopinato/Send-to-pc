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

**PRODUCTION SIGNING VERIFIED — v1.16.0**

First production run verificato:

- workflow: `Android release`
- run: `38159474760`
- source SHA: `3240f107875372a01e7ba2e6577a73397ee72766`
- package: `com.riccardopinato.inviaalpc`
- versionName: `1.16.0`
- versionCode: `27`
- expected certificate SHA-256: `248AE3BEDEC1CA6F80B2272F0ACDE5F010987864B4F5754FE2FA499353D77CE4`
- actual certificate SHA-256: `248AE3BEDEC1CA6F80B2272F0ACDE5F010987864B4F5754FE2FA499353D77CE4`
- APK SHA-256: `639f471c29788ad80d5777fea9944633b603e69e792b05d8d175226341c8f552`
- AAB SHA-256: `5ae82d96d4a43a8aef6c5851a0d2c9613a6a445d4ef2c45d8207c265a4d846ab`

Il workflow ha completato con successo:
- secret gate;
- restore temporaneo della keystore;
- test/lint;
- APK e AAB release;
- verifica firma;
- confronto fail-closed del fingerprint;
- release evidence;
- rimozione della keystore temporanea.

Il gate signing è quindi chiuso. Restano separati i gate di physical QA, update-in-place e final certification.

## Same-artifact rule

L'APK fisicamente verificato deve essere lo stesso artifact, con lo stesso SHA-256, poi distribuito. Una rebuild produce un artifact diverso e richiede nuova evidence.

## Versione sviluppo corrente

- versionName: 1.19.0
- versionCode: 30
- package: com.riccardopinato.inviaalpc
- signing identity: invariata
- AdMob production IDs: PENDING

La v1.16.0 sopra resta l'ultima release con Production Signing VERIFIED finché non viene eseguito un nuovo workflow release per la versione corrente.

La v1.19.0 è una feature-freeze tecnica, non ancora una release Store Ready: restano i gate documentati in STORE-READINESS.md.
